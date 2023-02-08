package com.lowdragmc.multiblocked.api.gui.dialogs;


import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.ShaderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextBoxWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.multiblocked.Multiblocked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

public class IShaderWidget extends DialogWidget {
    private final static String HELP = "" +
            "§2#version 120§r\n" +
            "\n" +
            "§2uniform§r §3vec2§r iResolution; §7// iResolution as Shadertoy (width, height)§r\n" +
            "§2uniform§r §3vec2§r iMouse; §7// iMouse as Shadertoy (mouseX, mouseY)§r\n" +
            "§2uniform§r §3float§r iTime; §7// iTime as Shadertoy (second)§r\n" +
            "\n" +
            "§3void§r mainImage( out §3vec4§r fragColor, in §3vec2§r fragCoord) {\n" +
            "    §7// write shader here like Shadertoy§r\n" +
            "}\n" +
            "\n" +
            "§3void§r main() {\n" +
            "    mainImage(gl_FragColor.rgba, §3vec2§r(gl_TexCoord[0].x * iResolution.x, gl_TexCoord[0].y * iResolution.y));\n" +
            "}\n";
    public final ShaderTexture shaderTexture;

    public IShaderWidget(WidgetGroup parent, String init) {
        super(parent, true);
        this.addWidget(new ImageWidget(0, 0, getSize().width, getSize().height, new ColorRectTexture(0xaf000000)));
        this.addWidget(new ImageWidget(35, 59, 138, 138, new GuiTextureGroup(new ColorBorderTexture(3, -1),
                shaderTexture = ShaderTexture.createRawShader(init))));
        TextFieldWidget textFieldWidget = new TextFieldWidget(181, 55, 120, 20,  null, null).setCurrentString(init);
        DraggableScrollableWidgetGroup tfGroup = new DraggableScrollableWidgetGroup(181, 80, 170, 120)
                .setBackground(new ColorRectTexture(0x3faaaaaa))
                .setYScrollBarWidth(4)
                .setYBarStyle(null, new ColorRectTexture(-1));
        TextBoxWidget textBox = new TextBoxWidget(0, 0, 165, Collections.singletonList(init)).setFontColor(-1).setShadow(true);

        this.addWidget(tfGroup);
        this.addWidget(textFieldWidget);
        this.addWidget(new ButtonWidget(305, 55, 40, 20, cd -> {
            try {
                textBox.setContent(Collections.singletonList(textFieldWidget.getCurrentString()));
                shaderTexture.updateRawShader(textFieldWidget.getCurrentString());
            } catch (Throwable e) {
                textBox.setContent(Collections.singletonList(e.getMessage()));
            }
            tfGroup.computeMax();
        })
                .setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.tips.update", -1).setDropShadow(true))
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips("multiblocked.gui.tips.update"));
        File path = new File(Multiblocked.location, "assets/multiblocked/shaders");
        this.addWidget(new ButtonWidget(350, 55, 20, 20, cd -> DialogWidget.showFileDialog(this, "select a shader file", path, true,
                DialogWidget.suffixFilter(".frag", ".fsh"), r -> {
                    if (r != null && r.isFile()) {
                        try {
                            textFieldWidget.setCurrentString(FileUtility.readInputStream(Files.newInputStream(r.toPath())));
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }
                }))
                .setButtonTexture(new ResourceTexture("multiblocked:textures/gui/darkened_slot.png"), new TextTexture("F", -1))
                .setHoverTooltips("multiblocked.gui.dialogs.renderer.shader"));
        tfGroup.addWidget(textBox);

        this.addWidget(new ButtonWidget(305, 15, 40, 20, cd -> new DialogWidget(this, true)
                .addWidget(new ImageWidget(0, 0, getSize().width, getSize().height, new ColorRectTexture(0xdf000000)))
                .addWidget(new TextBoxWidget(2, 2, getSize().width - 4, Collections.singletonList(HELP)).setFontColor(-1).setShadow(true))
                .addWidget(new ImageWidget(0, 0, getSize().width, getSize().height, new ColorBorderTexture(1, -1))))
                .setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.tips.help", -1).setDropShadow(true))
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips("multiblocked.gui.tips.help"));
    }

    @Override
    public void setGui(ModularUI gui) {
        super.setGui(gui);
        if (gui == null && shaderTexture != null) {
            shaderTexture.dispose();
        }
        if (gui != null && shaderTexture != null) {
            gui.registerCloseListener(shaderTexture::dispose);
        }
    }
}
