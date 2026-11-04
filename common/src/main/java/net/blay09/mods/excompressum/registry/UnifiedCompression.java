package net.blay09.mods.excompressum.registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
/** Stable legacy IDs remain readable, while crafting and drops use Omnia. */
public final class UnifiedCompression {
 public static Item canonical(Item item){var id=BuiltInRegistries.ITEM.getKey(item);if(!id.getNamespace().equals("excompressum"))return item;
  String target=switch(id.getPath()){
   case "compressed_dirt" -> "compressed_dirt";
   case "compressed_cobblestone" -> "compressed_cobblestone";
   case "compressed_gravel" -> "compressed_gravel";
   case "compressed_sand" -> "compressed_sand";
   case "compressed_dust" -> "compressed_dust";
   case "compressed_soul_sand" -> "compressed_soul_sand";
   case "compressed_crushed_netherrack" -> "compressed_gravel_nether";
   case "compressed_crushed_end_stone" -> "compressed_gravel_ender";
   default -> null;};
  if(target==null)return item;var key=Identifier.fromNamespaceAndPath("exnihiloomnia",target);return BuiltInRegistries.ITEM.containsKey(key)?BuiltInRegistries.ITEM.getValue(key):item;
 }
 public static Block canonical(Block block){return Block.byItem(canonical(block.asItem()));}
 public static net.minecraft.world.level.block.state.BlockState surface(net.minecraft.world.level.block.state.BlockState state){var block=canonical(state.getBlock());var id=BuiltInRegistries.BLOCK.getKey(block);if(!id.getNamespace().equals("exnihiloomnia")||!id.getPath().startsWith("compressed_"))return state;String name=id.getPath().substring("compressed_".length());String namespace=switch(name){case "dirt","cobblestone","gravel","sand","soul_sand"->"minecraft";default->"exnihiloomnia";};var target=Identifier.fromNamespaceAndPath(namespace,name);return BuiltInRegistries.BLOCK.containsKey(target)?BuiltInRegistries.BLOCK.getValue(target).defaultBlockState():state;}
 public static boolean legacy(Item item){return canonical(item)!=item;}
}
