#!/usr/bin/env python3
"""Prepare an immutable GitHub release and its stable Simple Mod Sync entry point."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import tempfile
from urllib.parse import quote, urlparse
import zipfile


def properties(path: Path) -> dict[str, str]:
    return dict(line.split("=", 1) for raw in path.read_text(encoding="utf-8").splitlines()
                if (line := raw.strip()) and not line.startswith("#") and "=" in line)


def sha(path: Path, algorithm: str) -> str:
    digest = hashlib.new(algorithm)
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def prepare(root: Path, tag: str, repository: str, output: Path) -> Path:
    props = properties(root / "gradle.properties")
    version = props["mod_version"]
    if not re.fullmatch(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)", version):
        raise ValueError("mod_version muss eine stabile Version wie 0.3.2 sein.")
    if tag != "v" + version:
        raise ValueError("Tag und mod_version stimmen nicht überein.")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]*/[A-Za-z0-9][A-Za-z0-9_.-]*", repository):
        raise ValueError("Ungültiger GitHub-Repositoryname.")
    if output.exists():
        raise ValueError("Der Ausgabeordner existiert bereits; einen neuen Ordner verwenden.")

    archive = props["archives_base_name"]
    if not re.fullmatch(r"[a-z0-9_-]+", archive):
        raise ValueError("Ungültiger JAR-Basisname.")
    jar = root / "build/libs" / f"{archive}-{version}.jar"
    sources = root / "build/libs" / f"{archive}-{version}-sources.jar"
    with zipfile.ZipFile(jar) as compiled:
        metadata = json.loads(compiled.read("fabric.mod.json"))
        if metadata.get("id") != "kompass4mc" or metadata.get("version") != version:
            raise ValueError("Die gebaute JAR gehört nicht zu dieser Mod-Version.")
        if metadata.get("depends", {}).get("minecraft") != props["minecraft_version"]:
            raise ValueError("Die gebaute JAR gehört zu einer anderen Minecraft-Version.")

    dependency = json.loads((root / "distribution/fabric-api.json").read_text(encoding="utf-8"))
    if (dependency["version"] != props["fabric_api_version"]
            or dependency["minecraft_version"] != props["minecraft_version"]):
        raise ValueError("Fabric-API-Lockdatei und gradle.properties stimmen nicht überein.")
    parsed = urlparse(dependency["url"])
    if parsed.scheme != "https" or parsed.hostname != "cdn.modrinth.com" or parsed.username:
        raise ValueError("Fabric API benötigt einen offiziellen HTTPS-Downloadlink.")
    if not re.fullmatch(r"[a-f0-9]{128}", dependency["sha512"]):
        raise ValueError("Ungültige Fabric-API-Prüfsumme.")

    assets = [jar, sources, root / "scripts/install-simple-mod-sync.ps1",
              root / "scripts/install-simple-mod-sync.command",
              root / "output/pdf/Kompass_Abenteuer_Eltern_Einrichtung.pdf",
              root / "output/pdf/Kompass_Abenteuer_Kinder_Spielanleitung.pdf"]
    for asset in assets:
        if not asset.is_file() or asset.stat().st_size == 0:
            raise ValueError(f"Release-Datei fehlt oder ist leer: {asset.name}")
    instructions = root / "docs/github-releases.md"
    if not instructions.is_file():
        raise ValueError("Die Installationsanleitung fehlt.")

    manifest = {
        "sync_version": 3,
        "minecraft_version": props["minecraft_version"],
        "fabric_loader_min": props["loader_version"],
        "mod_version": version,
        "sync": [
            {"name": "kompass4mc", "type": "mod", "version": version,
             "url": f"https://github.com/{repository}/releases/download/{quote(tag)}/{jar.name}",
             "sha512": sha(jar, "sha512")},
            {"name": "fabric-api", "type": "mod", "version": dependency["version"],
             "url": dependency["url"], "sha512": dependency["sha512"]},
        ],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    staging = Path(tempfile.mkdtemp(prefix="kompass-release-", dir=output.parent))
    try:
        for asset in assets:
            shutil.copy2(asset, staging / asset.name)
        shutil.copy2(instructions, staging / "INSTALLATION.md")
        (staging / "simple-mod-sync.json").write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        checksums = "".join(f"{sha(path, 'sha256')}  {path.name}\n"
                            for path in sorted(staging.iterdir()))
        (staging / "SHA256SUMS").write_text(checksums, encoding="utf-8")
        staging.rename(output)
    finally:
        if staging.exists():
            shutil.rmtree(staging)
    return output


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    try:
        prepare(root, args.tag, args.repository, args.output_dir.resolve())
    except (ValueError, KeyError, OSError, zipfile.BadZipFile) as error:
        parser.exit(1, f"Release konnte nicht vorbereitet werden: {error}\n")
    print(f"Release {args.tag} vorbereitet: {args.output_dir}")


if __name__ == "__main__":
    main()
