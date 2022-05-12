package com.lowdragmc.multiblocked.common.capability.widget;

import com.google.common.collect.Lists;
import com.lowdragmc.lowdraglib.gui.ingredient.Target;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.PhantomFluidWidget;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FluidContentWidget extends ContentWidget<FluidStack> {
    FluidTank fluidTank;

    @Override
    protected void onContentUpdate() {
        if (Multiblocked.isClient()) {
            List<String> tooltips = new ArrayList<>();
            tooltips.add(content.getFluid().getAttributes().getDisplayName(content).getContents());
            tooltips.add(I18n.get("multiblocked.fluid.amount", content.getAmount(), content.getAmount()));
            tooltips.add(I18n.get("multiblocked.fluid.temperature", content.getFluid().getAttributes().getTemperature(content)));
            tooltips.add(I18n.get(content.getFluid().getAttributes().isGaseous(content) ? "multiblocked.fluid.state_gas" : "multiblocked.fluid.state_liquid"));
            setHoverTooltips(tooltips.stream().reduce((a, b) -> a + "\n" + b).orElse("fluid"));
        }
        if (fluidTank == null) {
            addWidget(new TankWidget(fluidTank = new FluidTank(content.getAmount()), 1, 1, false, false).setDrawHoverTips(false));
        }
        fluidTank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        fluidTank.setCapacity(content.getAmount());
        fluidTank.fill(content.copy(), IFluidHandler.FluidAction.EXECUTE);
    }



    @Override
    public List<Target> getPhantomTargets(Object ingredient) {
        List<Target> pattern = super.getPhantomTargets(ingredient);
        if (pattern != null && pattern.size() > 0) return pattern;

        if (!(ingredient instanceof FluidStack) && PhantomFluidWidget.drainFrom(ingredient) == null) {
            return Collections.emptyList();
        }

        Rectangle2d rectangle = toRectangleBox();
        return Lists.newArrayList(new Target() {
            @Nonnull
            @Override
            public Rectangle2d getArea() {
                return rectangle;
            }

            @Override
            public void accept(@Nonnull Object ingredient) {
                FluidStack content;
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
