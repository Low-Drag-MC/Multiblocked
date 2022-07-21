package com.lowdragmc.multiblocked.api.block;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Author: KilaBash
 * Date: 2022/04/27
 * Description:
 */
public class CustomProperties {
    public boolean isOpaque;
    public float destroyTime;
    public float explosionResistance;
    public int lightEmissive;
    public float speedFactor;
    public float jumpFactor;
    public float friction;
    public boolean hasCollision;
    public String tabGroup;
    public int stackSize;
    public transient VoxelShape shape;

    public CustomProperties() {
        this.isOpaque = true;
        this.destroyTime = 1.5f;
        this.explosionResistance = 6f;
        this.lightEmissive = 0;
        this.speedFactor = 1f;
        this.jumpFactor = 1f;
        this.friction = 0.6f;
        this.hasCollision = true;
        this.tabGroup = "multiblocked.all";
        this.stackSize = 64;
        this.shape = Shapes.block();
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
        properties.strength(destroyTime, explosionResistance)
                .sound(SoundType.STONE)
                .explosionResistance(6.0f)
                .speedFactor(speedFactor)
                .jumpFactor(jumpFactor)
                .friction(friction)
                .lightLevel(s->lightEmissive);
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
}
