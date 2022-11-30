package com.lowdragmc.multiblocked.api.gui.controller.structure;

import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.lowdraglib.utils.ItemStackKey;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.pattern.TraceabilityPredicate;
import com.lowdragmc.multiblocked.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;
import com.lowdragmc.multiblocked.client.renderer.impl.CycleBlockStateRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class PatternWidget extends WidgetGroup {
    private static final TrackedDummyWorld world = new TrackedDummyWorld();
    private static BlockPos LAST_POS = new BlockPos(50, 50, 50);

    private static final ResourceTexture PAGE = new ResourceTexture("multiblocked:textures/gui/structure_page.png");
    private static final Map<ControllerDefinition, PatternWidget> CACHE = new HashMap<>();
    private static final IGuiTexture BACKGROUND = PAGE.getSubTexture(0, 0, 176 / 256.0, 221 / 256.0);
    private static final IGuiTexture LEFT_BUTTON = PAGE.getSubTexture(238 / 256.0, 54 / 256.0, 18 / 256.0, 18 / 256.0);
    private static final IGuiTexture LEFT_BUTTON_HOVER = PAGE.getSubTexture(238 / 256.0, 36 / 256.0, 18 / 256.0, 18 / 256.0);
    private static final IGuiTexture RIGHT_BUTTON = PAGE.getSubTexture(238 / 256.0, 18 / 256.0, 18 / 256.0, 18 / 256.0);
    private static final IGuiTexture RIGHT_BUTTON_HOVER = PAGE.getSubTexture(238 / 256.0, 0, 18 / 256.0, 18 / 256.0);
    private static final IGuiTexture FORMED_BUTTON = PAGE.getSubTexture(222 / 256.0, 0, 16 / 256.0, 16 / 256.0);
    private static final IGuiTexture FORMED_BUTTON_PRESSED = PAGE.getSubTexture(222 / 256.0, 16 / 256.0, 16 / 256.0, 16 / 256.0);

    private final SceneWidget sceneWidget;
    private final ButtonWidget leftButton;
    private final ButtonWidget rightButton;
    private final SwitchWidget switchWidget;
    public final ControllerDefinition controllerDefinition;
    public final MBPattern[] patterns;
    private final List<SimplePredicate> predicates;
    private int index;
    public int layer;
    private SlotWidget[] slotWidgets;
    private SlotWidget[] candidates;
    private TextTexture textTexture;

    private PatternWidget(ControllerDefinition controllerDefinition) {
        this(controllerDefinition, false);
    }

    public PatternWidget(ControllerDefinition controllerDefinition, boolean isREI) {
        super(0, 0, 176, 219);
        setClientSideWidget();
        predicates = new ArrayList<>();
        layer = -1;
        addWidget(new ImageWidget(7, 7, 162, 16,
                new TextTexture("block." + controllerDefinition.location.getNamespace() + "." + controllerDefinition.location.getPath(), -1)
                        .setType(TextTexture.TextType.ROLL)
                        .setWidth(162)
                        .setDropShadow(true))
                .setHoverTooltips(controllerDefinition.getDescription()));

        addWidget(sceneWidget = new SceneWidget(6, 30, 164, 140, world)
                .setOnSelected(this::onPosSelected)
                .setRenderFacing(false)
                .setRenderFacing(false));
        if (!isREI) {
            sceneWidget.useCacheBuffer(false);
        }

        this.controllerDefinition = controllerDefinition;

        HashSet<ItemStackKey> drops = new HashSet<>();
        drops.add(new ItemStackKey(this.controllerDefinition.getStackForm()));
        this.patterns = controllerDefinition.getDesigns().stream()
                .map(it -> initializePattern(it, drops))
                .filter(Objects::nonNull)
                .toArray(MBPattern[]::new);

        addWidget(switchWidget = (SwitchWidget) new SwitchWidget(151, 33, 16, 16, this::onFormedSwitch)
                .setTexture(FORMED_BUTTON, FORMED_BUTTON_PRESSED)
                .setHoverTooltips("multiblocked.structure_page.switch"));

        addWidget(leftButton = new ButtonWidget(9, 151, 18, 18, LEFT_BUTTON, (x) -> reset(index - 1)).setHoverTexture(LEFT_BUTTON_HOVER));

        addWidget(rightButton = new ButtonWidget(150, 53, 18, 18, RIGHT_BUTTON, (x) -> reset(index + 1)).setHoverTexture(RIGHT_BUTTON_HOVER));

        addWidget(new ButtonWidget(10, 80, 10, 10, new ResourceTexture("multiblocked:textures/gui/up.png"), cd -> updateLayer(1)).setHoverTooltips("multiblocked.gui.dialogs.pattern.next_aisle"));
        addWidget(new ImageWidget(10, 90, 10, 10, textTexture = new TextTexture("all")));
        addWidget(new ButtonWidget(10, 100, 10, 10, new ResourceTexture("multiblocked:textures/gui/down.png"), cd -> updateLayer(-1)).setHoverTooltips("multiblocked.gui.dialogs.pattern.last_aisle"));

        if (controllerDefinition.getCatalyst() != null && !controllerDefinition.getCatalyst().isEmpty()) {
            ItemStackHandler itemStackHandler;
            addWidget(new SlotWidget(itemStackHandler = new ItemStackHandler(), 0, 149, 151 - 20, false, false)
                    .setIngredientIO(IngredientIO.INPUT)
                    .setBackgroundTexture(ResourceBorderTexture.BUTTON_COMMON)
                    .setOnAddedTooltips((slot, list)-> list.add(new TranslatableComponent("multiblocked.gui.catalyst." + controllerDefinition.consumeCatalyst.ordinal()))));
            itemStackHandler.setStackInSlot(0, controllerDefinition.getCatalyst());
        }
    }

    private void updateLayer(int add) {
        MBPattern pattern = patterns[index];
        if (layer + add >= -1 && layer + add <= pattern.maxY - pattern.minY) {
            if (pattern.controllerBase.isFormed()) {
                onFormedSwitch(null, false);
                switchWidget.setPressed(pattern.controllerBase.isFormed());
            }
            layer += add;
            textTexture.updateText(layer == -1 ? "all" : layer + "");
        }
        setupScene(pattern);
    }

    private void setupScene(MBPattern pattern) {
        Stream<BlockPos> stream = pattern.blockMap.keySet().stream().filter(pos -> layer == -1 || layer + pattern.minY == pos.getY());
        if (pattern.controllerBase.isFormed()) {
            LongSet set = pattern.controllerBase.getMultiblockState().getMatchContext().getOrDefault("renderMask", LongSets.EMPTY_SET);
            Set<BlockPos> modelDisabled = set.stream().map(BlockPos::of).collect(Collectors.toSet());
            if (!modelDisabled.isEmpty()) {
                sceneWidget.setRenderedCore(stream.filter(pos->!modelDisabled.contains(pos)).collect(Collectors.toList()), null);
            } else {
                sceneWidget.setRenderedCore(stream.toList(), null);
            }
        } else {
            sceneWidget.setRenderedCore(stream.toList(), null);
        }
    }

    public static PatternWidget getPatternWidget(ControllerDefinition controllerDefinition) {
        boolean init = CACHE.containsKey(controllerDefinition);
        PatternWidget patternWidget = CACHE.computeIfAbsent(controllerDefinition, PatternWidget::new);
        if (!init) {
            patternWidget.reset(0);
        }
        return patternWidget;
    }

    public void reset(int index) {
        if (index >= patterns.length || index < 0) return;
        this.index = index;
        this.layer = -1;
        textTexture.updateText("all");
        MBPattern pattern = patterns[index];
        setupScene(pattern);
        if (slotWidgets != null) {
            for (SlotWidget slotWidget : slotWidgets) {
                removeWidget(slotWidget);
            }
        }
        slotWidgets = new SlotWidget[Math.min(pattern.parts.size(), 18)];
        IItemHandler itemHandler = new ItemStackHandler(pattern.parts);
        for (int i = 0; i < slotWidgets.length; i++) {
            slotWidgets[i] = new SlotWidget(itemHandler, i, 7 + (i % 9) * 18, 176 + (i / 9) * 18, false, false)
                    .setItemHook(this::itemHook).setIngredientIO(IngredientIO.INPUT);
            addWidget(slotWidgets[i]);
        }
        leftButton.setVisible(index > 0);
        rightButton.setVisible(index < patterns.length - 1);
        updateClientSlots();
        switchWidget.setPressed(pattern.controllerBase.isFormed());
    }

    private void onFormedSwitch(ClickData clickData, Boolean isPressed) {
        MBPattern pattern = patterns[index];
        IControllerComponent controllerBase = pattern.controllerBase;
        if (isPressed) {
            this.layer = -1;
            textTexture.updateText("all");
            loadControllerFormed(pattern.blockMap.keySet(), controllerBase);
        } else {
            sceneWidget.setRenderedCore(pattern.blockMap.keySet(), null);
            controllerBase.setMultiblockState(null);
            controllerBase.onStructureInvalid();
        }
    }

    private void onPosSelected(BlockPos pos, Direction facing) {
        if (index >= patterns.length || index < 0) return;
        TraceabilityPredicate predicate = patterns[index].predicateMap.get(pos);
        if (predicate != null) {
            predicates.clear();
            predicates.addAll(predicate.common);
            predicates.addAll(predicate.limited);
            predicates.removeIf(p -> p == null || p.candidates == null); // why it happens?
            if (candidates != null) {
                for (SlotWidget candidate : candidates) {
                    removeWidget(candidate);
                }
            }
            List<List<ItemStack>> candidateStacks = new ArrayList<>();
            List<List<String>> predicateTips = new ArrayList<>();
            for (SimplePredicate simplePredicate : predicates) {
                List<ItemStack> itemStacks = simplePredicate.getCandidates();
                if (!itemStacks.isEmpty()) {
                    candidateStacks.add(itemStacks);
                    predicateTips.add(simplePredicate.getToolTips(predicate));
                }
            }
            candidates = new SlotWidget[candidateStacks.size()];
            CycleItemStackHandler itemHandler = new CycleItemStackHandler(candidateStacks);
            for (int i = 0; i < candidateStacks.size(); i++) {
                int finalI = i;
                candidates[i] = new SlotWidget(itemHandler, i, 9 + (i / 6) * 18, 33 + (i % 6) * 18, false, false)
                        .setItemHook(this::itemHook)
                        .setBackgroundTexture(new ColorRectTexture(0x4fffffff))
                        .setOnAddedTooltips((slot, list) -> predicateTips.get(finalI).forEach(tip -> list.add(new TextComponent(tip))));
                addWidget(candidates[i]);
            }
            updateClientSlots();
        }
    }

    private ItemStack itemHook(ItemStack itemStack) {
        if (itemStack.getItem() instanceof BlockItem) {
            Block block = ((BlockItem) itemStack.getItem()).getBlock();
            if (block instanceof BlockComponent) {
                if (((BlockComponent) block).definition.getBaseRenderer() instanceof CycleBlockStateRenderer) {
                    CycleBlockStateRenderer renderer = ((CycleBlockStateRenderer) ((BlockComponent) block).definition.getBaseRenderer());
                    itemStack = renderer.getBlockInfo().getItemStackForm();
                }
            }
        }
        return itemStack;
    }

    private void updateClientSlots() {
        if (gui == null || gui.getModularUIGui() == null) return;
        gui.getModularUIGui().getMenu().slots.clear();
        for (SlotWidget slotWidget : getNativeWidgets()) {
            gui.getModularUIGui().getMenu().slots.add(slotWidget.getHandle());
        }
    }

    public static BlockPos locateNextRegion(int range) {
        BlockPos pos = LAST_POS;
        LAST_POS = LAST_POS.offset(range, 0, range);
        return pos;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    @Override
    public void drawInBackground(@Nonnull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int x = getPosition().x;
        int y = getPosition().y;
        int width = getSize().width;
        int height = getSize().height;
        RenderSystem.enableBlend();
        BACKGROUND.draw(matrixStack, mouseX, mouseY, x, y, width, height);
        super.drawInBackground(matrixStack, mouseX, mouseY, partialTicks);
    }

    private MBPattern initializePattern(MultiblockShapeInfo shapeInfo, HashSet<ItemStackKey> blockDrops) {
        Map<BlockPos, BlockInfo> blockMap = new HashMap<>();
        IControllerComponent controllerBase = null;
        BlockPos multiPos = locateNextRegion(500);

        BlockInfo[][][] blocks = shapeInfo.getBlocks();
        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    BlockState blockState = column[z].getBlockState();
                    BlockPos pos = multiPos.offset(x, y, z);
                    if (column[z].getBlockEntity(pos) instanceof IControllerComponent) {

                        controllerBase = (IControllerComponent) column[z].getBlockEntity(world, pos);
                    }
                    blockMap.put(pos, BlockInfo.fromBlockState(blockState));
                }
            }
        }

        world.addBlocks(blockMap);
        if (controllerBase != null) {
            world.setBlockEntity(controllerBase.self());
        }

        Map<ItemStackKey, PartInfo> parts = gatherBlockDrops(blockMap);
        blockDrops.addAll(parts.keySet());

        Map<BlockPos, TraceabilityPredicate> predicateMap = new HashMap<>();
        if (controllerBase != null) {
            loadControllerFormed(predicateMap.keySet(), controllerBase);
            predicateMap = controllerBase.getMultiblockState().getMatchContext().get("predicates");
        }
        return controllerBase == null ? null : new MBPattern(blockMap, parts.values().stream().sorted((one, two) -> {
            if (one.isController) return -1;
            if (two.isController) return +1;
            if (one.isTile && !two.isTile) return -1;
            if (two.isTile && !one.isTile) return +1;
            if (one.blockId != two.blockId) return two.blockId - one.blockId;
            return two.amount - one.amount;
        }).map(PartInfo::getItemStack).toArray(ItemStack[]::new), predicateMap, controllerBase);
    }

    private void loadControllerFormed(Collection<BlockPos> poses, IControllerComponent controllerBase) {
        controllerBase.setMultiblockState(new MultiblockState(world, controllerBase.self().getBlockPos()));
        BlockPattern pattern = controllerBase.getPattern();
        if (pattern != null && pattern.checkPatternAt(controllerBase.getMultiblockState(), true)) {
            controllerBase.onStructureFormed();
        }
        if (controllerBase.isFormed()) {
            LongSet set = controllerBase.getMultiblockState().getMatchContext().getOrDefault("renderMask", LongSets.EMPTY_SET);
            Set<BlockPos> modelDisabled = set.stream().map(BlockPos::of).collect(Collectors.toSet());
            if (!modelDisabled.isEmpty()) {
                sceneWidget.setRenderedCore(poses.stream().filter(pos->!modelDisabled.contains(pos)).collect(Collectors.toList()), null);
            } else {
                sceneWidget.setRenderedCore(poses, null);
            }
        } else {
            Multiblocked.LOGGER.warn("Pattern formed checking failed: {}", controllerBase.getDefinition().location);
        }
    }

    private Map<ItemStackKey, PartInfo> gatherBlockDrops(Map<BlockPos, BlockInfo> blocks) {
        Map<ItemStackKey, PartInfo> partsMap = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<BlockPos, BlockInfo> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState blockState = ((Level) PatternWidget.world).getBlockState(pos);
            ItemStack itemStack = blockState.getBlock().getCloneItemStack(blockState,
                    BlockHitResult.miss(
                    new Vec3(0.5, 1, 0.5).add(pos.getX(), pos.getY(), pos.getZ()),
                    Direction.UP,
                    pos), PatternWidget.world, pos, Minecraft.getInstance().player);

            if (itemStack.isEmpty() && !blockState.getFluidState().isEmpty()) {
                Fluid fluid = blockState.getFluidState().getType();
                itemStack = fluid.getBucket().getDefaultInstance();
            }

            ItemStackKey itemStackKey = new ItemStackKey(itemStack);
            partsMap.computeIfAbsent(itemStackKey, key -> new PartInfo(key, entry.getValue())).amount++;
        }
        return partsMap;
    }

    private static class PartInfo {
        final ItemStackKey itemStackKey;
        boolean isController = false;
        boolean isTile = false;
        final int blockId;
        int amount = 0;

        PartInfo(final ItemStackKey itemStackKey, final BlockInfo blockInfo) {
            this.itemStackKey = itemStackKey;
            this.blockId = Block.getId(blockInfo.getBlockState());
            this.isTile = blockInfo.hasBlockEntity();

            if (blockInfo.getBlockState().getBlock() instanceof BlockComponent component) {
                if (component.definition instanceof ControllerDefinition)
                    this.isController = true;
            }
        }

        ItemStack getItemStack() {
            ItemStack result = this.itemStackKey.getItemStack();
            result.setCount(this.amount);
            return result;
        }
    }

    private static class MBPattern {
        @Nonnull
        final NonNullList<ItemStack> parts;
        @Nonnull
        final Map<BlockPos, TraceabilityPredicate> predicateMap;
        @Nonnull
        final Map<BlockPos, BlockInfo> blockMap;
        @Nonnull
        final IControllerComponent controllerBase;
        final int maxY, minY;

        public MBPattern(@Nonnull Map<BlockPos, BlockInfo> blockMap, @Nonnull ItemStack[] parts, @Nonnull Map<BlockPos, TraceabilityPredicate> predicateMap, @Nonnull IControllerComponent controllerBase) {
            this.parts = NonNullList.of(ItemStack.EMPTY, parts);
            this.blockMap = blockMap;
            this.predicateMap = predicateMap;
            this.controllerBase = controllerBase;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (BlockPos pos : blockMap.keySet()) {
                min = Math.min(min, pos.getY());
                max = Math.max(max, pos.getY());
            }
            minY = min;
            maxY = max;
        }
    }
}
