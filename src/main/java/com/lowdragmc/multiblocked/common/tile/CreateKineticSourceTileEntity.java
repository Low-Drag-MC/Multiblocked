package com.lowdragmc.multiblocked.common.tile;

import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;
import com.lowdragmc.multiblocked.api.tile.part.IPartComponent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.common.definition.CreatePartDefinition;
import com.simibubi.create.content.contraptions.KineticNetwork;
import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

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

public class CreateKineticSourceTileEntity extends KineticTileEntity implements IPartComponent {

    public final CreatePartDefinition definition;
    protected IMultiblockedRenderer currentRenderer;
    public Object rendererObject; // used for renderer
    protected String status = "unformed";
    public Set<BlockPos> controllerPos = new HashSet<>();
    public float workingSpeed;

    public CreateKineticSourceTileEntity(
            CreatePartDefinition CreateStressDefinition) {
        super(CreateStressDefinition.getTileType());
        definition = CreateStressDefinition;
    }

    @Override
    public ComponentDefinition getDefinition() {
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

    public List<ControllerTileEntity> getControllers() {
        List<ControllerTileEntity> result = new ArrayList<>();
        for (BlockPos blockPos : controllerPos) {
            TileEntity controller = level.getBlockEntity(blockPos);
            if (controller instanceof ControllerTileEntity && ((ControllerTileEntity) controller).isFormed()) {
                result.add((ControllerTileEntity) controller);
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
    public ActionResultType use(PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
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
        TileEntity tileEntity = this.level.getBlockEntity(source);
        if (tileEntity instanceof KineticTileEntity) {
            KineticTileEntity sourceTe = (KineticTileEntity)tileEntity;
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

    public boolean addToGoggleTooltip(List<ITextComponent> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        float stressBase = this.calculateAddedStressCapacity();
        if (stressBase != 0.0F && IRotate.StressImpact.isEnabled()) {
            tooltip.add(componentSpacing.plainCopy().append(Lang.translate("gui.goggles.generator_stats")));
            tooltip.add(componentSpacing.plainCopy().append(Lang.translate("tooltip.capacityProvided").withStyle(TextFormatting.GRAY)));
            float speed = this.getTheoreticalSpeed();
            if (speed != this.getGeneratedSpeed() && speed != 0.0F) {
                stressBase *= this.getGeneratedSpeed() / speed;
            }

            speed = Math.abs(speed);
            float stressTotal = stressBase * speed;
            tooltip.add(componentSpacing.plainCopy().append((new StringTextComponent(" " + IHaveGoggleInformation.format((double)stressTotal))).append(Lang.translate("generic.unit.stress", new Object[0])).withStyle(TextFormatting.AQUA)).append(" ").append(Lang.translate("gui.goggles.at_current_speed", new Object[0]).withStyle(TextFormatting.DARK_GRAY)));
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
    protected void write(CompoundNBT compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putString("status", status);
        ListNBT list = new ListNBT();
        for (BlockPos pos : controllerPos) {
            list.add(NBTUtil.writeBlockPos(pos));
        }
        compound.put("controller", list);
        compound.putFloat("workingSpeed", workingSpeed);
    }

    @Override
    protected void fromTag(BlockState state, CompoundNBT compound, boolean clientPacket) {
        super.fromTag(state, compound, clientPacket);
        status = compound.contains("status") ? compound.getString("status") : "unformed";
        if (compound.contains("controller")) {
            controllerPos.clear();
            for (INBT controller : compound.getList("controller", 10)) {
                controllerPos.add(NBTUtil.readBlockPos((CompoundNBT) controller));
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
