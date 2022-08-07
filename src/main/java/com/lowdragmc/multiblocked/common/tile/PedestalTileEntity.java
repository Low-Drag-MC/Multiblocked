package com.lowdragmc.multiblocked.common.tile;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import com.lowdragmc.multiblocked.client.renderer.impl.PedestalRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PedestalTileEntity extends PartTileEntity.PartSimpleTileEntity {

    private final ItemStackHandler handler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markAsDirty();
            writeCustomData(-2, buf -> buf.writeNbt(handler.serializeNBT()));
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public PedestalTileEntity(PartDefinition definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
    }

    @Override
    public ModularUI createComponentUI(Player entityPlayer) {
        return null;
    }

    @Override
    public void receiveCustomData(int dataId, FriendlyByteBuf buf) {
        if (dataId == -2) {
            handler.deserializeNBT(buf.readNbt());
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }

    @Override
    public void writeInitialSyncData(FriendlyByteBuf buf) {
        super.writeInitialSyncData(buf);
        buf.writeNbt(handler.serializeNBT());
    }

    @Override
    public void receiveInitialSyncData(FriendlyByteBuf buf) {
        super.receiveInitialSyncData(buf);
        handler.deserializeNBT(buf.readNbt());
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag compound) {
        super.saveAdditional(compound);
        compound.put("item", handler.serializeNBT());
    }

    @Override
    public void load(@NotNull CompoundTag compound) {
        super.load(compound);
        if (compound.contains("item")) {
            handler.deserializeNBT(compound.getCompound("item"));
        }
    }

    public ItemStack getItemStack() {
        return handler.getStackInSlot(0);
    }

    public void setItemStack(ItemStack itemStack) {
        if (level != null && !level.isClientSide) {
            handler.setStackInSlot(0, itemStack);
        }
    }

    @Override
    public InteractionResult use(Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isCrouching()) return InteractionResult.PASS;
        ItemStack itemStack = player.getItemInHand(hand);
        ItemStack stored = getItemStack();
        if (level != null && itemStack.isEmpty() && !stored.isEmpty()){
            if (!level.isClientSide) {
                player.addItem(stored);
                setItemStack(ItemStack.EMPTY);
            }
            return InteractionResult.SUCCESS;
        } else if (!itemStack.isEmpty() && stored.isEmpty()) {
            if (!level.isClientSide) {
                ItemStack copy = itemStack.copy();
                copy.setCount(1);
                setItemStack(copy);
            }
            itemStack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @NotNull
    @Override
    public <K> LazyOptional<K> getCapability(@NotNull Capability<K> capability, @Nullable Direction facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> handler));
        }
        return super.getCapability(capability, facing);
    }

    public final static PartDefinition pedestalDefinition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, "pedestal"), PedestalTileEntity.class);

    public static void registerPedestal() {
        pedestalDefinition.baseRenderer = new PedestalRenderer();
        pedestalDefinition.allowRotate = false;
        pedestalDefinition.properties.isOpaque = false;
        pedestalDefinition.properties.shape = Shapes.or(
                Shapes.box(2 / 16f, 0 / 16f, 2 / 16f, 14 / 16f, 3 / 16f, 14 / 16f),
                Shapes.box(4 / 16f, 3 / 16f, 4 / 16f, 12 / 16f, 12 / 16f, 12 / 16f),
                Shapes.box(2 / 16f, 12 / 16f, 2 / 16f, 14 / 16f, 16 / 16f, 14 / 16f)
        );

        MbdComponents.registerComponent(pedestalDefinition);
    }

}
