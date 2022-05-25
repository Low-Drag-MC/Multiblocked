package com.lowdragmc.multiblocked.api.gui.blueprint_table.components;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.PhantomSlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.TabButton;
import com.lowdragmc.lowdraglib.gui.widget.TextBoxWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.gui.blueprint_table.RecipeMapBuilderWidget;
import com.lowdragmc.multiblocked.api.gui.blueprint_table.TemplateBuilderWidget;
import com.lowdragmc.multiblocked.api.gui.dialogs.JsonBlockPatternWidget;
import com.lowdragmc.multiblocked.api.pattern.JsonBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.predicates.PredicateComponent;
import com.lowdragmc.multiblocked.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDBlockStateRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ControllerWidget extends ComponentWidget<ControllerDefinition>{
    protected JsonBlockPattern pattern;
    protected final WidgetGroup S3;
    protected final WidgetGroup S4;
    protected final SceneWidget sceneWidget;
    protected final Set<DummyComponentTileEntity> tiles = new HashSet<>();
    protected boolean isFormed;
    protected String recipeMap;

    public ControllerWidget(WidgetGroup group, ControllerDefinition definition, JsonBlockPattern pattern, String recipeMap, Consumer<JsonObject> onSave) {
        super(group, definition, onSave);
        this.pattern = pattern;
        this.recipeMap = recipeMap;
        int x = 47;
        SimplePredicate predicate = this.pattern.predicates.get("controller");
        if (predicate instanceof PredicateComponent) {
            ((PredicateComponent) predicate).definition = definition;
        }
        S1.addWidget(createBoolSwitch(x + 100, 90, "consumeCatalyst", "multiblocked.gui.widget.controller.consume", definition.consumeCatalyst, r -> definition.consumeCatalyst = r));

        IItemHandlerModifiable handler;
        PhantomSlotWidget phantomSlotWidget = new PhantomSlotWidget(handler = new ItemStackHandler(1), 0, x + 205, 73);
        LabelWidget labelWidget = new LabelWidget(x + 230, 78, "multiblocked.gui.label.catalyst");
        S1.addWidget(phantomSlotWidget);
        S1.addWidget(labelWidget);
        phantomSlotWidget.setClearSlotOnRightClick(true)
                .setChangeListener(() -> definition.catalyst = handler.getStackInSlot(0))
                .setBackgroundTexture(new ColorBorderTexture(1, -1))
                .setHoverTooltips("multiblocked.gui.widget.controller.catalyst")
                .setVisible(definition.catalyst != null);
        handler.setStackInSlot(0, definition.catalyst == null ? ItemStack.EMPTY : definition.catalyst);
        labelWidget.setVisible(definition.catalyst != null);

        S1.addWidget(createBoolSwitch(x + 100, 75, "needCatalyst", "multiblocked.gui.widget.controller.need_catalyst", definition.catalyst != null, r -> {
            definition.catalyst = !r ? null : ItemStack.EMPTY;
            phantomSlotWidget.setVisible(r);
            labelWidget.setVisible(r);
        }));

        tabContainer.addTab((TabButton) new TabButton(88, 26, 20, 20)
                        .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0.5, 1, 0.5), new TextTexture("S3"))
                        .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0, 1, 0.5), new TextTexture("S3"))
                        .setHoverTooltips("multiblocked.gui.widget.controller.s3"),
                S3 = new WidgetGroup(0, 0, getSize().width, getSize().height));
        S3.addWidget(new ImageWidget(50, 66, 138, 138, new GuiTextureGroup(new ColorBorderTexture(3, -1), new ColorRectTexture(0xaf444444))));
        S3.addWidget(sceneWidget = new SceneWidget(50, 66, 138, 138, null)
                .useCacheBuffer()
                .setRenderFacing(false)
                .setRenderSelect(false));
        ResourceTexture PAGE = new ResourceTexture("multiblocked:textures/gui/structure_page.png");
        sceneWidget.addWidget(new SwitchWidget(138 - 20, 138 - 20, 16, 16, this::onFormedSwitch)
                .setPressed(isFormed)
                .setTexture(PAGE.getSubTexture(222 / 256.0, 0, 16 / 256.0, 16 / 256.0), PAGE.getSubTexture(222 / 256.0, 16 / 256.0, 16 / 256.0, 16 / 256.0))
                .setHoverTooltips("multiblocked.structure_page.switch"));
        S3.addWidget(new TextBoxWidget(200, 0, 175, Collections.singletonList("")).setFontColor(-1).setShadow(true));
        S3.addWidget(new ButtonWidget(200, 66, 100, 20,
                new GuiTextureGroup(ResourceBorderTexture.BAR, new TextTexture("multiblocked.gui.label.pattern_settings", -1).setDropShadow(true)), cd -> {
            new JsonBlockPatternWidget(this, this.pattern.copy(), this::savePattern);
        }).setHoverBorderTexture(1, -1));
        updateScene(this.pattern);

        tabContainer.addTab((TabButton) new TabButton(111, 26, 20, 20)
                        .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0.5, 1, 0.5), new TextTexture("S4"))
                        .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0, 1, 0.5), new TextTexture("S4"))
                        .setHoverTooltips("multiblocked.gui.widget.controller.s4"),
                S4 = new WidgetGroup(0, 0, getSize().width, getSize().height));
        S4.addWidget(new LabelWidget(80, 55, "multiblocked.gui.label.recipe_map"));
        S4.addWidget(new TextFieldWidget(80, 70, 100, 15, () -> this.recipeMap, s -> this.recipeMap = s));
        S4.addWidget(new RecipeMapBuilderWidget(this, 188, 50, 150, 170).setOnRecipeMapSelected(recipeMap1 -> this.recipeMap = recipeMap1.name));
    }

    @Override
    protected JsonObject getJsonObj() {
        JsonObject jsonObject = super.getJsonObj();
        jsonObject.add("basePattern", Multiblocked.GSON.toJsonTree(pattern));
        jsonObject.addProperty("recipeMap", this.recipeMap == null ? RecipeMap.EMPTY.name : this.recipeMap);
        if (definition.catalyst == null) {
            jsonObject.add("catalyst", null);
        }
        return jsonObject;
    }

    private void onFormedSwitch(ClickData clickData, boolean isPressed) {
        isFormed = isPressed;
        if (thread == null) {
            tiles.forEach(t->t.isFormed = isFormed);
        }
        sceneWidget.needCompileCache();
    }

    @OnlyIn(Dist.CLIENT)
    Thread thread;

    @OnlyIn(Dist.CLIENT)
    private void updateScene(JsonBlockPattern jsonPattern) {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        TrackedDummyWorld world = new TrackedDummyWorld();
        tiles.clear();
        sceneWidget.createScene(world);
        ImageWidget imageWidget;
        sceneWidget.addWidget(imageWidget = new ImageWidget(0, 0, sceneWidget.getSize().width, sceneWidget.getSize().height));
        imageWidget.setVisible(jsonPattern.pattern.length * jsonPattern.pattern[0].length * jsonPattern.pattern[0][0].length() > 1000);
        thread = new Thread(()->{
            int[] centerOffset = jsonPattern.getCenterOffset();
            String[][] pattern = jsonPattern.pattern;
            Set<BlockPos> posSet = new HashSet<>();
            int offset = Math.max(pattern.length, Math.max(pattern[0].length, pattern[0][0].length()));
            int sum = jsonPattern.pattern.length * jsonPattern.pattern[0].length * jsonPattern.pattern[0][0].length();
            AtomicDouble progress = new AtomicDouble(0);
            imageWidget.setImage(new TextTexture("building scene!").setSupplier(()-> "building scene! " + String.format("%.1f", progress.get()) + "%%").setWidth(sceneWidget.getSize().width));
            int count = 0;
            for (int i = 0; i < pattern.length; i++) {
                for (int j = 0; j < pattern[0].length; j++) {
                    for (int k = 0; k < pattern[0][0].length(); k++) {
                        if (Thread.interrupted()) {
                            sceneWidget.waitToRemoved(imageWidget);
                            return;
                        }
                        count++;
                        progress.set(count * 100.0 / sum);
                        char symbol = pattern[i][j].charAt(k);
                        BlockPos pos = jsonPattern.getActualPosOffset(k - centerOffset[2], j - centerOffset[1], i - centerOffset[0], Direction.NORTH).offset(offset, offset, offset);
                        world.addBlock(pos, BlockInfo.fromBlockState(MbdComponents.DummyComponentBlock.defaultBlockState()));
                        DummyComponentTileEntity tileEntity = (DummyComponentTileEntity) world.getBlockEntity(pos);
                        ComponentDefinition definition = null;
                        assert tileEntity != null;
                        boolean disableFormed = false;
                        if (jsonPattern.symbolMap.containsKey(symbol)) {
                            Set<BlockInfo> candidates = new HashSet<>();
                            for (String s : jsonPattern.symbolMap.get(symbol)) {
                                SimplePredicate predicate = jsonPattern.predicates.get(s);
                                if (predicate instanceof PredicateComponent && ((PredicateComponent) predicate).definition != null) {
                                    definition = ((PredicateComponent) predicate).definition;
                                    disableFormed |= predicate.disableRenderFormed;
                                    break;
                                } else if (predicate != null && predicate.candidates != null) {
                                    candidates.addAll(Arrays.asList(predicate.candidates.get()));
                                    disableFormed |= predicate.disableRenderFormed;
                                }
                            }
                            definition = TemplateBuilderWidget.getComponentDefinition(definition, candidates);
                        }
                        if (definition != null) {
                            tileEntity.setDefinition(definition);
                            if (disableFormed) {
                                definition.formedRenderer = new MBDBlockStateRenderer(
                                        Blocks.AIR.defaultBlockState());
                            }
                        }
                        tileEntity.isFormed = isFormed;
                        posSet.add(pos);
                        tiles.add(tileEntity);
                    }
                }
            }
            Minecraft.getInstance().execute(()->{
                sceneWidget.setRenderedCore(posSet, null);
                sceneWidget.waitToRemoved(imageWidget);
            });
            thread = null;
        });
        thread.start();
    }

    private void savePattern(JsonBlockPattern patternResult) {
        if (patternResult != null) {
            pattern = patternResult;
            pattern.cleanUp();
            updateScene(this.pattern);
        }
    }

    @Override
    protected void updateRegistryName(String s) {
        super.updateRegistryName(s);
        SimplePredicate predicate = pattern.predicates.get("controller");
        if (predicate instanceof PredicateComponent) {
            ((PredicateComponent) predicate).location = location;
        }
    }
}
