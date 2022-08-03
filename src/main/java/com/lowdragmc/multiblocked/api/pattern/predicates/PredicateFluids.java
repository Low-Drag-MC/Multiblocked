package com.lowdragmc.multiblocked.api.pattern.predicates;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PredicateFluids extends SimplePredicate {
    public Fluid[] fluids = new Fluid[0];

    public PredicateFluids() {
        super("fluids");
    }

    public PredicateFluids(Fluid... fluids) {
        this();
        this.fluids = fluids;
        buildPredicate();
    }

    @Override
    public SimplePredicate buildPredicate() {
        fluids = Arrays.stream(fluids).filter(Objects::nonNull).toArray(Fluid[]::new);
        if (fluids.length == 0) fluids = new Fluid[]{Fluids.WATER};
        predicate = state -> ArrayUtils.contains(fluids, state.getBlockState().getBlock());
        candidates = () -> Arrays.stream(fluids).map(fluid -> BlockInfo.fromBlockState(fluid.defaultFluidState().createLegacyBlock())).toArray(BlockInfo[]::new);
        return this;
    }

    @Override
    public List<WidgetGroup> getConfigWidget(List<WidgetGroup> groups) {
        super.getConfigWidget(groups);
        WidgetGroup group = new WidgetGroup(0, 0, 182, 100);
        groups.add(group);
        DraggableScrollableWidgetGroup container = new DraggableScrollableWidgetGroup(0, 25, 182, 80).setBackground(new ColorRectTexture(0xffaaaaaa));
        group.addWidget(container);
        List<Fluid> bucketList = Arrays.stream(fluids).filter(Objects::nonNull).collect(Collectors.toList());
        for (Fluid bucket : bucketList) {
            addBlockSelectorWidget(bucketList, container, bucket);
        }
        group.addWidget(new LabelWidget(0, 6, "multiblocked.gui.label.fluid_settings"));
        group.addWidget(new ButtonWidget(162, 0, 20, 20, cd -> {
            bucketList.add(null);
            addBlockSelectorWidget(bucketList, container, null);
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/add.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.predicate.blocks.add"));
        return groups;
    }

    private void addBlockSelectorWidget(List<Fluid> blockList, DraggableScrollableWidgetGroup container, Fluid initFluid) {
        WidgetGroup bsw = new WidgetGroup(0, container.widgets.size() * 21 + 1, 160, 20);
        container.addWidget(bsw);
        bsw.addWidget(new ButtonWidget(163, 1, 18, 18, cd -> {
            int index = (bsw.getSelfPosition().y - 1) / 21;
            blockList.remove(index);
            updateStates(blockList);
            for (int i = index + 1; i < container.widgets.size(); i++) {
                container.widgets.get(i).addSelfPosition(0, -21);
            }
            container.waitToRemoved(bsw);
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/remove.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.remove"));

        AtomicReference<Fluid> Fluid = new AtomicReference<>(initFluid);
        TextFieldWidget textField = (TextFieldWidget) new TextFieldWidget(22, 0, 160 - 26, 20, null, s -> {
            if (s != null && !s.isEmpty()) {
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(s));
                if (fluid == null || fluid == Fluids.EMPTY) {
                    Fluid.set(null);
                } else if (fluid != Fluid.get()) {
                    Fluid.set(fluid);

                    int index = (bsw.getSelfPosition().y - 1) / 21;
                    blockList.set(index, fluid);
                    updateStates(blockList);
                }
            }
        }).setResourceLocationOnly().setHoverTooltips("multiblocked.gui.tips.fluid_register");
        FluidTank handler;
        bsw.addWidget(new PhantomFluidWidget(handler = new FluidTank(1000), 1, 1)
                .setFluidStackUpdater(fluidStack -> {
                    if (fluidStack == null || fluidStack.isEmpty()) {
                        Fluid.set(null);
                    } else if (fluidStack.getFluid() != Fluid.get()){
                        Fluid.set(fluidStack.getFluid());
                    }

                    int index = (bsw.getSelfPosition().y - 1) / 21;
                    blockList.set(index, Fluid.get());
                    updateStates(blockList);
                    textField.setCurrentString(Fluid.get() == null ? "" : Fluid.get().getRegistryName().toString());
                }).setBackground(new ColorBorderTexture(1, -1)));
        handler.setFluid(new FluidStack(initFluid, 1000));
        bsw.addWidget(textField);
        textField.setCurrentString(Fluid.get() == null ? "" : Fluid.get().getRegistryName().toString());
    }

    private void updateStates(List<Fluid> blockList) {
        fluids = blockList.stream().filter(Objects::nonNull).toArray(Fluid[]::new);
        buildPredicate();
    }

    @Override
    public JsonObject toJson(JsonObject jsonObject) {
        JsonArray jsonArray = new JsonArray();
        for (Fluid fluid : fluids) {
            if (fluid.getRegistryName() != null) {
                final JsonObject blockObject = new JsonObject();
                blockObject.addProperty("id", fluid.getRegistryName().toString());
                jsonArray.add(blockObject);
            }
        }
        jsonObject.add("fluids", jsonArray);
        return super.toJson(jsonObject);
    }

    @Override
    public void fromJson(Gson gson, JsonObject jsonObject) {
        JsonArray jsonArray = jsonObject.get("fluids").getAsJsonArray();
        fluids = new Fluid[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            ResourceLocation id = new ResourceLocation(jsonArray.get(i).getAsJsonObject().get("id").getAsString());
            fluids[i] = ForgeRegistries.FLUIDS.getValue(id);
        }
        super.fromJson(gson, jsonObject);
    }
}
