package com.lowdragmc.multiblocked.api.sound;

import com.lowdragmc.multiblocked.api.tile.IComponent;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @date 2022/8/16
 * @implNote ComponentSound
 */
@OnlyIn(Dist.CLIENT)
public class ComponentSound extends TickableSound {
    @Nullable
    public final IComponent component;
    public final SoundState soundState;

    protected ComponentSound(SoundEvent soundEvent, SoundState soundState, @Nullable IComponent component) {
        super(soundEvent, SoundCategory.BLOCKS);
        this.component = component;
        this.soundState = soundState;
        this.looping = soundState.loop;
        this.delay = soundState.delay;
        this.volume = soundState.volume;
        this.pitch = soundState.pitch;
        if (component == null) {
            this.attenuation = AttenuationType.NONE;
            return;
        }
        this.x = component.self().getBlockPos().getX() + 0.5;
        this.y = component.self().getBlockPos().getY() + 0.5;
        this.z = component.self().getBlockPos().getZ() + 0.5;
    }

    @Override
    public void tick() {
        if (component != null) {
            World level = component.self().getLevel();
            if (!component.getStatus().equals(soundState.status) || level == null || component.self().isRemoved() || level.getBlockEntity(component.self().getBlockPos()) != component) {
                stop();
            }
        }
    }

    public void stopSound() {
        super.stop();
    }

}
