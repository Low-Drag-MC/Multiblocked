package com.lowdragmc.multiblocked.common.capability.widget;

import com.google.common.collect.Lists;
import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.gui.ingredient.Target;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class FluidContentWidget extends ContentWidget<FluidStack> {
    FluidTank fluidTank;

    @Override
    protected void onContentUpdate() {
        if (fluidTank == null) {
            addWidget(new TankWidget(fluidTank = new FluidTank(content.getAmount()), 1, 1, false, false).setOnAddedTooltips((s, l)-> {
                if (chance < 1) {
                    l.add(chance == 0 ? new TranslatableComponent("multiblocked.gui.content.chance_0") : new TranslatableComponent("multiblocked.gui.content.chance_1", String.format("%.1f", chance * 100)));
                }
                if (perTick) {
                    l.add(new TranslatableComponent("multiblocked.gui.content.per_tick"));
                }
            }));
        }
        fluidTank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        fluidTank.setCapacity(content.getAmount());
        fluidTank.fill(content.copy(), IFluidHandler.FluidAction.EXECUTE);
    }

    @Nullable
    @Override
    public Object getJEIIngredient(FluidStack content) {
        if (LDLMod.isReiLoaded()) {
            return EntryStacks.of(FluidStackHooksForge.fromForge(content));
        }
        return super.getJEIIngredient(content);
    }

    @Override
    public List<Target> getPhantomTargets(Object ingredient) {
        List<Target> pattern = super.getPhantomTargets(ingredient);
        if (pattern != null && pattern.size() > 0) return pattern;
        if (LDLMod.isReiLoaded() && ingredient instanceof dev.architectury.fluid.FluidStack fluidStack) {
            ingredient = FluidStackHooksForge.toForge(fluidStack);
        }
        if (!(ingredient instanceof FluidStack) && PhantomFluidWidget.drainFrom(ingredient) == null) {
            return Collections.emptyList();
        }

        Rect2i rectangle = toRectangleBox();
        return Lists.newArrayList(new Target() {
            @Nonnull
            @Override
            public Rect2i getArea() {
                return rectangle;
            }

            @Override
            public void accept(@Nonnull Object ingredient) {
                FluidStack content;
                if (LDLMod.isReiLoaded() && ingredient instanceof dev.architectury.fluid.FluidStack fluidStack) {
                    ingredient = FluidStackHooksForge.toForge(fluidStack);
                }
                if (ingredient instanceof FluidStack)
                    content = (FluidStack) ingredient;
                else
                    content = PhantomFluidWidget.drainFrom(ingredient);
                if (content != null) {
                    setContent(io, getJEIContent(content), chance, perTick);
                    if (onPhantomUpdate != null) {
                        onPhantomUpdate.accept(FluidContentWidget.this);
                    }
                }
            }
        });
    }

    @Override
    public FluidStack getJEIContent(Object content) {
        if (content instanceof FluidStack) {
            return new FluidStack(((FluidStack) content).getFluid(), this.content.getAmount());
        }
        return null;
    }

    @Override
    public void openConfigurator(WidgetGroup dialog) {
        super.openConfigurator(dialog);
        int x = 5;
        int y = 25;
        dialog.addWidget(new LabelWidget(5, y + 3, "multiblocked.gui.label.amount"));
        dialog.addWidget(new TextFieldWidget(125 - 60, y, 60, 15,  null, number -> {
            content = new FluidStack(content, Integer.parseInt(number));
            onContentUpdate();
        }).setNumbersOnly(1, Integer.MAX_VALUE).setCurrentString(content.getAmount()+""));
    }
}
