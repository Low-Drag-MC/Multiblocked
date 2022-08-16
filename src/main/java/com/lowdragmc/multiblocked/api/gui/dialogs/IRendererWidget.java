package com.lowdragmc.multiblocked.api.gui.dialogs;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.registry.MbdRenderers;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IRendererWidget extends DialogWidget {
    public Consumer<IMultiblockedRenderer> onSave;
    public final DummyComponentTileEntity tileEntity;
    private final DraggableScrollableWidgetGroup group;
    private final IMultiblockedRenderer originalRenderer;
    private Runnable onUpdate;

    public IRendererWidget(WidgetGroup parent, IMultiblockedRenderer renderer, Consumer<IMultiblockedRenderer> onSave) {
        super(parent, true);
        this.onSave = onSave;
        this.originalRenderer = renderer;
        this.addWidget(new ImageWidget(0, 0, getSize().width, getSize().height, new ColorRectTexture(0xaf000000)));
        TrackedDummyWorld world = new TrackedDummyWorld();
        world.addBlock(BlockPos.ZERO, BlockInfo.fromBlockState(MbdComponents.DummyComponentBlock.defaultBlockState()));
        tileEntity = (DummyComponentTileEntity) world.getBlockEntity(BlockPos.ZERO);
        setNewRenderer(renderer);
        this.addWidget(new ImageWidget(35, 59, 138, 138, new GuiTextureGroup(new ColorBorderTexture(3, -1), new ColorRectTexture(0xaf444444))));
        this.addWidget(new SceneWidget(35, 59,  138, 138, world)
                .setRenderedCore(Collections.singleton(BlockPos.ZERO), null)
                .setRenderSelect(false)
                .setRenderFacing(false));
        this.addWidget(group = new DraggableScrollableWidgetGroup(181, 80, 180, 120));
        this.addWidget(new ButtonWidget(285, 55, 40, 20, this::onUpdate)
                .setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.tips.update", -1).setDropShadow(true))
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips("multiblocked.gui.tips.update"));
        this.addWidget(new ButtonWidget(330, 55, 45, 20, cd -> Minecraft.getInstance().reloadResourcePacks())
                .setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.tips.refresh", -1).setDropShadow(true))
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips("multiblocked.gui.dialogs.renderer.refresh"));
        this.addWidget(new SelectorWidget(181, 55, 100, 20, getRendererList(), -1)
                .setValue(getType(renderer))
                .setOnChanged(this::onChangeRenderer)
                .setButtonBackground(new ColorBorderTexture(1, -1), new ColorRectTexture(0xff444444))
                .setBackground(new ColorRectTexture(0xff999999))
                .setHoverTooltips("multiblocked.gui.dialogs.renderer.renderer"));
        onChangeRenderer(getType(renderer));
        if (onSave == null) return;
        this.addWidget(new ButtonWidget(285, 30, 40, 20, cd -> {
            if (tileEntity != null) {
                onSave.accept(tileEntity.getRenderer());
            }
            super.close();
        })
                .setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.tips.save_1", -1).setDropShadow(true))
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips("multiblocked.gui.tips.save"));
    }

    private List<String> getRendererList() {
        List<String> list = new ArrayList<>();
        list.add("multiblocked.renderer.null");
        MbdRenderers.RENDERER_REGISTRY.values().stream().map(IMultiblockedRenderer::getUnlocalizedName).forEach(list::add);
        return list;
    }

    @Override
    public void close() {
        super.close();
        if (onSave != null) {
            onSave.accept(originalRenderer);
        }
    }

    private void setNewRenderer(IMultiblockedRenderer newRenderer) {
        PartDefinition definition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, "i_renderer"));
        definition.getBaseStatus().setRenderer(newRenderer);
        tileEntity.setDefinition(definition);
    }

    private void onUpdate(ClickData clickData) {
        if (onUpdate != null) onUpdate.run();
    }

    private void onChangeRenderer(String s) {
        group.clearAllWidgets();
        onUpdate = null;
        IMultiblockedRenderer current = tileEntity.getRenderer();
        String[] split = s.split("\\.");
        s = split[split.length - 1];
        if (s.equals("null")) {
            onUpdate = () -> setNewRenderer(null);
        } else {
            IMultiblockedRenderer renderer = MbdRenderers.getRenderer(s);
            if (renderer != null) {
                Supplier<IMultiblockedRenderer> supplier = renderer.createConfigurator(this, group, current);
                if (supplier != null) {
                    onUpdate = () -> setNewRenderer(supplier.get());
                }
            }
        }
    }

    public static String getType(IMultiblockedRenderer renderer) {
        if (renderer != null) {
            return renderer.getUnlocalizedName();
        }
        return "multiblocked.renderer.null";
    }

}
