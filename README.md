# The Lost Centuries

A Minecraft 1.21.1 (NeoForge) modpack about rebuilding civilization in the ruins of the old cities.
You start inside a ruined city, and your colony grows by **repairing the buildings around you**.
Each MineColonies building level is another stage of a ruin's restoration. See [docs/DESIGN.md](docs/DESIGN.md).

## Repo layout

```
pack/                     packwiz pack root (everything here ships to players)
  pack.toml               MC / NeoForge versions, pack metadata
  index.toml              generated file index (don't hand-edit; run `packwiz refresh`)
  mods/*.pw.toml          pinned mod metadata (source, version, hash) – no jars in git
  kubejs/                 KubeJS scripts (startup / server / client)
  blueprints/lostcenturies/   MineColonies style pack (ruin -> restored building schematics)
mod/                      companion NeoForge mod (Create <-> MineColonies glue), see mod/README.md
docs/                     design notes
```

## Core mods

| Role | Mod | Source |
|---|---|---|
| Worldgen | The Lost Cities, Terralith, Tectonic (+ Lithostitched) | Modrinth |
| Colony / progression | MineColonies (+ Structurize, BlockUI, Domum Ornamentum, Multi-Piston, TownTalk) | CurseForge |
| Scripting | KubeJS (+ Rhino, Architectury) | Modrinth |
| Custom machines | Custom Machinery (+ Custom Machinery Create) | Modrinth |
| Tech | Create, Crafts & Additions, New Age, Diesel Generators, TFMG | Modrinth |
| Colony <-> Create | Create: MineColonies Link, Colony Lifts | CurseForge |
| Quests | FTB Quests (+ FTB Library, FTB Teams), FTB XMod Compat (quests <-> KubeJS) | CurseForge |
| Survival | Thirst Was Taken (presets via the companion mod) | Modrinth |
| Recipe viewer | JEI | Modrinth |
| Per-player loot in ruins | Lootr | Modrinth |
| Performance | ModernFix, FerriteCore, Lithium, Sodium (+ Sodium Extra, Reese's Sodium Options), ImmediatelyFast, Entity Culling, spark (profiler) | Modrinth |
| Shaders | Iris + Complementary Reimagined / Unbound (in `shaderpacks/`, off by default) | Modrinth |
| Maps | Xaero's Minimap, Xaero's World Map | Modrinth |
| QoL | Jade (+ Jade Addons), Mouse Tweaks, AppleSkin, Controlling, Clumps, Inventory Essentials | Modrinth |
| Building | Effortless Building | Modrinth |

## Working on the pack

Install [packwiz](https://packwiz.infra.link/) (needs Go: `brew install go`, then
`go install github.com/packwiz/packwiz@latest`, and make sure `~/go/bin` is on your PATH).

All commands run from `pack/`:

```sh
packwiz mr add <modrinth-slug>      # add a Modrinth mod (+ deps)
packwiz cf add <curseforge-slug>    # add a CurseForge mod (+ deps)
packwiz update --all                # update every mod
packwiz remove <name>
packwiz refresh                     # rebuild index.toml after editing scripts/configs/blueprints
```

### Playtesting locally (Prism Launcher)

1. In `pack/`, run `packwiz serve` (serves the pack at `http://localhost:8080/pack.toml`).
2. In Prism, create a 1.21.1 NeoForge instance, add
   [packwiz-installer-bootstrap.jar](https://github.com/packwiz/packwiz-installer-bootstrap/releases)
   to the instance's `minecraft/` folder, and set the instance's pre-launch command to:
   `"$INST_JAVA" -jar packwiz-installer-bootstrap.jar http://localhost:8080/pack.toml`
3. Launch. The instance pulls the current pack state on every start.

### Releasing

Two tag-driven GitHub Actions workflows:

| Tag | Workflow | Result |
|---|---|---|
| `mod-vX.Y.Z` | `mod-release.yml` | builds the companion mod, GitHub release with the jar, updates `pack/mods/lostcenturies.pw.toml` on `main` (see `mod/README.md`) |
| `pack-vX.Y.Z` | `pack-release.yml` | GitHub release with a **Prism instance zip** + changelog of mod changes since the last pack tag |

Pack release:

```sh
# 1. bump version = "X.Y.Z" in pack/pack.toml
cd pack && packwiz refresh && cd ..
git commit -am "pack X.Y.Z" && git push
git tag pack-vX.Y.Z && git push origin pack-vX.Y.Z
```

The workflow fails if the tag doesn't match `pack.toml`'s version or if `index.toml` is stale.

The instance zip contains no mod jars, because some mods (e.g. FTB's) don't allow redistribution. It's a Prism
instance whose pre-launch command runs packwiz-installer against `pack.toml` **at that tag**, so each
release is reproducible and every mod is downloaded from its original source. (`packwiz mr export` would
bundle the CurseForge-only jars into the `.mrpack`, so we don't publish that.)

To track `main` instead (dev/testing), change the pre-launch URL in the instance to `.../main/pack/pack.toml`.

### Exporting to platforms (later)

`packwiz cf export` / `packwiz mr export` produce CurseForge / Modrinth packs, but each bundles the other
platform's jars, so publishing on CurseForge or Modrinth means sourcing every mod from that platform first.
