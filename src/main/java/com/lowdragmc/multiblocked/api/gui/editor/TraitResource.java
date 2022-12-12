package com.lowdragmc.multiblocked.api.gui.editor;

import com.google.gson.JsonElement;
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
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author KilaBash
 * @date 2022/12/11
 * @implNote TraitResource
 */
public class TraitResource extends Resource<Integer> {
    public final static String RESOURCE_NAME = "multiblocked.gui.editor.group.trait";
    @Override
    public String name() {
        return RESOURCE_NAME;
    }

    @Override
    public ResourceContainer<Integer, ? extends Widget> createContainer(ResourcePanel panel) {
        return new TraitResourceContainer(this, panel);
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

    private static class TraitResourceContainer extends ResourceContainer<Integer, ImageWidget>{

        public TraitResourceContainer(TraitResource resource, ResourcePanel panel) {
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
            menu.leaf(Icons.ADD_FILE, "multiblocked.gui.editor.menu.add_trait", this::addNewResource);
            menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", this::removeSelectedResource);
            return menu;
        }

        @Override
        protected void addNewResource() {
            DialogWidget.showFileDialog(panel.getEditor(), "ldlib.gui.editor.tips.load_resource", new File(panel.getEditor().getWorkSpace(), "definition"), true,
                    DialogWidget.suffixFilter(".json"), file -> {
                        if (file != null && file.isFile()) {
                            var json = FileUtility.loadJson(file);
                            if (json instanceof JsonObject definition) {
                                var traits = GsonHelper.getAsJsonObject(definition, "traits", new JsonObject());
                                for (Map.Entry<String, JsonElement> entry : traits.entrySet()) {
                                    MultiblockCapability<?> capability = MbdCapabilities.get(entry.getKey());
                                    if (capability != null && capability.hasTrait()) {
                                        CapabilityTrait trait = capability.createTrait();
                                        trait.serialize(entry.getValue());
                                        for (String slotName : trait.getSlotNames()) {
                                            resource.addResource(slotName, trait.capability == null ? -1 : trait.capability.color);
                                        }
                                    }
                                }
                                reBuild();
                            }
                        }
                    });
        }
    }

}
