# Progression outline (draft)

The skeleton for both mechanics and quests: each tier becomes one quest chapter. Tiers are gated by
**restoration levels** of colony buildings (see DESIGN.md) and by **colony needs** (water now, power later).

Status: **live** = in the pack now · **planned** = data/KubeJS work · **mod** = needs the companion mod

## Lore (working)

A severe pandemic overwhelmed health systems; the strain broke governments, instability turned into war, and
society collapsed. The city was **ground zero**: first outbreak, then quarantine, then the fighting. The player
emerges from a **bunker** generations later ("The Lost Centuries") into the dead city.

What this gives the design:
- **Quarantine zones:** parts of the city are still dangerous (sealed districts, contaminated areas, haunted
  buildings). They're cleared gradually, which drives exploration outward from the bunker.
- **Medicine as old-world tech:** hospitals/labs are high-danger, high-reward ruins; MineColonies' hospital and
  diseases fit naturally.
- **Military relics:** bunkers, checkpoints, a substation hardened for war.

## Overview

| Tier | Theme | Milestone (chapter end) | Key unlocks |
|---|---|---|---|
| 0 Wake | Out of the bunker, alone in a dead city | Find and claim the town hall ruin | Colony founded |
| 1 Survive | First survivors | Town hall L1 restored, 3+ citizens with water | Builder, condenser, basic Create |
| 2 Settle | A colony that lasts | Clean water for 10 citizens, warehouse running | Mechanic's Hut, deep well + filter, soil |
| 3 Rebuild | Old machines wake up | **The grid is back** (substation repaired) | Electricity, fuel, mechanized mine, purified water |
| 4 Reconnect | The world beyond the city | Radio tower repaired, first outpost past the dry zone | Visitors/traders, wet biomes, seeds, logistics |
| 5 Restore | Ground zero, reborn | City core fully restored (endgame goal TBD) | Fabrication, late tech |

## Tier 0: Wake

You start in a **bunker** under the city center (**planned**: a predefined Lost Cities building at chunk 0,0 with
`forceSpawnBuildings`; the first test of the blueprint → Lost Cities converter). Spawn/dry zone **live**.

- **Starting kit:** bunker supplies + one **saved sapling** (+ a little water). Everything grows from that.
- **Ex Deorum loop** (**live**): sapling → tree → leaves → **wooden crucible → water** (counts as *dirty*:
  Thirst Was Taken `defaultPurity = 0`, **live**);
  **barrels** collect rain and turn compost into dirt; **sieves** (dirt → seeds, more saplings) and **hammers**.
  Sieves give no ore chunks or gems (540 recipes removed in `exdeorum_limits.js`, **live**); iron/gold nuggets
  remain as a trickle. Metals come from salvage, mining and the colony's Sifter hut.
  Ex Deorum sieves can also take **crushed ruin rubble → salvage** (data recipes, **planned**).
- **Exploration:** loot ruins (Lootr, **live**). Outer buildings are safe-ish; the inner city is a
  **quarantine zone** (see "Danger zones").
- **Preset choice:** easy / normal / hard via a quest choice running `/lostcenturies preset` (**planned**).
- **Founding:** the **real town hall block sits in the town hall ruin** (part of that building's Lost Cities design).
  Right-clicking an unclaimed town hall opens MineColonies' normal founding dialog and founds the colony on that
  exact block (`GetColonyInfoMessage` → `CreateColonyMessage`; checked in MineColonies 1.1.1411). No code needed.
  - `minDistanceFromWorldSpawn` defaults to 0, so a colony at the city center is allowed.
  - Bonus: the hut is already at the ruin's anchor, so the builder's L1 "repair" lines up with the ruin.
    The ruin and the blueprints must share the same orientation (worldgen-placed huts have no rotation set).
  - **One town hall, teams join it** (decided). Town hall, supply camp and supply ship recipes are removed
    (`founding.js`, **live**), so the ruin is the only way to found a colony. Other players join as a team:
    colony permissions (town hall) + FTB Teams for shared quest progress.
  - **Automatic team joining** (**live**, mod): joining the founder's FTB party adds you to the colony as an
    officer; leaving the party removes you.
  - **Moving the town hall is allowed** (decided): breaking it drops it as an item, so a team can rebuild
    elsewhere if they want.
  - Option: the town hall ruin is lightly haunted (Lost Souls), so "clear the ruin" is the tutorial fight
    before founding.

## Tier 1: Survive

- **Buildings:** Town hall L1 (repair the ruin), Builder's Hut, first Residences.
- **Water:** colony starts with 3 days of water (**live**). **Atmospheric condenser**: cheap, slow, weather
  and biome dependent, so weak in the dry zone (**planned**). Colony water reserve (**live**).
- **Tech:** basic Create (andesite age, player-craftable): hand crank, rooftop windmills.
  Water wheels are mostly useless here (no surface water).
- **Salvage:** ruin loot gives first old-world parts (broken electronics, scrap) (**planned**: loot tables + items).
- Milestone: town hall L1, 3+ citizens, colony not out of water for a full day.

## Tier 2: Settle

- **Buildings:** Warehouse (+ Courier), Composter, Farmer, Mechanic's Hut L1–2, Lumberjack (scarce trees near
  the city: wood also comes from salvage).
