package net.blay09.mods.excompressum.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blay09.mods.excompressum.block.entity.HeavySieveBlockEntity;
import net.blay09.mods.excompressum.client.ModModels;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;

public class HeavySieveRenderer implements BlockEntityRenderer<HeavySieveBlockEntity, HeavySieveRenderer.HeavySieveRenderState> {

    public static class HeavySieveRenderState extends BlockEntityRenderState {
        public final BlockModelRenderState mesh = new BlockModelRenderState();
        public float progress;
        public final net.minecraft.client.renderer.block.BlockModelRenderState item = new net.minecraft.client.renderer.block.BlockModelRenderState();
    }

    private final net.minecraft.client.renderer.block.BlockModelResolver blockModelResolver;
    private static final net.minecraft.client.renderer.block.model.BlockDisplayContext CONTENT = net.minecraft.client.renderer.block.model.BlockDisplayContext.create();

    public HeavySieveRenderer(BlockEntityRendererProvider.Context context) {
        blockModelResolver = context.blockModelResolver();
    }

    @Override
    public HeavySieveRenderState createRenderState() {
        return new HeavySieveRenderState();
    }

    @Override
    public void extractRenderState(HeavySieveBlockEntity blockEntity, HeavySieveRenderState renderState, float delta, Vec3 vec, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, delta, vec, crumblingOverlay);

        final var meshModelName = blockEntity.getSieveMesh() != null ? blockEntity.getSieveMesh().getModelName() : null;
        final var meshModel = meshModelName != null ? ModModels.meshes.containsKey(meshModelName) ? ModModels.meshes.get(meshModelName).asBlockStateModel() : null : null;
        if (meshModel != null) {
            final var meshParts = renderState.mesh.setupModel(new Matrix4f(), false);
            meshModel.collectParts(renderState.mesh.scratchRandomSource(42), meshParts);
        } else {
            renderState.mesh.clear();
        }

        renderState.progress = Float.isFinite(blockEntity.getProgress()) ? net.minecraft.util.Mth.clamp(blockEntity.getProgress(),0F,1F) : 0F;

        var content = net.blay09.mods.excompressum.registry.UnifiedCompression.surface(net.blay09.mods.excompressum.utils.StupidUtils.getStateFromItemStack(blockEntity.getCurrentStack()));
        if(content.isAir())renderState.item.clear();else blockModelResolver.update(renderState.item,content,CONTENT);
    }

    @Override
    public void submit(HeavySieveRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        renderState.mesh.submit(poseStack, submitNodeCollector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        if (!renderState.item.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.0625f, 0.5625f, 0.0625f);
            float tt = 0.42f;
            poseStack.scale(0.88f, Math.max(.01F,tt - renderState.progress * tt), 0.88f);
            renderState.item.submit(poseStack, submitNodeCollector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

}
