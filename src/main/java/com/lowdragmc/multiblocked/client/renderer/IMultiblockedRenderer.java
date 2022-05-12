package com.lowdragmc.multiblocked.client.renderer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Customizable renderer that supports serialization and visual configuration
 */
public interface IMultiblockedRenderer extends IRenderer {
    /**
     * unique type id.
     */
    String getType();

    default void onPostAccess(ComponentTileEntity<?> tileEntity) {

    }

    default void onPreAccess(ComponentTileEntity<?> tileEntity) {

    }

    default String getUnlocalizedName() {
        return "multiblocked.renderer." + getType();
    }

    /**
     * deserialize.
     */
    IMultiblockedRenderer fromJson(Gson gson, JsonObject jsonObject);

    /**
     * serialize.
     */
    JsonObject toJson(Gson gson, JsonObject jsonObject);

    /**
     * configurator.
     * @param group group widget.
     * @param current current renderer.
     * @return called when updated.
     */
    default Supplier<IMultiblockedRenderer> createConfigurator(WidgetGroup parent, DraggableScrollableWidgetGroup group, IMultiblockedRenderer current) {
        group.addWidget(new LabelWidget(5,5,"multiblocked.gui.label.configurator"));
        return null;
    }

    default WidgetGroup createBoolSwitch(int x, int y, String text, String tips, boolean init, Consumer<Boolean> onPressed) {
        WidgetGroup widgetGroup = new WidgetGroup(x, y, 100, 15);
        widgetGroup.addWidget(new SwitchWidget(0, 0, 15, 15, (cd, r)->onPressed.accept(r))
                .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0,1,0.5))
                .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0.5,1,0.5))
                .setHoverTexture(new ColorBorderTexture(1, 0xff545757))
                .setPressed(init)
                .setHoverTooltips(tips));
        widgetGroup.addWidget(new LabelWidget(20, 3, ()->text).setTextColor(-1).setDrop(true));
        return widgetGroup;
    }
}
