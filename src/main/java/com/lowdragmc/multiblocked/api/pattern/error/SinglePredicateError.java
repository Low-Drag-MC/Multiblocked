package com.lowdragmc.multiblocked.api.pattern.error;

import com.lowdragmc.multiblocked.api.pattern.predicates.SimplePredicate;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;

public class SinglePredicateError extends PatternError {
    public final SimplePredicate predicate;
    public final int type;

    public SinglePredicateError(SimplePredicate predicate, int type) {
        this.predicate = predicate;
        this.type = type;
    }

    @Override
    public List<List<ItemStack>> getCandidates() {
        return Collections.singletonList(predicate.getCandidates());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public String getErrorInfo() {
        int number = -1;
        if (type == 0) {
            number = predicate.maxCount;
        }
        if (type == 1) {
            number = predicate.minCount;
        }
        return I18n.get("multiblocked.pattern.error.limited." + type, number);
    }
}
