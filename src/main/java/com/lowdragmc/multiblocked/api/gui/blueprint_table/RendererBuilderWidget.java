package com.lowdragmc.multiblocked.api.gui.blueprint_table;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.ShaderTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.gui.dialogs.IRendererWidget;
import com.lowdragmc.multiblocked.api.gui.dialogs.IShaderWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;

public class RendererBuilderWidget extends WidgetGroup {
    public final ShaderTexture shaderTexture;

    public RendererBuilderWidget() {
        super(0, 0, 384, 256);
        setClientSideWidget();
        this.addWidget(0, new ImageWidget(0, 0, getSize().width, getSize().height, new ResourceTexture("multiblocked:textures/gui/blueprint_page.png")));
        this.addWidget(new ButtonWidget(40, 40, 40, 40, new ItemStackTexture(new ItemStack(Blocks.BEACON)), this::renderer).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.builder.renderer.irenderer"));
//        this.addWidget(new ButtonWidget(90, 40, 40, 40, new ResourceTexture("multiblocked:textures/fx/fx.png"), this::particle).setHoverBorderTexture(1, -1).setHoverTooltip("multiblocked.gui.builder.renderer.particle"));
        this.addWidget(new ButtonWidget(140, 40, 40, 40, shaderTexture = ShaderTexture.createShader(new ResourceLocation(Multiblocked.MODID, "fbm")), this::shader).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.builder.renderer.shader"));
    }

    private void shader(ClickData clickData) {
        new IShaderWidget(this, shaderTexture.getRawShader());
    }
//
//    private void particle(ClickData clickData) {
//        new IParticleWidget(this);
//    }

    private void renderer(ClickData clickData) {
        new IRendererWidget(this, null, null);
    }

    @Override
    public void setGui(ModularUI gui) {
        super.setGui(gui);
        if (gui != null && shaderTexture != null) {
            gui.registerCloseListener(shaderTexture::dispose);
        }
    }

}
