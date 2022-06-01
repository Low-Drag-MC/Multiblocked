package com.lowdragmc.multiblocked.api.gui.tester;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.gui.widget.TextBoxWidget;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.gui.controller.PageWidget;
import com.lowdragmc.multiblocked.api.pattern.JsonBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.predicates.PredicateComponent;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ControllerTileTesterEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

public class ControllerScriptWidget extends PageWidget {

    private static final ResourceTexture PAGE = new ResourceTexture("multiblocked:textures/gui/json_loader_page.png");
    private final ControllerTileTesterEntity controller;
    private final DraggableScrollableWidgetGroup jsonList;
    private final TextBoxWidget textBox;
    private final DraggableScrollableWidgetGroup tfGroup;
    private File selected;

    public ControllerScriptWidget(ControllerTileTesterEntity controller, TabContainer tabContainer) {
        super(PAGE, tabContainer); //176, 256
        this.controller = controller;
        this.addWidget(new ImageWidget(5, 5, 176 - 10, 150 - 55, ResourceBorderTexture.BORDERED_BACKGROUND_BLUE));
        this.addWidget(jsonList = new DraggableScrollableWidgetGroup(10, 10, 176 - 20, 150 - 10 - 55).setBackground(new ColorRectTexture(0xff000000)));
        this.addWidget(new ButtonWidget(5, 105, 20, 20, new ResourceTexture("multiblocked:textures/gui/save.png"), cd->{
            if (!cd.isRemote) return;
            try {
                File dir = new File(Multiblocked.location, "definition/controller");
                Desktop.getDesktop().open(dir.isDirectory() ? dir : dir.getParentFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.open_folder"));
        this.addWidget(new ButtonWidget(30, 105, 140, 20, null, this::loadJson).setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("load script", -1).setDropShadow(true)).setHoverBorderTexture(1, -1));
        tfGroup = new DraggableScrollableWidgetGroup(5, 130, 176 - 10, 120)
                .setBackground(new ColorRectTexture(0xaf444444))
                .setYScrollBarWidth(4)
                .setYBarStyle(null, new ColorRectTexture(-1));
        textBox = new TextBoxWidget(0, 0, 176 - 14, Collections.singletonList("")).setFontColor(-1).setShadow(true);
        tfGroup.addWidget(textBox);
        this.addWidget(tfGroup);
        updateList();
    }

    private void loadJson(ClickData clickData) {
        if (selected != null && clickData.isRemote) {
            JsonElement jsonElement = FileUtility.loadJson(selected);
            if (jsonElement != null) {
                try {
                    String recipeMap = jsonElement.getAsJsonObject().get("recipeMap").getAsString();
                    JsonBlockPattern pattern = Multiblocked.GSON.fromJson(jsonElement.getAsJsonObject().get("basePattern"), JsonBlockPattern.class);
                    ControllerDefinition definition = Multiblocked.GSON.fromJson(jsonElement, ControllerDefinition.class);
                    pattern.predicates.put("controller", new PredicateComponent(definition));
                    definition.basePattern = pattern.build();
                    for (File file : Optional.ofNullable(new File(Multiblocked.location, "recipe_map").listFiles((f, n) -> n.endsWith(".json"))).orElse(new File[0])) {
                        JsonObject config = (JsonObject) FileUtility.loadJson(file);
                        if (config != null && config.get("name").getAsString().equals(recipeMap)) {
                            definition.recipeMap = Multiblocked.GSON.fromJson(config, RecipeMap.class);
                            break;
                        }
                    }
                    controller.setDefinition(definition);
                    MbdComponents.TEST_DEFINITION_REGISTRY.put(definition.location, definition);
                    writeClientAction(-1, buffer -> buffer.writeUtf(definition.location.toString()));
                } catch (Exception e) {
                    Multiblocked.LOGGER.error("tester: error while loading the controller json {}", selected.getName(), e);
                }
                textBox.setContent(Collections.singletonList(Multiblocked.GSON_PRETTY.toJson(jsonElement)));
                tfGroup.computeMax();
            }
        }
    }

    private void updateList() {
        jsonList.clearAllWidgets();
        selected = null;
        File path = new File(Multiblocked.location, "definition/controller");
        if (!path.isDirectory()) {
            if (!path.mkdirs()) {
                return;
            }
        }
        for (File file : Optional.ofNullable(path.listFiles()).orElse(new File[0])) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                jsonList.addWidget(new SelectableWidgetGroup(0, 1 + jsonList.widgets.size() * 11, jsonList
                        .getSize().width, 10)
                        .setSelectedTexture(-1, -1)
                        .setOnSelected(W -> selected = file)
                        .addWidget(new ImageWidget(0, 0, jsonList.getSize().width, 10, new ColorRectTexture(0xff000000)))
                        .addWidget(new ImageWidget(0, 0, jsonList.getSize().width, 10, new TextTexture(file.getName().replace(".json", "")).setWidth(
                                jsonList.getSize().width).setType(TextTexture.TextType.ROLL))));
            }
        }
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        if (id == -1) {
            controller.setDefinition((ControllerDefinition)MbdComponents.TEST_DEFINITION_REGISTRY.get(new ResourceLocation(buffer.readUtf(Short.MAX_VALUE))));
        } else {
            super.handleClientAction(id, buffer);
        }
    }
}
