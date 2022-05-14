package com.lowdragmc.multiblocked.api.pattern.predicates;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.api.tile.ControllerTileTesterEntity;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.List;

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
        predicate = state -> state.getTileEntity() instanceof ComponentTileEntity<?> && ((ComponentTileEntity<?>) state.getTileEntity()).getDefinition().location.equals(location);
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
        group.addWidget(new TextFieldWidget(0, 10, 120, 20, null, s -> {
            if (s != null && !s.isEmpty()) {
                location = new ResourceLocation(s);
                buildPredicate();
            }
        }).setCurrentString(location.toString()));
        return groups;
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
