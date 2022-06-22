package com.lowdragmc.multiblocked.common.capability.widget;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import javax.annotation.Nullable;
import java.math.BigInteger;

public class NumberContentWidget extends ContentWidget<Number> {
    protected boolean isDecimal;
    protected IGuiTexture contentTexture;
    protected String unit;

    public NumberContentWidget setContentTexture(IGuiTexture contentTexture) {
        this.contentTexture = contentTexture;
        return this;
    }

    public NumberContentWidget setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    @Nullable
    @Override
    public Object getJEIIngredient(Number content) {
        return null;
    }

    @Override
    protected void onContentUpdate() {
        isDecimal = content instanceof Float || content instanceof Double;
        this.setHoverTooltips(content + " " + unit);
    }

    @Override
    public void openConfigurator(WidgetGroup dialog) {
        super.openConfigurator(dialog);
        TextFieldWidget textFieldWidget;
        int x = 5;
        int y = 25;
        dialog.addWidget(new LabelWidget(5, y + 3, "multiblocked.gui.label.number"));
        dialog.addWidget(textFieldWidget = new TextFieldWidget(125 - 60, y, 60, 15,  null, number -> {
            if (content instanceof Float) {
                content = Float.parseFloat(number);
            } else if (content instanceof Double) {
                content = Double.parseDouble(number);
            } else if (content instanceof Integer) {
                content = Integer.parseInt(number);
            } else if (content instanceof Long) {
                content = Long.parseLong(number);
            } else if (content instanceof BigInteger) {
                content = new BigInteger(number);
            }
            onContentUpdate();
        }).setCurrentString(content.toString()));
        if (isDecimal) {
            textFieldWidget.setNumbersOnly(0f, Integer.MAX_VALUE);
        } else {
            if (content instanceof Long || content instanceof BigInteger) {
                textFieldWidget.setNumbersOnly(0, Long.MAX_VALUE);
            } else {
                textFieldWidget.setNumbersOnly(0, Integer.MAX_VALUE);
            }
        }
        dialog.addWidget(createButton(textFieldWidget, -10000, x, y + 66));
        dialog.addWidget(createButton(textFieldWidget, -100, x, y + 44));
        dialog.addWidget(createButton(textFieldWidget, -1, x, y + 22));
        dialog.addWidget(createButton(textFieldWidget, 1, x + 75, y + 22));
        dialog.addWidget(createButton(textFieldWidget, 100, x + 75, y + 44));
        dialog.addWidget(createButton(textFieldWidget, 10000, x + 75, y + 66));
    }

    private ButtonWidget createButton(TextFieldWidget textFieldWidget, int num, int x, int y) {
        return (ButtonWidget) new ButtonWidget(x, y, 45, 18,
                new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture((num >= 0 ? "+" : "") + num)),
                cd -> {
                    String number = textFieldWidget.getCurrentString();
                    Number newValue = null;
                    int scale = num * (cd.isShiftClick ? 10 : 1);
                    if (content instanceof Float) {
                        if (Float.parseFloat(number) + scale >= 0) {
                            newValue = Float.parseFloat(number) + scale;
                        }
                    } else if (content instanceof Double) {
                        if (Double.parseDouble(number) + scale >= 0) {
                            newValue = Double.parseDouble(number) + scale;
                        }
                    } else if (content instanceof Integer) {
                        if (Integer.parseInt(number) + scale >= 0) {
                            newValue = Integer.parseInt(number) + scale;
                        }
                    } else if (content instanceof Long) {
                        if (Long.parseLong(number) + scale >= 0) {
                            newValue = Long.parseLong(number) + scale;
                        }
                    } else if (content instanceof BigInteger) {
                        BigInteger add = new BigInteger(number).add(BigInteger.valueOf((scale)));
                        if (add.compareTo(BigInteger.ZERO) >= 0) {
                            newValue = add;
                        }
                    }
                    if (newValue != null) {
                        content = newValue;
                        onContentUpdate();
                        textFieldWidget.setCurrentString(newValue.toString());
                    }
                }).setHoverBorderTexture(1, -1).setHoverTooltips(
                LocalizationUtils.format("multiblocked.gui.shift_click") + ": " + (num >= 0 ? "+" : "") + num * 10);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (contentTexture != null) {
            contentTexture.updateTick();
        }
    }

    @Override
    public void drawHookBackground(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Position position = getPosition();
        Size size = getSize();
        if (contentTexture != null) {
            contentTexture.draw(matrixStack, mouseX, mouseY, position.x + 1, position.y + 1, size.width - 2, size.height - 2);
        }
        matrixStack.pushPose();
        matrixStack.scale(0.5f, 0.5f, 1);
        RenderSystem.disableDepthTest();
        String s = TextFormattingUtil.formatLongToCompactString(content.intValue(), 4);
        Font fontRenderer = Minecraft.getInstance().font;
        fontRenderer.drawShadow(matrixStack, s, (position.x + (size.width / 3f)) * 2 - fontRenderer.width(s) + 21, (position.y + (size.height / 3f) + 6) * 2, 0xFFFFFF);
        matrixStack.popPose();
    }
}
