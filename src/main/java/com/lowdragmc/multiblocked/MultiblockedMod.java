package com.lowdragmc.multiblocked;

import com.lowdragmc.multiblocked.client.ClientProxy;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MultiblockedMod.MODID)
public class MultiblockedMod {
    public static final String MODID = "multiblocked";
    public static final String MODNAME = "Multiblocked";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);

    public MultiblockedMod() {
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    }

}
