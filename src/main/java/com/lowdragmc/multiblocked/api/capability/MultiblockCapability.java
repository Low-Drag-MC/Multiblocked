package com.lowdragmc.multiblocked.api.capability;

import com.google.gson.*;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.serde.content.IContentSerializer;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import org.checkerframework.checker.units.qual.K;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @SuppressWarnings("unchecked")
    public final T copyContent(Object content) {
        return copyInner((T) content);
    }

    /**
     * create a proxy of this block.
     */
    public abstract CapabilityProxy<? extends T> createProxy(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity);

    /**
     * Create a Widget of given contents
     */
    public ContentWidget<? super T> createContentWidget() {
        return new ContentWidget<T>() {
            @Override
            protected void onContentUpdate() {
                if (Multiblocked.isClient()) {
                    setHoverTooltips(I18n.get("multiblocked.content.miss", io, I18n.get(MultiblockCapability.this.getUnlocalizedName())));
                }
            }

            @Override
            public void openConfigurator(WidgetGroup dialog) {
                super.openConfigurator(dialog);
                dialog.addWidget(new LabelWidget(5, 30, "multiblocked.gui.label.configurator"));
            }

        }.setBackground(new ColorRectTexture(color));
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
