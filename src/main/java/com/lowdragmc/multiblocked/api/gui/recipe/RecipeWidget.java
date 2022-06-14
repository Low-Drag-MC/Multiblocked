package com.lowdragmc.multiblocked.api.gui.recipe;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import net.minecraft.client.resources.language.I18n;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Map;
import java.util.function.DoubleSupplier;

public class RecipeWidget extends WidgetGroup {
    public final Recipe recipe;
    public final DraggableScrollableWidgetGroup inputs;
    public final DraggableScrollableWidgetGroup outputs;

    public RecipeWidget(Recipe recipe, ResourceTexture progress, IGuiTexture background) {
        this(recipe, ProgressWidget.JEIProgress, progress, background);
    }

    public RecipeWidget(Recipe recipe, ResourceTexture progress) {
        this(recipe, ProgressWidget.JEIProgress, progress, new ColorRectTexture(0x1f000000));
    }

    public RecipeWidget(Recipe recipe, DoubleSupplier doubleSupplier, ResourceTexture progress, IGuiTexture background) {
        super(0, 0, 176, 84);
        this.recipe = recipe;
        setClientSideWidget();
        inputs = new DraggableScrollableWidgetGroup(5, 5, 64, 64).setBackground(background);
        outputs = new DraggableScrollableWidgetGroup(176 - 64 - 5, 5, 64, 64).setBackground(background);
        this.addWidget(inputs);
        this.addWidget(outputs);
        String duration = I18n.get("multiblocked.recipe.duration", this.recipe.duration / 20.);
        this.addWidget(new ProgressWidget(doubleSupplier, 78, 27, 20, 20, progress).setHoverTooltips(duration));
        this.addWidget(new LabelWidget(5, 73, duration).setTextColor(0xff000000).setDrop(false));
        if (recipe.text != null) {
            this.addWidget(new LabelWidget(80, 73, recipe.text.getString()).setTextColor(0xff000000).setDrop(false));
        }
        int index = 0;
        for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : recipe.inputs.entrySet()) {
            MultiblockCapability<?> capability = entry.getKey();
            for (Content in : entry.getValue()) {
                inputs.addWidget(capability.createContentWidget().setContent(IO.IN, in, false).setSelfPosition(2 + 20 * (index % 3), 2 + 20 * (index / 3)));
                index++;
            }
        }
        for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : recipe.tickInputs.entrySet()) {
            MultiblockCapability<?> capability = entry.getKey();
            for (Content in : entry.getValue()) {
                inputs.addWidget(capability.createContentWidget().setContent(IO.IN, in, true).setSelfPosition(2 + 20 * (index % 3), 2 + 20 * (index / 3)));
                index++;
            }
        }
        if (index > 9) {
            inputs.setSize(new Size(64 + 4, 64));
            inputs.setYScrollBarWidth(4).setYBarStyle(null, new ColorRectTexture(-1));
        }

        index = 0;
        for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : recipe.outputs.entrySet()) {
            MultiblockCapability<?> capability = entry.getKey();
            for (Content out : entry.getValue()) {
                outputs.addWidget(capability.createContentWidget().setContent(IO.OUT, out, false).setSelfPosition(2 + 20 * (index % 3), 2 + 20 * (index / 3)));
                index++;
            }
        }
        for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : recipe.tickOutputs.entrySet()) {
            MultiblockCapability<?> capability = entry.getKey();
            for (Content out : entry.getValue()) {
                outputs.addWidget(capability.createContentWidget().setContent(IO.OUT, out, true).setSelfPosition(2 + 20 * (index % 3), 2 + 20 * (index / 3)));
                index++;
            }
        }
        if (index > 9) {
            outputs.setSize(new Size(64 + 4, 64));
            outputs.setYScrollBarWidth(4).setYBarStyle(null, new ColorRectTexture(-1));
        }
        index = 0;
        for (RecipeCondition condition : recipe.conditions) {
            index++;
            if (condition.isReverse()) {
                this.addWidget(new ImageWidget(168 - index * 16, 70, 16, 16, condition.getValidTexture())
                        .setHoverTooltips(new TranslatableComponent("multiblocked.gui.condition.reverse"), condition.getTooltips()));
            } else {
                this.addWidget(new ImageWidget(168 - index * 16, 70, 16, 16, condition.getValidTexture())
                        .setHoverTooltips(condition.getTooltips()));
            }

        }
    }
}
