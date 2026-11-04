# Design: The Lost Centuries

See also: [PROGRESSION.md](PROGRESSION.md) (tiers, milestones, unlock order).

## Premise

Some catastrophe ended the old world. The player wakes up inside a ruined city (The Lost Cities worldgen)
and rebuilds civilization by **reclaiming the city, building by building**.

## Core idea: restoration *is* the leveling system

MineColonies buildings have levels 1–5, and each level is a separate blueprint (`<hut><level>.blueprint`).
In this pack, those levels are stages of a ruin's restoration, not unrelated houses that get bigger:

| Level | State |
|---|---|
| 1 | Rubble cleared, holes patched, ground floor usable |
| 2 | Structural repairs, more floors made safe |
| 3 | Windows, interiors, utilities restored |
| 4 | Fully repaired |
| 5 | Improved beyond the original (rooftop gardens, solar, etc.) |

Machines (Custom Machinery) stand in for *recovered pre-catastrophe tech*. Colonist crafting
(MineColonies recipes via datapack/KubeJS) is what *people* can rebuild by hand.

## How it can line up technically

- **One source of truth per building.** Author each building once in its restored state, then derive the
  lower levels by progressively stripping it back. The same design is also registered as a **Lost Cities building**
  (Lost Cities buildings/parts are data and can be overridden or added by datapack). That way the ruins found in
  the world *are* damaged copies of the colony buildings.
- **Grid alignment.** Lost Cities buildings sit on the chunk grid (16×16 parts, 6-block floors). If every colony
  blueprint uses a chunk-multiple footprint with the hut block at a fixed offset, a hut placed at that spot in
  a ruin lines the blueprint up with the existing building. The builder then only clears debris and fills in
  what's missing, so the repair happens without extra code. *(To verify: exactly how the builder treats
  pre-existing matching/non-matching blocks.)* A KubeJS helper could snap hut placement to the grid.
- **Start in the town hall ruin.** Lost Cities profile options `forceSpawnInBuilding` + `forceSpawnBuildings`
  can spawn the player inside a specific building, e.g. the ruin of our town hall.

## Decisions

- **Worldgen:** custom Lost Cities profile `lostcenturies` (wasteland settings + single city), selected by default
  for new worlds via `pack/config/lostcities-server.toml` (`[profiles] selectedProfile`). NeoForge 21.1 keeps
  server configs in `config/`, not per world, and `defaultconfigs/` only fills in missing files, so the
  setting is shipped in `config/` where pack syncs overwrite it. The profile is stored in each world on creation.
  - Profile: `pack/config/lostcities/profiles/lostcenturies.json`. Copies the built-in `wasteland` profile's
    settings (rubble, heavy ruins, no water/foliage in buildings) and sets `cityChance = 0`, so **no random cities**.
  - **One city:** predefined city `lostcenturies:spawn_city` at chunk 0,0, radius 192 blocks
    (`pack/kubejs/data/lostcenturies/lostcities/predefinedcities/spawn_city.json`). With the default city
    threshold (0.2) that's roughly 300 blocks across. Keep the profile's `cityMaxRadius` >= the city radius:
    Lost Cities only searches that far for city centers.
  - **Spawn:** `spawnCity` + `forceSpawnInBuilding`: the player spawns inside a building in the inner 80% of the city.
    Later: add our town hall ruin as a predefined building at the city center and use `forceSpawnBuildings`.
  - Note: "no water / no foliage" in the profile only applies inside Lost Cities buildings.
- **Terrain:** Terralith (biomes) + Tectonic (terrain shape), both via Lithostitched. Terralith is pinned to
  2.6.2 (packwiz otherwise picks 2.5.8, which targets 1.21.2-1.21.4).
- **Dry zone ("ground zero"):** the companion mod biases the overworld climate (hotter, drier) around the city,
  fading to normal by ~4000 blocks, so biomes near the city are arid and wetter ones appear further out.
  Rivers/oceans still exist; only biome choice changes. Config: `dryZone.*` in `lostcenturies-server.toml`.
  - To check in playtesting: how Lost Cities' leveled city ground meets Tectonic's terrain at the city edge.

## Tech: Create + Custom Machinery, gated by the colony

- **Create** (plus add-ons) for power, fluids and automation; **Custom Machinery** (with Custom Machinery Create,
  so machines run on rotational power) for recovered old-world tech. AE2 and Mekanism are out: they make
  MineColonies' logistics and production redundant.
