package com.lowdragmc.multiblocked.api.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

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

    public BlockBehaviour.Properties createBlock() {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of(
                Material.METAL);
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
                .explosionResistance(6.0f)
                .speedFactor(speedFactor)
                .jumpFactor(jumpFactor)
                .friction(friction);
        return properties;
    }

    public Item.Properties createItem() {
        Item.Properties properties = new Item.Properties().stacksTo(stackSize);
        if (tabGroup != null) {
            for (CreativeModeTab tab : CreativeModeTab.TABS) {
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
