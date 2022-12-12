package com.lowdragmc.multiblocked.api.gui.editor;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author KilaBash
 * @date 2022/12/11
 * @implNote IngredientResource
 */
public class RecipeResource extends Resource<Integer> {
    public final static String RESOURCE_NAME = "multiblocked.gui.editor.group.recipe";
    @Override
    public String name() {
        return RESOURCE_NAME;
    }

    @Override
    public ResourceContainer<Integer, ? extends Widget> createContainer(ResourcePanel panel) {
        return new RecipeResourceContainer(this, panel);
    }

    @Nullable
    @Override
    public Tag serialize(Integer value) {
        return IntTag.valueOf(value);
    }

    @Override
    public Integer deserialize(Tag nbt) {
        return nbt instanceof IntTag intTag ? intTag.getAsInt() : -1;
    }

    private static class RecipeResourceContainer extends ResourceContainer<Integer, ImageWidget>{

        public RecipeResourceContainer(RecipeResource resource, ResourcePanel panel) {
            super(resource, panel);
            setDragging(key -> (IIdProvider) () -> key, key -> new TextTexture(key.get()));
        }

        @Override
        public void reBuild() {
            selected = null;
            container.clearAllWidgets();
            int width = (getSize().getWidth() - 16) / 2;
            int i = 0;
            for (var entry : resource.allResources()) {
                ImageWidget widget = new ImageWidget(width, 0, width, 15,
                        new GuiTextureGroup(ColorPattern.T_WHITE.rectTexture(),
                                new TextTexture("0").setSupplier(() -> {
                                    var project = panel.getEditor().getCurrentProject();
                                    if (project instanceof MBDProject mbdProject) {
                                        return mbdProject.root.getWidgetsById(Pattern.compile("^%s$".formatted(entry.getKey()))).size() + "";
                                    }
                                    return 0 + "";
                                })));
                widget.setHoverTooltips("multiblocked.gui.editor.tips.citation");
                widgets.put(entry.getKey(), widget);
                Size size = widget.getSize();
                SelectableWidgetGroup selectableWidgetGroup = new SelectableWidgetGroup(3, 3 + i * 17, width * 2, 15);
                selectableWidgetGroup.setDraggingProvider(draggingMapping == null ? entry::getValue : () -> draggingMapping.apply(entry.getKey()), (c, p) -> draggingRenderer.apply(c));
                selectableWidgetGroup.addWidget(new ImageWidget(0, 0, width, 15, new GuiTextureGroup(
                        new ColorRectTexture(entry.getValue()),
                        new TextTexture("" + entry.getKey() + " ").setWidth(size.width).setType(TextTexture.TextType.ROLL))));
                selectableWidgetGroup.addWidget(widget);
                selectableWidgetGroup.setOnSelected(s -> selected = entry.getKey());
                selectableWidgetGroup.setOnUnSelected(s -> selected = null);
                selectableWidgetGroup.setSelectedTexture(ColorPattern.T_GRAY.rectTexture());
                container.addWidget(selectableWidgetGroup);
                i++;
            }
        }

        @Override
        protected TreeBuilder.Menu getMenu() {
            var menu = TreeBuilder.Menu.start();
            menu.leaf(Icons.ADD_FILE, "multiblocked.gui.editor.menu.add_recipe", this::addNewResource);
            menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", this::removeSelectedResource);
            return menu;
        }

        @Override
        protected void addNewResource() {
            DialogWidget.showFileDialog(panel.getEditor(), "ldlib.gui.editor.tips.load_resource", new File(panel.getEditor().getWorkSpace(), "recipe_map"), true,
                    DialogWidget.suffixFilter(".json"), file -> {
                        if (file != null && file.isFile()) {
                            var json = FileUtility.loadJson(file);
                            if (json instanceof JsonObject jsonObject) {
                                var recipeMap = Multiblocked.GSON.fromJson(jsonObject, RecipeMap.class);
                                for (Recipe recipe : recipeMap.recipes.values()) {
                                    for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : recipe.inputs.entrySet()) {
                                        entry.getValue().stream().map(c->c.uiName).filter(Objects::nonNull).forEach(s -> resource.addResource(s, entry.getKey().color));
                                    }
                                    for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : recipe.outputs.entrySet()) {
                                        entry.getValue().stream().map(c->c.uiName).filter(Objects::nonNull).forEach(s -> resource.addResource(s, entry.getKey().color));
                                    }
                                    for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : recipe.tickInputs.entrySet()) {
                                        entry.getValue().stream().map(c->c.uiName).filter(Objects::nonNull).forEach(s -> resource.addResource(s, entry.getKey().color));
                                    }
                                    for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : recipe.tickOutputs.entrySet()) {
                                        entry.getValue().stream().map(c->c.uiName).filter(Objects::nonNull).forEach(s -> resource.addResource(s, entry.getKey().color));
                                    }
                                }
                                reBuild();
                            }
                        }
                    });
        }
    }

}
