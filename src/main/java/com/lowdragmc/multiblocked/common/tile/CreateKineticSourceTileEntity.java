package com.lowdragmc.multiblocked.common.tile;

import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;
import com.lowdragmc.multiblocked.api.tile.part.IPartComponent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.common.definition.CreatePartDefinition;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.ChatFormatting;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author KilaBash
 * @date 2022/06/01
 * @implNote CreateKineticTileEntity
 */

public class CreateKineticSourceTileEntity extends KineticBlockEntity implements IPartComponent {

    public final CreatePartDefinition definition;
    protected IMultiblockedRenderer currentRenderer;
    public Object rendererObject; // used for renderer
    protected String status = "unformed";
    public Set<BlockPos> controllerPos = new HashSet<>();
    public float workingSpeed;

    public CreateKineticSourceTileEntity(CreatePartDefinition CreateStressDefinition, BlockPos pos, BlockState state) {
        super(CreateStressDefinition.getTileType(), pos, state);
        definition = CreateStressDefinition;
    }

    @Override
    public PartDefinition getDefinition() {
        return definition;
    }

    public boolean isGenerator() {
        return definition.isOutput;
    }

    @Override
    public IMultiblockedRenderer getRenderer() {
        IMultiblockedRenderer lastRenderer = currentRenderer;
        currentRenderer = updateCurrentRenderer();
        if (lastRenderer != currentRenderer) {
            if (lastRenderer != null) {
                lastRenderer.onPostAccess(this);
            }
            if (currentRenderer != null) {
                currentRenderer.onPreAccess(this);
            }
        }
        return currentRenderer;
    }

    public List<IControllerComponent> getControllers() {
        List<IControllerComponent> result = new ArrayList<>();
        for (BlockPos blockPos : controllerPos) {
            BlockEntity controller = level.getBlockEntity(blockPos);
            if (controller instanceof IControllerComponent && ((IControllerComponent) controller).isFormed()) {
                result.add((IControllerComponent) controller);
            }
        }
        return result;
    }

    @Override
    public void addedToController(@Nonnull IControllerComponent controller) {
        if (controllerPos.add(controller.self().getBlockPos())) {
            setStatus("idle");
            sendData();
        }
    }

    @Override
    public void removedFromController(@Nonnull IControllerComponent controller) {
        if (controllerPos.remove(controller.self().getBlockPos())) {
            if (getControllers().isEmpty()) {
                setStatus("unformed");
            }
            sendData();
        }
    }

    public boolean canShared() {
        return false;
    }

    @Override
    public boolean hasController(BlockPos controllerPos) {
        return this.controllerPos.contains(controllerPos);
    }

    @Override
    public InteractionResult use(Player player, InteractionHand hand, BlockHitResult hit) {
        return IPartComponent.super.use(player, hand, hit);
    }

    @Override
    public void setRendererObject(Object o) {
        rendererObject = o;
    }

    @Override
    public void setStatus(String status) {
        if (level != null && !level.isClientSide) {
           if (!this.status.equals(status)) {
               this.status = status;
               sendData();
           }
        }
    }

    @Override
    public Object getRendererObject() {
        return rendererObject;
    }

    // ********** create
    public boolean reActivateSource;

    @Override
    public float getGeneratedSpeed() {
        return workingSpeed;
    }

    protected void notifyStressCapacityChange(float capacity) {
        this.getOrCreateNetwork().updateCapacityFor(this, capacity);
    }

    public void removeSource() {
        if (definition.isOutput && this.hasSource() && this.isSource()) {
            this.reActivateSource = true;
        }

        super.removeSource();
    }

    public void setSource(BlockPos source) {
        super.setSource(source);
        if (!definition.isOutput) return;
        BlockEntity tileEntity = this.level.getBlockEntity(source);
        if (tileEntity instanceof KineticBlockEntity sourceTe) {
            if (this.reActivateSource && Math.abs(sourceTe.getSpeed()) >= Math.abs(this.getGeneratedSpeed())) {
                this.reActivateSource = false;
            }

        }
    }