- **Rule:** basic Create stays player-craftable; advanced Create requires a colony building to produce.
  Gate *components*, not machines. A few chokepoints lock most of the advanced tier:

| Component | Locks |
|---|---|
| `create:electron_tube` | deployer, mechanical crafter, brass funnel/tunnel, smart chute/pipe, sequenced gearshift, track signals, display/nixie |
| `create:precision_mechanism` | mechanical arm, factory gauge, rotation speed controller, extendo grip |
| `create:brass_hand` | deployer, extendo grip |
| `create:transmitter` | redstone link, display link, stock link (Create 6 logistics) |

- **Mechanism (data-only, no Java):** KubeJS removes the player recipe, and a MineColonies `crafterrecipes`
  datapack entry lets a hut's worker craft it at a `min-building-level` (= restoration stage), optionally behind
  a `research-id` (the research tree is data-driven too). See `pack/kubejs/server_scripts/colony_gates.js`.
  Proof of concept: electron tube → Mechanic's Hut level 2.
- **Phase 2: custom huts** (e.g. an Engineer's Workshop dedicated to Create components) need a small Java
  companion mod; MineColonies add-ons like Greenhouse Gardener and Volcanic Mason show this works on 1.21.1.
- Add-ons in the pack: Crafts & Additions and New Age (electricity), Diesel Generators and TFMG (oil/fuel),
  Create: MineColonies Link (Create stock network fills colony warehouse requests), Colony Lifts
  (colonists ride Create elevators).

## Mechanized huts (idea)

Fold Create's early machines into huts as **productivity bonuses** instead of replacements:
the mine's higher restoration levels include a drill rig and a Create elevator.

- **Elevator:** Colony Lifts already lets colonists path to an Elevator Contact, ride, and continue. Put a lift
  in the mine/quarry schematics from level 3 up.
- **Drill bonus:** MineColonies' worker mining delay (`AbstractEntityAIInteract.calculateWorkerMiningDelay`)
  uses block hardness, tool, worker skill, and the colony-wide `BLOCK_BREAK_SPEED` research effect.
  Potion effects like Haste are *not* used, so a per-building bonus needs Java:
  - Option A (data only): a "Mechanized Mining" research costing Create parts, granting `BLOCK_BREAK_SPEED`.
    Easy, but colony-wide and not tied to powered machines.
  - **Option B (chosen, implemented in `mod/`):** if the worker's building has a powered Create drill inside its
    bounds, reduce the mining delay, scaled by RPM. The drill consumes stress while turning, so the bonus has
    a real power cost. Works for the Miner and the Quarry (`EntityAIStructureMiner`, `EntityAIQuarrier`).
  - Option C (kept for later): a full "Mechanized Mine" building type subclassing the miner. Most work, most
    control; the companion mod is where it would live.
  - Same pattern can extend to other huts later (e.g. powered harvester -> farmer, plough -> farmer/planter).

## Thirst & difficulty presets (implemented)

**Thirst Was Taken** for player thirst (water purity: dirty water can poison you). One per-world preset governs
player and colony; see `mod/README.md` for details and config.

| Preset | Player thirst | Colonists when the colony is out of water |
|---|---|---|
| Easy | Off | Colonies don't need water |
| Normal | Light (x0.5) | Slowed (Slowness II) |
| Hard | Regular (x1) | Workers stop working (paused) |

- Set with `/lostcenturies preset <easy|normal|hard>` (op); default `normal` from `survival.defaultPreset`.
  Later: let the player pick it in a starter FTB quest (reward runs the command).
- Colony water: per-colony reserve, refilled from water containers (purity >= acceptable) in the warehouse(s)
  and town hall; 1 unit per citizen per day; new colonies get 3 days of grace. `/lostcenturies water` shows status.
- Next step for the theme: a purified-water production chain (Custom Machinery water purifier / colonist recipes)
  that feeds the warehouse, so a growing colony needs real water infrastructure.

## Open questions


- Should Create's early colonist-competing machines (mechanical harvester/plough/drill) be gated, or
  folded into huts as productivity bonuses (see "Mechanized huts")?
- Diesel Generators and TFMG both add crude oil, diesel and gasoline: unify the fluids with tags/KubeJS
  or drop one after playtesting.
- Huts that don't map to city buildings (farms, mines, lumberjack): parks, rooftop farms,
  or out-of-city outposts?
- Quest structure: FTB Quests chapters tracking restoration milestones (e.g. "restore 3 buildings to level 2").
