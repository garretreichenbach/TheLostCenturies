// Colony gates: advanced items that players cannot craft by hand.
//
// Pattern for each gated item:
//   1. Remove every player/machine recipe that outputs it (here).
//   2. Add a MineColonies worker recipe so a specific hut crafts it, at a minimum
//      building level (= restoration stage), optionally behind a research:
//      kubejs/data/minecolonies/crafterrecipes/lostcenturies/<item>.json
//      Fields: crafter, inputs, result, min-building-level, max-building-level,
//              research-id, not-research-id, must-exist, show-tooltip
//
// Because the player recipe no longer exists, the item also can't be "taught"
// to a crafter hut, so the datapack recipe is the only way to make it.
// MineColonies' JEI plugin lists hut recipes, so players can still find them.

const COLONY_GATED = [
  // item                     // made by (see crafterrecipes json)
  'create:electron_tube',     // Mechanic's Hut, level 2
]

ServerEvents.recipes(event => {
  COLONY_GATED.forEach(item => event.remove({ output: item }))
})
