package com.lowdragmc.multiblocked.api.gui.blueprint_table.components;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.TabButton;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.gui.widget.TextBoxWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.gui.dialogs.IRendererWidget;
import com.lowdragmc.multiblocked.api.gui.dialogs.ResourceTextureWidget;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.function.Consumer;

public class ComponentWidget<T extends ComponentDefinition> extends
        DialogWidget {
    protected final T definition;
    protected ResourceLocation location;
    protected final TabContainer tabContainer;
    protected final WidgetGroup S1;
    protected final WidgetGroup S2;
    protected final WidgetGroup JSON;
    private final DraggableScrollableWidgetGroup tfGroup;
    private final TextBoxWidget textBox;
    private final Consumer<JsonObject> onSave;
    private boolean isPretty;

    public ComponentWidget(WidgetGroup group, T definition, Consumer<JsonObject> onSave) {
        super(group, true);
        setParentInVisible();
        this.onSave = onSave;
        this.definition = definition;
        this.location = definition.location;
        this.addWidget(new ImageWidget(0, 0, 384, 256, new ResourceTexture("multiblocked:textures/gui/component.png")));
        this.addWidget(tabContainer = new TabContainer(0, 0, 384, 256).setOnChanged(this::onTabChanged));
        tabContainer.addTab((TabButton) new TabButton(42, 26, 20, 20)
                        .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0.5, 1, 0.5), new TextTexture("S1"))
                        .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0, 1, 0.5), new TextTexture("S1"))
                        .setHoverTooltips("multiblocked.gui.widget.component.s1"),
                S1 = new WidgetGroup(0, 0, getSize().width, getSize().height));
        int x = 47;
        S1.addWidget(new LabelWidget(x, 57, "multiblocked.gui.label.registry_name"));
        S1.addWidget(new TextFieldWidget(x + 80, 54, 150, 15,  null, this::updateRegistryName).setResourceLocationOnly().setCurrentString(this.location.toString()));
        S1.addWidget(createBoolSwitch(x, 75, "allowRotate", "multiblocked.gui.widget.component.allowRotate", definition.allowRotate, r -> definition.allowRotate = r));
        S1.addWidget(createBoolSwitch(x, 90, "showInJei", "multiblocked.gui.widget.component.jei", definition.showInJei, r -> definition.showInJei = r));
        S1.addWidget(createBoolSwitch(x, 105, "isOpaqueCube", "multiblocked.gui.widget.component.opaque", definition.properties.isOpaque, r -> definition.properties.isOpaque = r));
        S1.addWidget(createScene(x - 2, 125, "baseRenderer", "multiblocked.gui.widget.component.basic_renderer", definition.baseRenderer, r -> definition.baseRenderer = r));
        S1.addWidget(createScene(x + 98, 125, "formedRenderer", "multiblocked.gui.widget.component.formed_renderer", definition.formedRenderer, r -> definition.formedRenderer = r));
        S1.addWidget(createScene(x + 198, 125, "workingRenderer", "multiblocked.gui.widget.component.working_renderer", definition.workingRenderer, r -> definition.workingRenderer = r));

        tabContainer.addTab((TabButton) new TabButton(65, 26, 20, 20)
                        .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0.5, 1, 0.5), new TextTexture("S2"))
                        .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0, 1, 0.5), new TextTexture("S2"))
                        .setHoverTooltips("multiblocked.gui.widget.component.s2"),
                S2 = new WidgetGroup(0, 0, getSize().width, getSize().height));
        int y = 55;
        for (MultiblockCapability<?> capability : MbdCapabilities.CAPABILITY_REGISTRY.values()) {
            if (capability.hasTrait()) {
                WidgetGroup widgetGroup = new WidgetGroup(47, y, 100, 15);
                Runnable configurator = () -> {
                    DialogWidget dialog = new DialogWidget(group, true);
                    CapabilityTrait trait = capability.createTrait();
                    trait.serialize(definition.traits.get(capability.name));

                    // set background
                    int xOffset = (384 - 176) / 2;
                    dialog.addWidget(new ImageWidget(0, 0, 384, 256, new ColorRectTexture(0xaf000000)));
                    ImageWidget imageWidget;
                    dialog.addWidget(imageWidget = new ImageWidget(xOffset, 0, 176, 256, null));
                    imageWidget.setImage(new ResourceTexture(JSONUtils.getAsString(definition.traits, "background", "multiblocked:textures/gui/custom_gui.png")));
                    dialog.addWidget(new ButtonWidget(xOffset - 20,10, 20, 20, new ResourceTexture("multiblocked:textures/gui/option.png"), cd2 -> {
                        new ResourceTextureWidget(dialog, texture -> {
                            if (texture != null) {
                                imageWidget.setImage(texture);
                            }
                        });
                    }).setHoverTooltips("multiblocked.gui.widget.component.set_bg"));

                    // open trait settings
                    trait.openConfigurator(dialog);

                    // save when closed
                    dialog.setOnClosed(() -> {
                        String background = ((ResourceTexture)imageWidget.getImage()).imageLocation.toString();
                        if (!background.equals("multiblocked:textures/gui/custom_gui.png")) {
                            definition.traits.addProperty("background", background);
                        }
                        definition.traits.add(capability.name, trait.deserialize());
                    });
                };
                ButtonWidget buttonWidget = new ButtonWidget(20, 0, 15, 15, new ResourceTexture("multiblocked:textures/gui/option.png"), cd -> {
                    if (definition.traits.has(capability.name)) {
                        configurator.run();
                    }
                });
                buttonWidget.setVisible(definition.traits.has(capability.name));
                widgetGroup.addWidget(buttonWidget);
                widgetGroup.addWidget(new SwitchWidget(0, 0, 15, 15, (cd, r)-> {
                    if (r) {
                        configurator.run();
                    } else {
                        definition.traits.remove(capability.name);
                    }
                    buttonWidget.setVisible(r);
                })
                        .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0,1,0.5))
                        .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0.5,1,0.5))
                        .setHoverTexture(new ColorBorderTexture(1, 0xff545757))
                        .setPressed(definition.traits.has(capability.name))
                        .setHoverTooltips(capability.getUnlocalizedName()));
                widgetGroup.addWidget(new LabelWidget(40, 3, capability.getUnlocalizedName()));
                S2.addWidget(widgetGroup);
                y += 15;
            }
        }

        tabContainer.addTab((TabButton) new TabButton(235, 26, 20, 20)
                        .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0.5, 1, 0.5), new TextTexture("J"))
                        .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/switch_common.png").getSubTexture(0, 0, 1, 0.5), new TextTexture("F"))
                        .setHoverTooltips("multiblocked.gui.widget.component.f"),
                JSON = new WidgetGroup(0, 0, getSize().width, getSize().height));
        JSON.addWidget(new SwitchWidget(50, 54, 16, 16, (cd,r) -> {
            isPretty = r;
            updatePatternJson();
        }).setHoverBorderTexture(1, -1).setTexture(new ResourceTexture("multiblocked:textures/gui/pretty.png"), new ResourceTexture("multiblocked:textures/gui/pretty_active.png")).setHoverTooltips("multiblocked.gui.tips.pretty"));
        JSON.addWidget(new ButtonWidget(70, 54, 16, 16, cd -> Minecraft.getInstance().keyboardHandler.setClipboard(isPretty ? Multiblocked.prettyJson(getComponentJson()) : getComponentJson())).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/copy.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.copy"));
        JSON.addWidget(new ImageWidget(47, 75, 285, 136, new ColorBorderTexture(1, 0xafafaf00)));
        JSON.addWidget(tfGroup = new DraggableScrollableWidgetGroup(47, 75, 285, 136)
                .setBackground(new ColorRectTexture(0x8f111111))
                .setYScrollBarWidth(4)
                .setYBarStyle(null, new ColorRectTexture(-1)));
        tfGroup.addWidget(textBox = new TextBoxWidget(0, 0, 285, Collections.singletonList("")).setFontColor(-1).setShadow(true));

        this.addWidget(new ButtonWidget(260, 26, 80, 20, null, cd -> {
            if (onSave != null) onSave.accept(getJsonObj());
            super.close();
        }).setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.label.save_pattern", -1).setDropShadow(true)).setHoverBorderTexture(1, -1));
    }

    @Override
    public void close() {
        super.close();
        if (onSave != null) onSave.accept(null);
    }

    protected void onTabChanged(WidgetGroup oldTag, WidgetGroup newTag) {
        if (newTag == JSON) {
            updatePatternJson();
        }
    }

    protected JsonObject getJsonObj() {
        JsonObject jsonObject = (JsonObject) Multiblocked.GSON.toJsonTree(definition);
        jsonObject.addProperty("location", location.toString());
        return jsonObject;
    }

    private String getComponentJson() {
        return Multiblocked.GSON.toJson(getJsonObj());
    }

    private void updatePatternJson() {
        textBox.setContent(Collections.singletonList(isPretty ? Multiblocked.prettyJson(getComponentJson()) : getComponentJson()));
        tfGroup.computeMax();
    }

    protected void updateRegistryName(String s) {
        location = (s != null && !s.isEmpty()) ? new ResourceLocation(s) : location;
    }

    protected WidgetGroup createBoolSwitch(int x, int y, String text, String tips, boolean init, Consumer<Boolean> onPressed) {
        WidgetGroup widgetGroup = new WidgetGroup(x, y, 100, 15);
        widgetGroup.addWidget(new SwitchWidget(0, 0, 15, 15, (cd, r)->onPressed.accept(r))
                .setBaseTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0,1,0.5))
                .setPressedTexture(new ResourceTexture("multiblocked:textures/gui/boolean.png").getSubTexture(0,0.5,1,0.5))
                .setHoverTexture(new ColorBorderTexture(1, 0xff545757))
                .setPressed(init)
                .setHoverTooltips(tips));
        widgetGroup.addWidget(new LabelWidget(20, 3, text));
        return widgetGroup;
    }

    @OnlyIn(Dist.CLIENT)
    protected WidgetGroup createScene(int x, int y, String text, String tips, IMultiblockedRenderer init, Consumer<IMultiblockedRenderer> onUpdate) {
        TrackedDummyWorld world = new TrackedDummyWorld();
        world.addBlock(BlockPos.ZERO, BlockInfo.fromBlockState(MbdComponents.DummyComponentBlock.defaultBlockState()));
        DummyComponentTileEntity tileEntity = (DummyComponentTileEntity) world.getBlockEntity(BlockPos.ZERO);
        tileEntity.setDefinition(new PartDefinition(new ResourceLocation(Multiblocked.MODID, "component_widget")));
        tileEntity.getDefinition().baseRenderer = init;
        WidgetGroup widgetGroup = new WidgetGroup(x, y, 90, 90);
        widgetGroup.addWidget(new LabelWidget(0, 0, text));
        widgetGroup.addWidget(new ImageWidget(0, 12,  90, 80, new ColorBorderTexture(2, 0xff4A82F7)));
        widgetGroup.addWidget(new SceneWidget(0, 12,  90, 80, world)
                .setRenderedCore(Collections.singleton(BlockPos.ZERO), null)
                .setRenderSelect(false)
                .setRenderFacing(false));
        widgetGroup.addWidget(new ButtonWidget(90-15, 12, 15, 15, new ResourceTexture("multiblocked:textures/gui/option.png"),(cd) ->
                new IRendererWidget(this, tileEntity.getRenderer(), r -> {
                    tileEntity.getDefinition().baseRenderer = r;
                    onUpdate.accept(r);
                })).setHoverBorderTexture(1, -1).setHoverTooltips(tips));

        return widgetGroup;
    }
}
