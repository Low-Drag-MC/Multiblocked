package com.lowdragmc.multiblocked.api.block;

import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Author: KilaBash
 * Date: 2022/04/23
 * Description:
 */
@ParametersAreNonnullByDefault
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
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
    }

    @Nonnull
    @Override
    public IRenderer getRenderer(ItemStack stack) {
        IRenderer renderer = getDefinition().baseRenderer;
        return renderer == null ? IRenderer.EMPTY : renderer;
    }
}
