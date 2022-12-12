package com.lowdragmc.multiblocked.api.pattern.error;

import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;

public class PatternStringError extends PatternError{
    public final String translateKey;

    public PatternStringError(String translateKey) {
        this.translateKey = translateKey;
    }

    @Override
    public String getErrorInfo() {
        return LocalizationUtils.format(translateKey);
    }
}
