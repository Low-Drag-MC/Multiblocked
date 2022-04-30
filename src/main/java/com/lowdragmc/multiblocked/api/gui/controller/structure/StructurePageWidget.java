package com.lowdragmc.multiblocked.api.gui.controller.structure;

import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TabButton;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class StructurePageWidget extends WidgetGroup {

    public StructurePageWidget(ControllerDefinition controllerDefinition, TabContainer tabContainer) {
        super(20, 0, 176, 256);
        ResourceTexture page = new ResourceTexture("multiblocked:textures/gui/structure_page.png");
        tabContainer.addTab(new TabButton(0, tabContainer.widgets.size() * 10, 20, 20)
                        .setTexture(page.getSubTexture(202 / 256.0, 0 / 256.0, 20 / 256.0, 20 / 256.0),
                                page.getSubTexture(202 / 256.0, 20 / 256.0, 20 / 256.0, 20 / 256.0)),
                this);
        setClientSideWidget();
        if (isRemote()) {
            addWidget(getPatternWidget(controllerDefinition));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public WidgetGroup getPatternWidget(ControllerDefinition controllerDefinition) {
        if (isRemote()) {
            return PatternWidget.getPatternWidget(controllerDefinition);
        }
        return null;
    }

}
