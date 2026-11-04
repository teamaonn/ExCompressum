package net.blay09.mods.excompressum.fabric.compat.omnia;
import exnihiloomnia.fabric.ExNihiloOmnia;
import net.blay09.mods.excompressum.block.*;
import net.blay09.mods.excompressum.block.entity.*;
import net.blay09.mods.excompressum.client.render.blockentity.*;
import net.blay09.mods.balm.world.level.block.entity.BalmBlockEntityUtils;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.Vec3;
public final class CompatClientTests implements FabricClientGameTest {
 private static void check(boolean good,String text){if(!good)throw new AssertionError(text);}
 public void runTest(ClientGameTestContext context){try(var world=context.worldBuilder().create()){
  var positions=world.getServer().computeOnServer(server->{var player=world.getConnection().getServerPlayer();player.setGameMode(GameType.SURVIVAL);return new BlockPos[]{player.blockPosition().offset(-2,0,3),player.blockPosition().offset(1,0,3),player.blockPosition().offset(-8,0,12)};});
  world.getServer().runOnServer(server->{var level=server.overworld();level.setBlock(positions[0],ModBlocks.heavySieves.get(HeavySieveType.OAK).defaultBlockState(),3);var heavy=(HeavySieveBlockEntity)level.getBlockEntity(positions[0]);heavy.setMeshStack(new ItemStack(ExNihiloOmnia.ITEMS.get("mesh_diamond")));check(heavy.addSiftable(level,world.getConnection().getServerPlayer(),new ItemStack(ExNihiloOmnia.ITEMS.get("compressed_dirt"))),"Manual heavy sieve accepts canonical dirt");
   level.setBlock(positions[1],ModBlocks.autoHeavySieve.asBlock().defaultBlockState(),3);var auto=(AutoHeavySieveBlockEntity)level.getBlockEntity(positions[1]);auto.getBackingContainer().setItem(21,new ItemStack(ExNihiloOmnia.ITEMS.get("mesh_diamond")));auto.getBackingContainer().setItem(0,new ItemStack(ExNihiloOmnia.ITEMS.get("compressed_dirt")));auto.setEnergyStored(auto.getMaxEnergyStored());auto.serverTick();auto.setDisabledByRedstone(true);BalmBlockEntityUtils.sync(auto);
   var house=server.getStructureTemplateManager().getOrCreate(net.minecraft.resources.Identifier.parse("actuallyadditions:andrew_period_house"));check(house.getSize().getX()>0,"Engineer house template loaded");house.placeInWorld(level,positions[2],positions[2],new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),level.getRandom(),2);
  });
  world.getConnection().waitForChunksDownload();context.runOnClient(mc->{mc.player.setYRot(0);mc.player.setXRot(18);});
  context.waitFor(mc->mc.level.getBlockEntity(positions[0]) instanceof HeavySieveBlockEntity h&&!h.getCurrentStack().isEmpty()&&mc.level.getBlockEntity(positions[1]) instanceof AutoHeavySieveBlockEntity a&&!a.getCurrentStack().isEmpty());
  for(float progress:new float[]{0F,.5F,.99F}){
   world.getServer().runOnServer(server->{var level=server.overworld();var h=(HeavySieveBlockEntity)level.getBlockEntity(positions[0]);var tag=h.getUpdateTag(level.registryAccess());tag.putFloat("Progress",progress);h.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));BalmBlockEntityUtils.sync(h);var a=(AutoHeavySieveBlockEntity)level.getBlockEntity(positions[1]);a.setProgress(progress);BalmBlockEntityUtils.sync(a);});
   context.waitFor(mc->mc.level.getBlockEntity(positions[0]) instanceof HeavySieveBlockEntity h&&Math.abs(h.getProgress()-progress)<.01F&&mc.level.getBlockEntity(positions[1]) instanceof AutoHeavySieveBlockEntity a&&Math.abs(a.getProgress()-progress)<.01F);
   context.runOnClient(mc->{var heavy=(HeavySieveBlockEntity)mc.level.getBlockEntity(positions[0]);check(heavy.getBlockState().getRenderShape()==RenderShape.MODEL,"Heavy frame retains native block rendering");var renderer=(HeavySieveRenderer)(Object)mc.getBlockEntityRenderDispatcher().getRenderer(heavy);var state=renderer.createRenderState();renderer.extractRenderState(heavy,state,0,Vec3.ZERO,null);check(!state.mesh.isEmpty()&&!state.item.isEmpty(),"Manual mesh and shrinking fill remain renderable at "+progress);var auto=(AutoHeavySieveBlockEntity)mc.level.getBlockEntity(positions[1]);var ar=(AutoSieveRenderer<AutoHeavySieveBlockEntity>)(Object)mc.getBlockEntityRenderDispatcher().getRenderer(auto);var as=ar.createRenderState();ar.extractRenderState(auto,as,0,Vec3.ZERO,null);check(!as.sieve.isEmpty()&&!as.mesh.isEmpty()&&!as.content.isEmpty(),"Auto-heavy frame, mesh and fill remain renderable");});
   context.waitTicks(3);context.takeScreenshot("unified-sieves-"+(int)(progress*100));
  }
  world.getServer().runOnServer(server->{var level=server.overworld();var h=(HeavySieveBlockEntity)level.getBlockEntity(positions[0]);h.processContents(world.getConnection().getServerPlayer());var a=(AutoHeavySieveBlockEntity)level.getBlockEntity(positions[1]);a.setDisabledByRedstone(false);a.setProgress(1F);a.serverTick();a.setDisabledByRedstone(true);BalmBlockEntityUtils.sync(a);});
  context.waitFor(mc->((HeavySieveBlockEntity)mc.level.getBlockEntity(positions[0])).getCurrentStack().isEmpty()&&((AutoHeavySieveBlockEntity)mc.level.getBlockEntity(positions[1])).getCurrentStack().isEmpty());context.waitTicks(3);context.takeScreenshot("unified-sieves-complete");
  world.getServer().runOnServer(server->{var player=world.getConnection().getServerPlayer();var level=server.overworld();player.setGameMode(GameType.SPECTATOR);for(var p:BlockPos.betweenClosed(positions[2],positions[2].offset(20,10,20)))if(BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString().equals("actuallyadditions:crusher")){boolean found=false;for(var offset:new int[][]{{0,2},{2,0},{0,-2},{-2,0},{1,1},{-1,1},{1,-1},{-1,-1}}){var feet=p.offset(offset[0],0,offset[1]);var eye=new Vec3(feet.getX()+.5,feet.getY()+1.72,feet.getZ()+.5);if(!level.getBlockState(feet).canBeReplaced()||!level.getBlockState(feet.above()).canBeReplaced())continue;var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,Vec3.atCenterOf(p),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));if(!hit.getBlockPos().equals(p))continue;player.teleportTo(level,feet.getX()+.5,feet.getY()+.1,feet.getZ()+.5,java.util.Set.of(),180F,18F,true);player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(p));found=true;break;}check(found,"An interior viewpoint sees the village machine");break;}});
  world.getConnection().waitForChunksDownload();context.waitTicks(5);context.takeScreenshot("engineer-house-machinery");
  context.runOnClient(mc->{for(var item:BuiltInRegistries.ITEM){String ns=BuiltInRegistries.ITEM.getKey(item).getNamespace();if(!java.util.Set.of("excompressum","exnihiloomnia","actuallyadditions").contains(ns))continue;var state=new net.minecraft.client.renderer.item.ItemStackRenderState();mc.getItemModelResolver().updateForTopItem(state,new ItemStack(item),ItemDisplayContext.GUI,mc.level,mc.player,0);check(!state.isEmpty(),"Item model resolves: "+item);}});
 }}
}
