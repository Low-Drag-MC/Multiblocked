package com.lowdragmc.multiblocked.api.pattern.predicates;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PredicateComponent extends SimplePredicate {
    public ResourceLocation
            location = new ResourceLocation("mod_id", "component_id");
    public ComponentDefinition definition;

    public PredicateComponent() {
        super("component");
    }

    public PredicateComponent(ComponentDefinition definition) {
        this(definition.location);
        this.definition = definition;
        buildPredicate();
    }

    public PredicateComponent(ResourceLocation location) {
        this();
        this.location = location;
        buildPredicate();
    }

    @Override
    public SimplePredicate buildPredicate() {
        predicate = state -> state.getTileEntity() instanceof ComponentTileEntity<?> && ((ComponentTileEntity<?>) state.getTileEntity()).getDefinition().location.equals(location);
        candidates = () -> {
            if (MbdComponents.COMPONENT_BLOCKS_REGISTRY.containsKey(location)) {
                return new BlockInfo[]{new BlockInfo(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(location).defaultBlockState(), true)};
            } else {
                if (definition == null) return new BlockInfo[0];
                return new BlockInfo[]{new BlockInfo(MbdComponents.DummyComponentBlock.defaultBlockState(), be -> {
                    if (be instanceof DummyComponentTileEntity dummy) {
                        dummy.setDefinition(definition);
                    }
                })};
            }
        };
        return this;
    }

    @Override
    public List<WidgetGroup> getConfigWidget(List<WidgetGroup> groups) {
        super.getConfigWidget(groups);
        WidgetGroup group = new WidgetGroup(0, 0, 100, 20);
        groups.add(group);
        group.addWidget(new LabelWidget(0, 0, "multiblocked.gui.label.component_registry_name"));
        group.addWidget(new SelectorWidget(0, 10, 120, 20, getAvailableComponents(), -1)
                .setValue(this.location.toString())
                .setOnChanged(name -> {
                    if (name != null && !name.isEmpty()) {
                        this.location = new ResourceLocation(name);
                        buildPredicate();
                    }
                })
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(new ColorRectTexture(0xff333333))
                .setHoverTooltips("multiblocked.gui.tips.component"));
        return groups;
    }

    private List<String> getAvailableComponents() {
        File dir = new File(Multiblocked.location, "definition/part");
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                return Arrays.stream(files).map(file -> {
                    try {
                        return FileUtility.loadJson(file).getAsJsonObject().get("location").getAsString();
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public JsonObject toJson(JsonObject jsonObject) {
        jsonObject.add("location", Multiblocked.GSON.toJsonTree(location));
        return super.toJson(jsonObject);
    }

    @Override
    public void fromJson(Gson gson, JsonObject jsonObject) {
        location = gson.fromJson(jsonObject.get("location"), ResourceLocation.class);
        super.fromJson(gson, jsonObject);
    }
}
