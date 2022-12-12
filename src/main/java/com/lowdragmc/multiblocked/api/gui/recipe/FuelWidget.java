package com.lowdragmc.multiblocked.api.gui.recipe;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2022/11/29
 * @implNote FuelWidget
 */
public class FuelWidget extends WidgetGroup {
    public final RecipeMap recipeMap;
    public final Recipe recipe;
    public final DraggableScrollableWidgetGroup inputs;

    public FuelWidget(RecipeMap recipeMap, Recipe recipe) {
        super(0, 0, 176, 44);
        this.recipeMap = recipeMap;
        this.recipe = recipe;
        String duration = LocalizationUtils.format("multiblocked.recipe.duration", recipe.duration / 20.);
        setClientSideWidget();
        inputs = new DraggableScrollableWidgetGroup(5, 5, 64, 24).setBackground(new ColorRectTexture(0x1f000000));
        this.addWidget(inputs);
        this.addWidget(new ImageWidget(176 - 64 - 5 + 22, 7, 20, 20, recipeMap.categoryTexture));
        this.addWidget(new ProgressWidget(ProgressWidget.JEIProgress, 78, 7, 20, 20, recipeMap.fuelTexture).setFillDirection(ProgressTexture.FillDirection.DOWN_TO_UP).setHoverTooltips(duration));
        this.addWidget(new LabelWidget(5, 33, duration).setTextColor(0xff000000).setDropShadow(false));

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

        Map<String, List<RecipeCondition>> conditionMap = new HashMap<>();
        for (RecipeCondition condition : recipe.conditions) {
            if (condition.isReverse()) {
                conditionMap.computeIfAbsent(condition.getType(), s->new ArrayList<>()).add(condition);
            } else {
                conditionMap.computeIfAbsent(condition.getType(), s->new ArrayList<>()).add(0, condition);
            }
        }

        index = 0;
        for (Map.Entry<String, List<RecipeCondition>> entry : conditionMap.entrySet()) {
            List<RecipeCondition> list = entry.getValue();
            if (!list.isEmpty()) {
                index++;
                boolean reversed = false;
                List<Component> components = new ArrayList<>();
                for (RecipeCondition condition : list) {
                    if (!reversed && condition.isReverse()) {
                        reversed = true;
                        components.add(new TranslatableComponent("multiblocked.gui.condition.reverse"));
                    }
                    components.add(condition.getTooltips());
                }
                this.addWidget(new ImageWidget(168 - index * 16, 30, 16, 16, list.get(0).getValidTexture()).setHoverTooltips(components));
            }

        }
    }
}
