# Ex Compressum — Omnia compatibility for Fabric 26.3

This branch keeps Ex Compressum's original machines, interfaces, models, animations,
and configuration, and connects them to Joshua's Ex Nihilo Omnia Fabric fork.
It is a separate mod from Ex Nihilo Automation, with different block IDs.

## Requirements

Install on client and server:

- Minecraft 26.3, Java 25, Fabric Loader 0.19.5 or newer.
- Fabric API 0.160.7+26.3 or a compatible newer release.
- Balm 26.3.0.1 for Fabric.
- Ex Nihilo Omnia 0.5.0-alpha.3 (optional-JEI release) with its existing API.

## Integration

- Omnia string, flint, iron and diamond meshes work in Ex Compressum's machines.
  Ex Compressum's own iron mesh also maps to Omnia's iron tier.
- Normal inputs use the Auto Sieve. Omnia compressed inputs use the Heavy Sieve
  or Auto Heavy Sieve, with exactly nine independent passes through Omnia's live
  drop table. Meshes are not consumed or damaged, matching Omnia.
- Normal hammer conversions use Omnia's cobblestone/deepslate, gravel, sand/red
  sand, netherrack and End stone conversions. Omnia compressed hammer inputs use
  the Auto Compressed Hammer or compressed hammers and retain the compressed
  cobblestone -> gravel -> sand -> dust chain.
- Compressed crooks reproduce Omnia's two vanilla leaf/plant loot rolls and
  silkworm chance, using a surrogate crook to avoid nested loot-modifier recursion.
- Crafting tags accept Omnia's wooden sieve, heavy sieve, hammers and wooden
  crook. Dust and crushed Nether/End stone recipes recognize Omnia resources.
- Recipe-viewer data exposes Omnia hammer conversions and all seven sieve inputs
  at each of the four mesh tiers. Vanilla nuggets are not legacy ore pieces.
- Omnia meshes have no native Fortune/Efficiency behavior; this integration does
  not invent mesh enchantment bonuses.

Ex Compressum's own compressed-block recipes keep its original configured roll
count. The explicit nine-roll rule above applies to Omnia's compressed blocks.
Do not add generated heavy-sieve recipes for those same Omnia inputs unless
intending to add extra rewards: the provider already processes them directly.

## Power and existing worlds

The original machines retain Ex Compressum's external energy system. This branch
adds no coal slot or generator. The separate Automation companion's fuel power
is not a feature of Ex Compressum.

Automation companion machines are not converted automatically. Empty and remove
those blocks before uninstalling that companion from an existing world, or keep
both mods installed while moving your production to Ex Compressum.

## Building

Put your existing Omnia release JAR at:

`libs/exnihiloomnia-fabric-26.3-0.5.0-alpha.3-jei.jar`

Alternatively, pass `-Pomnia_jar=/absolute/path/to/your/omnia.jar`.
The dependency is external and is not bundled into Ex Compressum.

Fabric builds already compile the shared source files themselves. To avoid
running the redundant NeoForm common-project compiler, use:

```
./gradlew :fabric:build -Pinclude_neoforge=false -x :common:compileJava -x :common:createMinecraftArtifacts
```

Server integration checks:

```
./gradlew :fabric:runGameTest -Pinclude_neoforge=false -x :common:compileJava -x :common:createMinecraftArtifacts
```

The checks cover every material/mesh combination against the actual Omnia table,
original Auto Sieve processing and mesh retention, heavy-registry nine-roll
routing, crafting tags and compressed hammer conversions. Client visuals and
external generator interoperability require an in-game check.

## Validation for 26.3.0.0-omnia.1

The Fabric build completed successfully using the commands above. All five
required server checks passed (four Omnia integration checks and Fabric's
registration check). This is server validation, not a client visual check.
Omnia does not provide crushed andesite/diorite/granite; recipes requiring those
optional resources remain unavailable without another provider or a data pack.
