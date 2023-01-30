package com.lowdragmc.multiblocked.api.recipe;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.kubejs.events.RecipeFinishEvent;
import com.lowdragmc.multiblocked.api.kubejs.events.SearchRecipeEvent;
import com.lowdragmc.multiblocked.api.kubejs.events.SetupRecipeEvent;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import dev.latvian.mods.kubejs.script.ScriptType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class RecipeLogic {
    public final IControllerComponent controller;
    public Recipe lastRecipe;
    public boolean dynamic;
    public List<Recipe> lastFailedMatches;

    public int progress;
    public int duration;
    public int fuelTime;
    public int fuelMaxTime;
    public int timer;
    private Status status = Status.IDLE;
    private long lastPeriod;
    private final MultiblockWorldSavedData mbwsd;

    public RecipeLogic(IControllerComponent controller) {
        this.controller = controller;
        this.timer = Multiblocked.RNG.nextInt();
        this.lastPeriod = Long.MIN_VALUE;
        this.mbwsd = MultiblockWorldSavedData.getOrCreate(controller.self().getLevel());
    }

    public boolean needFuel() {
        return controller.getDefinition().getRecipeMap().isFuelRecipeMap();
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
            if (lastFailedMatches != null) {
                for (Recipe recipe : lastFailedMatches) {
                    if (recipe.checkConditions(this)) {
                        setupRecipe(recipe);
                    }
                    if (lastRecipe != null && getStatus() == Status.WORKING) {
                        lastFailedMatches = null;
                        break;
                    }
                }
            }
        }
        if (fuelTime > 0) fuelTime--;
    }

    public void handleRecipeWorking() {
        Status last = this.status;
        if (lastRecipe.checkConditions(this) && handleFuelRecipe()) {
            setStatus(Status.WORKING);
            progress++;
            handleTickRecipe(lastRecipe);
        } else {
            setStatus(Status.SUSPEND);
        }
        if (last == Status.WORKING && getStatus() != Status.WORKING) {
            lastRecipe.postWorking(controller);
        } else if (last != Status.WORKING && getStatus() == Status.WORKING) {
            lastRecipe.preWorking(controller);
        }
        markDirty();
    }

    private void checkAsyncRecipeSearching(Runnable changed) {
        if (mbwsd.getPeriodID() < lastPeriod) {
            lastPeriod = mbwsd.getPeriodID();
            changed.run();
        } else {
            if (controller.hasProxies() && asyncChanged()) {
                changed.run();
            }
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
        lastFailedMatches = null;
        if (lastRecipe != null && lastRecipe.matchRecipe(this.controller) && lastRecipe.matchTickRecipe(this.controller) && lastRecipe.checkConditions(this)) {
            recipe = lastRecipe;
            lastRecipe = null;
            setupRecipe(recipe);
        } else {
            RecipeMap recipeMap = controller.getDefinition().getRecipeMap();
            List<Recipe> matches = recipeMap != null ? recipeMap.searchRecipe(this.controller) : new ArrayList<>();

            if (Multiblocked.isKubeJSLoaded() && controller.self().getLevel() != null) {
                SearchRecipeEvent event = new SearchRecipeEvent(this);
                event.post(ScriptType.of(controller.self().getLevel()), SearchRecipeEvent.ID, controller.getSubID());
                Recipe dynamicRecipe = event.getRecipe();
                if (dynamicRecipe != null && dynamicRecipe.matchRecipe(this.controller) && dynamicRecipe.matchTickRecipe(this.controller))
                    matches.add(dynamicRecipe);
            }

            lastRecipe = null;
            for (Recipe match : matches) {
                if (match.checkConditions(this)) {
                    setupRecipe(match);
                }
                if (lastRecipe != null && getStatus() == Status.WORKING) {
                    lastFailedMatches = null;
                    break;
                }
                if (lastFailedMatches == null) {
                    lastFailedMatches = new ArrayList<>();
                }
                lastFailedMatches.add(match);
            }
        }
    }

    public boolean handleFuelRecipe() {
        if (!needFuel() || fuelTime > 0) return true;
        for (Recipe recipe : controller.getDefinition().getRecipeMap().searchFuelRecipe(controller)) {
            if (recipe.checkConditions(this) && recipe.handleRecipeIO(IO.IN, this.controller)) {
                fuelMaxTime = recipe.duration;
                fuelTime = fuelMaxTime;
                markDirty();
            }
            if (fuelTime > 0) return true;
        }
        return false;
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
        if (Multiblocked.isKubeJSLoaded() && controller != null && controller.self().getLevel() != null) {
            SetupRecipeEvent event = new SetupRecipeEvent(this, recipe);
            if (event.post(ScriptType.of(controller.self().getLevel()), SetupRecipeEvent.ID, controller.getSubID())) {
                return;
            }
            recipe = event.getRecipe();
            this.dynamic = ((dynamic && recipe == lastRecipe) ||
                    !controller.getDefinition().getRecipeMap().recipes.containsValue(recipe));
        }
        if (dynamic) {
            if (!(recipe.checkConditions(this) && recipe.matchRecipe(this.controller) && recipe.matchTickRecipe(this.controller)))
                return;
        }
        if (handleFuelRecipe()) {
            recipe.preWorking(this.controller);
            if (recipe.handleRecipeIO(IO.IN, this.controller)) {
                lastRecipe = recipe;
                setStatus(Status.WORKING);
                progress = 0;
                duration = recipe.duration;
                markDirty();
            }
        }
    }

    public void setStatus(Status status) {
        if (this.status != status) {
            this.status = status;
            controller.setStatus(status.name);
        }
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isWorking() {
        return status == Status.WORKING;
    }

    public boolean isIdle() {
        return status == Status.IDLE;
    }

    public boolean isSuspend() {
        return status == Status.SUSPEND;
    }

    public void onRecipeFinish() {
        Recipe recipe = lastRecipe;
        boolean prePosted = false;
        if (Multiblocked.isKubeJSLoaded() && controller != null && controller.self().getLevel() != null) {
            RecipeFinishEvent event = new RecipeFinishEvent.Pre(this);
            if (event.post(ScriptType.of(controller.self().getLevel()), RecipeFinishEvent.Pre.ID, controller.getSubID())) {
                return;
            }
            prePosted = true;
            recipe = event.getRecipe();
        }
        if (recipe != null) {
            recipe.postWorking(this.controller);
            recipe.handleRecipeIO(IO.OUT, this.controller);
        }
        if (prePosted) {
            RecipeFinishEvent event = new RecipeFinishEvent.Post(this);
            if (event.post(ScriptType.of(controller.self().getLevel()), RecipeFinishEvent.Post.ID, controller.getSubID())) {
                return;
            }
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
        if (compound.contains("recipe")) {
            boolean dynamic = compound.contains("dynamic") && compound.getBoolean("dynamic");
            lastRecipe = (dynamic ? Multiblocked.GSON.fromJson(compound.getString("recipe"), Recipe.class) :
                    controller.getDefinition().getRecipeMap().recipes.get(compound.getString("recipe")));
        } else lastRecipe = null;

        if (lastRecipe != null) {
            status = compound.contains("status") ? Status.values()[compound.getInt("status")] : Status.WORKING;
            duration = lastRecipe.duration;
            progress = compound.contains("progress") ? compound.getInt("progress") : 0;
            fuelTime = compound.contains("fuelTime") ? compound.getInt("fuelTime") : 0;
            fuelMaxTime = compound.contains("fuelMaxTime") ? compound.getInt("fuelMaxTime") : fuelTime;
        }
    }

    public CompoundTag writeToNBT(CompoundTag compound) {
        if (lastRecipe != null && status != Status.IDLE) {
            compound.putBoolean("dynamic", dynamic);
            if (dynamic) compound.putString("recipe", Multiblocked.GSON.toJson(lastRecipe));
            else compound.putString("recipe", lastRecipe.uid);
            compound.putInt("status", status.ordinal());
            compound.putInt("progress", progress);
            compound.putInt("fuelTime", fuelTime);
            compound.putInt("fuelMaxTime", fuelMaxTime);
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
