package com.lowdragmc.multiblocked.api.kubejs;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;


public class NBTUtils {
    //put value into tag0
    public static INBT putTagInto(CompoundNBT tag0, String key, INBT value) {
        return tag0.put(key, value);
    }

    //merge tag1 with tag0
    public static INBT mergeTags(CompoundNBT tag0, CompoundNBT tag1) {
        return tag0.merge(tag1);
    }
}
