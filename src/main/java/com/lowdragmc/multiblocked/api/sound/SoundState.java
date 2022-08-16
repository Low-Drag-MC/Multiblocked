package com.lowdragmc.multiblocked.api.sound;

import com.lowdragmc.multiblocked.api.tile.IComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @date 2022/8/16
 * @implNote SoundState
 */
public class SoundState {
    public static final SoundState EMPTY = new SoundState(new ResourceLocation("multiblocked:empty"));
    private transient boolean init;
    private transient SoundEvent soundEvent;
    public transient String status;
    public final ResourceLocation sound;
    public boolean loop;
    public int delay;
    public float volume;
    public float pitch;

    public SoundState(ResourceLocation sound) {
        this.sound = sound;
        this.loop = false;
        this.delay = 0;
        this.volume = 1;
        this.pitch = 1;
    }

    public SoundState copy(ResourceLocation sound) {
        SoundState soundState = new SoundState(sound);
        soundState.loop = this.loop;
        soundState.delay = this.delay;
        soundState.volume = this.volume;
        soundState.pitch = this.pitch;
        return soundState;
    }

    public SoundState copy() {
        if (this == EMPTY) return EMPTY;
        return copy(sound);
    }

    @Nullable
    public SoundEvent getSoundEvent() {
        if (this == EMPTY) return null;
        if (soundEvent == null && !init) {
            init = true;
            soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(sound);
        }
        return soundEvent;
    }

    @OnlyIn(Dist.CLIENT)
    public ComponentSound playGUISound() {
        if (this == EMPTY) return null;
        SoundEvent sound = getSoundEvent();
        ComponentSound componentSound = null;
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().play(componentSound = new ComponentSound(sound, this, null));
        }
        return componentSound;
    }

    @OnlyIn(Dist.CLIENT)
    public ComponentSound playSound(IComponent component) {
        if (this == EMPTY) return null;
        SoundEvent sound = getSoundEvent();
        ComponentSound componentSound = null;
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().play(componentSound = new ComponentSound(sound, this, component));
        }
        return componentSound;
    }

    public boolean playLevelSound(Level level, BlockPos pos) {
        if (this == EMPTY) return false;
        SoundEvent sound = getSoundEvent();
        if (sound == null) return false;
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
        return true;
    }
}
