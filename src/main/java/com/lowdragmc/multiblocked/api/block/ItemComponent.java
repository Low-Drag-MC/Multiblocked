package com.lowdragmc.multiblocked.api.block;

import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: KilaBash
 * Date: 2022/04/23
 * Description:
 */
public class ItemComponent extends BlockItem implements IItemRendererProvider {

    public ItemComponent(BlockComponent block) {
        super(block, block.definition.getItemProperties());
        setRegistryName(block.definition.location);
    }

    public ComponentDefinition getDefinition() {
        return ((BlockComponent)getBlock()).definition;
    }

    @Nullable
    @Override
    public String getCreatorModId(@Nonnull ItemStack itemStack) {
        return getDefinition().location.getNamespace();
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable World pLevel, @Nonnull List<ITextComponent> pTooltip, @Nonnull ITooltipFlag pFlag) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
    }

    @Nonnull
    @Override
    public IRenderer getRenderer(ItemStack stack) {
        IRenderer renderer = getDefinition().getBaseRenderer();
        return renderer == null ? IRenderer.EMPTY : renderer;
    }
}
