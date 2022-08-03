package com.lowdragmc.multiblocked.api.recipe;


import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.kubejs.events.RecipeFinishEvent;
import com.lowdragmc.multiblocked.api.kubejs.events.SetupRecipeEvent;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import dev.latvian.mods.kubejs.script.ScriptType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class RecipeLogic {
    public final ControllerTileEntity controller;
    public Recipe lastRecipe;
    public List<Recipe> lastFaildMattches;

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
            if (lastFaildMattches != null) {
                for (Recipe recipe : lastFaildMattches) {
                    if (recipe.checkConditions(this)) {
                        setupRecipe(recipe);
                    }
                    if (lastRecipe != null && getStatus() == Status.WORKING) {
                        lastFaildMattches = null;
                        return;
                    }
                }
            }
        }
    }

    public void handleRecipeWorking() {
        if (lastRecipe.checkConditions(this)) {
            setStatus(Status.WORKING);
            progress++;
            handleTickRecipe(lastRecipe);
        } else {
            setStatus(Status.SUSPEND);
        }
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
        lastFaildMattches = null;
        if (lastRecipe != null && lastRecipe.matchRecipe(this.controller) && lastRecipe.matchTickRecipe(this.controller) && lastRecipe.checkConditions(this)) {
            recipe = lastRecipe;
            lastRecipe = null;
            setupRecipe(recipe);
        } else {
            List<Recipe> matches = controller.getDefinition().recipeMap.searchRecipe(this.controller);
            lastRecipe = null;
            for (Recipe match : matches) {
                if (match.checkConditions(this)) {
                    setupRecipe(match);
                }
                if (lastRecipe != null && getStatus() == Status.WORKING) {
                    lastFaildMattches = null;
                    break;
                }
                if (lastFaildMattches == null) {
                    lastFaildMattches = new ArrayList<>();
                }
                lastFaildMattches.add(match);
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
        if (Multiblocked.isKubeJSLoaded() && controller != null && controller.getLevel() != null) {
            SetupRecipeEvent event = new SetupRecipeEvent(this, recipe);
            if (event.post(ScriptType.of(controller.getLevel()), SetupRecipeEvent.ID, controller.getSubID())) {
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
        Recipe recipe = lastRecipe;
        if (Multiblocked.isKubeJSLoaded() && controller != null && controller.getLevel() != null) {
            RecipeFinishEvent event = new RecipeFinishEvent(this);
            if (event.post(ScriptType.of(controller.getLevel()), RecipeFinishEvent.ID, controller.getSubID())) {
                return;
            }
            recipe = event.getRecipe();
        }
        if (recipe != null) {
            recipe.postWorking(this.controller);
            recipe.handleRecipeIO(IO.OUT, this.controller);
        }
        if (lastRecipe.matchRecipe(this.controller) && lastRecipe.matchTickRecipe(this.controller) && lastRecipe.checkConditions(this)) {
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

    public void readFromNBT(CompoundTag compound) {
        lastRecipe = compound.contains("recipe") ? controller.getDefinition().recipeMap.recipes.get(compound.getString("recipe")) : null;
        if (lastRecipe != null) {
            status = compound.contains("status") ? Status.values()[compound.getInt("status")] : Status.WORKING;
            duration = lastRecipe.duration;
            progress = compound.contains("progress") ? compound.getInt("progress") : 0;
        }
    }

    public CompoundTag writeToNBT(CompoundTag compound) {
        if (lastRecipe != null && status != Status.IDLE) {
            compound.putString("recipe", lastRecipe.uid);
            compound.putInt("status", status.ordinal());
            compound.putInt("progress", progress);
        }
        return compound;
    }

    public void inValid() {
        if (lastRecipe != null && isWorking()) {
            lastRecipe.postWorking(controller);
        }
    }

    public enum Status {
        IDLE("idle"),
        WORKING("working"),
        SUSPEND("suspend");

        public final String name;
        Status(String name) {
            this.name = name;
        }
    }
}
