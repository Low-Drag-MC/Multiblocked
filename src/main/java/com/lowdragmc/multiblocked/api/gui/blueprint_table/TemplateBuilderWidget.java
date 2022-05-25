package com.lowdragmc.multiblocked.api.gui.blueprint_table;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.SelectableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.gui.dialogs.JsonBlockPatternWidget;
import com.lowdragmc.multiblocked.api.item.ItemBlueprint;
import com.lowdragmc.multiblocked.api.pattern.JsonBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.predicates.PredicateComponent;
import com.lowdragmc.multiblocked.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.registry.MbdItems;
import com.lowdragmc.multiblocked.api.tile.BlueprintTableTileEntity;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.impl.CycleBlockStateRenderer;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDBlockStateRenderer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TemplateBuilderWidget extends WidgetGroup {
    public final BlueprintTableTileEntity table;
    public final ButtonWidget templateButton;
    public SceneWidget sceneWidget;
    public ItemStack selected;
    public int selectedSlot;
    public BlockPos pos;
    public Direction facing;
    protected DraggableScrollableWidgetGroup containers;

    public TemplateBuilderWidget(BlueprintTableTileEntity table) {
        super(0, 0, 384, 256);
        this.table = table;
        this.addWidget(0, new ImageWidget(0, 0, getSize().width, getSize().height, new ResourceTexture("multiblocked:textures/gui/blueprint_page.png")));
        this.addWidget(new ImageWidget(30, 34, 160, 180, new GuiTextureGroup(new ColorBorderTexture(3, -1), new ColorRectTexture(0xaf444444))));
        this.addWidget(new LabelWidget(200, 34, this::status).setTextColor(-1).setDrop(true));
        this.addWidget(new LabelWidget(200, 49, this::size).setTextColor(-1).setDrop(true));
        this.addWidget(new LabelWidget(200, 64, this::description).setTextColor(-1).setDrop(true));
        this.addWidget(templateButton = new ButtonWidget(200, 100, 20, 20, new ItemStackTexture(
                MbdItems.BUILDER), this::onBuildTemplate));
        this.addWidget(sceneWidget = (SceneWidget) new SceneWidget(30, 34, 160, 180, null)
                .useCacheBuffer()
                .setOnSelected(((pos, facing) -> {
                    this.pos = pos;
                    this.facing = facing;
                }))
                .setRenderSelect(false)
                .setRenderFacing(false)
                .setClientSideWidget());
        this.addWidget(new ImageWidget(200 - 4, 120 - 4, 150 + 8, 98 + 8, ResourceBorderTexture.BORDERED_BACKGROUND_BLUE));
        templateButton.setHoverTooltips("multiblocked.gui.builder.template.create");
        templateButton.setVisible(false);
    }

    protected void onBuildTemplate(ClickData clickData) {
        if (isRemote() && ItemBlueprint.isItemBlueprint(selected)) {
            JsonBlockPattern pattern = null;
            if (ItemBlueprint.isRaw(selected)) {
                BlockPos[] poses = ItemBlueprint.getPos(selected);
                Level world = table.getLevel();
                if (poses != null && world.hasChunksAt(poses[0], poses[1])) {
                    ControllerTileEntity controller = null;
                    for (int x = poses[0].getX(); x <= poses[1].getX(); x++) {
                        for (int y = poses[0].getY(); y <= poses[1].getY(); y++) {
                            for (int z = poses[0].getZ(); z <= poses[1].getZ(); z++) {
                                BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
                                if (te instanceof ControllerTileEntity) {
                                    controller = (ControllerTileEntity) te;
                                }
                            }
                        }
                    }
                    if (controller != null) {
                        pattern = new JsonBlockPattern(table.getLevel(), controller.getLocation(), controller.getBlockPos(), controller.getFrontFacing(),
                                poses[0].getX(), poses[0].getY(), poses[0].getZ(),
                                poses[1].getX(), poses[1].getY(), poses[1].getZ());

                    } else {
                        // TODO tips dialog
                    }
                } else {
                    // TODO tips dialog
                }
            } else if (selected.getTagElement("pattern") != null){
                String json = selected.getTagElement("pattern").getString("json");
                pattern = Multiblocked.GSON.fromJson(json, JsonBlockPattern.class);
            }
            if (pattern != null) {
                new JsonBlockPatternWidget(this, pattern, patternResult -> {
                    if (patternResult != null) {
                        if (ItemBlueprint.setPattern(selected) && patternResult.predicates.get("controller") instanceof PredicateComponent) {
                            patternResult.cleanUp();
                            String json = patternResult.toJson();
                            String controller = ((PredicateComponent)patternResult.predicates.get("controller")).location.toString();
                            selected.getOrCreateTagElement("pattern").putString("json", json);
                            selected.getOrCreateTagElement("pattern").putString("controller", controller);
                            writeClientAction(-1, buffer -> {
                                buffer.writeVarInt(selectedSlot);
                                buffer.writeUtf(json);
                                buffer.writeUtf(controller);
                            });
                        }
                    }
                });
            }
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        super.readUpdateInfo(id, buffer);
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        if (id == -1) {
            int slotIndex = buffer.readVarInt();
            String json = buffer.readUtf(Short.MAX_VALUE);
            String controller = buffer.readUtf(Short.MAX_VALUE);
            BlockEntity tileEntity = table.getLevel().getBlockEntity(table.getBlockPos().relative(Direction.UP).relative(table.getFrontFacing().getOpposite()).relative(table.getFrontFacing().getClockWise()));
            if (tileEntity != null) {
                tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                    if (handler.getSlots() > slotIndex) {
                        ItemStack itemStack = handler.getStackInSlot(slotIndex);
                        if (ItemBlueprint.isItemBlueprint(itemStack)) {
                            ItemBlueprint.setPattern(itemStack);
                            itemStack.getOrCreateTagElement("pattern").putString("json", json);
                            itemStack.getOrCreateTagElement("pattern").putString("controller", controller);
                        }
                    }
                });

            }
        } else {
            super.handleClientAction(id, buffer);
        }
    }

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        BlockEntity tileEntity = table.getLevel().getBlockEntity(table.getBlockPos().relative(Direction.UP).relative(table.getFrontFacing().getOpposite()).relative(table.getFrontFacing().getClockWise()));
        Map<Integer, ItemStack> caught = new Int2ObjectOpenHashMap<>();
        if (tileEntity != null) {
            tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler->{
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack itemStack = handler.getStackInSlot(i);
                    if (ItemBlueprint.isItemBlueprint(itemStack)) {
                        caught.put(i, itemStack);
                    }
                }
            });
        }
        buffer.writeVarInt(caught.size());
        caught.forEach((k, v) -> {
            buffer.writeItem(v);
            buffer.writeVarInt(k);
        });
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        this.addWidget(containers = new DraggableScrollableWidgetGroup(200, 120, 150, 98));
        containers.setClientSideWidget();
        for (int i = buffer.readVarInt(); i > 0; i--) {
            ItemStack itemStack = buffer.readItem();
            int slotIndex = buffer.readVarInt();
            containers.addWidget( new SelectableWidgetGroup(0, containers.widgets.size() * 22, containers.getSize().width, 20)
                    .setSelectedTexture(-2, 0xff00aa00)
                    .setOnSelected(w -> onSelected(itemStack, slotIndex))
                    .addWidget(new ImageWidget(0, 0, 150, 20, new ColorRectTexture(0x4faaaaaa)))
                    .addWidget(new ImageWidget(32, 0, 100, 20, new TextTexture(itemStack.getDisplayName().getString()).setWidth(100).setType(TextTexture.TextType.ROLL)))
                    .addWidget(new ImageWidget(4, 2, 18, 18, new ItemStackTexture(itemStack))));
        }
    }

    private String size() {
        String result = LocalizationUtils.format("multiblocked.gui.builder.template.size");
        if (selected != null) {
            BlockPos[] poses = ItemBlueprint.getPos(selected);
            if (poses != null) {
                result += String.format("(%dX%dX%d)", poses[1].getX() - poses[0].getX() + 1,  poses[1].getY() - poses[0].getY() + 1,  poses[1].getZ() - poses[0].getZ() + 1);
            }
        }
        return result;
    }

    private String description() {
        String result = LocalizationUtils.format("multiblocked.gui.builder.template.description") + "\n";
        if (selected != null) {
            BlockPos[] poses = ItemBlueprint.getPos(selected);
            if (poses != null) {
                result += LocalizationUtils.format("multiblocked.gui.builder.template.from", poses[0].getX(), poses[0].getY(), poses[0].getZ()) + "\n";
                result += LocalizationUtils.format("multiblocked.gui.builder.template.to", poses[0].getX(), poses[0].getY(), poses[0].getZ());
            }
        }
        return result;
    }

    private String status() {
        return LocalizationUtils.format("multiblocked.gui.builder.template.status") + " " +
                (selected == null ? "" : ItemBlueprint.isRaw(selected) ?
                        ChatFormatting.YELLOW + LocalizationUtils.format("multiblocked.gui.builder.template.raw") :
                        ChatFormatting.GREEN + LocalizationUtils.format("multiblocked.gui.builder.template.pattern"));
    }

    @OnlyIn(Dist.CLIENT)
    public void onSelected(ItemStack itemStack, int slot) {
        if (this.selected != itemStack) {
            this.selected = itemStack;
            this.selectedSlot = slot;
            if (selected != null && isRemote()) {
                this.pos = null;
                this.facing = null;
                templateButton.setVisible(true);
                if (ItemBlueprint.isRaw(itemStack)) {
                    BlockPos[] poses = ItemBlueprint.getPos(itemStack);
                    Level world = table.getLevel();
                    sceneWidget.createScene(world);
                    if (poses != null && world.hasChunksAt(poses[0], poses[1])) {
                        Set<BlockPos> rendered = new HashSet<>();
                        for (int x = poses[0].getX(); x <= poses[1].getX(); x++) {
                            for (int y = poses[0].getY(); y <= poses[1].getY(); y++) {
                                for (int z = poses[0].getZ(); z <= poses[1].getZ(); z++) {
                                    if (!world.isEmptyBlock(new BlockPos(x, y, z))) {
                                        rendered.add(new BlockPos(x, y, z));
                                    }
                                }
                            }
                        }
                        sceneWidget.setRenderedCore(rendered, null);
                    }
                } else if (itemStack.getTagElement("pattern") != null){
                    String json = itemStack.getTagElement("pattern").getString("json");
                    JsonBlockPattern pattern = Multiblocked.GSON.fromJson(json, JsonBlockPattern.class);
                    int[] centerOffset = pattern.getCenterOffset();
                    String[][] patternString = pattern.pattern;
                    Set<BlockPos> rendered = new HashSet<>();
                    TrackedDummyWorld world = new TrackedDummyWorld();
                    sceneWidget.createScene(world);
                    int offset = Math.max(patternString.length, Math.max(patternString[0].length, patternString[0][0].length()));
                    for (int i = 0; i < patternString.length; i++) {
                        for (int j = 0; j < patternString[0].length; j++) {
                            for (int k = 0; k < patternString[0][0].length(); k++) {
                                char symbol = patternString[i][j].charAt(k);
                                BlockPos pos = pattern.getActualPosOffset(k - centerOffset[2], j - centerOffset[1], i - centerOffset[0], Direction.NORTH).offset(offset, offset, offset);
                                world.addBlock(pos, BlockInfo.fromBlockState(MbdComponents.DummyComponentBlock.defaultBlockState()));
                                DummyComponentTileEntity tileEntity = (DummyComponentTileEntity) world.getBlockEntity(pos);
                                ComponentDefinition definition = null;
                                assert tileEntity != null;
                                if (pattern.symbolMap.containsKey(symbol)) {
                                    Set<BlockInfo> candidates = new HashSet<>();
                                    for (String s : pattern.symbolMap.get(symbol)) {
                                        SimplePredicate predicate = pattern.predicates.get(s);
                                        if (predicate instanceof PredicateComponent && ((PredicateComponent) predicate).definition != null) {
                                            definition = ((PredicateComponent) predicate).definition;
                                            break;
                                        } else if (predicate != null && predicate.candidates != null) {
                                            candidates.addAll(Arrays.asList(predicate.candidates.get()));
                                        }
                                    }
                                    definition = getComponentDefinition(definition, candidates);
                                }
                                if (definition != null) {
                                    tileEntity.setDefinition(definition);
                                }
                                tileEntity.isFormed = false;
                                rendered.add(pos);
                            }
                        }
                    }
                    sceneWidget.setRenderedCore(rendered, null);
                }
            }
        }
    }

    public static ComponentDefinition getComponentDefinition(ComponentDefinition definition, Set<BlockInfo> candidates) {
        if (candidates.size() == 1) {
            definition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, "i_renderer"));
            definition.baseRenderer = new MBDBlockStateRenderer(candidates.toArray(new BlockInfo[0])[0]);
        } else if (!candidates.isEmpty()) {
            definition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, "i_renderer"));
            definition.baseRenderer = new CycleBlockStateRenderer(candidates.toArray(new BlockInfo[0]));
        }
        return definition;
    }
}
