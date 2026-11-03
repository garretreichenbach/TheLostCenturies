# The Lost Centuries Core (companion mod)

NeoForge 1.21.1 mod for gameplay glue that KubeJS/datapacks can't do. Mod id: `lostcenturies`.

## Features

### Mechanized huts: drill bonus
Powered Create **mechanical drills** inside a **Miner's** or **Quarrier's** hut footprint (the hut's schematic
corners) shorten the worker's per-block mining delay. Bonus scales with the drills' combined RPM; overstressed
drills don't count. A spinning drill costs stress, so the bonus has a real power cost.

Implementation: a mixin on `AbstractEntityAIInteract#calculateWorkerMiningDelay` (MineColonies), plus a cached
scan of the hut's block entities (`mechanized/MechanizedBuildings`).

Per-world config `serverconfig/lostcenturies-server.toml`:

| Key | Default | |
|---|---|---|
| `mechanized.drillBonusEnabled` | `true` | |
| `mechanized.maxDelayReduction` | `0.5` | up to 50% shorter mining delay |
| `mechanized.rpmForMaxBonus` | `256` | combined drill RPM for the full bonus |
| `mechanized.scanIntervalTicks` | `100` | how long a hut scan is cached |

### Dry zone ("ground zero")
The overworld is hotter and drier around the world center, where the spawn city is, and fades back to the
normal climate further out. Implemented by wrapping the overworld climate sampler's **temperature** and
**humidity** in a distance-based bias (`worldgen/RadialBias`, applied in `worldgen/DryZone` on overworld load,
before spawn search and chunk generation). Biome mods (Terralith, Tectonic, vanilla) then pick their own
hot/dry biomes near the city. Terrain shape doesn't use these two values, so landforms are unchanged,
and rivers/oceans/aquifers still exist: biomes get drier, water isn't removed.

| Key | Default | |
|---|---|---|
| `dryZone.enabled` | `true` | |
| `dryZone.innerRadius` | `1500` | blocks from center with the full effect |
| `dryZone.outerRadius` | `4000` | blocks from center where the climate is back to normal |
| `dryZone.temperatureBias` | `0.6` | added to temperature noise (> 0.55 = hot) |
| `dryZone.dryness` | `0.8` | subtracted from humidity noise (< -0.35 = arid) |

Set these before creating a world. Changing them later only affects new chunks (biome seams).

### Survival presets (thirst)
Per-world preset, stored in the world (`data/lostcenturies.dat`). New worlds use `survival.defaultPreset`.

```
/lostcenturies preset                    show the current preset
/lostcenturies preset easy|normal|hard   change it (op)
/lostcenturies water                     colony water status
```

| Preset | Player thirst (Thirst Was Taken) | Colony out of water |
|---|---|---|
| easy | off (exhaustion x0, bar kept full) | no effect, colonies don't drink |
| normal | light (`normalThirstRate`, default x0.5) | citizens get Slowness II |
| hard | regular (`hardThirstRate`, default x1) | workers are paused (only ones we paused get unpaused) |

Player thirst: a mixin scales `PlayerThirst#addExhaustion`. Thirst Was Taken's own biome modifier still applies,
so deserts (the dry zone around the city) make you thirsty faster.

### Colony drinking water
Each colony has a water reserve. Every `checkIntervalTicks` it drinks `citizens x waterPerCitizenPerDay`
(scaled to the interval). When the reserve runs out it refills from water containers (bottles, buckets,
terracotta bowls, canteens...) in the colony's **warehouses**, then the **town hall** inventory, accepting only
purity >= `minPurity` (default 2, "acceptable"). Empty containers go back into storage. New colonies start with
`startingReserveDays` of water. Unloaded colonies are frozen and don't drink. The colony's important-message
players get a message when water runs out and when it's restored.

| Key (`colonyWater.`) | Default | |
|---|---|---|
| `checkIntervalTicks` | `1200` | 1 minute |
| `waterPerCitizenPerDay` | `1.0` | a bottle is 1 unit |
| `bucketUnits` | `3` | |
| `minPurity` | `2` | 0 dirty, 1 slightly dirty, 2 acceptable, 3 purified |
| `startingReserveDays` | `3.0` | grace period for new colonies |

## Building

Requires JDK 21.

```sh
./gradlew build                  # jar in build/libs/
./gradlew build -PskipRecompile  # skip decompiling Minecraft (less RAM, no MC sources in IDE)
./gradlew runClient              # dev client with MineColonies + Create on the classpath
```

Dependency versions live in `gradle.properties` and should match `../pack/mods/*.pw.toml`
(MineColonies etc. via CurseMaven file IDs, Create via its Modrinth version ID).

## Playtesting in Prism Launcher

```sh
./gradlew runPrism        # build, install into the Prism instance, launch it
./gradlew installToPrism  # build + install only
```

`installToPrism` replaces any older `lostcenturies-*.jar` in the instance's `mods/` folder and leaves
everything else alone (packwiz-installer only manages files it installed itself, so it won't touch this jar).
`runPrism` calls Prism with `--launch <instance>`; if Prism is already open, the running window takes the launch.

Settings (`gradle.properties`, or override in `~/.gradle/gradle.properties` / with `-P`):

| Property | Default |
|---|---|
| `prism_instance` | `1.21.1` (the instance's folder name) |
| `prism_instances_dir` | `~/Library/Application Support/PrismLauncher/instances` |
| `prism_executable` | auto: `/Applications/Prism Launcher.app/...`, `/Applications/PrismLauncher.app/...`, `~/Applications/...` |

If the instance syncs the pack via packwiz-installer from `http://localhost:8080`, start `packwiz serve` in
`../pack` first.

## Getting it into the pack

For release, publish it (Modrinth/CurseForge or GitHub Releases) and add it with packwiz
(`packwiz mr add` / `packwiz url add`), so the pack keeps pinning by hash.
