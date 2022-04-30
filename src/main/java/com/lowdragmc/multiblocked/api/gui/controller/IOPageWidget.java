package com.lowdragmc.multiblocked.api.gui.controller;

import com.google.common.collect.Table;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.utils.BlockPosFace;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class IOPageWidget extends PageWidget {
    private static final ResourceTexture PAGE = new ResourceTexture("multiblocked:textures/gui/io_page.png");
    private static final IGuiTexture RIGHT_BUTTON = PAGE.getSubTexture(176 / 256.0, 84 / 256.0, 5 / 256.0, 17 / 256.0);
    private static final IGuiTexture RIGHT_BUTTON_HOVER = PAGE.getSubTexture(181 / 256.0, 84 / 256.0, 5 / 256.0, 17 / 256.0);
    private static final IGuiTexture LEFT_BUTTON = PAGE.getSubTexture(176 / 256.0, 101 / 256.0, 5 / 256.0, 17 / 256.0);
    private static final IGuiTexture LEFT_BUTTON_HOVER = PAGE.getSubTexture(181 / 256.0, 101 / 256.0, 5 / 256.0, 17 / 256.0);
    private static final Map<IO, IGuiTexture> LINE_0_MAP;
    private static final Map<IO, IGuiTexture> LINE_1_MAP;

    static {
        LINE_0_MAP = new EnumMap<>(IO.class);
        LINE_0_MAP.put(IO.IN, PAGE.getSubTexture(211 / 256.0, 0, 45 / 256.0, 28 / 256.0));
        LINE_0_MAP.put(IO.OUT, PAGE.getSubTexture(211 / 256.0, 28 / 256.0, 45 / 256.0, 28 / 256.0));
        LINE_0_MAP.put(IO.BOTH, PAGE.getSubTexture(211 / 256.0, 56 / 256.0, 45 / 256.0, 28 / 256.0));

        LINE_1_MAP = new EnumMap<>(IO.class);
        LINE_1_MAP.put(IO.IN, PAGE.getSubTexture(186 / 256.0, 0, 4 / 256.0, 35 / 256.0));
        LINE_1_MAP.put(IO.OUT, PAGE.getSubTexture(190 / 256.0, 0, 4 / 256.0, 35 / 256.0));
        LINE_1_MAP.put(IO.BOTH, PAGE.getSubTexture(194 / 256.0, 0, 4 / 256.0, 35 / 256.0));
    }

    private final ControllerTileEntity controller;
    private final ImageWidget[][] lines;
    private final TextTexture[] labels;
    private final SelectorWidget[] selectors;

    private Map<Long, EnumMap<IO, Set<MultiblockCapability<?>>>> capabilityMap;
    @OnlyIn(Dist.CLIENT)
    private Map<MultiblockCapability<?>, Tuple<IO, Direction>> capabilitySettings;
    private BlockPos pos;
    int index;

    public IOPageWidget(ControllerTileEntity controller, TabContainer container) {
        super(PAGE, container);
        this.controller = controller;
        if (controller.state.cache == null) {
            controller.checkPattern();
        }
        capabilityMap = controller.state.getMatchContext().get("capabilities");
        capabilityMap = capabilityMap == null ? new HashMap<>() : capabilityMap;
        if (controller.isRemote()) {
            capabilitySettings = new HashMap<>();
        }
        lines = new ImageWidget[2][3];
        addWidget(lines[0][0] = new ImageWidget(14, 172, 45, 28));
        addWidget(lines[0][1] = new ImageWidget(66, 172, 45, 28));
        addWidget(lines[0][2] = new ImageWidget(118, 172, 45, 28));

        addWidget(lines[1][0] = new ImageWidget(34, 202, 4, 35));
        addWidget(lines[1][1] = new ImageWidget(86, 202, 4, 35));
        addWidget(lines[1][2] = new ImageWidget(138, 202, 4, 35));

        SceneWidget sceneWidget;
        addWidget(sceneWidget = new SceneWidget(6, 6, 164, 132, isRemote() ? getWorld() : null)
                .useCacheBuffer()
                .setRenderedCore(controller.state.getCache(), null)
                .setOnSelected(this::onPosSelected)
                .setRenderFacing(false));
        if (isRemote()) {
            sceneWidget.getRenderer().setBlocked(controller.state.getCache().stream().filter(pos -> MultiblockWorldSavedData.modelDisabled.contains(pos)).collect(Collectors.toSet()));
        }
        addWidget(new ButtonWidget(4, 156, 5, 17, LEFT_BUTTON, this::onLeftClick).setHoverTexture(LEFT_BUTTON_HOVER));
        addWidget(new ButtonWidget(167, 156, 5, 17, RIGHT_BUTTON, this::onRightClick).setHoverTexture(RIGHT_BUTTON_HOVER));
        labels = new TextTexture[3];
        addWidget(new ImageWidget(11, 156, 50, 15).setImage(labels[0] = new TextTexture("Empty", -1)
                .setType(TextTexture.TextType.ROLL)
                .setWidth(50)
                .setDropShadow(true)));
        addWidget(new ImageWidget(63, 156, 50, 15).setImage(labels[1] = new TextTexture("Empty", -1)
                .setType(TextTexture.TextType.ROLL)
                .setWidth(50)
                .setDropShadow(true)));
        addWidget(new ImageWidget(115, 156, 50, 15).setImage(labels[2] = new TextTexture("Empty", -1)
                .setType(TextTexture.TextType.ROLL)
                .setWidth(50)
                .setDropShadow(true)));
        selectors = new SelectorWidget[3];

        List<String> facings = Arrays.stream(Direction.values()).map(Direction::name).collect(Collectors.toList());;
        addWidget(selectors[0] = new SelectorWidget(11, 142, 50, 12, facings, -1).setIsUp(true).setOnChanged(facing -> setFacing(Direction.valueOf(facing), 0)));
        addWidget(selectors[1] = new SelectorWidget(63, 142, 50, 12, facings, -1).setIsUp(true).setOnChanged(facing -> setFacing(Direction.valueOf(facing), 1)));
        addWidget(selectors[2] = new SelectorWidget(115, 142, 50, 12, facings, -1).setIsUp(true).setOnChanged(facing -> setFacing(Direction.valueOf(facing), 2)));

        selectors[0].setHoverTooltips("set the io facing").setVisible(false);
        selectors[1].setHoverTooltips("set the io facing").setVisible(false);
        selectors[2].setHoverTooltips("set the io facing").setVisible(false);

        if (isRemote()) {
            setupSceneWidget(sceneWidget);
        }

        addWidget(new ButtonWidget(30, 208, 12, 12, IGuiTexture.EMPTY, cd -> click(cd, index, IO.IN)).setHoverTexture(new ColorRectTexture(0x4fffffff)));
        addWidget(new ButtonWidget(30, 229, 12, 12, IGuiTexture.EMPTY, cd -> click(cd, index, IO.OUT)).setHoverTexture(new ColorRectTexture(0x4fffffff)));
        addWidget(new ButtonWidget(82, 208, 12, 12, IGuiTexture.EMPTY, cd -> click(cd, index + 1, IO.IN)).setHoverTexture(new ColorRectTexture(0x4fffffff)));
        addWidget(new ButtonWidget(82, 229, 12, 12, IGuiTexture.EMPTY, cd -> click(cd, index + 1, IO.OUT)).setHoverTexture(new ColorRectTexture(0x4fffffff)));
        addWidget(new ButtonWidget(134, 208, 12, 12, IGuiTexture.EMPTY, cd -> click(cd, index + 2, IO.IN)).setHoverTexture(new ColorRectTexture(0x4fffffff)));
        addWidget(new ButtonWidget(134, 229, 12, 12, IGuiTexture.EMPTY, cd -> click(cd, index + 2, IO.OUT)).setHoverTexture(new ColorRectTexture(0x4fffffff)));

    }

    @SuppressWarnings("ConstantConditions")
    private void setFacing(Direction facing, int i) {
        if (isRemote()) {
            List<MultiblockCapability<?>> values = new ArrayList<>(capabilitySettings.keySet());
            if (values.size() > (i + index)) {
                MultiblockCapability<?> capability = values.get(i + index);
                if (capabilitySettings.get(capability) != null && capabilitySettings.get(capability).getA() != null) {
                    capabilitySettings.put(capability, new Tuple<>(capabilitySettings.get(capability).getA(), facing));
                    writeClientAction(-2, buffer -> {
                        Tuple<IO, Direction> tuple = capabilitySettings.get(capability);
                        buffer.writeUtf(capability.name);
                        buffer.writeEnum(tuple.getA());
                        buffer.writeEnum(tuple.getB());
                    });
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private World getWorld() {
        return Minecraft.getInstance().level;
    }

    @OnlyIn(Dist.CLIENT)
    private void setupSceneWidget(SceneWidget sceneWidget) {
        sceneWidget.getRenderer().setAfterWorldRender(renderer -> {
            sceneWidget.renderBlockOverLay(renderer);
            RenderUtils.useLightMap(240, 240, () -> {
                RenderSystem.disableCull();
                int inner = 0;
                for (Map.Entry<MultiblockCapability<?>, Tuple<IO, Direction>> entry : capabilitySettings.entrySet()) {
                    if (entry.getValue() != null) {
                        sceneWidget.drawFacingBorder(new MatrixStack(), new BlockPosFace(pos, entry.getValue().getB()), entry.getKey().color, inner);
                        inner++;
                    }
                }
                RenderSystem.enableCull();
            });
        });
    }

    private void refresh() {
        List<MultiblockCapability<?>> values = new ArrayList<>(capabilitySettings.keySet());
        for (int i = index; i < index + 3; i++) {
            MultiblockCapability<?> capability = null;
            if (i < values.size()) {
                capability = values.get(i);
                labels[i - index].setBackgroundColor(capability.color).updateText(capability.getUnlocalizedName());
                if (capabilitySettings.get(capability) != null && capabilitySettings.get(capability).getA() != null) {
                    selectors[i - index].setValue(capabilitySettings.get(capability).getB().name()).setVisible(true);
                } else {
                    selectors[i - index].setVisible(false);
                }
            } else {
                labels[i - index].setBackgroundColor(0).updateText("Empty");
                selectors[i - index].setVisible(false);
            }
            IO io = capabilitySettings.get(capability) == null ?  null : capabilitySettings.get(capability).getA();
            lines[0][i - index].setImage(LINE_0_MAP.get(io));
            lines[1][i - index].setImage(LINE_1_MAP.get(io));
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void click(ClickData clickData, int index, IO io) {
        if (clickData.isRemote) {
            EnumMap<IO, Set<MultiblockCapability<?>>> enumMap;
            if (pos != null && capabilityMap.containsKey(pos.asLong())) {
                enumMap = capabilityMap.get(pos.asLong());
            } else {
                return;
            }
            List<MultiblockCapability<?>> values = new ArrayList<>(capabilitySettings.keySet());
            if (index >= 0 && index < values.size()) {
                MultiblockCapability<?> capability = values.get(index);
                Tuple<IO, Direction> tuple = capabilitySettings.get(capability);
                IO originalIO = tuple == null ? null : tuple.getA();
                Direction originalFacing = tuple == null ? Direction.UP : tuple.getB();
                boolean find = false;
                if (enumMap.get(io) != null && enumMap.get(io).contains(capability)) {
                    find = true;
                }
                if (enumMap.get(IO.BOTH) != null && enumMap.get(IO.BOTH).contains(capability)) {
                    find = true;
                }
                if (!find) return;
                if (originalIO == null ) {
                    capabilitySettings.put(capability, new Tuple<>(io, originalFacing));
                } else if (originalIO == io) {
                    capabilitySettings.put(capability, new Tuple<>(null, originalFacing));
                } else if (originalIO == IO.BOTH) {
                    capabilitySettings.put(capability, new Tuple<>(io == IO.IN ? IO.OUT : IO.IN, originalFacing));
                } else {
                    capabilitySettings.put(capability, new Tuple<>(IO.BOTH, originalFacing));
                }
                writeClientAction(-1, buffer -> {
                    buffer.writeUtf(capability.name);
                    buffer.writeBoolean(originalIO != null);
                    if (originalIO != null) {
                        buffer.writeEnum(originalIO);
                    }
                    IO newIO = capabilitySettings.get(capability).getA();
                    buffer.writeBoolean(newIO != null);
                    if (newIO != null) {
                        buffer.writeEnum(newIO);
                        buffer.writeEnum(capabilitySettings.get(capability).getB());
                    }
                });
                refresh();
            }
        }
    }

    private void onRightClick(ClickData clickData) {
        if (clickData.isRemote) {
            List<MultiblockCapability<?>> values = new ArrayList<>(capabilitySettings.keySet());
            if (index < values.size() - 3) {
                index ++;
                refresh();
            }
        }
    }

    private void onLeftClick(ClickData clickData) {
        if (clickData.isRemote) {
            if (index > 0) {
                index --;
                refresh();
            }
        }
    }

    private void onPosSelected(BlockPos pos, Direction facing) {
        if (Objects.equals(pos, this.pos)) return;
        this.pos = pos;
        if (!isRemote()) {
            if (!capabilityMap.containsKey(pos.asLong()) || controller.getCapabilitiesProxy() == null) return;
            writeUpdateInfo(-1, buffer -> {
                long posLong = pos.asLong();
                Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilities = controller.getCapabilitiesProxy();
                List<Tuple<IO, CapabilityProxy<?>>> real = new ArrayList<>();
                for (Map.Entry<IO, Set<MultiblockCapability<?>>> entry : capabilityMap.get(pos.asLong()).entrySet()) {
                    Set<MultiblockCapability<?>> set = entry.getValue();
                    for (MultiblockCapability<?> capability : set) {
                        if (capabilities.contains(entry.getKey(), capability) && capabilities.get(entry.getKey(), capability).containsKey(posLong)) {
                            real.add(new Tuple<>(entry.getKey(), capabilities.get(entry.getKey(), capability).get(posLong)));
                        }
                        if (entry.getKey() == IO.BOTH) {
                            if (capabilities.contains(IO.IN, capability) && capabilities.get(IO.IN, capability).containsKey(posLong)) {
                                real.add(new Tuple<>(IO.IN, capabilities.get(IO.IN, capability).get(posLong)));
                            }
                            if (capabilities.contains(IO.OUT, capability) && capabilities.get(IO.OUT, capability).containsKey(posLong)) {
                                real.add(new Tuple<>(IO.OUT, capabilities.get(IO.OUT, capability).get(posLong)));
                            }
                        }
                    }
                }
                buffer.writeVarInt(real.size());
                for (Tuple<IO, CapabilityProxy<?>> tuple : real) {
                    buffer.writeEnum(tuple.getA());
                    buffer.writeUtf(tuple.getB().capability.name);
                    buffer.writeEnum(tuple.getB().facing);
                }
            });
        } else {
            capabilitySettings.clear();
            if (capabilityMap.containsKey(pos.asLong())) {
                capabilityMap.get(pos.asLong()).values().stream().flatMap(Collection::stream).forEach(capability -> capabilitySettings.put(capability, null));
            } else {
                refresh();
            }
        }
    }

    @Override
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        if (id == -1) {
            int size = buffer.readVarInt();
            for (int i = size; i > 0; i--) {
                IO io = buffer.readEnum(IO.class);
                MultiblockCapability<?> capability = MbdCapabilities.get(buffer.readUtf());
                Direction facing = buffer.readEnum(Direction.class);
                capabilitySettings.put(capability, new Tuple<>(io, facing));
            }
            refresh();
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        if (id == -1) {
            MultiblockCapability<?> capability = MbdCapabilities.get(buffer.readUtf());
            Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilities = controller.getCapabilitiesProxy();
            if (buffer.readBoolean()) {
                IO io = buffer.readEnum(IO.class);
                capabilities.get(io, capability).remove(pos.asLong());
                if (capabilities.get(io, capability).isEmpty()) {
                    capabilities.remove(io, capability);
                }
            }
            if (buffer.readBoolean()) {
                IO io = buffer.readEnum(IO.class);
                TileEntity entity = controller.getLevel().getBlockEntity(pos);
                if (entity != null && capability.isBlockHasCapability(io, entity)) {
                    if (!capabilities.contains(io, capability)) {
                        capabilities.put(io, capability, new Long2ObjectOpenHashMap<>());
                    }
                    CapabilityProxy<?> proxy = capability.createProxy(io, entity);
                    proxy.facing = buffer.readEnum(Direction.class);
                    capabilities.get(io, capability).put(pos.asLong(), proxy);
                }
            }
            controller.markAsDirty();
        } else if (id == -2) {
            MultiblockCapability<?> capability = MbdCapabilities.get(buffer.readUtf());
            Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilities = controller.getCapabilitiesProxy();
            IO io = buffer.readEnum(IO.class);
            TileEntity entity = controller.getLevel().getBlockEntity(pos);
            if (entity != null && capability.isBlockHasCapability(io, entity)) {
                if (!capabilities.contains(io, capability)) {
                    capabilities.put(io, capability, new Long2ObjectOpenHashMap<>());
                }
                CapabilityProxy<?> proxy = capability.createProxy(io, entity);
                proxy.facing = buffer.readEnum(Direction.class);
                capabilities.get(io, capability).put(pos.asLong(), proxy);
            }
            controller.markAsDirty();
        } else {
            super.handleClientAction(id, buffer);
        }
    }
}
