# Fabric 26.3 Omnia compatibility update

The Fabric fork uses Omnia's stable compressed block IDs for dirt, cobblestone,
gravel, sand, dust, soul sand, Nether gravel and End gravel. Omnia supplies the
compression/decompression recipes and processing behavior; its block models use
the original Ex Compressum textures.

Duplicate Ex Compressum recipes and generated heavy-sieve recipes are removed.
Existing Ex Compressum compressed IDs remain registered for saved worlds, but are
hidden from the creative tab. Craft one legacy stack item into its canonical
Omnia equivalent; breaking a legacy block also drops the canonical item. The
adapter accepts old stacks in processing, without applying the nine-roll rewards
twice. Other compressed materials remain Ex Compressum items.

Manual and automatic heavy sieves now send native block-entity updates for mesh,
contents and progress. Manual fill uses a block surface, with positive thickness
through the last animation frame; automatic fill is bounded likewise. The tiny
operator has a stable fallback skin rather than changing it every frame.

## Build

Use Java 25 and Gradle 9.7.1:

    gradle -p fabric-26.3-omnia build -Pomnia_jar=/path/to/exnihiloomnia-fabric-26.3-0.5.0-alpha.4.jar

The standalone Fabric module compiles the existing common and Fabric sources.
Optional `aa_jar` and `jei_jar` properties add those real release JARs to tests.
`runGameTest` checks canonical recipes, legacy conversion, exact nine-roll sieve
processing and persistence; `runClientGameTest` renders live manual/automatic
sieves at multiple stages with all three mods loaded.
