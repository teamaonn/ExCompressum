package net.blay09.mods.excompressum.fabric.compat.omnia;

import exnihiloomnia.fabric.CrookItem;
import exnihiloomnia.fabric.ExNihiloOmnia;
import exnihiloomnia.fabric.SieveDrops;
import net.blay09.mods.excompressum.api.ExNihiloProvider;
import net.blay09.mods.excompressum.api.recipe.*;
import net.blay09.mods.excompressum.api.sievemesh.*;
import net.blay09.mods.excompressum.loot.LootTableUtils;
import net.blay09.mods.excompressum.registry.ExNihilo;
import net.blay09.mods.excompressum.registry.sievemesh.SieveMeshRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import org.jspecify.annotations.Nullable;

import java.util.*;

/** Connects the original machines to Omnia's live processing tables. */
public final class OmniaAddon implements ExNihiloProvider {
    private static final CommonMeshType[] MESH_TYPES = {
            CommonMeshType.STRING, CommonMeshType.FLINT, CommonMeshType.IRON, CommonMeshType.DIAMOND
    };
    private static final Block[] HAMMER_INPUTS = {
            Blocks.COBBLESTONE, Blocks.COBBLED_DEEPSLATE, Blocks.GRAVEL, Blocks.SAND,
            Blocks.RED_SAND, Blocks.NETHERRACK, Blocks.END_STONE
    };

    public OmniaAddon() {
        ExNihilo.setInstance(this);
        SieveMeshRegistry.registerDefaults(3);
        for (int i = 0; i < MESH_TYPES.length; i++) {
            var mesh = new SieveMeshRegistryEntry(MESH_TYPES[i],
                    new ItemStack(ExNihiloOmnia.ITEMS.get(ExNihiloOmnia.MESHES[i])), i + 1);
            mesh.setHeavy(true);
            mesh.setModelName(MESH_TYPES[i].getSerializedName());
            SieveMeshRegistry.add(mesh);
        }
    }

    public static int material(ItemLike item) {
        for (int i = 1; i <= ExNihiloOmnia.MATERIALS.length; i++) {
            if (net.blay09.mods.excompressum.registry.UnifiedCompression.canonical(item.asItem()) == ExNihiloOmnia.materialItem(i, false)
                    || net.blay09.mods.excompressum.registry.UnifiedCompression.canonical(item.asItem()) == ExNihiloOmnia.materialItem(i, true)) return i;
        }
        return 0;
    }

    public static boolean compressed(ItemLike item) {
        for (int i = 1; i <= ExNihiloOmnia.MATERIALS.length; i++) {
            if (net.blay09.mods.excompressum.registry.UnifiedCompression.canonical(item.asItem()) == ExNihiloOmnia.materialItem(i, true)) return true;
        }
        return net.blay09.mods.excompressum.registry.UnifiedCompression.canonical(item.asItem()) == ExNihiloOmnia.ITEMS.get("compressed_cobblestone");
    }

    private static int tier(@Nullable SieveMeshRegistryEntry mesh) {
        if (mesh == null) return 0;
        for (int i = 0; i < MESH_TYPES.length; i++) {
            if (mesh.getMeshType() == MESH_TYPES[i]) return i + 1;
        }
        return 0;
    }

    @Override
    public boolean isHammerable(Level level, BlockState state) {
        return !compressed(state.getBlock()) && ExNihiloOmnia.hammerOutput(net.blay09.mods.excompressum.registry.UnifiedCompression.canonical(state.getBlock())) != null;
    }

    @Override
    public List<ItemStack> rollHammerRewards(Level level, BlockState state, ItemInstance tool, RandomSource rand) {
        var output = ExNihiloOmnia.hammerOutput(net.blay09.mods.excompressum.registry.UnifiedCompression.canonical(state.getBlock()));
        return output == null ? List.of() : List.of(new ItemStack(output));
    }

    @Override
    public boolean isHammerableCompressed(ItemStack input) {
        return input.getItem() instanceof BlockItem block && compressed(block.getBlock())
                && ExNihiloOmnia.hammerOutput(net.blay09.mods.excompressum.registry.UnifiedCompression.canonical(block.getBlock())) != null;
    }

    @Override
    public Collection<ItemStack> rollCompressedHammerRewards(Level level, LootContext context, ItemStack input) {
        if (!isHammerableCompressed(input)) return List.of();
        return rollHammerRewards(level, ((BlockItem) input.getItem()).getBlock().defaultBlockState(), input, context.getRandom());
    }

    @Override
    public boolean isSiftableWithMesh(Level level, BlockState sieve, BlockState input, @Nullable SieveMeshRegistryEntry mesh) {
        return material(input.getBlock()) != 0 && !compressed(input.getBlock()) && tier(mesh) > 0;
    }

    @Override
    public boolean isHeavySiftableWithMesh(BlockState sieve, BlockState input, @Nullable SieveMeshRegistryEntry mesh) {
        return material(input.getBlock()) != 0 && compressed(input.getBlock()) && tier(mesh) > 0;
    }

    @Override
    public Collection<ItemStack> rollSieveRewards(Level level, BlockState sieve, BlockState input,
                                                 SieveMeshRegistryEntry mesh, float luck, RandomSource rand) {
        if (!isSiftableWithMesh(level, sieve, input, mesh)) return List.of();
        List<ItemStack> result = new ArrayList<>();
        SieveDrops.roll(material(input.getBlock()), tier(mesh), 1, rand, result::add);
        return result;
    }

