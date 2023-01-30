package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.api.recipe.serde.recipe.MBDRecipeReloadListener;
import com.lowdragmc.multiblocked.common.recipe.conditions.PredicateCondition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(ReloadCommand.class)
public class ReloadMixin {
    @Inject(method = "reloadPacks", at = @At("RETURN"))
    private static void reloadPacks(Collection<String> p_138236_, CommandSourceStack p_138237_, CallbackInfo ci) {
        MBDRecipeReloadListener.INSTANCE.reloadRecipes();
    }

    /**
     * Because KubeJs add recipes too early, and the predicate map is not cleared.
     * We need to clear the map before the recipe added.
     */
    @Inject(method = "reloadPacks", at = @At("HEAD"))
    private static void onReloadPacksHead(Collection<String> pSelectedIds, CommandSourceStack pSource, CallbackInfo ci) {
        PredicateCondition.PREDICATE_MAP.clear();
    }

}
