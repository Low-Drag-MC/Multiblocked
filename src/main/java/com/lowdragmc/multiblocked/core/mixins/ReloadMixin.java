package com.lowdragmc.multiblocked.core.mixins;

import com.lowdragmc.multiblocked.api.recipe.serde.recipe.MBDRecipeReloadListener;
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
}
