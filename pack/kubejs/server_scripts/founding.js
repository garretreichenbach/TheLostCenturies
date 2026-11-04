// Founding: there is exactly one town hall, sitting in the town hall ruin in the city (see docs/PROGRESSION.md).
// Players found the colony by right-clicking it; everyone else joins that colony as a team
// (colony permissions via the town hall; FTB Teams for shared quest progress).
//
// So no other way to get a town hall: no crafting it, and no supply camp / supply ship (they start a colony anywhere).
ServerEvents.recipes(event => {
  ;[
    'minecolonies:blockhuttownhall',
    'minecolonies:supplycampdeployer',
    'minecolonies:supplychestdeployer',
  ].forEach(item => event.remove({ output: item }))
})
