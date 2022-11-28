package com.lowdragmc.multiblocked.rei.multipage;

import com.lowdragmc.lowdraglib.rei.ModularDisplay;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.gui.controller.structure.PatternWidget;

public class MultiblockInfoDisplay extends ModularDisplay<PatternWidget> {
    public final ControllerDefinition definition;

    public MultiblockInfoDisplay(ControllerDefinition definition) {
        super(() -> {
            PatternWidget patternWidget = new PatternWidget(definition, true);
            patternWidget.reset(0);
            return patternWidget;
        }, MultiblockInfoDisplayCategory.CATEGORY);
        this.definition = definition;
    }

}
