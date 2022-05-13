package com.lowdragmc.multiblocked.client.renderer;

import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.item.ItemBlueprint;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Multiblocked.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BlueprintRegionRenderer {

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity p = mc.player;
        if (p == null) return;
        ItemStack held = p.getItemInHand(Hand.MAIN_HAND);
        if (held.getItem() instanceof ItemBlueprint) {
            BlockPos[] poses = ItemBlueprint.getPos(held);
            if (poses == null) return;
            MatrixStack stack = event.getMatrixStack();

            Vector3d pos = mc.gameRenderer.getMainCamera().getPosition();

            stack.pushPose();
            stack.translate(-pos.x, -pos.y, -pos.z);

            RenderSystem.disableDepthTest();
            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            RenderBufferUtils.renderCubeFace(stack, buffer, poses[0].getX(), poses[0].getY(), poses[0].getZ(), poses[1].getX() + 1, poses[1].getY() + 1, poses[1].getZ() + 1, 0.2f, 0.2f, 1f, 0.25f, true);

            tessellator.end();


            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            RenderSystem.lineWidth(3);

            RenderBufferUtils.renderCubeFrame(stack, buffer, poses[0].getX(), poses[0].getY(), poses[0].getZ(), poses[1].getX() + 1, poses[1].getY() + 1, poses[1].getZ() + 1, 0.0f, 0.0f, 1f, 0.5f);

            tessellator.end();

            RenderSystem.enableCull();

            RenderSystem.disableBlend();
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
//            RenderSystem.color4f(1, 1, 1, 1);

            stack.popPose();
        }
    }

}
