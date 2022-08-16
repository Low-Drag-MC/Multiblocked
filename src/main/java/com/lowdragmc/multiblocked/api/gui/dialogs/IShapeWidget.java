package com.lowdragmc.multiblocked.api.gui.dialogs;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class IShapeWidget extends DialogWidget {
    public Consumer<VoxelShape> onSave;
    public final DummyComponentTileEntity tileEntity;
    private final DraggableScrollableWidgetGroup container;
    private final List<AxisAlignedBB> aabbs;
    private VoxelShape shape;

    public IShapeWidget(WidgetGroup parent, IMultiblockedRenderer renderer, VoxelShape shape, Consumer<VoxelShape> onSave) {
        super(parent, true);
        this.onSave = onSave;
        this.shape = shape;
        this.addWidget(new ImageWidget(0, 0, getSize().width, getSize().height, new ColorRectTexture(0xaf000000)));
        TrackedDummyWorld world = new TrackedDummyWorld();
        world.addBlock(BlockPos.ZERO, BlockInfo.fromBlockState(MbdComponents.DummyComponentBlock.defaultBlockState()));
        tileEntity = (DummyComponentTileEntity) world.getBlockEntity(BlockPos.ZERO);
        setNewRenderer(renderer);
        this.addWidget(new ImageWidget(35, 59, 138, 138, new GuiTextureGroup(new ColorBorderTexture(3, -1), new ColorRectTexture(0xaf444444))));
        this.addWidget(new SceneWidget(35, 59,  138, 138, world) {
            @Override
            @OnlyIn(Dist.CLIENT)
            public void renderBlockOverLay(WorldSceneRenderer renderer) {
                super.renderBlockOverLay(renderer);
                MatrixStack matrixStack = new MatrixStack();

                RenderSystem.enableBlend();
                RenderSystem.disableDepthTest();
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                matrixStack.pushPose();

                Tessellator tessellator = Tessellator.getInstance();
                RenderSystem.disableTexture();
                BufferBuilder buffer = tessellator.getBuilder();
                buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                RenderSystem.lineWidth(3);
                Matrix4f matrix4f = matrixStack.last().pose();

                IShapeWidget.this.shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
                    buffer.vertex(matrix4f, (float)(x0), (float)(y0), (float)(z0)).color(255, 0, 0, 255).endVertex();
                    buffer.vertex(matrix4f, (float)(x1), (float)(y1), (float)(z1)).color(255, 0, 0, 255).endVertex();
                });

                tessellator.end();

                matrixStack.popPose();
                RenderSystem.enableDepthTest();
            }
        }
                .setRenderedCore(Collections.singleton(BlockPos.ZERO), null)
                .setRenderSelect(false)
                .setRenderFacing(false));
        this.addWidget(new ButtonWidget(210, 55, 40, 20, this::onUpdate)
                .setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.tips.update", -1).setDropShadow(true))
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips("multiblocked.gui.tips.update"));
        container = new DraggableScrollableWidgetGroup(180, 80, 185, 120).setBackground(new ColorRectTexture(0xffaaaaaa));
        this.addWidget(container);
        this.addWidget(new ButtonWidget(320, 55, 40, 20, cd -> {
            onSave.accept(this.shape);
            super.close();
        })
                .setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.tips.save_1", -1).setDropShadow(true))
                .setHoverBorderTexture(1, -1)
                .setHoverTooltips("multiblocked.gui.tips.save"));
        aabbs = this.shape.toAabbs();

        this.addWidget(new ButtonWidget(180, 55, 20, 20, new ResourceTexture("multiblocked:textures/gui/add.png"), cd->{
            aabbs.add(new AxisAlignedBB(0, 0, 0, 0, 0, 0));
            updateShapeList();
        }).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.shape.add"));
        updateShapeList();
    }

    private void updateShapeList() {
        container.clearAllWidgets();
        for (int i = 0; i < aabbs.size(); i++) {
            WidgetGroup group = new WidgetGroup(2, container.widgets.size() * 35, container.getSize().width - 4, 30);
            group.setBackground(new ColorRectTexture(0x5f444444));
            final int finalI = i;

            int x = 0;
            group.addWidget(new LabelWidget(x, 3, "minX"));
            group.addWidget(new TextFieldWidget(x + 25, 3, 25, 10, null, s->{
                AxisAlignedBB aabb = aabbs.get(finalI);
                aabbs.set(finalI, new AxisAlignedBB(Float.parseFloat(s), aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ));
            }).setNumbersOnly(0f, 2f).setCurrentString(aabbs.get(finalI).minX + ""));
            x += 55;

            group.addWidget(new LabelWidget(x, 3, "minY"));
            group.addWidget(new TextFieldWidget(x + 25, 3, 25, 10, null, s->{
                AxisAlignedBB aabb = aabbs.get(finalI);
                aabbs.set(finalI, new AxisAlignedBB(aabb.minX, Float.parseFloat(s), aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ));
            }).setNumbersOnly(0f, 2f).setCurrentString(aabbs.get(finalI).minY + ""));
            x += 55;

            group.addWidget(new LabelWidget(x, 3, "minZ"));
            group.addWidget(new TextFieldWidget(x + 25, 3, 25, 10, null, s->{
                AxisAlignedBB aabb = aabbs.get(finalI);
                aabbs.set(finalI, new AxisAlignedBB(aabb.minX, aabb.minY, Float.parseFloat(s), aabb.maxX, aabb.maxY, aabb.maxZ));
            }).setNumbersOnly(0f, 2f).setCurrentString(aabbs.get(finalI).minZ + ""));
            x = 0;

            group.addWidget(new LabelWidget(x, 20, "maxX"));
            group.addWidget(new TextFieldWidget(x + 25, 20, 25, 10, null, s->{
                AxisAlignedBB aabb = aabbs.get(finalI);
                aabbs.set(finalI, new AxisAlignedBB(aabb.minX, aabb.minY, aabb.minZ, Float.parseFloat(s), aabb.maxY, aabb.maxZ));
            }).setNumbersOnly(0f, 2f).setCurrentString(aabbs.get(finalI).maxX + ""));
            x += 55;

            group.addWidget(new LabelWidget(x, 20, "maxY"));
            group.addWidget(new TextFieldWidget(x + 25, 20, 25, 10, null, s->{
                AxisAlignedBB aabb = aabbs.get(finalI);
                aabbs.set(finalI, new AxisAlignedBB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, Float.parseFloat(s), aabb.maxZ));
            }).setNumbersOnly(0f, 2f).setCurrentString(aabbs.get(finalI).maxY + ""));
            x += 55;

            group.addWidget(new LabelWidget(x, 20, "maxZ"));
            group.addWidget(new TextFieldWidget(x + 25, 20, 25, 10, null, s->{
                AxisAlignedBB aabb = aabbs.get(finalI);
                aabbs.set(finalI, new AxisAlignedBB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, Float.parseFloat(s)));
            }).setNumbersOnly(0f, 2f).setCurrentString(aabbs.get(finalI).maxZ + ""));
            container.addWidget(group);
            x += 55;

            group.addWidget(new ButtonWidget(x, 8, 15, 15, new ResourceTexture("multiblocked:textures/gui/remove.png"), cd -> {
                aabbs.remove(finalI);
                updateShapeList();
            }).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.remove"));
        }
    }

    @Override
    public void close() {
        super.close();
        if (onSave != null) {
            onSave.accept(shape);
        }
    }

    public void setNewRenderer(IMultiblockedRenderer newRenderer) {
        PartDefinition definition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, "i_renderer"));
        definition.getBaseStatus().setRenderer(newRenderer);
        tileEntity.setDefinition(definition);
    }

    private void onUpdate(ClickData clickData) {
        shape = aabbs.stream().map(VoxelShapes::create).reduce(VoxelShapes.empty(), VoxelShapes::or);
    }

}
