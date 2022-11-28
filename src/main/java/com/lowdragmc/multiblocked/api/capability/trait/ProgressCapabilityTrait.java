package com.lowdragmc.multiblocked.api.capability.trait;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.JsonUtil;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.gui.dialogs.ResourceTextureWidget;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class ProgressCapabilityTrait extends SingleCapabilityTrait {

    protected int width;
    protected int height;
    protected String texture;
    protected ProgressTexture.FillDirection fillDirection;

    public ProgressCapabilityTrait(MultiblockCapability<?> capability) {
        super(capability);
    }

    @Override
    public void serialize(@Nullable JsonElement jsonElement) {
        super.serialize(jsonElement);
        if (jsonElement == null) {
            jsonElement = new JsonObject();
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        width = JSONUtils.getAsInt(jsonObject, "width", 60);
        height = JSONUtils.getAsInt(jsonObject, "height", 18);
        texture = JSONUtils.getAsString(jsonObject, "texture", "multiblocked:textures/gui/energy_bar.png");
        fillDirection = JsonUtil.getEnumOr(jsonObject, "fillDirection", ProgressTexture.FillDirection.class, ProgressTexture.FillDirection.LEFT_TO_RIGHT);
        jsonObject.addProperty("fillDirection", fillDirection.name());
    }

    @Override
    public JsonElement deserialize() {
        JsonObject jsonObject = super.deserialize().getAsJsonObject();
        jsonObject.addProperty("width", width);
        jsonObject.addProperty("height", height);
        jsonObject.addProperty("texture", texture);
        jsonObject.addProperty("fillDirection", fillDirection.name());
        return jsonObject;
    }

    protected abstract String dynamicHoverTips(double progress);

    protected abstract double getProgress();
    
    @Override
    public void createUI(ComponentTileEntity<?> component, WidgetGroup group, PlayerEntity player) {
        super.createUI(component, group, player);
        group.addWidget(new ProgressWidget(
                this::getProgress,
                x, y, width, height,
                new ResourceTexture(texture))
                .setDynamicHoverTips(this::dynamicHoverTips)
                .setFillDirection(fillDirection));
    }

    protected void refreshSlots(DraggableScrollableWidgetGroup dragGroup) {
        dragGroup.widgets.forEach(dragGroup::waitToRemoved);
        ButtonWidget setting = (ButtonWidget) new ButtonWidget(width - 8, 0, 8, 8, new ResourceTexture("multiblocked:textures/gui/option.png"), null).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.settings");
        ImageWidget imageWidget = new ImageWidget(0, 0, width, height, new GuiTextureGroup(createAutoProgressTexture(), new ColorBorderTexture(1, getColorByIO(capabilityIO))));
        setting.setVisible(false);
        DraggableWidgetGroup slot = new DraggableWidgetGroup(x, y, width, height);
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

    @Override
    protected void initSettingDialog(DialogWidget dialog, DraggableWidgetGroup slot) {
        ImageWidget imageWidget = (ImageWidget) slot.widgets.get(0);
        ButtonWidget setting = (ButtonWidget) slot.widgets.get(1);
        ButtonWidget imageSelector = (ButtonWidget) new ButtonWidget(5, 85, width, height, new GuiTextureGroup(new ColorBorderTexture(1, -1), createAutoProgressTexture()), null)
                .setHoverTooltips("multiblocked.gui.tips.select_image");
        dialog.addWidget(new TextFieldWidget(5, 25, 50, 15, null, s -> {
            width = Integer.parseInt(s);
            Size size = new Size(width, height);
            slot.setSize(size);
            imageWidget.setSize(size);
            imageSelector.setSize(size);
            setting.setSelfPosition(new Position(width - 8, 0));
        }).setCurrentString(width + "").setNumbersOnly(1, 180).setHoverTooltips("multiblocked.gui.trait.set_width"));
        dialog.addWidget(new TextFieldWidget(5, 45, 50, 15, null, s -> {
            height = Integer.parseInt(s);
            Size size = new Size(width, height);
            slot.setSize(size);
            imageWidget.setSize(size);
            imageSelector.setSize(size);
            setting.setSelfPosition(new Position(width - 8, 0));
        }).setCurrentString(height + "").setNumbersOnly(1, 180).setHoverTooltips("multiblocked.gui.trait.set_height"));
        dialog.addWidget(new SelectorWidget(5, 5, 50, 15, Arrays.stream(IO.VALUES).map(Enum::name).collect(
                Collectors.toList()), -1)
                .setValue(capabilityIO.name())
                .setOnChanged(io-> {
                    capabilityIO = IO.valueOf(io);
                    imageWidget.setImage(new GuiTextureGroup(createAutoProgressTexture(), new ColorBorderTexture(1, getColorByIO(capabilityIO))));
                })
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(new ColorRectTexture(0xffaaaaaa))
                .setHoverTooltips("multiblocked.gui.trait.capability_io"));

        dialog.addWidget(imageSelector);
        dialog.addWidget(new SelectorWidget(5, 65, 60, 15, Arrays.stream(ProgressTexture.FillDirection.values()).map(Enum::name).collect(Collectors.toList()), -1)
                .setValue(fillDirection.name())
                .setOnChanged(io -> {
                    fillDirection = ProgressTexture.FillDirection.valueOf(io);
                    ProgressTexture autoProgressTexture = createAutoProgressTexture();
                    imageSelector.setButtonTexture(new GuiTextureGroup(new ColorBorderTexture(1, -1), autoProgressTexture));
                    imageWidget.setImage(new GuiTextureGroup(autoProgressTexture, new ColorBorderTexture(1, getColorByIO(capabilityIO))));
                })
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(new ColorRectTexture(0xffaaaaaa))
                .setHoverTooltips("multiblocked.gui.trait.fill_direction"));
        imageSelector.setOnPressCallback(cd -> new ResourceTextureWidget(dialog.getParent().getGui().mainGroup, texture1 -> {
            if (texture1 != null) {
                texture = texture1.imageLocation.toString();
                ProgressTexture autoProgressTexture = createAutoProgressTexture();
                imageSelector.setButtonTexture(new GuiTextureGroup(new ColorBorderTexture(1, -1), autoProgressTexture));
                imageWidget.setImage(new GuiTextureGroup(autoProgressTexture, new ColorBorderTexture(1, getColorByIO(capabilityIO))));
            }
        }));
    }

    private ProgressTexture createAutoProgressTexture() {
        ProgressTexture.Auto texture = new ProgressTexture.Auto(new ResourceTexture(this.texture).getSubTexture(0, 0, 1, 0.5), new ResourceTexture(this.texture).getSubTexture(0, 0.5, 1, 0.5));
        texture.setFillDirection(fillDirection);
        return texture;
    }

}