- **Water:** **deep well** (dirty water, depth-based) + **sediment filter** (sand/charcoal cartridges) →
  "acceptable" purity, which is the colony's minimum (**planned**). **Bottler** fills containers for the warehouse.
- **Food:** **soil restorer** turns sand/coarse dirt + compost into fertile soil for fields (**planned**).
- **Tech gates:** electron tubes made by the Mechanic's Hut at L2 (**live**). Create logistics stay locked.
- Milestone: water reserve sustains 10 citizens; warehouse holds water + food.

## Tier 3: Rebuild

- **Buildings:** Mine L3 (drill rig + lift), Smeltery, Blacksmith, Mechanic's Hut L3–4, University.
- **Mechanized huts:** powered Create drills speed up the miner (**live**, mod); Colony Lifts in the shaft (**live**).
- **Salvage line:** **scrap reclaimer** (Create crushing/washing + sorter) and **electronics bench**
  (break found electronics into components) (**planned**).
- **Water:** **boiler distiller** (Create heat or fuel) → purified water (**planned**).
- **Fuel:** oil pump on deposits → Diesel Generators / TFMG (**live** mods, recipes to tune).
- **The grid:** repair the **substation relic** in the city (relic repair mechanic, **planned**).
  Unlocks electricity (Crafts & Additions / New Age) and the colony **power need** (**mod**: happiness/productivity
  bonus while powered).
- **Gates:** precision mechanism at Mechanic L4 / research (**planned**); "Old World Engineering" research branch.
- Milestone: substation online, distilled water, first electric machines.

## Tier 4: Reconnect

- **Radio tower relic** (Lost Cities already scatters radio towers): repair → visitors at the tavern, traders,
  ruin locations revealed via quests (**planned**).
- **Expansion past the dry zone:** outposts in wetter biomes for wood, fish, crops (dry zone, **live**).
- **Seed vault relic:** recovered seeds unlock crops (**planned**).
- **Logistics:** Create stock links unlocked; Create: MineColonies Link connects the network to the warehouse
  (**live** mod, gate **planned**).
- Milestone: radio online, first outpost, logistics network feeding the warehouse.

## Tier 5: Restore

- **Fabricator** as equipment in an Electronics Workshop hut: colony makes old-world components itself,
  salvage becomes optional (**mod**: custom hut + "machines as hut equipment").
- **UV purifier**, full grid, late Create / New Age tech.
- **Endgame goal (TBD):** e.g. restore the city's central tower / relight the city / all core buildings at L5.
- Milestone: endgame.

## Danger zones (quarantine)

The city is cleared bit by bit, from the bunker outward.

- **Hand-designed city layout:** the predefined city can place specific buildings at specific chunks
  (`buildings` / `streets` in `spawn_city.json`), so we can draw districts: a safer ring near the bunker,
  sealed quarantine districts, a hospital district, a military checkpoint, and so on.
- **Haunted buildings (Lost Souls, live):** per-building-type settings (spawn chance, mobs, armor, health
  bonus). A building is cleared after enough kills, and its **chests stay locked until then**. Quarantine-district
  buildings get high chances and tougher mobs; outer ones low or none.
- **Contamination (planned, KubeJS):** inside quarantine districts the player slowly gets an infection effect
  unless wearing protection (gas mask → hazmat suit). Decontaminating a district (a machine/quest goal) lifts it
  permanently. That ties the disease lore to gear progression and the machine tiers.
- **Build server:** keep haunting/contamination off there (`hauntedChance = 0.0` in the server's local
  `config/lostsouls-server.toml`; the pack deliberately doesn't ship that file so local edits survive syncs).

## Cross-cutting mechanics (from the brainstorm)

| Mechanic | Tiers | Status |
|---|---|---|
| Relic repair: broken machine blocks in ruins, repair kit swaps in the working machine | 3–5 | planned (data + KubeJS) |
| Machines as hut equipment: colonist recipes need the machine powered inside the hut | 3–5 | mod |
| Biome/weather/time-dependent machine output | 1–4 | planned (Custom Machinery requirements) |
| Colony needs: water (live), power, food bonuses | 1–5 | water live; power mod |
| Salvage → repair → fabricate for old-world components | 1–5 | planned |

## Open questions

- **Lore details:** name of the disease/war, the city, who built the bunker; the endgame goal.
- **Fisher / water huts:** no surface water near the city. Outpost-only, or cut?

- **Early Create machines** (harvester/plough/drill): gated, or hut bonuses only?
- **Presets:** should tiers differ by preset beyond thirst (e.g. easy skips the deep well)?

