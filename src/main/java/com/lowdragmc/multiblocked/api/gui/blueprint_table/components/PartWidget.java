package com.lowdragmc.multiblocked.api.gui.blueprint_table.components;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.pattern.JsonBlockPattern;

import java.util.function.Consumer;

public class PartWidget extends ComponentWidget<PartDefinition>{
    protected JsonBlockPattern pattern;

    public PartWidget(WidgetGroup group, PartDefinition definition, Consumer<JsonObject> onSave) {
        super(group, definition, onSave);
        int x = 47;
        S1.addWidget(createBoolSwitch(x + 100, 90, "canShared", "multiblocked.gui.widget.part.shared=", definition.canShared, r -> definition.canShared = r));

    }
}
