package com.lowdragmc.multiblocked.jei.multipage;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.gui.controller.structure.PatternWidget;

public class MultiblockInfoWrapper extends ModularWrapper<PatternWidget> {
    public final ControllerDefinition definition;

    public MultiblockInfoWrapper(ControllerDefinition definition) {
        super(PatternWidget.getPatternWidget(definition));
        this.definition = definition;
    }

}
