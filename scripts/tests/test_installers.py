"""Exercise the native installers with local fixtures, never a real Minecraft folder.

Only the test copy replaces HTTPS transport and the process guard; hash checking,
JAR metadata inspection, configuration merging, backups and installation remain real.
macOS runs the Bash installer; Windows runs PowerShell (5.1 or 7).
"""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile

SCRIPTS = Path(__file__).resolve().parents[1]
SMS_HASH = "87d3b8abba706aab9bf942df683b631ae0060c3f034a0c7d7d59231e1717384032745d32f042d0f02436d2223c3176e21bed279d4171ccaf816a263537e273d4"
MANIFEST_URL = "https://fixture.invalid/manifest.json"
SMS_URL = "https://cdn.modrinth.com/data/8IFHiuMI/versions/AVuvo48S/simplemodsync-fabric-26.3-1.4.1.jar"


def jar(path, mod_id):
    with zipfile.ZipFile(path, "w") as archive:
        archive.writestr("fabric.mod.json", json.dumps({"id": mod_id, "version": "test"}))
    return hashlib.sha512(path.read_bytes()).hexdigest()


class InstallerTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if sys.platform == "darwin":
            cls.kind = "mac"
        elif sys.platform == "win32":
            cls.kind = "windows"
        else:
            raise unittest.SkipTest("Native installer fixture tests run on macOS and Windows.")

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="kompass-installer-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.game = self.root / "Game with spaces"
        (self.game / "mods").mkdir(parents=True)
        (self.game / "config").mkdir()
        (self.game / "saves" / "Family world").mkdir(parents=True)
        (self.game / "saves" / "Family world" / "level.dat").write_bytes(b"world stays unchanged")
        self.fixture = self.root / "fixtures"
        self.fixture.mkdir()
        entries = []
        self.urls = {MANIFEST_URL: str(self.fixture / "manifest.json")}
        for mod_id, version in (("kompass4mc", "0.3.2"), ("fabric-api", "0.162.0+26.3")):
            path = self.fixture / (mod_id + ".jar")
            checksum = jar(path, mod_id)
            url = "https://fixture.invalid/" + path.name
            self.urls[url] = str(path)
            entries.append({"name": mod_id, "version": version, "type": "mod", "url": url, "sha512": checksum})
        self.manifest = {"sync_version": 3, "minecraft_version": "26.3", "sync": entries}
        sms = self.fixture / "sms.jar"
        self.sms_hash = jar(sms, "simple-mod-sync")
        self.urls[SMS_URL] = str(sms)
        self.config_path = self.game / "config" / "simplemodsync.json"
        self.original_other = self.game / "mods" / "other-mod.jar"
        jar(self.original_other, "unrelated")
        # Include a renamed managed JAR and multiple old versions.
        jar(self.game / "mods" / "renamed-learning-mod.jar", "kompass4mc")
        jar(self.game / "mods" / "kompass4mc-0.1.0.jar", "kompass4mc")
        jar(self.game / "mods" / "fabric-api-old.jar", "fabric-api")
        self.original = self.snapshot()

    def snapshot(self):
        return {str(path.relative_to(self.game)): path.read_bytes()
                for path in self.game.rglob("*") if path.is_file()
                and ".kompass-backups" not in path.parts}

    def run_installer(self, prepare=False):
        (self.fixture / "manifest.json").write_text(json.dumps(self.manifest), encoding="utf-8")
        if self.kind == "mac":
            script = (SCRIPTS / "install-simple-mod-sync.command").read_text()
            script = re.sub(r"assert_game_closed\(\) \{.*?\n\}\n", "assert_game_closed() { :; }\n", script, count=1, flags=re.S)
            mapping = "\n".join("        " + json.dumps(url) + ") /bin/cp " + json.dumps(path) + ' "$2" ;;'
                                for url, path in self.urls.items())
            replacement = 'download() {\n    case "$1" in\n' + mapping + '\n        *) return 41 ;;\n    esac\n}\n'
            script = re.sub(r"download\(\) \{.*?\n\}\n", lambda _: replacement, script, count=1, flags=re.S)
            path = self.root / "installer.command"
            path.write_text(script.replace(SMS_HASH, self.sms_hash), encoding="utf-8")
            args = ["/bin/bash", str(path), str(self.game), "--manifest-url", MANIFEST_URL]
            if prepare:
                args.append("--prepare-only")
        else:
            script = (SCRIPTS / "install-simple-mod-sync.ps1").read_text()
            mapping = "; ".join("'" + url + "'='" + path.replace("'", "''") + "'" for url, path in self.urls.items())
            overrides = ("function Assert-GameClosed {}\n"
                         "function Receive-HttpsFile([string]$Url,[string]$Output) {\n"
                         " $files = @{" + mapping + "}\n"
                         " if (-not $files.ContainsKey($Url)) { throw 'Unknown fixture URL' }\n"
                         " [IO.File]::Copy($files[$Url],$Output)\n}\n")
            script = script.replace("if (-not $GameDirectory) {", overrides + "\nif (-not $GameDirectory) {", 1)
            path = self.root / "installer.ps1"
            path.write_text(script.replace(SMS_HASH, self.sms_hash), encoding="utf-8-sig")
            powershell = shutil.which("pwsh") or shutil.which("powershell")
            self.assertIsNotNone(powershell, "PowerShell must be installed on the Windows runner")
            args = [powershell, "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", str(path), "-GameDirectory", str(self.game), "-ManifestUrl", MANIFEST_URL]
            if prepare:
                args.append("-PrepareOnly")
        return subprocess.run(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=45)

    def test_install_backs_up_all_old_managed_jars_and_keeps_other_data(self):
        custom = {"keep": [1, 2, None, True, "text"], "empty": [], "optional": None}
        self.config_path.write_text(json.dumps({"custom": custom, "seen": {"other": True}}))
        result = self.run_installer()
        self.assertEqual(result.returncode, 0, result.stdout)
        names = {path.name for path in (self.game / "mods").iterdir()}
        self.assertEqual(names, {"kompass4mc-0.3.2.jar", "fabric-api-0.162.0+26.3.jar", "simplemodsync-1.4.1.jar", "other-mod.jar"})
        self.assertEqual(self.original_other.read_bytes(), self.original["mods/other-mod.jar"])
        self.assertEqual((self.game / "saves" / "Family world" / "level.dat").read_bytes(), b"world stays unchanged")
        config = json.loads(self.config_path.read_text(encoding="utf-8-sig"))
        self.assertEqual(config["custom"], custom)
        self.assertTrue(config["seen"]["other"])
        self.assertTrue(config["sync"]["sync_on_startup"])
        self.assertEqual(config["sync"]["schema_file_url"], MANIFEST_URL)
        backups = list((self.game / ".kompass-backups").rglob("*.jar"))
        self.assertEqual(len(backups), 3)
        self.assertEqual(len(list(self.game.glob(".kompass-install-*"))), 0)

    def test_bad_hash_changes_no_installed_files(self):
        self.manifest["sync"][0]["sha512"] = "0" * 128
        result = self.run_installer()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertEqual(self.snapshot(), self.original)

    def test_another_modpack_config_is_not_overwritten(self):
        self.config_path.write_text(json.dumps({"sync": {"schema_file_url": "https://other.invalid/modpack.json"}}))
        before = self.snapshot()
        result = self.run_installer()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertEqual(self.snapshot(), before)

    def test_prepare_only_does_not_change_installed_files(self):
        result = self.run_installer(prepare=True)
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertEqual(self.snapshot(), self.original)

    def test_duplicate_manifest_entry_is_rejected(self):
        self.manifest["sync"][1] = self.manifest["sync"][0].copy()
        result = self.run_installer()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertEqual(self.snapshot(), self.original)

    def test_unrelated_mod_with_target_filename_is_not_overwritten(self):
        jar(self.game / "mods" / "kompass4mc-0.3.2.jar", "unrelated-collision")
        before = self.snapshot()
        result = self.run_installer()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertEqual(self.snapshot(), before)

    def test_fresh_install_without_config(self):
        for name in ("renamed-learning-mod.jar", "kompass4mc-0.1.0.jar", "fabric-api-old.jar"):
            (self.game / "mods" / name).unlink()
        result = self.run_installer()
        self.assertEqual(result.returncode, 0, result.stdout)
        config = json.loads(self.config_path.read_text(encoding="utf-8-sig"))
        self.assertTrue(config["sync"]["sync_on_startup"])
        self.assertEqual(config["sync"]["schema_file_url"], MANIFEST_URL)
        self.assertTrue((self.game / "mods" / "kompass4mc-0.3.2.jar").is_file())

    @unittest.skipUnless(sys.platform == "win32", "Windows file sharing semantics")
    def test_locked_managed_jar_blocks_mutation(self):
        with (self.game / "mods" / "kompass4mc-0.1.0.jar").open("rb"):
            result = self.run_installer()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertEqual(self.snapshot(), self.original)


if __name__ == "__main__":
    unittest.main()
