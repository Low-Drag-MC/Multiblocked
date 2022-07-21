package com.lowdragmc.multiblocked.client.renderer.impl;


import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.common.tile.PedestalTileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author KilaBash
 * @date 2022/7/20
 * @implNote PedestalRenderer
 */
public class PedestalRenderer extends MBDIModelRenderer{
    public PedestalRenderer() {
        super(new ResourceLocation(Multiblocked.MODID,"block/pedestal"));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasTESR(BlockEntity BlockEntity) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity BlockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (BlockEntity instanceof PedestalTileEntity tile) {
            ItemStack itemStack = tile.getItemStack();
            Minecraft mc = Minecraft.getInstance();
            if (!itemStack.isEmpty() && mc.level != null) {
                stack.pushPose();
                int i = itemStack.isEmpty() ? 187 : Item.getId(itemStack.getItem()) + itemStack.getDamageValue();
                mc.level.random.setSeed(i);
                BakedModel bakedmodel = mc.getItemRenderer().getModel(itemStack, tile.getLevel(), null, i);
                boolean flag = bakedmodel.isGui3d();

                float time = (System.currentTimeMillis() % 2400000) / 50f;
                float yOffset = Mth.sin(time / 10.0F) * 0.1F + 1.3F;
                stack.translate(0.5D, yOffset, 0.5D);
                float rotation = time * Mth.TWO_PI / 80;
                stack.mulPose(Vector3f.YP.rotation(rotation));
                stack.pushPose();
                mc.getItemRenderer().render(itemStack, ItemTransforms.TransformType.GROUND, false, stack, buffer, combinedLight, OverlayTexture.NO_OVERLAY, bakedmodel);
                stack.popPose();
                if (!flag) {
                    stack.translate(0.0, 0.0, 0.09375F);
                }
                stack.popPose();
            }
        }
    }
}
