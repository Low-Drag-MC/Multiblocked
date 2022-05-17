package com.lowdragmc.multiblocked.api.capability.trait;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.DraggableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.JsonUtil;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class PlayerCapabilityTrait extends CapabilityTrait {
    protected String playerName = "";
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected TextTexture.TextType textType;

    public PlayerCapabilityTrait(MultiblockCapability<?> capability) {
        super(capability);
    }

    public String getPlayerName() {
        return playerName.isEmpty() ? (playerName = getPlayer() == null ? "" : getPlayer().getName().getString()) : playerName;
    }

    @Nullable
    public PlayerEntity getPlayer() {
        return component.getOwner();
    }

    @Override
    public void setComponent(ComponentTileEntity<?> component) {
        super.setComponent(component);
    }

    @Override
    public void readFromNBT(CompoundNBT compound) {
        super.readFromNBT(compound);
        playerName = compound.getString("player");
        getPlayerName();
    }

    @Override
    public void writeToNBT(CompoundNBT compound) {
        super.writeToNBT(compound);
        compound.putString("player", getPlayerName());
    }

    @Override
    public void serialize(@Nullable JsonElement jsonElement) {
        if (jsonElement == null) {
            jsonElement = new JsonObject();
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        x = JSONUtils.getAsInt(jsonObject, "x", 5);
        y = JSONUtils.getAsInt(jsonObject, "y", 5);
        width = JSONUtils.getAsInt(jsonObject, "width", 60);
        height = JSONUtils.getAsInt(jsonObject, "height", 18);
        textType = JsonUtil.getEnumOr(jsonObject, "textType", TextTexture.TextType.class, TextTexture.TextType.LEFT);
    }

    @Override
    public JsonElement deserialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("x", x);
        jsonObject.addProperty("y", y);
        jsonObject.addProperty("width", width);
        jsonObject.addProperty("height", height);
        jsonObject.addProperty("textType", textType.ordinal());
        return jsonObject;
    }

    @Override
    public void createUI(ComponentTileEntity<?> component, WidgetGroup group, PlayerEntity player) {
        super.createUI(component, group, player);
        group.addWidget(new ImageWidget(x, y, width, height, new TextTexture("").setSupplier(()->playerName).setWidth(width).setType(textType)) {
            @Override
            public void writeInitialData(PacketBuffer buffer) {
                super.writeInitialData(buffer);
                buffer.writeUtf(getPlayerName());
            }

            @Override
            public void readInitialData(PacketBuffer buffer) {
                super.readInitialData(buffer);
                playerName = buffer.readUtf(Short.MAX_VALUE);
            }
        });
    }

    protected void refreshSlots(DraggableScrollableWidgetGroup dragGroup) {
        dragGroup.widgets.forEach(dragGroup::waitToRemoved);
        ButtonWidget setting = (ButtonWidget) new ButtonWidget(width - 8, 0, 8, 8, new ResourceTexture("multiblocked:textures/gui/option.png"), null).setHoverBorderTexture(1, -1).setHoverTooltips("settings");
        ImageWidget imageWidget = new ImageWidget(0, 0, width, height, new TextTexture("Player Name").setWidth(width).setType(textType).setBackgroundColor(0xff000000));
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

    protected void initSettingDialog(DialogWidget dialog, DraggableWidgetGroup slot) {
        ImageWidget imageWidget = (ImageWidget) slot.widgets.get(0);
        ButtonWidget setting = (ButtonWidget) slot.widgets.get(1);
        dialog.addWidget(new TextFieldWidget(5, 25, 50, 15, null, s -> {
            width = Integer.parseInt(s);
            Size size = new Size(width, height);
            slot.setSize(size);
            imageWidget.setSize(size);
            ((TextTexture)imageWidget.getImage()).setWidth(width);
            setting.setSelfPosition(new Position(width - 8, 0));
        }).setCurrentString(width + "").setNumbersOnly(10, 180).setHoverTooltips("set width"));
        dialog.addWidget(new TextFieldWidget(5, 45, 50, 15, null, s -> {
            height = Integer.parseInt(s);
            Size size = new Size(width, height);
            slot.setSize(size);
            imageWidget.setSize(size);
            setting.setSelfPosition(new Position(width - 8, 0));
        }).setCurrentString(height + "").setNumbersOnly(10, 180).setHoverTooltips("set height"));

        dialog.addWidget(new SelectorWidget(5, 5, 50, 15, Arrays.stream(TextTexture.TextType.values()).map(Enum::name).collect(
                Collectors.toList()), -1)
                .setValue(textType.name())
                .setOnChanged(io-> {
                    textType = TextTexture.TextType.valueOf(io);
                    ((TextTexture)imageWidget.getImage()).setType(textType);
                })
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(new ColorRectTexture(0xffaaaaaa))
                .setHoverTooltips("TextType"));
    }

    @Override
    public void openConfigurator(WidgetGroup parentDialog) {
        DraggableScrollableWidgetGroup dragGroup = new DraggableScrollableWidgetGroup((384 - 176) / 2, 0, 176, 256);
        parentDialog.addWidget(dragGroup);
        refreshSlots(dragGroup);
    }

}
