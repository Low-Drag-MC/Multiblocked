package com.lowdragmc.multiblocked.client.renderer.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.particle.LParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.ShaderParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.TextureParticle;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.util.function.Supplier;

public class ParticleRenderer implements IMultiblockedRenderer {
    public final static ParticleRenderer INSTANCE = new ParticleRenderer(null);
    public ResourceLocation texture;
    public boolean shouldCull = true;
    public boolean isShader = false;
    public float scale = 1;
    public int light = -1;

    public ParticleRenderer(ResourceLocation texture) {
        this.texture = texture;
    }

    @OnlyIn(Dist.CLIENT)
    protected LParticle createParticle(IComponent component, double x, double y, double z) {
        Level level = component.self().getLevel();
        LParticle particle;
        if (level instanceof ClientLevel clientLevel) {
            particle = isShader ? new ShaderParticle(clientLevel, x, y, z, texture) : new TextureParticle(clientLevel, x, y, z).setTexture(texture);
        } else {
            particle = isShader ? new ShaderParticle(null, x, y, z, texture) : new TextureParticle(null, x, y, z).setTexture(texture);
            particle.setLevel(level);
        }
        particle.scale(scale);
        if (light >= 0) {
            particle.setLight(light << 20 | light << 4);
        } else {
            particle.setLight(-1);
        }
        return particle;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public final void onPreAccess(IComponent component) {
        synchronized (INSTANCE) {
            BlockPos pos = component.self().getBlockPos();
            LParticle particle = createParticle(component, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            component.setRendererObject(particle);
            if (particle != null) {
                particle.setOnUpdate(p -> {
                    if (component.self().isRemoved() || component.self().getLevel().getBlockEntity(component.self().getBlockPos()) != component) p.remove();
                });
                particle.setImmortal();
                particle.setCull(shouldCull);
                particle.addParticle();
            }
        }
    }

    
    @Override
    public String getType() {
        return "particle";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public final void onPostAccess(IComponent component) {
        if (component.getRendererObject() instanceof LParticle particle) {
            particle.remove();
            component.setRendererObject(null);
        }
    }

    @Override
    public IMultiblockedRenderer fromJson(Gson gson, JsonObject jsonObject) {
        ParticleRenderer renderer = new ParticleRenderer(gson.fromJson(jsonObject.get("texture"), ResourceLocation.class));
        renderer.isShader = jsonObject.get("isShader").getAsBoolean();
        renderer.scale = jsonObject.get("scale").getAsFloat();
        renderer.light = jsonObject.get("light").getAsInt();
        renderer.shouldCull = jsonObject.get("shouldCull").getAsBoolean();
        return renderer;
    }

    @Override
    public JsonObject toJson(Gson gson, JsonObject jsonObject) {
        jsonObject.addProperty("shouldCull", shouldCull);
        jsonObject.add("texture", gson.toJsonTree(texture, ResourceLocation.class));
        jsonObject.addProperty("isShader", isShader);
        jsonObject.addProperty("scale", scale);
        jsonObject.addProperty("light", light);
        return jsonObject;
    }

    @Override
    public Supplier<IMultiblockedRenderer> createConfigurator(WidgetGroup parent, DraggableScrollableWidgetGroup group, IMultiblockedRenderer current) {
        TextFieldWidget tfw = new TextFieldWidget(1,1,150,20, null, null).setResourceLocationOnly();
        group.addWidget(tfw);
        ParticleRenderer renderer = new ParticleRenderer(new ResourceLocation(""));
        if (current instanceof  ParticleRenderer) {
            renderer.texture = ((ParticleRenderer) current).texture;
            renderer.light = ((ParticleRenderer) current).light;
            renderer.scale = ((ParticleRenderer) current).scale;
            renderer.isShader =((ParticleRenderer) current).isShader;
            renderer.shouldCull = ((ParticleRenderer) current).shouldCull;
        }
        File png = new File(Multiblocked.location, "assets/multiblocked/textures");
        File shader = new File(Multiblocked.location, "assets/multiblocked/shaders");
        group.addWidget(new ButtonWidget(155, 1, 20, 20, cd -> DialogWidget.showFileDialog(parent, "select a texture/shader file", renderer.isShader ? shader : png, true,
                renderer.isShader ? DialogWidget.suffixFilter(".frag") : DialogWidget.suffixFilter(".png"), r -> {
                    if (r != null && r.isFile()) {
                        if (renderer.isShader) {
                            tfw.setCurrentString("multiblocked:" + r.getPath().replace(shader.getPath(), "").substring(1).replace(".frag", "").replace('\\', '/'));
                        } else {
                            tfw.setCurrentString("multiblocked:" + r.getPath().replace(png.getPath(), "textures").replace('\\', '/'));
                        }
                    }
                })).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/darkened_slot.png"), new TextTexture("F", -1)).setHoverTooltips("multiblocked.gui.tips.file_selector"));
        tfw.setCurrentString(renderer.texture.toString());
        group.addWidget(createBoolSwitch(1, 25, "isShader", "multiblocked.gui.predicate.particle.0", renderer.isShader, r->{
            if (renderer.isShader != r) {
                renderer.isShader = r;
                tfw.setCurrentString("");
            }
        }));
        group.addWidget(createBoolSwitch(1, 40, "shouldCull", "multiblocked.gui.predicate.particle.1", renderer.shouldCull, r -> renderer.shouldCull = r));
        group.addWidget(new TextFieldWidget(1,75,70,10,null, num->renderer.scale = Float.parseFloat(num))
                .setNumbersOnly(0f, 100f).setCurrentString(renderer.scale+"").setHoverTooltips("multiblocked.gui.predicate.particle.3"));
        group.addWidget(new LabelWidget(75, 75, "multiblocked.gui.label.scale"));
        group.addWidget(new TextFieldWidget(1,90,70,10,null, num->renderer.light = Integer.parseInt(num))
                .setNumbersOnly(-1, 15).setCurrentString(renderer.light+"").setHoverTooltips("multiblocked.gui.predicate.particle.4"));
        group.addWidget(new LabelWidget(75, 90, "multiblocked.gui.label.light"));
        return () -> {
            if (tfw.getCurrentString().isEmpty()) {
                return null;
            } else {
                ParticleRenderer newRenderer = new ParticleRenderer(new ResourceLocation(tfw.getCurrentString()));
                newRenderer.light = renderer.light;
                newRenderer.scale = renderer.scale;
                newRenderer.isShader = renderer.isShader;
                newRenderer.shouldCull = renderer.shouldCull;
                return newRenderer;
            }
        };
    }

}
