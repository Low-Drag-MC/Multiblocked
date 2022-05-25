package com.lowdragmc.multiblocked.api.pattern.predicates;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.pattern.error.PatternStringError;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PredicateAnyCapability extends SimplePredicate {
    public String capability = "item";

    public PredicateAnyCapability() {
        super("capability");
    }
    
    public PredicateAnyCapability(MultiblockCapability<?> capability) {
        this();
        this.capability = capability.name;
        buildPredicate();
    }

    @Override
    public SimplePredicate buildPredicate() {
        MultiblockCapability<?> capability = MbdCapabilities.get(this.capability);
        if (capability == null) {
            predicate = state -> false;
            candidates = () -> new BlockInfo[] {new BlockInfo(Blocks.BARRIER)};
            return this;
        }
        predicate = state -> state.getBlockState().getBlock() == capability.getAnyBlock() || checkCapability(io, capability, state);
        candidates = () -> new BlockInfo[]{BlockInfo.fromBlockState(capability.getAnyBlock().defaultBlockState())};
        toolTips = new ArrayList<>();
        toolTips.add(String.format("Any Capability: %s IO: %s", capability.name, io == null ? "NULL" : io.name()));
        return this;
    }

    private static boolean checkCapability(IO io, MultiblockCapability<?> capability, MultiblockState state) {
        if (io != null) {
            BlockEntity tileEntity = state.getTileEntity();
            if (tileEntity != null && capability.isBlockHasCapability(io, tileEntity)) {
                Map<Long, EnumMap<IO, Set<MultiblockCapability<?>>>> capabilities = state.getMatchContext().getOrCreate("capabilities", Long2ObjectOpenHashMap::new);
                capabilities.computeIfAbsent(state.getPos().asLong(), l-> new EnumMap<>(IO.class))
                        .computeIfAbsent(io, x->new HashSet<>())
                        .add(capability);
                return true;
            }
        }
        state.setError(new PatternStringError(LocalizationUtils.format("multiblocked.pattern.error.capability", LocalizationUtils.format(capability.getUnlocalizedName()), io == null ? "NULL" : io.name())));
        return false;
    }

    @Override
    public List<WidgetGroup> getConfigWidget(List<WidgetGroup> groups) {
        super.getConfigWidget(groups);
        WidgetGroup group = new WidgetGroup(0, 0, 100, 20);
        groups.add(group);
        MultiblockCapability<?> current = MbdCapabilities.get(capability);
        group.addWidget(new SelectorWidget(0, 0, 120, 20, MbdCapabilities.CAPABILITY_REGISTRY.values().stream().map(MultiblockCapability::getUnlocalizedName).collect(
                Collectors.toList()), -1)
                .setValue(current == null ? "" : current.getUnlocalizedName())
                .setOnChanged(capability-> {
                    this.capability = capability.replace("multiblocked.capability.", "");
                    buildPredicate();
                })
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(new ColorRectTexture(0xffaaaaaa))
                .setHoverTooltips("multiblocked.gui.predicate.capability"));
        return groups;
    }

    @Override
    public JsonObject toJson(JsonObject jsonObject) {
        jsonObject.addProperty("capability", capability);
        return super.toJson(jsonObject);
    }

    @Override
    public void fromJson(Gson gson, JsonObject jsonObject) {
        capability = GsonHelper.getAsString(jsonObject, "capability", "");
        super.fromJson(gson, jsonObject);
    }
}
