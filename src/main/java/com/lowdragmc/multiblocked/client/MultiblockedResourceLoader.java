package com.lowdragmc.multiblocked.client;

import com.lowdragmc.multiblocked.Multiblocked;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.resources.FilePack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MultiblockedResourceLoader extends FilePack {

    public static final MultiblockedResourceLoader INSTANCE = new MultiblockedResourceLoader();

    private MultiblockedResourceLoader() {
        super(Multiblocked.location);
    }

}
