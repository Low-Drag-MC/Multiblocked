package com.lowdragmc.multiblocked.api.gui.editor;

import com.lowdragmc.lowdraglib.gui.editor.annotation.RegisterUI;
import com.lowdragmc.lowdraglib.gui.editor.data.Project;
import com.lowdragmc.lowdraglib.gui.editor.data.UIProject;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;

/**
 * @author KilaBash
 * @date 2022/12/10
 * @implNote MBDProject
 */
@RegisterUI(name = "mbdui", group = "project")
public class MBDProject extends UIProject {

    private MBDProject() {
        this(null, null);
    }

    public MBDProject(MBDResources resources, WidgetGroup root) {
        super(resources, root);
    }

    public MBDProject(CompoundTag tag) {
        super(tag);
    }

    public MBDProject newEmptyProject() {
        return new MBDProject(MBDResources.defaultResource(),
                (WidgetGroup) new WidgetGroup(30, 30, 200, 200).setBackground(ResourceBorderTexture.BORDERED_BACKGROUND));
    }

    @Override
    public Project loadProject(File file) {
        try {
            var tag = NbtIo.read(file);
            if (tag != null) {
                return new MBDProject(tag);
            }
        } catch (IOException ignored) {}
        return null;
    }

    @Override
    public MBDResources loadResources(CompoundTag tag) {
        return MBDResources.fromNBT(tag);
    }

}
