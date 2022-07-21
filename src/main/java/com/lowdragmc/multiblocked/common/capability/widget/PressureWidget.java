package com.lowdragmc.multiblocked.common.capability.widget;

import com.google.common.collect.Lists;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.multiblocked.common.capability.trait.PneumaticMachineTrait;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author KilaBash
 * @date 2022/6/30
 * @implNote PressureWidget
 */
public class PressureWidget extends Widget {

    private float dangerPressure;
    private float criticalPressure;
    private float pressure;
    private int volume;
    private int air;
    private final PneumaticMachineTrait trait;

    public PressureWidget(int xPosition, int yPosition, float dangerPressure, float criticalPressure, float pressure, int volume, int air, PneumaticMachineTrait trait) {
        super(xPosition, yPosition, 44, 44);
        this.dangerPressure = dangerPressure;
        this.criticalPressure = criticalPressure;
        this.pressure = pressure;
        this.volume = volume;
        this.air = air;
        this.trait = trait;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(poseStack, mouseX, mouseY, partialTicks);
        PressureGaugeRenderer2D.drawPressureGauge(poseStack, Minecraft.getInstance().font, -1.0f, criticalPressure, dangerPressure,  -3.4028235E38F, pressure, getPosition().x, getPosition().y, 0);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if (gui != null && isMouseOverElement(mouseX + 22, mouseY + 22)) {
            RenderSystem.enableDepthTest();
            List<Component> tooltip = Lists.newArrayList(
                    new TranslatableComponent("multiblocked.gui.trait.pressure.current", pressure),
                    new TranslatableComponent("multiblocked.gui.trait.pressure.air", air),
                    new TranslatableComponent("multiblocked.gui.trait.pressure.volume", volume)
            );
            setHoverTooltips(tooltip);
        }
    }

    @Override
    public void detectAndSendChanges() {
        if (trait.dangerPressure - dangerPressure != 0) {
            dangerPressure = trait.dangerPressure;
            writeUpdateInfo(0, buffer -> buffer.writeFloat(dangerPressure));
        }
        if (trait.criticalPressure - criticalPressure != 0) {
            criticalPressure = trait.criticalPressure;
            writeUpdateInfo(1, buffer -> buffer.writeFloat(criticalPressure));
        }
        IAirHandler airHandler = trait.getAirHandler();
        if (airHandler.getPressure() - pressure != 0) {
            pressure = airHandler.getPressure();
            writeUpdateInfo(2, buffer -> buffer.writeFloat(pressure));
        }
        if (airHandler.getVolume() != volume) {
            volume = airHandler.getVolume();
            writeUpdateInfo(3, buffer -> buffer.writeInt(volume));
        }
        if (airHandler.getAir() != air) {
            air = airHandler.getAir();
            writeUpdateInfo(4, buffet -> buffet.writeInt(air));
        }
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        switch (id) {
            case 0 -> dangerPressure = buffer.readFloat();
            case 1 -> criticalPressure = buffer.readFloat();
            case 2 -> pressure = buffer.readFloat();
            case 3 -> volume = buffer.readInt();
            case 4 -> air = buffer.readInt();
        }
    }
}
