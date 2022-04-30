package com.lowdragmc.multiblocked.api.gui.controller;


import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TabButton;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;

public abstract class PageWidget extends WidgetGroup {
    protected final ResourceTexture page;

    public PageWidget(ResourceTexture page, TabContainer tabContainer) {
        super(20, 0, 176, 256);
        this.page = page;
        setBackground(page.getSubTexture(0, 0, 176 / 256.0, 1));
        tabContainer.addTab(new TabButton(0, tabContainer.containerGroup.widgets.size() * 20, 20, 20)
                        .setTexture(page.getSubTexture(176 / 256.0, 216 / 256.0, 20 / 256.0, 20 / 256.0),
                                page.getSubTexture(176 / 256.0, 236 / 256.0, 20 / 256.0, 20 / 256.0)),
                this);
    }

}
