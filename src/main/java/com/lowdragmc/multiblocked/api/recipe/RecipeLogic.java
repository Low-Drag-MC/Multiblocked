package com.lowdragmc.multiblocked.api.recipe;


import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.kubejs.events.RecipeFinishEvent;
import com.lowdragmc.multiblocked.api.kubejs.events.SetupRecipeEvent;
import com.lowdragmc.multiblocked.api.kubejs.events.UpdateTickEvent;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import dev.latvian.kubejs.script.ScriptType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundNBT;

import java.util.List;

public class RecipeLogic {
    public final ControllerTileEntity controller;
    public Recipe lastRecipe;
    public int progress;
    public int duration;
    public int timer;
    private Status status = Status.IDLE;
    private long lastPeriod;
    private final MultiblockWorldSavedData mbwsd;

    public RecipeLogic(ControllerTileEntity controller) {
        this.controller = controller;
        this.timer = Multiblocked.RNG.nextInt();
        this.lastPeriod = Long.MIN_VALUE;
        this.mbwsd = MultiblockWorldSavedData.getOrCreate(controller.getLevel());
    }

    public void update() {
        timer++;
        if (getStatus() != Status.IDLE && lastRecipe != null) {
            if (getStatus() == Status.SUSPEND && timer % 5 == 0) {
                checkAsyncRecipeSearching(this::handleRecipeWorking);
            } else {
                handleRecipeWorking();
                if (progress == duration) {
                    onRecipeFinish();
                }
            }
        } else if (lastRecipe != null) {
            findAndHandleRecipe();
        } else if (timer % 5 == 0) {
            checkAsyncRecipeSearching(this::findAndHandleRecipe);
        }
    }

    public void handleRecipeWorking() {
        progress++;
        handleTickRecipe(lastRecipe);
        markDirty();
    }

    private void checkAsyncRecipeSearching(Runnable changed) {
        if (controller.asyncRecipeSearching) {
            if (mbwsd.getPeriodID() < lastPeriod) {
                lastPeriod = mbwsd.getPeriodID();
                changed.run();
            } else {
                if (controller.hasProxies() && asyncChanged()) {
                    changed.run();
                }
            }
        } else {
            changed.run();
        }
    }

    public boolean asyncChanged() {
        boolean needSearch = false;
        for (Long2ObjectOpenHashMap<CapabilityProxy<?>> map : controller.getCapabilitiesProxy().values()) {
            if (map != null) {
                for (CapabilityProxy<?> proxy : map.values()) {
                    if (proxy != null) {
                        if (proxy.getLatestPeriodID() > lastPeriod) {
                            lastPeriod = proxy.getLatestPeriodID();
                            needSearch = true;
                            break;
                        }
                    }
                }
            }
            if (needSearch) break;
        }
        return needSearch;
    }

    public void findAndHandleRecipe() {
        Recipe recipe;
        if (lastRecipe != null && lastRecipe.matchRecipe(this.controller) && lastRecipe.matchTickRecipe(this.controller)) {
            recipe = lastRecipe;
            lastRecipe = null;
            setupRecipe(recipe);
        } else {
            List<Recipe> matches = controller.getDefinition().recipeMap.searchRecipe(this.controller);
            lastRecipe = null;
            for (Recipe match : matches) {
                setupRecipe(match);
                if (lastRecipe != null && getStatus() == Status.WORKING) {
                    break;
                }
            }
        }
    }

    public void handleTickRecipe(Recipe recipe) {
        if (recipe.hasTick()) {
            if (recipe.matchTickRecipe(this.controller)) {
                recipe.handleTickRecipeIO(IO.IN, this.controller);
                recipe.handleTickRecipeIO(IO.OUT, this.controller);
                setStatus(Status.WORKING);
            } else {
                progress--;
                setStatus(Status.SUSPEND);
            }
        }
    }

    public void setupRecipe(Recipe recipe) {
        if (Multiblocked.isKubeJSLoaded()) {
            SetupRecipeEvent event = new SetupRecipeEvent(this, recipe);
            if (event.post(ScriptType.SERVER, SetupRecipeEvent.ID, controller.getSubID())) {
                return;
            }
            recipe = event.getRecipe();
        }
        recipe.preWorking(this.controller);
        if (recipe.handleRecipeIO(IO.IN, this.controller)) {
            lastRecipe = recipe;
            setStatus(Status.WORKING);
            progress = 0;
            duration = recipe.duration;
            markDirty();
        }
    }

    public void setStatus(Status status) {
        if (this.status != status) {
            this.status = status;
            controller.setStatus(status.name);
        }
    }

    public Status getStatus() {
        return status;
    }

    public boolean isWorking(){
        return status == Status.WORKING;
    }

    public boolean isIdle(){
        return status == Status.IDLE;
    }

    public boolean isSuspend(){
        return status == Status.SUSPEND;
    }

    public void onRecipeFinish() {
        if (Multiblocked.isKubeJSLoaded()) {
            new RecipeFinishEvent(this).post(ScriptType.SERVER, RecipeFinishEvent.ID, controller.getSubID());
        }
        lastRecipe.postWorking(this.controller);
        lastRecipe.handleRecipeIO(IO.OUT, this.controller);
        if (lastRecipe.matchRecipe(this.controller) && lastRecipe.matchTickRecipe(this.controller)) {
            setupRecipe(lastRecipe);
        } else {
            setStatus(Status.IDLE);
            progress = 0;
            duration = 0;
            markDirty();
        }
    }

    public void markDirty() {
        this.controller.markAsDirty();
    }

    public void readFromNBT(CompoundNBT compound) {
        lastRecipe = compound.contains("recipe") ? controller.getDefinition().recipeMap.recipes.get(compound.getString("recipe")) : null;
        if (lastRecipe != null) {
            status = compound.contains("status") ? Status.values()[compound.getInt("status")] : Status.WORKING;
            duration = lastRecipe.duration;
            progress = compound.contains("progress") ? compound.getInt("progress") : 0;
        }
    }

    public CompoundNBT writeToNBT(CompoundNBT compound) {
        if (lastRecipe != null && status != Status.IDLE) {
            compound.putString("recipe", lastRecipe.uid);
            compound.putInt("status", status.ordinal());
            compound.putInt("progress", progress);
        }
        return compound;
    }

    public enum Status {
        IDLE("idle"),
        WORKING("working"),
        SUSPEND("suspend");

        public String name;
        Status(String name) {
            this.name = name;
        }
    }
}
