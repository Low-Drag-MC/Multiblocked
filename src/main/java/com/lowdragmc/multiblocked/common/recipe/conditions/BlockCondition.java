package com.lowdragmc.multiblocked.common.recipe.conditions;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.BlockSelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/6/14
 * @implNote BlockCondition, check whether such blocks in the structure
 */
public class BlockCondition extends RecipeCondition {
    public final static BlockCondition INSTANCE = new BlockCondition();

    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public int count = 0;

    private BlockCondition() {
    }

    public BlockCondition(BlockState blockState, int count) {
        this.blockState = blockState;
        this.count = count;
    }

    @Override
    public String getType() {
        return "block";
    }

    @Override
    public Component getTooltips() {
        return blockState.getBlock().getName().append(new TranslatableComponent("multiblocked.gui.condition.block.count")).append(" (" + count + ")");
    }

    @Override
    public boolean test(@Nonnull Recipe recipe, @Nonnull RecipeLogic recipeLogic) {
        int amount = 0;
        for (BlockPos pos : recipeLogic.controller.state.getCache()) {
            if (recipeLogic.controller.getLevel().getBlockState(pos) == blockState) {
                amount++;
                if (amount >= count) break;
            }
        }
        return amount >= count;
    }

    @Override
    public RecipeCondition createTemplate() {
        return new BlockCondition();
    }


    @Nonnull
    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        jsonObject.add("block", Multiblocked.GSON.toJsonTree(blockState));
        jsonObject.addProperty("count", count);
        return jsonObject;
    }

    @Override
    public RecipeCondition deserialize(@Nonnull JsonObject config) {
        super.deserialize(config);
        blockState = Multiblocked.GSON.fromJson(config.get("block"), BlockState.class);
        count = config.get("count").getAsInt();
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        blockState = Block.stateById(buf.readVarInt());
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeVarInt(Block.getId(blockState));
    }

    @Override
    public void openConfigurator(WidgetGroup group) {
        super.openConfigurator(group);
        group.addWidget(new BlockSelectorWidget(0, 20, 80, true).setOnBlockStateUpdate(state -> blockState = state).setBlock(blockState));
        group.addWidget(new TextFieldWidget(0, 45, 60, 15, null, s -> count = Integer.parseInt(s))
                .setCurrentString(count + "")
                .setNumbersOnly(Integer.MIN_VALUE, Integer.MAX_VALUE)
                .setHoverTooltips("multiblocked.gui.condition.block.count"));
    }
}
