package com.lowdragmc.multiblocked.common.capability.trait;

import com.lowdragmc.multiblocked.api.capability.trait.PlayerCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.EMCProjectECapability;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.math.BigInteger;

public class EMCPlayerCapabilityTrait extends PlayerCapabilityTrait {

    public EMCPlayerCapabilityTrait() {
        super(EMCProjectECapability.CAP);
    }

    public IKnowledgeProvider getCapability() {
        Player player = getPlayer();
        return player == null ? null : player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY, null).orElse(null);
    }

    public BigInteger updateEMC(BigInteger emc, boolean simulate) {
        Player player = getPlayer();
        if (player instanceof ServerPlayer serverPlayer) {
            IKnowledgeProvider emcCap = getCapability();
            if (emcCap != null) {
                BigInteger stored = emcCap.getEmc();
                BigInteger emcL = stored.add(emc).max(BigInteger.ZERO);
                if (!simulate) {
                    emcCap.setEmc(emcL);
                    emcCap.sync(serverPlayer); // send to client
                }
                return emcL.subtract(stored.add(emc)).abs();
            }
        }
        return emc.abs();
    }
}