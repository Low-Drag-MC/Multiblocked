package com.lowdragmc.multiblocked.common.gui.component;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.gui.GuiUtils;
import com.lowdragmc.multiblocked.api.gui.blueprint_table.components.ComponentWidget;
import com.lowdragmc.multiblocked.common.definition.CreatePartDefinition;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public class CreatePartWidget extends ComponentWidget<CreatePartDefinition> {

    public CreatePartWidget(WidgetGroup group, CreatePartDefinition definition, Consumer<JsonObject> onSave) {
        super(group, definition, onSave);
        int x = 47;
        S1.addWidget(GuiUtils.createBoolSwitch(x + 140, 165, "isOutput", "multiblocked.gui.widget.part.create.output", definition.isOutput, r -> definition.isOutput = r));
        S1.addWidget(new TextFieldWidget(x + 100, 182, 60, 11,  null, stress -> definition.stress = Mth.clamp(Float.parseFloat(stress), 1, 256)).setNumbersOnly(1, 256).setCurrentString(definition.stress + ""));
        S1.addWidget(new LabelWidget(x + 165, 184, "basic Stress").setDrop(true));
        S3.clearAllWidgets();
    }

    @Override
    protected JsonObject getJsonObj() {
        JsonObject jsonObject = super.getJsonObj();
        jsonObject.addProperty("type", "create_part");
        return jsonObject;
    }

}
