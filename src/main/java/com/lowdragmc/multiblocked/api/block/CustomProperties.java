package com.lowdragmc.multiblocked.api.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Direction;
import net.minecraftforge.common.ToolType;

import java.util.function.Predicate;

/**
 * Author: KilaBash
 * Date: 2022/04/27
 * Description:
 */
public class CustomProperties {
    public RotationState rotationState;
    public boolean showInJei;
    public boolean isOpaque;
    public boolean hasDynamicShape;
    public boolean hasCollision;
    public float destroyTime;
    public float explosionResistance;
    public float speedFactor;
    public float jumpFactor;
    public float friction;
    public int harvestLevel;
    public int stackSize;
    public String tabGroup;

    public CustomProperties() {
        this.isOpaque = true;
        this.destroyTime = 1.5f;
        this.explosionResistance = 6f;
        this.harvestLevel = 1;
        this.speedFactor = 1f;
        this.jumpFactor = 1f;
        this.friction = 0.6f;
        this.hasCollision = true;
        this.tabGroup = "multiblocked.all";
        this.stackSize = 64;
        this.hasDynamicShape = false;
        this.rotationState = RotationState.ALL;
        this.showInJei = true;
    }

    public AbstractBlock.Properties createBlock() {
        AbstractBlock.Properties properties = AbstractBlock.Properties.of(Material.METAL);
        if (!isOpaque) {
            properties.noOcclusion();
        }
        if (!hasCollision) {
            properties.noCollission();
        }
        if (hasDynamicShape) {
            properties.dynamicShape();
        }
        properties.strength(destroyTime, explosionResistance)
                .sound(SoundType.STONE)
                .harvestLevel(harvestLevel)
                .speedFactor(speedFactor)
                .jumpFactor(jumpFactor)
                .friction(friction)
                .harvestTool(ToolType.PICKAXE);
        return properties;
    }

    public Item.Properties createItem() {
        Item.Properties properties = new Item.Properties().stacksTo(stackSize);
        if (tabGroup != null) {
            for (ItemGroup tab : ItemGroup.TABS) {
                if (tab.getRecipeFolderName().equals(tabGroup)) {
                    properties.tab(tab);
                    break;
                }
            }
        }
        return properties;
    }

    public enum RotationState implements Predicate<Direction> {
        ALL(dir -> true),
        NONE(dir -> false),
        Y_AXIS(dir -> dir.getAxis() == Direction.Axis.Y),
        NON_Y_AXIS(dir -> dir.getAxis() != Direction.Axis.Y);

        final Predicate<Direction> predicate;

        RotationState(Predicate<Direction> predicate){
            this.predicate = predicate;
        }

        @Override
        public boolean test(Direction dir) {
            return predicate.test(dir);
        }
    }
}
