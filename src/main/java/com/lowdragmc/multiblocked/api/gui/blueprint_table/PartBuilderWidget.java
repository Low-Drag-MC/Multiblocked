package com.lowdragmc.multiblocked.api.gui.blueprint_table;

import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.gui.blueprint_table.components.PartWidget;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import jdk.nashorn.internal.ir.Block;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PartBuilderWidget extends WidgetGroup {
    DraggableScrollableWidgetGroup containers;
    DummyComponentTileEntity tileEntity;
    List<SelectableWidgetGroup> files = new ArrayList<>();

    public PartBuilderWidget() {
        super(0, 0, 384, 256);
        setClientSideWidget();
        if (!isRemote()) return;
        this.addWidget(0, new ImageWidget(0, 0, getSize().width, getSize().height, new ResourceTexture("multiblocked:textures/gui/blueprint_page.png")));
        this.addWidget(new ImageWidget(200 - 4, 30 - 4, 150 + 8, 190 + 8, ResourceBorderTexture.BORDERED_BACKGROUND_BLUE));
        this.addWidget(containers = new DraggableScrollableWidgetGroup(200, 30, 150, 190));
        this.addWidget(new ButtonWidget(200 - 4 - 20, 30, 20, 20, new ResourceTexture("multiblocked:textures/gui/save.png"), cd -> {
            if (cd.isRemote) {
                try {
                    File dir = new File(Multiblocked.location, "definition/part");
                    Desktop.getDesktop().open(dir.isDirectory() ? dir : dir.getParentFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.open_folder"));
        this.addWidget(new ButtonWidget(200 - 4 - 20, 51, 20, 20, new ResourceTexture("multiblocked:textures/gui/add.png"), cd -> {
            new PartWidget(this, new PartDefinition(new ResourceLocation("mod_id:component_id")), jsonObject -> {

                if (jsonObject != null) {
                    FileUtility.saveJson(new File(Multiblocked.location, "definition/part/" + jsonObject.get("location").getAsString().replace(":", "_") + ".json"), jsonObject);
                }
                updateList();
            });
        }).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.builder.part.create"));
        initScene();
        updateList();
    }

    @OnlyIn(Dist.CLIENT)
    private void initScene() {
        TrackedDummyWorld world = new TrackedDummyWorld();
        world.addBlock(BlockPos.ZERO, BlockInfo.fromBlockState(MbdComponents.DummyComponentBlock.defaultBlockState()));
        tileEntity = (DummyComponentTileEntity) world.getBlockEntity(BlockPos.ZERO);
        this.addWidget(new ImageWidget(30, 59, 138, 138, new GuiTextureGroup(new ColorBorderTexture(3, -1), new ColorRectTexture(0xaf444444))));
        this.addWidget(new SceneWidget(30, 59,  138, 138, world)
                .setRenderedCore(Collections.singleton(BlockPos.ZERO), null)
                .setRenderSelect(false)
                .setRenderFacing(false));
    }

    private void setNewRenderer(IMultiblockedRenderer newRenderer) {
        PartDefinition definition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, "i_renderer"));
        definition.baseRenderer = newRenderer;
        tileEntity.setDefinition(definition);
    }

    protected void updateList() {
        setNewRenderer(null);
        int size = files.size();
        files.forEach(containers::waitToRemoved);
        files.clear();
        File path = new File(Multiblocked.location, "definition/part");
        if (!path.isDirectory()) {
            if (!path.mkdirs()) {
                return;
            }
        }
        for (File file : Optional.ofNullable(path.listFiles((s, name) -> name.endsWith(".json"))).orElse(new File[0])) {
            SelectableWidgetGroup widgetGroup = (SelectableWidgetGroup) new SelectableWidgetGroup(0, (containers.widgets.size() - size) * 22, containers.getSize().width, 20)
                    .setOnSelected(group -> {
                        JsonElement jsonElement = FileUtility.loadJson(file);
                        if (jsonElement != null) {
                            try {
                                setNewRenderer(Multiblocked.GSON.fromJson(jsonElement, PartDefinition.class).baseRenderer);
                            } catch (Exception ignored) {}
                        }
                    })
                    .setSelectedTexture(-2, 0xff00aa00)
                    .addWidget(new ImageWidget(0, 0, 150, 20, new ColorRectTexture(0x4faaaaaa)))
                    .addWidget(new ButtonWidget(134, 4, 12, 12, new ResourceTexture("multiblocked:textures/gui/option.png"), cd -> {
                        JsonElement jsonElement = FileUtility.loadJson(file);
                        if (jsonElement != null) {
                            try {
                                PartDefinition definition = Multiblocked.GSON.fromJson(jsonElement, PartDefinition.class);
                                new PartWidget(this, definition, jsonObject -> {
                                    if (jsonObject != null) {
                                        FileUtility.saveJson(file, jsonObject);
                                    }
                                });
                            } catch (Exception ignored) {}
                        }
                    }).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.settings"))
                    .addWidget(new ImageWidget(32, 0, 100, 20, new TextTexture(file.getName().replace(".json", "")).setWidth(100).setType(TextTexture.TextType.ROLL)))
                    .addWidget(new ImageWidget(4, 2, 18, 18, new ItemStackTexture(Items.PAPER)));
            files.add(widgetGroup);
            containers.addWidget(widgetGroup);
        }
    }
}
