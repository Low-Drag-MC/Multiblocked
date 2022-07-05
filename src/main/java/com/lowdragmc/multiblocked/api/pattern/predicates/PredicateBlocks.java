package com.lowdragmc.multiblocked.api.pattern.predicates;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.BlockSelectorWidget;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PredicateBlocks extends SimplePredicate {
    public Block[] blocks = new Block[0];
    
    public PredicateBlocks() {
        super("blocks");
    }
    
    public PredicateBlocks(Block... blocks) {
        this();
        this.blocks = blocks;
        buildPredicate();
    }

    @Override
    public SimplePredicate buildPredicate() {
        blocks = Arrays.stream(blocks).filter(Objects::nonNull).toArray(Block[]::new);
        if (blocks.length == 0) blocks = new Block[]{Blocks.BARRIER};
        predicate = state -> ArrayUtils.contains(blocks, state.getBlockState().getBlock());
        candidates = () -> Arrays.stream(blocks).map(block -> BlockInfo.fromBlockState(block.defaultBlockState())).toArray(BlockInfo[]::new);
        return this;
    }

    @Override
    public List<WidgetGroup> getConfigWidget(List<WidgetGroup> groups) {
        super.getConfigWidget(groups);
        WidgetGroup group = new WidgetGroup(0, 0, 182, 100);
        groups.add(group);
        DraggableScrollableWidgetGroup container = new DraggableScrollableWidgetGroup(0, 25, 182, 80).setBackground(new ColorRectTexture(0xffaaaaaa));
        group.addWidget(container);
        List<BlockState> blockList = Arrays.stream(blocks).map(Block::defaultBlockState).collect(Collectors.toList());
        for (BlockState blockState : blockList) {
            addBlockSelectorWidget(blockList, container, blockState);
        }
        group.addWidget(new LabelWidget(0, 6, "multiblocked.gui.label.block_settings"));
        group.addWidget(new ButtonWidget(162, 0, 20, 20, cd -> {
            blockList.add(null);
            addBlockSelectorWidget(blockList, container, null);
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/add.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.predicate.blocks.add"));
        return groups;
    }

    private void addBlockSelectorWidget(List<BlockState> blockList, DraggableScrollableWidgetGroup container, BlockState blockState) {
        BlockSelectorWidget bsw = new BlockSelectorWidget(0, container.widgets.size() * 21 + 1, 160, false);
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
        if (blockState != null) {
            bsw.setBlock(blockState);
        }
        bsw.setOnBlockStateUpdate(state->{
            int index = (bsw.getSelfPosition().y - 1) / 21;
            blockList.set(index, state);
            updateStates(blockList);
        });
    }

    private void updateStates(List<BlockState> blockList) {
        blocks = blockList.stream().filter(Objects::nonNull).map(BlockState::getBlock).toArray(Block[]::new);
        buildPredicate();
    }

    @Override
    public JsonObject toJson(JsonObject jsonObject) {
        JsonArray jsonArray = new JsonArray();
        for (Block block : blocks) {
            if (block.getRegistryName() != null) {
                final JsonObject blockObject = new JsonObject();
                blockObject.addProperty("id", block.getRegistryName().toString());
                jsonArray.add(blockObject);
            }
        }
        jsonObject.add("blocks", jsonArray);
        return super.toJson(jsonObject);
    }

    @Override
    public void fromJson(Gson gson, JsonObject jsonObject) {
        blocks = gson.fromJson(jsonObject.get("blocks"), Block[].class);
        super.fromJson(gson, jsonObject);
    }
}
