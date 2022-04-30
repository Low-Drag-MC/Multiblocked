package com.lowdragmc.multiblocked.network.s2c;

import com.lowdragmc.lowdraglib.networking.IPacket;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

public class SPacketRemoveDisabledRendering implements IPacket {
    private BlockPos controllerPos;

    public SPacketRemoveDisabledRendering() {
    }

    public SPacketRemoveDisabledRendering(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeVarLong(controllerPos.asLong());
    }

    @Override
    public void decode(PacketBuffer buf) {
        this.controllerPos = BlockPos.of(buf.readVarLong());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void execute(NetworkEvent.Context handler) {
        MultiblockWorldSavedData.removeDisableModel(controllerPos);
    }

}
