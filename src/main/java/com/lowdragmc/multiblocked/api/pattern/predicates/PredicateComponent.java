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
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ControllerTileTesterEntity;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import com.lowdragmc.multiblocked.client.renderer.impl.CycleBlockStateRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PredicateComponent extends SimplePredicate {
    public ResourceLocation location = new ResourceLocation("mod_id", "component_id");
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
        predicate = state -> {
            TileEntity tileEntity = state.getTileEntity();
            if (tileEntity instanceof IComponent) {
                return ((IComponent) tileEntity).getDefinition().location.equals(location);
            }
            return false;
        };
        candidates = () -> {
            if (MbdComponents.COMPONENT_BLOCKS_REGISTRY.containsKey(location)) {
                return new BlockInfo[]{new BlockInfo(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(location).defaultBlockState(), MbdComponents.DEFINITION_REGISTRY.get(location).createNewTileEntity())};
            } else {
                if (definition == null) return new BlockInfo[0];
                if (definition instanceof ControllerDefinition){
                    ControllerTileTesterEntity te = new ControllerTileTesterEntity(ControllerTileTesterEntity.DEFAULT_DEFINITION);
                    te.setDefinition((ControllerDefinition) definition);
                    return new BlockInfo[]{new BlockInfo(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(ControllerTileTesterEntity.DEFAULT_DEFINITION.location).defaultBlockState(), te)};
                } else {
                    DummyComponentTileEntity te = new DummyComponentTileEntity(MbdComponents.DummyComponentBlock.definition);
                    te.setDefinition(definition);
                    return new BlockInfo[]{new BlockInfo(MbdComponents.DummyComponentBlock.defaultBlockState(), te)};
                }
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
        Set<String> components = new HashSet<>();
        for (ComponentDefinition value : MbdComponents.DEFINITION_REGISTRY.values()) {
            if (value.baseRenderer instanceof CycleBlockStateRenderer) continue;
            if (value instanceof ControllerDefinition) continue;
            components.add(value.location.toString());
        }
        File dir = new File(Multiblocked.location, "definition/part");
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.stream(files).map(file -> {
                    try {
                        return FileUtility.loadJson(file).getAsJsonObject().get("location").getAsString();
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(Objects::nonNull).forEach(components::add);
            }
        }
        return new ArrayList<>(components);
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
