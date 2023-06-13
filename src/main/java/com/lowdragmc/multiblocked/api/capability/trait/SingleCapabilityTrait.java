package com.lowdragmc.multiblocked.api.capability.trait;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.JsonUtil;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SingleCapabilityTrait extends CapabilityTrait {
    protected IO capabilityIO;
    protected IO guiIO;
    protected IO mbdIO;
    protected String slotName;
    protected int x;
    protected int y;

    public SingleCapabilityTrait(MultiblockCapability<?> capability) {
        super(capability);
    }

    @Override
    public void serialize(@Nullable JsonElement jsonElement) {
        if (jsonElement == null) {
            jsonElement = new JsonObject();
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        capabilityIO = JsonUtil.getEnumOr(jsonObject, "cIO", IO.class, IO.BOTH);
        guiIO = JsonUtil.getEnumOr(jsonObject, "gIO", IO.class, IO.BOTH);
        mbdIO = JsonUtil.getEnumOr(jsonObject, "mbdIO", IO.class, IO.BOTH);
        slotName = GsonHelper.getAsString(jsonObject, "slotName", "");
        x = GsonHelper.getAsInt(jsonObject, "x", 5);
        y = GsonHelper.getAsInt(jsonObject, "y", 5);
    }

    @Override
    public JsonElement deserialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("cIO", capabilityIO.ordinal());
        jsonObject.addProperty("gIO", guiIO.ordinal());
        jsonObject.addProperty("mbdIO", mbdIO.ordinal());
        jsonObject.addProperty("slotName", slotName);
        jsonObject.addProperty("x", x);
        jsonObject.addProperty("y", y);
        return jsonObject;
    }

    @Override
    public Set<String> getSlotNames() {
        return (slotName != null && !slotName.isEmpty()) ? Set.of(slotName) : Set.of();
    }

    protected int getColorByIO(IO io) {
        return io == IO.IN ? 0xaf00ff00 : io == IO.OUT ? 0xafff0000 : 0xaf0000ff;
    }

    protected void refreshSlots(DraggableScrollableWidgetGroup dragGroup) {
        dragGroup.widgets.forEach(dragGroup::waitToRemoved);
        ButtonWidget setting = (ButtonWidget) new ButtonWidget(10, 0, 8, 8, new ResourceTexture("multiblocked:textures/gui/option.png"), null).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.settings");
        ImageWidget imageWidget = new ImageWidget(1, 1, 16, 16, new GuiTextureGroup(new ColorRectTexture(getColorByIO(guiIO)), new ColorBorderTexture(1, getColorByIO(capabilityIO))));
        setting.setVisible(false);
        DraggableWidgetGroup slot = new DraggableWidgetGroup(x, y, 18, 18);
        slot.setOnSelected(w -> setting.setVisible(true));
        slot.setOnUnSelected(w -> setting.setVisible(false));
        slot.addWidget(imageWidget);
        slot.addWidget(setting);
        slot.setOnEndDrag(b -> {
            x = b.getSelfPosition().x;
            y = b.getSelfPosition().y;
        });
        dragGroup.addWidget(slot);

        setting.setOnPressCallback(cd2 -> {
            DialogWidget dialog = new DialogWidget(dragGroup, true);
            dialog.addWidget(new ImageWidget(0, 0, 176, 256, new ColorRectTexture(0xaf000000)));
            initSettingDialog(dialog, slot);
        });
    }

    protected boolean hasIOSettings() {
        return true;
    }

    protected void initSettingDialog(DialogWidget dialog, DraggableWidgetGroup slot) {
        dialog.addWidget(new TextFieldWidget(5, 10, 65, 15, null, s -> slotName = s)
                .setCurrentString(slotName + "")
                .setHoverTooltips("multiblocked.gui.trait.slot_name"));
        if (!hasIOSettings()) return;
        ImageWidget imageWidget = (ImageWidget) slot.widgets.get(0);
        dialog.addWidget(new SelectorWidget(5, 30, 40, 15, Arrays.stream(IO.VALUES).map(Enum::name).collect(Collectors.toList()), -1)
                .setValue(capabilityIO.name())
                .setOnChanged(io-> {
                    capabilityIO = IO.valueOf(io);
                    imageWidget.setImage(new GuiTextureGroup(new ColorRectTexture(getColorByIO(guiIO)), new ColorBorderTexture(1, getColorByIO(capabilityIO))));
                })
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(new ColorRectTexture(0xffaaaaaa))
                .setHoverTooltips("multiblocked.gui.trait.capability_io"));
        dialog.addWidget(new SelectorWidget(50, 30, 40, 15, Arrays.stream(IO.VALUES).map(Enum::name).collect(Collectors.toList()), -1)
                .setValue(guiIO.name())
                .setOnChanged(io-> {
                    guiIO = IO.valueOf(io);
                    imageWidget.setImage(new GuiTextureGroup(new ColorRectTexture(getColorByIO(guiIO)), new ColorBorderTexture(1, getColorByIO(capabilityIO))));
                })
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(new ColorRectTexture(0xffaaaaaa))
                .setHoverTooltips("multiblocked.gui.trait.gui_io"));
        dialog.addWidget(new SelectorWidget(95, 30, 40, 15, Arrays.stream(IO.VALUES).map(Enum::name).collect(Collectors.toList()), -1)
                .setValue(mbdIO.name())
                .setOnChanged(io-> mbdIO = IO.valueOf(io))
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(new ColorRectTexture(0xffaaaaaa))
                .setHoverTooltips("multiblocked.gui.trait.mbd_io"));
    }

    @Override
    public void openConfigurator(WidgetGroup parentDialog) {
        DraggableScrollableWidgetGroup dragGroup = new DraggableScrollableWidgetGroup((384 - 176) / 2, 0, 176, 256);
        parentDialog.addWidget(dragGroup);
        refreshSlots(dragGroup);
    }

    protected IO getRealMbdIO() {
        return mbdIO == IO.IN ? IO.OUT : mbdIO == IO.OUT ? IO.IN : mbdIO;
    }

}
