package com.lowdragmc.multiblocked.common.tile;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.client.renderer.impl.GTRenderer;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDIModelRenderer;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.block.BlockStressValues;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author KilaBash
 * @date 2022/06/01
 * @implNote CreateKineticTileEntity
 */

public class CreateKineticSourceTileEntity extends GeneratingKineticTileEntity implements IComponent {

    public final PartDefinition definition;
    protected IMultiblockedRenderer currentRenderer;
    public Object rendererObject; // used for renderer
    protected String status = "unformed";
    public Set<BlockPos> controllerPos = new HashSet<>();

    public CreateKineticSourceTileEntity(PartDefinition partDefinition) {
        super(partDefinition.getTileType());
        definition = partDefinition;
    }

    public final static PartDefinition partDefinition = new PartDefinition(new ResourceLocation(
            Multiblocked.MODID, "test_create"), CreateKineticSourceTileEntity::new);

    public static void registerTest() {
        partDefinition.baseRenderer = new GTRenderer(new ResourceLocation("multiblocked:blocks/gregtech_base"), new ResourceLocation("multiblocked:blocks/gregtech_front"));
        MbdComponents.registerComponent(partDefinition);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return definition;
    }

    @Override
    public float getGeneratedSpeed() {
        return 15f;
    }

    @Override
    protected Block getStressConfigKey() {
        return super.getStressConfigKey();
    }


    @Override
    public void tick() {
        super.tick();
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

    private IMultiblockedRenderer updateCurrentRenderer() {
        if (definition.workingRenderer != null) {
            for (ControllerTileEntity controller : getControllers()) {
                if (controller.isFormed() && controller.getStatus().equals("working")) {
                    return definition.workingRenderer;
                }
            }
        }
        IMultiblockedRenderer renderer;
        if (isFormed()) {
            renderer = definition.formedRenderer == null ? definition.baseRenderer : definition.formedRenderer;
        } else {
            renderer = definition.baseRenderer;
        }
        return renderer;
    }

    @Override
    public boolean isFormed() {
        for (BlockPos blockPos : controllerPos) {
            TileEntity controller = level.getBlockEntity(blockPos);
            if (controller instanceof ControllerTileEntity && ((ControllerTileEntity) controller).isFormed()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setRendererObject(Object o) {
        rendererObject = o;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Object getRendererObject() {
        return rendererObject;
    }

}
