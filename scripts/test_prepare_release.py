import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

from prepare_release import prepare


class ReleasePackagingTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.output = self.root / "dist"
        (self.root / "gradle.properties").write_text(
            "mod_version=0.3.2\nminecraft_version=26.3\nloader_version=0.19.5\n"
            "fabric_api_version=0.162.0+26.3\narchives_base_name=kompass4mc\n")
        self.jar = self.root / "build/libs/kompass4mc-0.3.2.jar"
        self.jar.parent.mkdir(parents=True)
        self.write_jar()
        assets = ["build/libs/kompass4mc-0.3.2-sources.jar", "scripts/install-simple-mod-sync.ps1",
                  "scripts/install-simple-mod-sync.command", "docs/github-releases.md",
                  "output/pdf/Kompass_Abenteuer_Eltern_Einrichtung.pdf",
                  "output/pdf/Kompass_Abenteuer_Kinder_Spielanleitung.pdf"]
        for name in assets:
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b"fixture")
        self.lock = self.root / "distribution/fabric-api.json"
        self.lock.parent.mkdir()
        self.dependency = {"version": "0.162.0+26.3", "minecraft_version": "26.3",
                           "url": "https://cdn.modrinth.com/data/api.jar", "sha512": "a" * 128}
        self.write_lock()

    def write_jar(self, version="0.3.2", minecraft="26.3", mod_id="kompass4mc"):
        with zipfile.ZipFile(self.jar, "w") as jar:
            jar.writestr("fabric.mod.json", json.dumps(
                {"id": mod_id, "version": version, "depends": {"minecraft": minecraft}}))

    def write_lock(self):
        self.lock.write_text(json.dumps(self.dependency))

    def package(self, tag="v0.3.2", repository="larsjochenthoms/MCKompass"):
        return prepare(self.root, tag, repository, self.output)

    def test_manifest_pins_the_matching_jar_and_compatible_fabric_api(self):
        self.package()
        manifest = json.loads((self.output / "simple-mod-sync.json").read_text())
        self.assertEqual(3, manifest["sync_version"])
        self.assertEqual("26.3", manifest["minecraft_version"])
        mod, dependency = manifest["sync"]
        self.assertEqual("kompass4mc", mod["name"])
        self.assertEqual("fabric-api", dependency["name"])
        self.assertEqual("https://github.com/larsjochenthoms/MCKompass/releases/download/"
                         "v0.3.2/kompass4mc-0.3.2.jar", mod["url"])
        self.assertEqual(hashlib.sha512(self.jar.read_bytes()).hexdigest(), mod["sha512"])
        self.assertEqual(self.dependency["sha512"], dependency["sha512"])

    def test_checksums_cover_every_release_file_except_the_checksum_list(self):
        self.package()
        lines = (self.output / "SHA256SUMS").read_text().splitlines()
        covered = set()
        for line in lines:
            checksum, filename = line.split("  ", 1)
            covered.add(filename)
            self.assertEqual(hashlib.sha256((self.output / filename).read_bytes()).hexdigest(), checksum)
        self.assertEqual({path.name for path in self.output.iterdir()} - {"SHA256SUMS"}, covered)

    def test_existing_release_output_is_never_overwritten(self):
        self.package()
        with self.assertRaises(ValueError):
            self.package()

    def test_mismatched_or_prerelease_tags_are_rejected_without_output(self):
        for tag in ("v0.3.1", "v0.3.2-rc.1", "0.3.2", "v0.3.2\n"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                self.package(tag=tag)
        self.assertFalse(self.output.exists())

    def test_jar_metadata_must_match_the_release(self):
        for values in (("0.3.1", "26.3", "kompass4mc"), ("0.3.2", "26.2", "kompass4mc"),
                       ("0.3.2", "26.3", "other")):
            self.write_jar(*values)
            with self.subTest(values=values), self.assertRaises(ValueError):
                self.package()
        self.assertFalse(self.output.exists())

    def test_incompatible_dependency_or_missing_hash_is_rejected(self):
        for key, value in (("version", "old"), ("minecraft_version", "26.2"), ("sha512", "bad"),
                           ("url", "http://cdn.modrinth.com/api.jar"),
                           ("url", "https://another.example/api.jar")):
            original = self.dependency[key]
            self.dependency[key] = value
            self.write_lock()
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                self.package()
            self.dependency[key] = original
        self.assertFalse(self.output.exists())

    def test_missing_installer_prevents_any_release_output(self):
        (self.root / "scripts/install-simple-mod-sync.ps1").unlink()
        with self.assertRaises(ValueError):
            self.package()
        self.assertFalse(self.output.exists())

    def test_repository_cannot_inject_a_url_path_or_shell_text(self):
        for repo in ("../bad", "owner/repo/extra", "owner/repo?x=y", "owner/$(uname)"):
            with self.subTest(repo=repo), self.assertRaises(ValueError):
                self.package(repository=repo)
        self.assertFalse(self.output.exists())


if __name__ == "__main__":
    unittest.main()
