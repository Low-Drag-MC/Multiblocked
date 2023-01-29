package com.lowdragmc.multiblocked.api.capability;

import com.google.gson.*;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.ContentModifier;
import com.lowdragmc.multiblocked.api.recipe.serde.content.IContentSerializer;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Used to detect whether a machine has a certain capability. And provide its capability in proxy {@link CapabilityProxy}.
 */
public abstract class MultiblockCapability<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    public final String name;
    public final int color;
    public IContentSerializer<T> serializer;

    protected MultiblockCapability(String name, int color, IContentSerializer<T> serializer) {
        this.name = name;
        this.color = color;
        this.serializer = serializer;
    }

    public String getUnlocalizedName() {
        return "multiblocked.capability." + name;
    }

    /**
     * default content for the RecipeMapWidget selector
     */
    public abstract T defaultContent();

    /**
     * detect whether this block has capability
     */
    public abstract boolean isBlockHasCapability(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity);

    /**
     * deep copy of this content. recipe need it for searching and such things
     */
    public abstract T copyInner(T content);

    /**
     * deep copy and modify the size attribute for those Content that have the size attribute.
     */
    public  T copyWithModifier(T content, ContentModifier modifier){
        return copyInner(content);
    }

    @SuppressWarnings("unchecked")
    public final T copyContent(Object content) {
        return copyInner((T) content);
    }

    /**
     * create a proxy of this block.
     */
    protected abstract CapabilityProxy<? extends T> createProxy(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity);

    /**
     * create a proxy of this block.
     */
    public CapabilityProxy<? extends T> createProxy(@Nonnull IO io, @Nonnull BlockEntity tileEntity, Direction facing, @Nullable Map<Long, Set<String>> slotsMap) {
        CapabilityProxy<? extends T> proxy = createProxy(io, tileEntity);
        proxy.facing = facing;
        proxy.slots = slotsMap == null ? null : slotsMap.get(tileEntity.getBlockPos().asLong());
        if (tileEntity instanceof IInnerCapabilityProvider slotNameProvider) {
            if (proxy.slots == null) {
                proxy.slots = slotNameProvider.getSlotNames();
            } else {
                proxy.slots.addAll(slotNameProvider.getSlotNames());
            }
        }
        return proxy;
    }

    /**
     * Create a Widget of given contents
     */
    public ContentWidget<? super T> createContentWidget() {
        return new ContentWidget<T>() {
            @Override
            protected void onContentUpdate() {
                if (Multiblocked.isClient()) {
                    setHoverTooltips(LocalizationUtils.format("multiblocked.content.miss", io, LocalizationUtils.format(MultiblockCapability.this.getUnlocalizedName())));
                }
            }

            @Override
            public void openConfigurator(WidgetGroup dialog) {
                super.openConfigurator(dialog);
                dialog.addWidget(new LabelWidget(5, 30, "multiblocked.gui.label.configurator"));
            }

        }.setBackground(new ColorRectTexture(color));
    }


    /**
     * create ui for jei recipes
     */
    public void handleRecipeUI(Widget widget, Content in, IngredientIO ingredientIO) {
    }

    public boolean hasTrait() {
        return false;
    }

    public CapabilityTrait createTrait() {
        return null;
    }

    public <C> Set<C> getCapability(Capability<C> capability, @Nonnull
            BlockEntity tileEntity) {
        Set<C> found = new LinkedHashSet<>();
        for (Direction facing : Direction.values()) {
            tileEntity.getCapability(capability, facing).ifPresent(found::add);
        }
        return found;
    }

    /**
     * Get candidate blocks for display in JEI as well as automated builds
     */
    public abstract BlockInfo[] getCandidates();

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public final BlockComponent getAnyBlock() {
        return (BlockComponent) MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, name + ".any"));
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        return serializer.toJson(src);
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return serializer.fromJson(json);
    }

    @SuppressWarnings("unchecked")
    public final JsonElement serialize(Object obj) {
        return serializer.toJson((T) obj);
    }

    public final T deserialize(JsonElement jsonElement) {
        return serializer.fromJson(jsonElement);
    }

    /**
     * used for recipe builder via KubeJs.
     */
    public T of(Object o) {
        return serializer.of(o);
    }

}
