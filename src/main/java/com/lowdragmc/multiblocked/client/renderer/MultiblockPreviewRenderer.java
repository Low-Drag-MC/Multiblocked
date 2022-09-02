package com.lowdragmc.multiblocked.client.renderer;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Multiblocked.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MultiblockPreviewRenderer {

    private static BlockPos mbpPos;
    private static long mbpEndTime;
    private static int layer;
    private static Direction facing, previewFacing, spin;
    private static Rotation rotatePreviewBy;
    private static IControllerComponent mte;
    private static BlockPos controllerPos;
    private static TrackedDummyWorld world;
    private static Map<BlockPos, BlockInfo> blockMap;

    @SubscribeEvent
    public static void renderWorldLastEvent(RenderLevelLastEvent event) {
        if (mbpPos != null) {
            Minecraft mc = Minecraft.getInstance();
            long time = System.currentTimeMillis();
            if (time > mbpEndTime || !(mc.level != null && mc.level.getBlockEntity(mbpPos) instanceof IControllerComponent)) {
                resetMultiblockRender();
                layer = 0;
                return;
            }
            PoseStack stack = event.getPoseStack();

            Vec3 pos = mc.gameRenderer.getMainCamera().getPosition();

            stack.pushPose();
            stack.translate(-pos.x, -pos.y, -pos.z);

            RenderSystem.enableTexture();
            RenderSystem.enableBlend();
            RenderSystem.enableCull();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            render(stack, buffer);
            buffer.endBatch();

            stack.popPose();

        }
    }


    public static void renderMultiBlockPreview(IControllerComponent controller, long durTimeMillis) {
        if (!controller.self().getBlockPos().equals(mbpPos)) {
            layer = 0;
        } else {
            if (mbpEndTime - System.currentTimeMillis() < 200) return;
            layer++;
        }
        resetMultiblockRender();
        mbpEndTime = System.currentTimeMillis() + durTimeMillis;
        List<MultiblockShapeInfo> shapes = controller.getDefinition().getDesigns();
        if (!shapes.isEmpty()) renderControllerInList(controller, shapes.get(0), layer);
    }

    public static void resetMultiblockRender() {
        mbpPos = null;
        mbpEndTime = 0;
    }

    public static void renderControllerInList(IControllerComponent controllerBase, MultiblockShapeInfo shapeInfo, int layer) {
        Direction frontFacing;
        previewFacing = Direction.NORTH;
        controllerPos = BlockPos.ZERO;
        mte = null;
        BlockInfo[][][] blocks = shapeInfo.getBlocks();
        blockMap = new HashMap<>();
        int maxY = 0;
        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            maxY = Math.max(maxY, aisle.length);
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    blockMap.put(new BlockPos(x, y, z), column[z]);
                    BlockEntity te = column[z].getBlockEntity(new BlockPos(x, y, z));
                    IControllerComponent metaTE = te instanceof IControllerComponent ? (IControllerComponent) te : null;
                    if (metaTE != null) {
                        if (metaTE.getDefinition().location.equals(controllerBase.getDefinition().location)) {
                            controllerPos = new BlockPos(x, y, z);
                            mte = metaTE;
                        }
                    }
                }
            }
        }
        world = new TrackedDummyWorld();
        world.addBlocks(blockMap);
        int finalMaxY = layer % (maxY + 1);
        world.setRenderFilter(pos -> pos.getY() + 1 == finalMaxY || finalMaxY == 0);

        facing = controllerBase.getFrontFacing();
        spin = Direction.NORTH;
        frontFacing = facing.getStepY() == 0 ? facing : facing.getStepY() < 0 ? spin : spin.getOpposite();
        rotatePreviewBy = Rotation.values()[(4 + frontFacing.get2DDataValue() - previewFacing.get2DDataValue()) % 4];
        if (mte != null) {
            mbpPos = controllerBase.self().getBlockPos();
            world.setBlockEntity(mte.self());
        }
    }

    public static void render(PoseStack stack, MultiBufferSource buffer) {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher brd = mc.getBlockRenderer();
        stack.pushPose();
        stack.translate(mbpPos.getX(), mbpPos.getY(), mbpPos.getZ());

        stack.translate(0.5, 0, 0.5);
        stack.mulPose(new Quaternion(new Vector3f(0, -1, 0), rotatePreviewBy.ordinal() * 90, true));
        stack.translate(-0.5, 0, -0.5);

        if (facing == Direction.UP) {
            stack.translate(0.5, 0.5, 0.5);
            stack.mulPose(new Quaternion(new Vector3f(-previewFacing.getStepZ(), 0, previewFacing.getStepX()), 90, true));
            stack.translate(-0.5, -0.5, -0.5);
        } else if (facing == Direction.DOWN) {
            stack.translate(0.5, 0.5, 0.5);
            stack.mulPose(new Quaternion(new Vector3f(previewFacing.getStepZ(), 0, -previewFacing.getStepX()), 90, true));
            stack.translate(-0.5, -0.5, -0.5);
        } else {
            int degree = 90 * (spin == Direction.EAST ? -1 : spin == Direction.SOUTH ? 2 : spin == Direction.WEST ? 1 : 0);
            stack.translate(0.5, 0.5, 0.5);
            stack.mulPose(new Quaternion(new Vector3f(previewFacing.getStepX(), 0, previewFacing.getStepZ()), degree, true));
            stack.translate(-0.5, -0.5, -0.5);
        }

        if (mte != null) {
            mte.checkPattern();
        }

        RenderType lastType = MinecraftForgeClient.getRenderType();
        for (BlockPos pos : blockMap.keySet()) {
            if (controllerPos.equals(pos)) continue;
            stack.pushPose();
            BlockPos.MutableBlockPos tPos = pos.subtract(controllerPos).mutable();
            stack.translate(tPos.getX(), tPos.getY(), tPos.getZ());
            stack.translate(0.125, 0.125, 0.125);
            stack.scale(0.75f, 0.75f, 0.75f);

            BlockState state = world.getBlockState(pos);

            for (RenderType renderType : RenderType.chunkBufferLayers()) {
                ForgeHooksClient.setRenderType(renderType);
                brd.renderBatched(state, pos, world, stack, buffer.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)), false, Multiblocked.RNG, EmptyModelData.INSTANCE);
            }

            BlockEntity tileEntity = world.getBlockEntity(pos);

            if (tileEntity != null) {
                BlockEntityRenderer<BlockEntity> tesr = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tileEntity);
                if (tesr != null) {
                    try {
                        tesr.render(tileEntity, Minecraft.getInstance().getFrameTime(), stack, buffer, 0xf000f0, OverlayTexture.NO_OVERLAY);
                        RenderSystem.enableTexture();
                        RenderSystem.enableBlend();
                        RenderSystem.enableCull();
                        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    } catch (Exception ignored) {

                    }
                }
            }

            stack.popPose();
        }
        ForgeHooksClient.setRenderType(lastType);

        stack.popPose();
    }

}
