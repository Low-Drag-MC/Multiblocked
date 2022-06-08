package com.lowdragmc.multiblocked.api.recipe;

public class Content {
    public transient Object content;
    public float chance;
    public String slotName;

    public Content(Object content, float chance, String slotName) {
        this.content = content;
        this.chance = chance;
        this.slotName = slotName;
    }

    public Object getContent() {
        return content;
    }

}
