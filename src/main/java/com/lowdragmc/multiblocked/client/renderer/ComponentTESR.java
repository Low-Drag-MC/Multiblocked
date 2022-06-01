package com.lowdragmc.multiblocked.client.renderer;

import com.lowdragmc.lowdraglib.client.renderer.ATESRRendererProvider;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@OnlyIn(Dist.CLIENT)
public class ComponentTESR extends ATESRRendererProvider<TileEntity> {

    public ComponentTESR(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

}
