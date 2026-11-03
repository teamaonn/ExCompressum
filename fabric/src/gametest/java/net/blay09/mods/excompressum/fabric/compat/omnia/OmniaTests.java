package net.blay09.mods.excompressum.fabric.compat.omnia;

import exnihiloomnia.fabric.ExNihiloOmnia;
import exnihiloomnia.fabric.SieveDrops;
import net.blay09.mods.excompressum.api.sievemesh.SieveMeshRegistryEntry;
import net.blay09.mods.excompressum.block.ModBlocks;
import net.blay09.mods.excompressum.block.entity.AutoSieveBlockEntity;
import net.blay09.mods.excompressum.registry.ExNihilo;
import net.blay09.mods.excompressum.registry.ExRegistries;
import net.blay09.mods.excompressum.registry.sievemesh.SieveMeshRegistry;
import net.blay09.mods.excompressum.tag.ModItemTags;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

public final class OmniaTests {
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
    private static ItemStack omnia(String name) { return new ItemStack(ExNihiloOmnia.ITEMS.get(name)); }
    private static Map<Item, Integer> counts(Collection<ItemStack> stacks) {
        Map<Item, Integer> result = new HashMap<>();
        for (var stack : stacks) result.merge(stack.getItem(), stack.getCount(), Integer::sum);
        return result;
    }
    private static SieveMeshRegistryEntry mesh(int tier) {
        return Objects.requireNonNull(SieveMeshRegistry.getEntry(omnia(ExNihiloOmnia.MESHES[tier - 1])));
    }
    @GameTest public void liveDropsMatchForEveryMaterialAndMesh(GameTestHelper h) {
        check(ExNihilo.getInstance() instanceof OmniaAddon, "Omnia provider must be installed");
        var sieve = ModBlocks.autoSieve.asBlock().defaultBlockState();
        var heavy = ModBlocks.autoHeavySieve.asBlock().defaultBlockState();
        for (int input = 1; input <= 7; input++) for (int tier = 1; tier <= 4; tier++) {
            for (boolean compressed : new boolean[]{false, true}) {
                var block = ((BlockItem) ExNihiloOmnia.materialItem(input, compressed)).getBlock().defaultBlockState();
                List<ItemStack> expected = new ArrayList<>();
                SieveDrops.roll(input, tier, compressed ? 9 : 1, RandomSource.create(9123), expected::add);
                var actual = compressed
                        ? ExNihilo.getInstance().rollHeavySieveRewards(h.getLevel(), heavy, block, mesh(tier), 0, RandomSource.create(9123))
                        : ExNihilo.getInstance().rollSieveRewards(h.getLevel(), sieve, block, mesh(tier), 0, RandomSource.create(9123));
                check(counts(actual).equals(counts(expected)), "Live Omnia drops differ for input=" + input + " tier=" + tier);
            }
        }
        h.succeed();
    }
    @GameTest public void originalMachinesAcceptOmniaAndRun(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        h.getLevel().setBlock(pos, ModBlocks.autoSieve.asBlock().defaultBlockState(), 3);
        var sieve = (AutoSieveBlockEntity) h.getLevel().getBlockEntity(pos);
        sieve.getBackingContainer().setItem(0, new ItemStack(Items.DIRT));
        sieve.getBackingContainer().setItem(21, omnia("mesh_diamond"));
        sieve.setEnergyStored(sieve.getMaxEnergyStored());
        for (int i = 0; i < 200; i++) sieve.serverTick();
        check(sieve.getBackingContainer().getItem(0).isEmpty() && sieve.getCurrentStack().isEmpty(), "Auto sieve must finish its input");
        int total = 0;
        for (int i = 1; i < 21; i++) total += sieve.getBackingContainer().getItem(i).getCount();
        check(total >= 2, "Dirt must produce its guaranteed pebbles");
        check(sieve.getMeshStack().getCount() == 1 && sieve.getMeshStack().getDamageValue() == 0, "Omnia mesh must be retained");
        h.succeed();
    }
    @GameTest public void compressedInputsRouteThroughHeavyRegistryOnce(GameTestHelper h) {
        var heavy = ModBlocks.autoHeavySieve.asBlock().defaultBlockState();
        var input = omnia("compressed_dirt");
        check(ExRegistries.getHeavySieveRegistry().isSiftable(h.getLevel(), heavy, input, mesh(4)), "Heavy registry must accept Omnia compressed dirt");
        List<ItemStack> expected = new ArrayList<>();
        SieveDrops.roll(1, 4, 9, RandomSource.create(583), expected::add);
        h.getLevel().getRandom().setSeed(583);
        var context = net.blay09.mods.excompressum.loot.LootTableUtils.buildLootContext(h.getLevel(), input);
        var actual = net.blay09.mods.excompressum.registry.heavysieve.HeavySieveRegistry.rollSieveRewards(h.getLevel(), context, heavy, mesh(4), input);
        check(counts(actual).equals(counts(expected)), "Heavy registry must apply exactly nine rolls without generated-recipe duplicates");
        h.succeed();
    }
    @GameTest public void craftingTagsAndHammerConversions(GameTestHelper h) {
        check(omnia("sieve_wood").is(ModItemTags.SIEVES), "Auto sieve crafting tag");
        check(omnia("heavy_sieve").is(ModItemTags.HEAVY_SIEVES), "Auto heavy sieve crafting tag");
        check(omnia("hammer_diamond").is(ModItemTags.DIAMOND_HAMMERS), "Auto hammer crafting tag");
        check(omnia("crook_wood").is(ModItemTags.WOODEN_CROOKS), "Compressed crook crafting tag");
        var provider = ExNihilo.getInstance();
        var output = provider.rollHammerRewards(h.getLevel(), Blocks.COBBLESTONE.defaultBlockState(), omnia("hammer_iron"), RandomSource.create(1));
        check(output.size() == 1 && output.getFirst().is(Items.GRAVEL), "Cobblestone must hammer to gravel");
        var input = omnia("compressed_sand");
        var context = net.blay09.mods.excompressum.loot.LootTableUtils.buildLootContext(h.getLevel(), input);
        var compressed = provider.rollCompressedHammerRewards(h.getLevel(), context, input);
        check(compressed.size() == 1 && compressed.iterator().next().is(ExNihiloOmnia.ITEMS.get("compressed_dust")), "Compressed hammer must preserve Omnia compression");
        check(!provider.isCompressableOre(new ItemStack(Items.IRON_NUGGET)), "Nuggets are not legacy ore fragments");
        h.succeed();
    }
}
