package com.lowdragmc.multiblocked.api.gui.blueprint_table;

import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.gui.dialogs.RecipeMapWidget;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import net.minecraft.util.Util;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class RecipeMapBuilderWidget extends WidgetGroup {
    private DraggableScrollableWidgetGroup recipeMapList;
    private File selected;
    private Consumer<RecipeMap> onRecipeMapSelected;
    private final WidgetGroup parent;

    public RecipeMapBuilderWidget(WidgetGroup parent, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.setClientSideWidget();
        this.parent = parent;
        if (!isRemote()) return;
        this.addWidget(new ImageWidget(20, 0, width - 20, height, ResourceBorderTexture.BORDERED_BACKGROUND_BLUE));
        this.addWidget(recipeMapList = new DraggableScrollableWidgetGroup(20, 4, width - 20, height - 8));
        this.addWidget(new ButtonWidget(0, 5, 20, 20, new ResourceTexture("multiblocked:textures/gui/save.png"), cd->{
            File dir = new File(Multiblocked.location, "recipe_map");
            Util.getPlatform().openFile(dir.isDirectory() ? dir : dir.getParentFile());
        }).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.open_folder"));
        this.addWidget(new ButtonWidget(0, 26, 20, 20, new ResourceTexture("multiblocked:textures/gui/add.png"), cd-> new RecipeMapWidget(parent, new RecipeMap(UUID.randomUUID().toString()), recipeMap -> {
            if (recipeMap != null) {
                File path = new File(Multiblocked.location, "recipe_map/" + recipeMap.name + ".json");
                JsonElement element = Multiblocked.GSON.toJsonTree(recipeMap);
                FileUtility.saveJson(path, element);
                updateRecipeMapList();
            }
        })).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.builder.recipe_map.create"));
        updateRecipeMapList();
    }

    public RecipeMapBuilderWidget setOnRecipeMapSelected(Consumer<RecipeMap> onRecipeMapSelected) {
        this.onRecipeMapSelected = onRecipeMapSelected;
        return this;
    }

    private void updateRecipeMapList() {
        recipeMapList.clearAllWidgets();
        if (onRecipeMapSelected != null) {
            onRecipeMapSelected.accept(RecipeMap.EMPTY);
        }
        selected = null;
        File path = new File(Multiblocked.location, "recipe_map");
        if (!path.isDirectory()) {
            if (!path.mkdirs()) {
                return;
            }
        }
        for (File file : Optional.ofNullable(path.listFiles()).orElse(new File[0])) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                recipeMapList.addWidget(new SelectableWidgetGroup(5, 1 + recipeMapList.widgets.size() * 22, getSize().width - 30, 20)
                        .setSelectedTexture(-2, 0xff00aa00)
                        .setOnSelected(W -> {
                            selected = file;
                            if (onRecipeMapSelected != null) {
                                onRecipeMapSelected.accept(Multiblocked.GSON.fromJson(FileUtility.loadJson(file), RecipeMap.class));
                            }
                        })
                        .addWidget(new ImageWidget(0, 0, 120, 20, new ColorRectTexture(0x4faaaaaa)))
                        .addWidget(new ButtonWidget(104, 4, 12, 12, new ResourceTexture("multiblocked:textures/gui/option.png"), cd -> new RecipeMapWidget(parent, Multiblocked.GSON.fromJson(FileUtility.loadJson(file), RecipeMap.class), recipeMap -> {
                            if (recipeMap != null) {
                                if (selected == file) {
                                    if (onRecipeMapSelected != null) {
                                        onRecipeMapSelected.accept(recipeMap);
                                    }
                                }
                                JsonElement element = Multiblocked.GSON.toJsonTree(recipeMap);
                                FileUtility.saveJson(file, element);
                            }
                        })).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.settings"))
                        .addWidget(new ImageWidget(2, 0, 96, 20, new TextTexture(file.getName().replace(".json", "")).setWidth(96).setType(
                                TextTexture.TextType.ROLL))));
            }
        }
    }

}