    public void tick() {
        super.tick();
        if (definition.isOutput && this.reActivateSource) {
            this.updateGeneratedRotation();
            this.reActivateSource = false;
        }

    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        float stressBase = this.calculateAddedStressCapacity();
        if (stressBase != 0.0F && IRotate.StressImpact.isEnabled()) {
            Lang.translate("gui.goggles.generator_stats").forGoggles(tooltip);
            Lang.translate("tooltip.capacityProvided").style(ChatFormatting.GRAY).forGoggles(tooltip);
            float speed = this.getTheoreticalSpeed();
            if (speed != this.getGeneratedSpeed() && speed != 0.0F) {
                stressBase *= this.getGeneratedSpeed() / speed;
            }

            speed = Math.abs(speed);
            float stressTotal = stressBase * speed;
            Lang.number(stressTotal).translate("generic.unit.stress").style(ChatFormatting.AQUA).space().add(Lang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
            added = true;
        }

        return added;
    }

    public void updateGeneratedRotation() {
        if (!definition.isOutput) return;
        float speed = this.getGeneratedSpeed();
        float prevSpeed = this.speed;
        if (!this.level.isClientSide) {
            if (prevSpeed != speed) {
                if (!this.hasSource()) {
                    IRotate.SpeedLevel levelBefore = IRotate.SpeedLevel.of(this.speed);
                    IRotate.SpeedLevel levelafter = IRotate.SpeedLevel.of(speed);
                    if (levelBefore != levelafter) {
                        this.effects.queueRotationIndicators();
                    }
                }

                this.applyNewSpeed(prevSpeed, speed);
            }

            if (this.hasNetwork() && speed != 0.0F) {
                KineticNetwork network = this.getOrCreateNetwork();
                this.notifyStressCapacityChange(this.calculateAddedStressCapacity());
                this.getOrCreateNetwork().updateStressFor(this, this.calculateStressApplied());
                network.updateStress();
            }

            this.onSpeedChanged(prevSpeed);
            this.sendData();
        }
    }

    public void applyNewSpeed(float prevSpeed, float speed) {
        if (speed == 0.0F) {
            if (this.hasSource()) {
                this.notifyStressCapacityChange(0.0F);
                this.getOrCreateNetwork().updateStressFor(this, this.calculateStressApplied());
            } else {
                this.detachKinetics();
                this.setSpeed(0.0F);
                this.setNetwork(null);
            }
        } else if (prevSpeed == 0.0F) {
            this.setSpeed(speed);
            this.setNetwork(this.createNetworkId());
            this.attachKinetics();
        } else if (this.hasSource()) {
            if (Math.abs(prevSpeed) >= Math.abs(speed)) {
                if (Math.signum(prevSpeed) != Math.signum(speed)) {
                    this.level.destroyBlock(this.worldPosition, true);
                }
            } else {
                this.detachKinetics();
                this.setSpeed(speed);
                this.source = null;
                this.setNetwork(this.createNetworkId());
                this.attachKinetics();
            }
        } else {
            this.detachKinetics();
            this.setSpeed(speed);
            this.attachKinetics();
        }
    }

    public Long createNetworkId() {
        return this.worldPosition.asLong();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putString("status", status);
        ListTag list = new ListTag();
        for (BlockPos pos : controllerPos) {
            list.add(NbtUtils.writeBlockPos(pos));
        }
        compound.put("controller", list);
        compound.putFloat("workingSpeed", workingSpeed);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        status = compound.contains("status") ? compound.getString("status") : "unformed";
        if (compound.contains("controller")) {
            controllerPos.clear();
            for (Tag controller : compound.getList("controller", 10)) {
                controllerPos.add(NbtUtils.readBlockPos((CompoundTag) controller));
            }
        }
        workingSpeed = compound.contains("workingSpeed") ? compound.getFloat("workingSpeed") : 0;
    }

    public float setupWorkingCapacity(float sum, boolean simulate) {
        if (isGenerator()) {
            float speed = Math.min(256f, sum / definition.stress);
            if (!simulate) {
                workingSpeed = speed;
                updateGeneratedRotation();
            }
            return speed * definition.stress;
        }
        return 0;
    }

    public void stopWorkingCapacity() {
        if (isGenerator()) {
            workingSpeed = 0;
            updateGeneratedRotation();
        }
    }
}
