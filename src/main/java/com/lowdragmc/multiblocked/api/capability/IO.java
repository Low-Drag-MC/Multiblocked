package com.lowdragmc.multiblocked.api.capability;

/**
 * The capability can be input or output or both
 */
public enum IO {
    IN,
    OUT,
    BOTH,
    NONE;

    public static final IO[] VALUES = IO.values();
}