    @Override
    public Collection<ItemStack> rollHeavySieveRewards(Level level, BlockState sieve, BlockState input,
                                                      SieveMeshRegistryEntry mesh, float luck, RandomSource rand) {
        if (!isHeavySiftableWithMesh(sieve, input, mesh)) return List.of();
        List<ItemStack> result = new ArrayList<>();
        SieveDrops.roll(material(input.getBlock()), tier(mesh), 9, rand, result::add);
        return result;
    }

    /** Used for Ex Compressum's own generated compressed recipes and recipe viewers. */
    @Override
    public LootTable generateHeavySieveLootTable(Level level, BlockState sieve, ItemLike source, int count, SieveMeshRegistryEntry mesh) {
        return sieveLoot(material(source), tier(mesh), count);
    }

    private static LootTable sieveLoot(int material, int tier, int rolls) {
        var table = LootTable.lootTable();
        if (material == 0 || tier == 0 || rolls <= 0) return table.build();
        for (var drop : SieveDrops.entries()) {
            if (drop.input() != material || drop.minimumMesh() > tier) continue;
            var entry = LootTableUtils.buildLootEntry(new ItemStack(drop.item(), drop.count()), drop.probability());
            if (drop.bonusChance() > 0) {
                entry.apply(SetItemCountFunction.setCount(Holder.direct(new ConstantValue(drop.count() + 1)))
                        .when(LootItemRandomChanceCondition.randomChance(drop.bonusChance())));
            }
            table.withPool(LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(rolls))).add(entry));
        }
        return table.build();
    }

    @Override
    public List<ItemStack> rollCrookRewards(ServerLevel level, BlockPos pos, BlockState state,
                                          @Nullable Entity entity, ItemInstance tool, RandomSource rand) {
        // Use an Omnia crook for the nested vanilla loot calls to avoid recursively
        // entering Ex Compressum's compressed-crook loot modifier.
        var crook = new ItemStack(ExNihiloOmnia.ITEMS.get("crook_bone"));
        if (!CrookItem.crookable(state)) return Block.getDrops(state, level, pos, null, entity, crook);
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, entity, crook));
        drops.addAll(Block.getDrops(state, level, pos, null, entity, crook));
        if (rand.nextInt(100) <= 1) drops.add(new ItemStack(ExNihiloOmnia.ITEMS.get("silkworm")));
        return drops;
    }

    @Override public boolean doMeshesHaveDurability() { return false; }
    @Override public int getMeshFortune(ItemStack mesh) { return 0; }
    @Override public int getMeshEfficiency(ItemStack mesh) { return 0; }
    // Omnia uses vanilla nuggets, which must not be treated as legacy ore pieces.
    @Override public boolean isCompressableOre(ItemStack input) { return false; }
    @Override public boolean isHammerableOre(ItemStack input) { return false; }

    private static LootTable hammerLoot(Block input) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .add(LootTableUtils.buildLootEntry(new ItemStack(Objects.requireNonNull(ExNihiloOmnia.hammerOutput(input))), 1F))).build();
    }

    @Override
    public List<HammerRecipe> getHammerRecipes() {
        List<HammerRecipe> recipes = new ArrayList<>();
        for (var input : HAMMER_INPUTS) recipes.add(new Hammer(Ingredient.of(input), hammerLoot(input)));
        return recipes;
    }

    @Override
    public List<CompressedHammerRecipe> getCompressedHammerRecipes() {
        List<CompressedHammerRecipe> recipes = new ArrayList<>();
        for (var name : new String[]{"compressed_cobblestone", "compressed_gravel", "compressed_sand"}) {
            var block = ExNihiloOmnia.BLOCKS.get(name);
            recipes.add(new CompressedHammer(Ingredient.of(block), hammerLoot(block)));
        }
        return recipes;
    }

    @Override
    public List<SieveRecipe> getSieveRecipes() {
        List<SieveRecipe> recipes = new ArrayList<>();
        for (int input = 1; input <= ExNihiloOmnia.MATERIALS.length; input++) {
            for (int tier = 1; tier <= MESH_TYPES.length; tier++) {
                recipes.add(new Sieve(Ingredient.of(ExNihiloOmnia.materialItem(input, false)),
                        sieveLoot(input, tier, 1), Set.of(MESH_TYPES[tier - 1])));
            }
        }
        return recipes;
    }

    @Override
    public List<HeavySieveRecipe> getHeavySieveRecipes() {
        List<HeavySieveRecipe> recipes = new ArrayList<>();
        for (int input = 1; input <= ExNihiloOmnia.MATERIALS.length; input++) {
            for (int tier = 1; tier <= MESH_TYPES.length; tier++) {
                recipes.add(new HeavySieve(Ingredient.of(ExNihiloOmnia.materialItem(input, true)),
                        sieveLoot(input, tier, 9), Set.of(MESH_TYPES[tier - 1])));
            }
        }
        return recipes;
    }

    private record Hammer(Ingredient getIngredient, LootTable getLootTable) implements HammerRecipe {}
    private record CompressedHammer(Ingredient getIngredient, LootTable getLootTable) implements CompressedHammerRecipe {}
    private record Sieve(Ingredient getIngredient, LootTable getLootTable, Set<CommonMeshType> getMeshes) implements SieveRecipe {
        @Override public boolean isWaterlogged() { return false; }
    }
    private record HeavySieve(Ingredient getIngredient, LootTable getLootTable, Set<CommonMeshType> getMeshes) implements HeavySieveRecipe {
        @Override public boolean isWaterlogged() { return false; }
    }
}
