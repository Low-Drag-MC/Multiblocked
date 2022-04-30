package com.lowdragmc.multiblocked.api.gui.recipe;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

public class ProgressWidget extends Widget {
    public final static DoubleSupplier JEIProgress = () -> Math.abs(System.currentTimeMillis() % 2000) / 2000.;

    public final DoubleSupplier progressSupplier;
    private IGuiTexture emptyBarArea;
    private IGuiTexture filledBarArea;
    private Function<Double, String> dynamicHoverTips;

    private double lastProgressValue;

    public ProgressWidget(DoubleSupplier progressSupplier, int x, int y, int width, int height, ResourceTexture fullImage) {
        super(new Position(x, y), new Size(width, height));
        this.progressSupplier = progressSupplier;
        this.emptyBarArea = fullImage.getSubTexture(0.0, 0.0, 1.0, 0.5);
        this.filledBarArea = fullImage.getSubTexture(0.0, 0.5, 1.0, 0.5);
        this.lastProgressValue = -1;
    }

    public ProgressWidget(DoubleSupplier progressSupplier, int x, int y, int width, int height) {
        super(new Position(x, y), new Size(width, height));
        this.progressSupplier = progressSupplier;
    }

    public ProgressWidget setProgressBar(IGuiTexture emptyBarArea, IGuiTexture filledBarArea) {
        this.emptyBarArea = emptyBarArea;
        this.filledBarArea = filledBarArea;
        return this;
    }

    public ProgressWidget setDynamicHoverTips(Function<Double, String> hoverTips) {
        this.dynamicHoverTips = hoverTips;
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Position pos = getPosition();
        Size size = getSize();
        if (emptyBarArea != null) {
            emptyBarArea.draw(matrixStack, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
        }
        if (filledBarArea != null) {
            if (progressSupplier == JEIProgress) {
                lastProgressValue = progressSupplier.getAsDouble();
                if (dynamicHoverTips != null) {
                    setHoverTooltips(dynamicHoverTips.apply(lastProgressValue));
                }
            }
            filledBarArea.drawSubArea(matrixStack, pos.x, pos.y, (int) (size.width * (lastProgressValue < 0 ? 0 : lastProgressValue)), size.height,
                    0, 0, ((int) (size.width * (lastProgressValue < 0 ? 0 : lastProgressValue))) / (size.width * 1f), 1);
        }
    }

    @Override
    public void detectAndSendChanges() {
        double actualValue = progressSupplier.getAsDouble();
        if (actualValue - lastProgressValue != 0) {
            this.lastProgressValue = actualValue;
            writeUpdateInfo(0, buffer -> buffer.writeDouble(actualValue));
        }
    }

    @Override
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        if (id == 0) {
            this.lastProgressValue = buffer.readDouble();
            if (dynamicHoverTips != null) {
                setHoverTooltips(dynamicHoverTips.apply(lastProgressValue));
            }
        }
    }

}
