// Ex Deorum is the early survival loop (one saved sapling -> water, dirt, seeds, saplings).
// Its sieves must not become an ore/gem farm: metals come from salvage, mining and the colony's Sifter hut.
//
// Ex Deorum sieve recipe IDs are exdeorum:(compressed_)sieve/<input>/<mesh>/<output>, where <output> is the
// result item's path (checked for all 1692 sieve recipes in Ex Deorum 3.12). This removes the 540 that give
// ore chunks or gems. Kept: pebbles, flint, seeds, saplings, crops, bones, and iron/gold nuggets as a trickle.
const EXDEORUM_SIEVE_BLOCKED = /^exdeorum:(compressed_)?sieve\/.+\/(\w+_ore_chunk|raw_gold|diamond|emerald|lapis_lazuli|redstone|quartz|amethyst_shard|ancient_debris)(_\d+)?$/

ServerEvents.recipes(event => {
  event.remove({ id: EXDEORUM_SIEVE_BLOCKED })
})
