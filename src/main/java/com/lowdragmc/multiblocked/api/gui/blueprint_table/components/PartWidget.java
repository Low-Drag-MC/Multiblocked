package com.lowdragmc.multiblocked.api.gui.blueprint_table.components;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.gui.GuiUtils;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PartWidget extends ComponentWidget<PartDefinition>{

    public PartWidget(WidgetGroup group, PartDefinition definition, Consumer<JsonObject> onSave) {
        super(group, definition, onSave);
        int x = 47;
        S1.addWidget(GuiUtils.createSelector(x, 75, "rotateState", "multiblocked.gui.widget.component.rotate_state", definition.properties.rotationState.name(), Arrays.stream(CustomProperties.RotationState.values()).map(Enum::name).collect(Collectors.toList()), r -> definition.properties.rotationState = CustomProperties.RotationState.valueOf(r)));
        S1.addWidget(GuiUtils.createBoolSwitch(x + 100, 150, "canShared", "multiblocked.gui.widget.part.shared", definition.canShared, r -> definition.canShared = r));
    }
}
