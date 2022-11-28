package com.lowdragmc.multiblocked.common.capability.widget;

import com.lowdragmc.lowdraglib.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.common.capability.ChemicalMekanismCapability;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.math.MathUtils;
import mekanism.client.gui.GuiUtils;
import mekanism.client.render.MekanismRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;

public class ChemicalStackWidget<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> extends ContentWidget<STACK> {
    public final ChemicalMekanismCapability<CHEMICAL, STACK> CAP;
    public IChemicalHandler<CHEMICAL, STACK> handler;
    public int index;
    public ChemicalStackWidget(ChemicalMekanismCapability<CHEMICAL, STACK> CAP) {
        this.CAP = CAP;
    }

    public ChemicalStackWidget(ChemicalMekanismCapability<CHEMICAL, STACK> CAP, IChemicalHandler<CHEMICAL, STACK> handler, int index, int x, int y) {
        this(CAP);
        this.setSelfPosition(x - 1, y - 1);
        this.handler = handler;
        this.index = index;
        setContent(handler.getChemicalInTank(index));
    }

    private void setContent(STACK stack) {
        setContent(IO.BOTH, stack, 1, false);
    }

    @Override
    protected void onContentUpdate() {
        if (isRemote() && content != null) {
            String chemical = LocalizationUtils.format(CAP.getUnlocalizedName());
            this.setHoverTooltips(
                    ChatFormatting.AQUA + content.getType().getTextComponent().getString() + ChatFormatting.RESET,
                    handler == null ?
                    LocalizationUtils.format("multiblocked.gui.trait.mek.amount", chemical, content.getAmount()) :
                    LocalizationUtils.format("multiblocked.gui.trait.mek.amount2", chemical, content.getAmount(), lastCapability));
        }
    }

    @Override
    public STACK getJEIContent(Object content) {
        return super.getJEIContent(content);
    }

    STACK lastStack;
    long lastCapability;

    @Override
    public void detectAndSendChanges() {
        if (handler != null) {
            if (handler.getTankCapacity(index) != lastCapability || lastStack == null || !handler.getChemicalInTank(index).isStackIdentical(lastStack)) {
                lastCapability = handler.getTankCapacity(index);
                lastStack = handler.getChemicalInTank(index);
                writeUpdateInfo(-3, this::writeTank);
            }
        }
    }

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        writeTank(buffer);
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        readTank(buffer);
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == -3) {
            readTank(buffer);
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    private void writeTank(FriendlyByteBuf buffer) {
        buffer.writeVarLong(lastCapability);
        lastStack.writeToPacket(buffer);
    }

    private void readTank(FriendlyByteBuf buffer) {
        lastCapability = buffer.readVarLong();
        lastStack = CAP.readFromBuffer.apply(buffer);
        setContent(lastStack);
    }

    @Override
    public void drawHookBackground(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        if (content != null && !content.isEmpty()) {
            Position pos = getPosition();
            Size size = getSize();
            Minecraft minecraft = Minecraft.getInstance();
            stack.pushPose();
            RenderSystem.enableBlend();
            drawChemical(stack, pos.x + 1, pos.y + 1, 18, 18, content);
            stack.scale(0.5f, 0.5f, 1);
            String s = TextFormattingUtil.formatLongToCompactStringBuckets(content.getAmount(), 3) + "B";
            Font fontRenderer = minecraft.font;
            fontRenderer.drawShadow(stack, s, (pos.x + (size.width / 3f)) * 2 - fontRenderer.width(s) + 21, (pos.y + (size.height / 3f) + 6) * 2, 0xFFFFFF);
            stack.popPose();
        }
    }

    public static void drawChemical(PoseStack matrix, int xPosition, int yPosition, int width, int height, @Nonnull ChemicalStack<?> stack) {
        int desiredHeight = MathUtils.clampToInt(height);
        if (desiredHeight < 1) {
            desiredHeight = 1;
        }

        if (desiredHeight > height) {
            desiredHeight = height;
        }

        Chemical<?> chemical = stack.getType();
        MekanismRenderer.color(chemical);
        GuiUtils.drawTiledSprite(matrix, xPosition, yPosition, height, width, desiredHeight, MekanismRenderer.getSprite(chemical.getIcon()), 16, 16, 100, GuiUtils.TilingDirection.UP_RIGHT, false);
        MekanismRenderer.resetColor();
    }

    @Override
    public void openConfigurator(WidgetGroup dialog) {
        super.openConfigurator(dialog);
        int x = 5;
        int y = 25;
        dialog.addWidget(new LabelWidget(5, y + 3, "multiblocked.gui.label.amount"));
        dialog.addWidget(new TextFieldWidget(125 - 60, y, 60, 15, null, number -> {
            content = CAP.copyInner(content);
            content.setAmount(Long.parseLong(number));
            onContentUpdate();
        }).setNumbersOnly(1L, Long.MAX_VALUE).setCurrentString(content.getAmount() + ""));
    }

}
