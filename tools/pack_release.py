#!/usr/bin/env python3
"""Build a pack release: a Prism Launcher instance zip + release notes.

The zip contains no mod jars (several mods, e.g. FTB's, don't allow redistribution). It's a Prism instance
that runs packwiz-installer-bootstrap before launch, pinned to this release's tag, so every mod is downloaded
from its original source (Modrinth / CurseForge / GitHub).

Usage (from the repo root):
  python3 tools/pack_release.py --tag pack-v0.1.0 --repo garretreichenbach/TheLostCenturies \
      --bootstrap packwiz-installer-bootstrap.jar [--prev-tag pack-v0.0.9] [--out dist]
"""
import argparse
import json
import re
import subprocess
import sys
import zipfile
from pathlib import Path

try:
    import tomllib
except ModuleNotFoundError:  # Python < 3.11
    import tomli as tomllib

PACK = Path("pack")


def fail(msg):
    print(f"::error::{msg}", file=sys.stderr)
    sys.exit(1)


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout


def mods_at(ref):
    """{metafile path: (name, filename, side)} for pack/mods/*.pw.toml at a git ref (None = working tree)."""
    out = {}
    if ref is None:
        paths = sorted(str(p) for p in (PACK / "mods").glob("*.pw.toml"))
        read = lambda p: Path(p).read_text()
    else:
        paths = [p for p in git("ls-tree", "--name-only", f"{ref}:pack/mods").split()]
        paths = [f"pack/mods/{p}" for p in paths if p.endswith(".pw.toml")]
        read = lambda p: git("show", f"{ref}:{p}")
    for p in paths:
        t = tomllib.loads(read(p))
        out[p] = (t.get("name", p), t.get("filename", "?"), t.get("side", "both"))
    return out


def notes(version, pack, tag, repo, prev_tag):
    mc, neo = pack["versions"]["minecraft"], pack["versions"].get("neoforge", "?")
    lines = [
        f"## The Lost Centuries {version}",
        "",
        f"Minecraft **{mc}**, NeoForge **{neo}**.",
        "",
        "### Install (Prism Launcher)",
        f"Download `TheLostCenturies-{version}-prism.zip` below and drag it into Prism (or *Add Instance → Import*).",
        "The first launch downloads all mods from their original sources, so it takes a while.",
        "",
        "Server / other launchers: point packwiz-installer at",
        f"`https://raw.githubusercontent.com/{repo}/{tag}/pack/pack.toml`",
        "",
    ]
    cur = mods_at(None)
    if prev_tag:
        old = mods_at(prev_tag)
        added = [cur[p] for p in cur if p not in old]
        removed = [old[p] for p in old if p not in cur]
        updated = [(old[p], cur[p]) for p in cur if p in old and old[p][1] != cur[p][1]]
        lines.append(f"### Changes since {prev_tag}")
        if not (added or removed or updated):
            lines.append("No mod changes (configs/scripts only).")
        for n, f, _ in sorted(added):
            lines.append(f"- **Added** {n} (`{f}`)")
        for n, f, _ in sorted(removed):
            lines.append(f"- **Removed** {n}")
        for (n, f0, _), (_, f1, _) in sorted(updated):
            lines.append(f"- **Updated** {n}: `{f0}` → `{f1}`")
    else:
        lines.append(f"### Mods ({len(cur)})")
        for n, f, side in sorted(cur.values(), key=lambda x: x[0].lower()):
            lines.append(f"- {n} (`{f}`)" + ("" if side == "both" else f" *{side} only*"))
    return "\n".join(lines) + "\n"


def instance_zip(path, version, pack, tag, repo, bootstrap):
    url = f"https://raw.githubusercontent.com/{repo}/{tag}/pack/pack.toml"
    instance_cfg = "\n".join([
        "[General]",
        "ConfigVersion=1.2",
        "InstanceType=OneSix",
        f"name=The Lost Centuries {version}",
        "iconKey=default",
        "OverrideCommands=true",
        f'PreLaunchCommand="$INST_JAVA" -jar packwiz-installer-bootstrap.jar {url}',
        "OverrideMemory=true",
        "MinMemAlloc=2048",
        "MaxMemAlloc=8192",
        "",
    ])
    mmc_pack = {
        "formatVersion": 1,
        "components": [
            {"uid": "org.lwjgl3", "version": "3.3.3", "dependencyOnly": True},
            {"uid": "net.minecraft", "version": pack["versions"]["minecraft"], "important": True},
            {"uid": "net.neoforged", "version": pack["versions"]["neoforge"]},
        ],
    }
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("instance.cfg", instance_cfg)
        z.writestr("mmc-pack.json", json.dumps(mmc_pack, indent=2) + "\n")
        z.write(bootstrap, "minecraft/packwiz-installer-bootstrap.jar")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tag", required=True)
    ap.add_argument("--repo", required=True)
    ap.add_argument("--bootstrap", required=True, help="path to packwiz-installer-bootstrap.jar")
    ap.add_argument("--prev-tag", default="")
    ap.add_argument("--out", default="dist")
    a = ap.parse_args()

    m = re.fullmatch(r"pack-v(\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?)", a.tag)
    if not m:
        fail(f"Tag '{a.tag}' must look like pack-v1.2.3")
    version = m.group(1)
    pack = tomllib.loads((PACK / "pack.toml").read_text())
    if pack.get("version") != version:
        fail(f"pack/pack.toml has version '{pack.get('version')}' but the tag is {a.tag}. "
             f"Set version = \"{version}\" in pack/pack.toml, run `packwiz refresh`, commit, and re-tag.")

    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    zip_path = out / f"TheLostCenturies-{version}-prism.zip"
    instance_zip(zip_path, version, pack, a.tag, a.repo, a.bootstrap)
    (out / "notes.md").write_text(notes(version, pack, a.tag, a.repo, a.prev_tag or None))
    print(f"Built {zip_path} ({zip_path.stat().st_size} bytes) and {out / 'notes.md'}")


if __name__ == "__main__":
    main()
