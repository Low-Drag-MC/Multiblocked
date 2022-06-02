package com.lowdragmc.multiblocked.client.renderer.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.data.IModelData;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimatableModel;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.GeoModelProvider;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;
import software.bernie.geckolib3.resource.GeckoLibCache;
import software.bernie.geckolib3.util.RenderUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class GeoComponentRenderer extends AnimatedGeoModel<GeoComponentRenderer.ComponentFactory> implements IMultiblockedRenderer, IGeoRenderer<GeoComponentRenderer.ComponentFactory> {
    public final static GeoComponentRenderer INSTANCE = new GeoComponentRenderer(null, false);
    private static final Set<String> particleTexture = new HashSet<>();

    static {
        if (Multiblocked.isClient()) {
            AnimationController.addModelFetcher((IAnimatable object) -> {
                if (object instanceof ComponentFactory) {
                    ComponentFactory factory = (ComponentFactory) object;
                    return (IAnimatableModel<Object>) factory.renderer.getGeoModelProvider();
                }
                return null;
            });
        }
    }

    public final String modelName;
    public final boolean isGlobal;
    @OnlyIn(Dist.CLIENT)
    private ComponentFactory itemFactory;

    public GeoComponentRenderer(String modelName, boolean isGlobal) {
        this.modelName = modelName;
        this.isGlobal = isGlobal;
        if (Multiblocked.isClient() && modelName != null && particleTexture.add(modelName)) {
            registerTextureSwitchEvent();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderItem(ItemStack stack, ItemTransforms.TransformType transformType, boolean leftHand, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel bakedModel) {
        if (itemFactory == null) {
            itemFactory = new ComponentFactory(null, this);
        }
        GeoModel model = this.getModel(this.getModelLocation(itemFactory));
        this.setLivingAnimations(itemFactory, this.getUniqueID(itemFactory));
        matrixStack.pushPose();
        matrixStack.translate(0, 0.01f, 0);
        matrixStack.translate(0.5, 0, 0.5);
        render(model, itemFactory, Minecraft.getInstance().getFrameTime(), RenderType.entityTranslucent(getTextureLocation(itemFactory)),
                matrixStack, buffer, null, combinedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        matrixStack.popPose();
    }


    @Override
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> renderModel(BlockAndTintGetter level, BlockPos pos,
                                       BlockState state, Direction side,
                                       Random rand, IModelData modelData) {
        return Collections.emptyList();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onTextureSwitchEvent(TextureStitchEvent.Pre event) {
        event.addSprite(new ResourceLocation(Multiblocked.MODID, modelName));
    }

    @Nonnull
    @Override
    @OnlyIn(Dist.CLIENT)
    public TextureAtlasSprite getParticleTexture() {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(new ResourceLocation(Multiblocked.MODID, modelName));
    }

    @Override
    public boolean isGlobalRenderer(@Nonnull BlockEntity te) {
        return isGlobal;
    }

    @Override
    public String getType() {
        return "geo";
    }

    @Override
    public IMultiblockedRenderer fromJson(Gson gson, JsonObject jsonObject) {
        return new GeoComponentRenderer(jsonObject.get("modelName").getAsString(), GsonHelper.getAsBoolean(jsonObject, "isGlobal", false));
    }

    @Override
    public JsonObject toJson(Gson gson, JsonObject jsonObject) {
        jsonObject.addProperty("modelName", modelName);
        if (isGlobal) {
            jsonObject.addProperty("isGlobal", true);
        }
        return jsonObject;
    }

    @Override
    public Supplier<IMultiblockedRenderer> createConfigurator(WidgetGroup parent, DraggableScrollableWidgetGroup group, IMultiblockedRenderer current) {
        TextFieldWidget tfw = new TextFieldWidget(1, 1, 150, 20, null, null);
        File path = new File(Multiblocked.location, "assets/multiblocked/geo");
        AtomicBoolean isGlobal = new AtomicBoolean(false);
        if (current instanceof GeoComponentRenderer) {
            tfw.setCurrentString(((GeoComponentRenderer) current).modelName);
            isGlobal.set(((GeoComponentRenderer) current).isGlobal);
        }
        group.addWidget(new ButtonWidget(155, 1, 20, 20, cd -> DialogWidget.showFileDialog(parent, "select a geo file", path, true,
                DialogWidget.suffixFilter(".geo.json"), r -> {
                    if (r != null && r.isFile()) {
                        tfw.setCurrentString(r.getName().replace(".geo.json", ""));
                    }
                })).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/darkened_slot.png"), new TextTexture("F", -1)).setHoverTooltips("multiblocked.gui.tips.file_selector"));
        group.addWidget(tfw);
        group.addWidget(createBoolSwitch(1,25, "isGlobal", "multiblocked.gui.predicate.geo.0", isGlobal.get(), isGlobal::set));
        return () -> {
            if (tfw.getCurrentString().isEmpty()) {
                return null;
            } else {
                return new GeoComponentRenderer(tfw.getCurrentString(), isGlobal.get());
            }
        };
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isRaw() {
        return !GeckoLibCache.getInstance().getGeoModels().containsKey(this.getModelLocation(null));
    }

    @Override
    public boolean hasTESR(BlockEntity tileEntity) {
        return true;
    }

    @Override
    public void onPostAccess(IComponent component) {
        component.setRendererObject(null);

    }

    @Override
    public void onPreAccess(IComponent component) {
        component.setRendererObject(new ComponentFactory(component, this));
    }

    @Override
    public void render(BlockEntity te, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (te instanceof IComponent && ((IComponent) te).getRendererObject() instanceof  ComponentFactory) {
            IComponent controller = (IComponent) te;
            ComponentFactory factory = (ComponentFactory) controller.getRendererObject();
            GeoModel model = this.getModel(this.getModelLocation(factory));
            this.setLivingAnimations(factory, this.getUniqueID(factory));

            stack.pushPose();
            stack.translate(0, 0.01f, 0);
            stack.translate(0.5, 0, 0.5);

            switch (controller.getFrontFacing()) {
                case SOUTH:
                    stack.mulPose(Vector3f.YP.rotationDegrees(180));
                    break;
                case WEST:
                    stack.mulPose(Vector3f.YP.rotationDegrees(90));
                    break;
                case NORTH:
                    stack.mulPose(Vector3f.YP.rotationDegrees(0));
                    break;
                case EAST:
                    stack.mulPose(Vector3f.YP.rotationDegrees(270));
                    break;
                case UP:
                    stack.mulPose(Vector3f.XP.rotationDegrees(90));
                    break;
                case DOWN:
                    stack.mulPose(Vector3f.XN.rotationDegrees(90));
                    break;
            }

            render(model, stack, buffer, combinedLight);
            stack.popPose();
        }
    }

    void render(GeoModel model, PoseStack matrixStackIn, MultiBufferSource buffers, int packedLightIn) {
        VertexConsumer currentBuffer = buffers.getBuffer(RenderType.entityCutout(getTextureLocation(null)));
        for (GeoBone group : model.topLevelBones) {
            currentBuffer = renderRecursively(group, matrixStackIn, buffers, currentBuffer, packedLightIn);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public VertexConsumer renderRecursively(GeoBone bone, PoseStack stack, MultiBufferSource buffers, VertexConsumer currentBuffer, int packedLightIn) {
        if (bone.name.startsWith("emissive")) {
            packedLightIn = 0xf000f0;
        }
        boolean isTranslucent = bone.name.startsWith("translucent");
        if (isTranslucent) {
            currentBuffer = buffers.getBuffer(RenderType.entityTranslucentCull(getTextureLocation(null)));
        }
        stack.pushPose();
        RenderUtils.translate(bone, stack);
        RenderUtils.moveToPivot(bone, stack);
        RenderUtils.rotate(bone, stack);
        RenderUtils.scale(bone, stack);
        RenderUtils.moveBackFromPivot(bone, stack);

        if (!bone.isHidden()) {
            for (GeoCube cube : bone.childCubes) {
                stack.pushPose();
                if (!bone.cubesAreHidden()) {
                    renderCube(cube, stack, currentBuffer, packedLightIn, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                }
                stack.popPose();
            }
        }
        if (!bone.childBonesAreHiddenToo()) {
            for (GeoBone childBone : bone.childBones) {
                currentBuffer = renderRecursively(childBone, stack, buffers, currentBuffer, packedLightIn);
            }
        }
        if (isTranslucent) {
            currentBuffer = buffers.getBuffer(RenderType.entityCutout(getTextureLocation(null)));
        }
        stack.popPose();
        return currentBuffer;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ComponentFactory entity) {
        return new ResourceLocation(Multiblocked.MODID, String.format("animations/%s.animation.json", modelName));
    }

    @Override
    public ResourceLocation getModelLocation(ComponentFactory animatable) {
        return new ResourceLocation(Multiblocked.MODID, String.format("geo/%s.geo.json", modelName));
    }

    @Override
    public ResourceLocation getTextureLocation(ComponentFactory entity) {
        return new ResourceLocation(Multiblocked.MODID, String.format("textures/%s.png", modelName));
    }

    @Override
    public GeoModelProvider<?> getGeoModelProvider() {
        return this;
    }

    public static class ComponentFactory implements IAnimatable {
        public final IComponent component;
        public final GeoComponentRenderer renderer;
        public final AnimationFile animationFile;
        public String currentStatus;

        public ComponentFactory(IComponent component, GeoComponentRenderer renderer) {
            this.component = component;
            this.renderer = renderer;
            animationFile = GeckoLibCache.getInstance().getAnimations().get(renderer.getAnimationFileLocation(this));
        }

        private final AnimationFactory factory = new AnimationFactory(this);

        private PlayState predicate(AnimationEvent<ComponentFactory> event) {
            AnimationController<ComponentFactory> controller = event.getController();
            String lastStatus = currentStatus;
            currentStatus = component.getStatus();
            if (!Objects.equals(lastStatus, currentStatus)) {
                if (currentStatus == null) return PlayState.STOP;
                AnimationBuilder animationBuilder = new AnimationBuilder();
                if (lastStatus != null) {
                    Animation trans = animationFile.getAnimation(lastStatus + "-" + currentStatus);
                    if (trans != null) animationBuilder.addAnimation(trans.animationName);
                }
                if (animationFile.getAnimation(currentStatus) != null) {
                    animationBuilder.addAnimation(currentStatus);
                }
                controller.setAnimation(animationBuilder);
            }
            return PlayState.CONTINUE;
        }

        @Override
        public void registerControllers(AnimationData data) {
            data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
        }

        @Override
        public AnimationFactory getFactory() {
            return factory;
        }

    }

}
