package com.lowdragmc.multiblocked.api.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.ICapabilityProxyHolder;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Recipe {
    public static final ImmutableMap<String, Object> EMPTY = ImmutableMap.of();
    public final String uid;
    public final ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> inputs;
    public final ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> outputs;
    public final ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> tickInputs;
    public final ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> tickOutputs;
    public final ImmutableMap<String, Object> data;
    public final int duration;
    public final ITextComponent text;
    public final ImmutableList<RecipeCondition> conditions;

    public Recipe(String uid,
                  ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> inputs,
                  ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> outputs,
                  ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> tickInputs,
                  ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> tickOutputs,
                  ImmutableList<RecipeCondition> conditions,
                  int duration) {
        this(uid, inputs, outputs, tickInputs, tickOutputs, conditions, EMPTY, null, duration);
    }

    public Recipe(String uid,
                  ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> inputs,
                  ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> outputs,
                  ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> tickInputs,
                  ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> tickOutputs,
                  ImmutableList<RecipeCondition> conditions,
                  ImmutableMap<String, Object> data,
                  ITextComponent text,
                  int duration) {
        this.uid = uid;
        this.inputs = inputs;
        this.outputs = outputs;
        this.tickInputs = tickInputs;
        this.tickOutputs = tickOutputs;
        this.duration = duration;
        this.data = data;
        this.text = text;
        this.conditions = conditions;
    }

    public Object getData(String key) {
        if (data.containsKey(key)) {
            return data.get(key);
        }
        return null;
    }

    public List<Content> getInputContents(MultiblockCapability<?> capability) {
        if (inputs.containsKey(capability)) {
            return inputs.get(capability);
        } else {
            return Collections.emptyList();
        }
    }

    public List<Content> getOutputContents(MultiblockCapability<?> capability) {
        if (outputs.containsKey(capability)) {
            return outputs.get(capability);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Does the recipe match the owned proxy.
     *
     * @param holder proxies
     * @return result
     */
    public boolean matchRecipe(ICapabilityProxyHolder holder) {
        if (!holder.hasProxies()) return false;
        if (!matchRecipe(IO.IN, holder, inputs)) return false;
        if (!matchRecipe(IO.OUT, holder, outputs)) return false;
        return true;
    }

    public boolean matchTickRecipe(ICapabilityProxyHolder holder) {
        if (hasTick()) {
            if (!holder.hasProxies()) return false;
            if (!matchRecipe(IO.IN, holder, tickInputs)) return false;
            if (!matchRecipe(IO.OUT, holder, tickOutputs)) return false;
        }
        return true;
    }

    @SuppressWarnings("ALL")
    public boolean matchRecipe(IO io, ICapabilityProxyHolder holder, ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> contents) {
        Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilityProxies = holder.getCapabilitiesProxy();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : contents.entrySet()) {
            Set<CapabilityProxy<?>> used = new HashSet<>();
            List content = new ArrayList<>();
            Map<String, List> contentSlot = new HashMap<>();
            for (Content cont : entry.getValue()) {
                if (cont.slotName == null) {
                    content.add(cont.content);
                } else {
                    contentSlot.computeIfAbsent(cont.slotName, s->new ArrayList<>()).add(cont.content);
                }
            }
            if (content.isEmpty() && contentSlot.isEmpty()) continue;
            if (content.isEmpty()) content = null;
            if (capabilityProxies.contains(io, entry.getKey())) {
                for (CapabilityProxy<?> proxy : capabilityProxies.get(io, entry.getKey()).values()) { // search same io type
                    if (used.contains(proxy)) continue;
                    used.add(proxy);
                    if (content != null) {
                        content = proxy.searchingRecipe(io, this, content, null);
                    }
                    if (proxy.slots != null) {
                        Iterator<String> iterator = contentSlot.keySet().iterator();
                        while (iterator.hasNext()) {
                            String key = iterator.next();
                            if (proxy.slots.contains(key)) {
                                List<?> left = proxy.searchingRecipe(io, this, contentSlot.get(key), key);
                                if (left == null) iterator.remove();
                            }
                        }
                    }
                    if (content == null && contentSlot.isEmpty()) break;
                }
            }
            if (content == null && contentSlot.isEmpty()) continue;
            if (capabilityProxies.contains(IO.BOTH, entry.getKey())) {
                for (CapabilityProxy<?> proxy : capabilityProxies.get(IO.BOTH, entry.getKey()).values()) { // search both type
                    if (used.contains(proxy)) continue;
                    used.add(proxy);
                    if (content != null) {
                        content = proxy.searchingRecipe(io, this, content, null);
                    }
                    if (proxy.slots != null) {
                        Iterator<String> iterator = contentSlot.keySet().iterator();
                        while (iterator.hasNext()) {
                            String key = iterator.next();
                            if (proxy.slots.contains(key)) {
                                List<?> left = proxy.searchingRecipe(io, this, contentSlot.get(key), key);
                                if (left == null) iterator.remove();
                            }
                        }
                    }
                    if (content == null && contentSlot.isEmpty()) break;
                }
            }
            if (content != null || !contentSlot.isEmpty()) return false;
        }
        return true;
    }

    public boolean handleTickRecipeIO(IO io, ICapabilityProxyHolder holder) {
        if (!holder.hasProxies() || io == IO.BOTH) return false;
        return handleRecipe(io, holder, io == IO.IN ? tickInputs : tickOutputs);
    }

    public boolean handleRecipeIO (IO io, ICapabilityProxyHolder holder) {
        if (!holder.hasProxies() || io == IO.BOTH) return false;
        return handleRecipe(io, holder, io == IO.IN ? inputs : outputs);
    }

    @SuppressWarnings("ALL")
    public boolean handleRecipe(IO io, ICapabilityProxyHolder holder, ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> contents) {
        Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilityProxies = holder.getCapabilitiesProxy();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : contents.entrySet()) {
            Set<CapabilityProxy<?>> used = new HashSet<>();
            List content = new ArrayList<>();
            Map<String, List> contentSlot = new HashMap<>();
            for (Content cont : entry.getValue()) {
                if (cont.chance == 1 || Multiblocked.RNG.nextFloat() < cont.chance) { // chance input
                    if (cont.slotName == null) {
                        content.add(cont.content);
                    } else {
                        contentSlot.computeIfAbsent(cont.slotName, s->new ArrayList<>()).add(cont.content);
                    }
                }
            }
            if (content.isEmpty() && contentSlot.isEmpty()) continue;
            if (content.isEmpty()) content = null;
            if (capabilityProxies.contains(io, entry.getKey())) {
                for (CapabilityProxy<?> proxy : capabilityProxies.get(io, entry.getKey()).values()) { // search same io type
                    if (used.contains(proxy)) continue;
                    used.add(proxy);
                    if (content != null) {
                        content = proxy.handleRecipe(io, this, content, null);
                    }
                    if (proxy.slots != null) {
                        Iterator<String> iterator = contentSlot.keySet().iterator();
                        while (iterator.hasNext()) {
                            String key = iterator.next();
                            if (proxy.slots.contains(key)) {
                                List<?> left = proxy.handleRecipe(io, this, contentSlot.get(key), key);
                                if (left == null) iterator.remove();
                            }
                        }
                    }
                    if (content == null && contentSlot.isEmpty()) break;
                }
            }
            if (content == null && contentSlot.isEmpty()) continue;
            if (capabilityProxies.contains(IO.BOTH, entry.getKey())){
                for (CapabilityProxy<?> proxy : capabilityProxies.get(IO.BOTH, entry.getKey()).values()) { // search both type
                    if (used.contains(proxy)) continue;
                    used.add(proxy);
                    if (content != null) {
                        content = proxy.handleRecipe(io, this, content, null);
                    }
                    if (proxy.slots != null) {
                        Iterator<String> iterator = contentSlot.keySet().iterator();
                        while (iterator.hasNext()) {
                            String key = iterator.next();
                            if (proxy.slots.contains(key)) {
                                List<?> left = proxy.handleRecipe(io, this, contentSlot.get(key), key);
                                if (left == null) iterator.remove();
                            }
                        }
                    }
                    if (content == null && contentSlot.isEmpty()) break;
                }
            }
            if (content != null || !contentSlot.isEmpty()) {
                Multiblocked.LOGGER.warn("io error while handling a recipe {} outputs. holder: {}", uid, holder);
                return false;
            }
        }
        return true;
    }

    public boolean hasTick() {
        return !tickInputs.isEmpty() || !tickOutputs.isEmpty();
    }

    public void preWorking(ICapabilityProxyHolder holder) {
        handlePre(inputs, holder, IO.IN);
        handlePre(outputs, holder, IO.OUT);
    }

    public void postWorking(ICapabilityProxyHolder holder) {
        handlePost(inputs, holder, IO.IN);
        handlePost(outputs, holder, IO.OUT);
    }

    public void handlePre(ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> contents, ICapabilityProxyHolder holder, IO io) {
        contents.forEach(((capability, tuples) -> {
            if (holder.getCapabilitiesProxy().contains(io, capability)) {
                for (CapabilityProxy<?> capabilityProxy : holder.getCapabilitiesProxy().get(io, capability).values()) {
                    capabilityProxy.preWorking(holder, io, this);
                }
            } else if (holder.getCapabilitiesProxy().contains(IO.BOTH, capability)) {
                for (CapabilityProxy<?> capabilityProxy : holder.getCapabilitiesProxy().get(IO.BOTH, capability).values()) {
                    capabilityProxy.preWorking(holder, io, this);
                }
            }
        }));
    }

    public void handlePost(ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> contents, ICapabilityProxyHolder holder, IO io) {
        contents.forEach(((capability, tuples) -> {
            if (holder.getCapabilitiesProxy().contains(io, capability)) {
                for (CapabilityProxy<?> capabilityProxy : holder.getCapabilitiesProxy().get(io, capability).values()) {
                    capabilityProxy.postWorking(holder, io, this);
                }
            } else if (holder.getCapabilitiesProxy().contains(IO.BOTH, capability)) {
                for (CapabilityProxy<?> capabilityProxy : holder.getCapabilitiesProxy().get(IO.BOTH, capability).values()) {
                    capabilityProxy.postWorking(holder, io, this);
                }
            }
        }));
    }

    public boolean checkConditions(@Nonnull RecipeLogic recipeLogic) {
        return conditions.isEmpty() || conditions.stream().allMatch(c -> c.test(this, recipeLogic) ^ c.isReverse());
    }
}
