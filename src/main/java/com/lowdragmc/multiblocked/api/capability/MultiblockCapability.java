package com.lowdragmc.multiblocked.api.capability;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Used to detect whether a machine has a certain capability. And provide its capability in proxy {@link CapabilityProxy}.
 *
 */
public abstract class MultiblockCapability<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    public final String name;
    public final int color;

    protected MultiblockCapability(String name, int color) {
        this.name = name;
        this.color = color;
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
    public abstract boolean isBlockHasCapability(@Nonnull IO io, @Nonnull TileEntity tileEntity);

    /**
     * deep copy of this content. recipe need it for searching and such things
     */
    public abstract T copyInner(T content);
    
    /**
     * create a proxy of this block.
     */
    protected abstract CapabilityProxy<? extends T> createProxy(@Nonnull IO io, @Nonnull TileEntity tileEntity);

    /**
     * create a proxy of this block.
     */
    public CapabilityProxy<? extends T> createProxy(@Nonnull IO io, @Nonnull TileEntity tileEntity, Direction facing, Map<Long, Set<String>> slotsMap) {
        CapabilityProxy<? extends T> proxy = createProxy(io, tileEntity);
        proxy.facing = facing;
        proxy.slots = slotsMap == null ? null : slotsMap.get(tileEntity.getBlockPos().asLong());
        if (tileEntity instanceof IInnerCapabilityProvider) {
            IInnerCapabilityProvider slotNameProvider = (IInnerCapabilityProvider) tileEntity;
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

    public <C> Set<C> getCapability(Capability<C> capability, @Nonnull TileEntity tileEntity) {
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

    public final JsonElement serialize(Object obj) {
        return serialize((T)obj, null, null);
    }

    public final T deserialize(JsonElement jsonElement){
        return deserialize(jsonElement, null, null);
    }

    /**
     * used for recipe builder via KubeJs.
     */
    public abstract T of(Object o);
}
