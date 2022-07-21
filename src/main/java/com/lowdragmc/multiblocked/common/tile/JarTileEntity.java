package com.lowdragmc.multiblocked.common.tile;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.utils.FluidUtils;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import com.lowdragmc.multiblocked.client.renderer.impl.JarRenderer;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JarTileEntity extends PartTileEntity.PartSimpleTileEntity {

    private final FluidTank handler = new FluidTank(5000) {
        @Override
        protected void onContentsChanged() {
            markAsDirty();
            writeCustomData(-2, buf -> buf.writeNbt(handler.writeToNBT(new CompoundTag())));
        }
    };

    public JarTileEntity(PartDefinition definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
    }

    @Override
    public ModularUI createComponentUI(Player entityPlayer) {
        return null;
    }

    @Override
    public void receiveCustomData(int dataId, FriendlyByteBuf buf) {
        if (dataId == -2) {
            handler.readFromNBT(buf.readNbt());
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }

    @Override
    public void writeInitialSyncData(FriendlyByteBuf buf) {
        super.writeInitialSyncData(buf);
        buf.writeNbt(handler.writeToNBT(new CompoundTag()));
    }

    @Override
    public void receiveInitialSyncData(FriendlyByteBuf buf) {
        super.receiveInitialSyncData(buf);
        handler.readFromNBT(buf.readNbt());
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag compound) {
        super.saveAdditional(compound);
        compound.put("item", handler.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(@NotNull CompoundTag compound) {
        super.load(compound);
        if (compound.contains("item")) {
            handler.readFromNBT(compound.getCompound("item"));
        }
    }

    public FluidStack getFluidStack() {
        return handler.getFluid();
    }

    @Override
    public InteractionResult use(Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isCrouching()) return InteractionResult.PASS;
        ItemStack itemStack = player.getItemInHand(hand);
        var handler = FluidUtils.getFluidHandler(itemStack);
        if (handler.isPresent() && level != null){
            FluidUtil.interactWithFluidHandler(player, hand, this.handler);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @NotNull
    @Override
    public <K> LazyOptional<K> getCapability(@NotNull Capability<K> capability, @Nullable Direction facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> handler));
        }
        return super.getCapability(capability, facing);
    }

    public final static PartDefinition jarlDefinition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, "jar"), JarTileEntity.class);

    public static void registerJar() {
        jarlDefinition.baseRenderer = new JarRenderer();
        jarlDefinition.properties.isOpaque = false;
        jarlDefinition.properties.shape = Shapes.or(
                Shapes.box(3 / 16f, 0 / 16f, 3 / 16f, 13 / 16f, 12 / 16f, 13 / 16f),
                Shapes.box(5 / 16f, 12 / 16f, 5 / 16f, 11 / 16f, 14 / 16f, 11 / 16f)
        );

        MbdComponents.registerComponent(jarlDefinition);
    }

}
