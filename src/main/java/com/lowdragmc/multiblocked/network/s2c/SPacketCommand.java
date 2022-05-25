package com.lowdragmc.multiblocked.network.s2c;

import com.lowdragmc.lowdraglib.networking.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

public class SPacketCommand implements IPacket {
    private String cmd;

    public SPacketCommand() {
    }

    public SPacketCommand(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(cmd);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        this.cmd = buf.readUtf();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void execute(NetworkEvent.Context handler) {
//        if (Minecraft.getMinecraft().player != null) {
//            Minecraft.getMinecraft().player.sendMessage(new TextComponentString(cmd));
//        }
//        if (cmd.startsWith("nbt: ")) {
//            String tag = cmd.substring(5);
//            if (Minecraft.getMinecraft().player != null) {
//                Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
//                        TextFormatting.RED + "tag has been copied to the paste board"));
//                GuiScreen.setClipboardString(tag);
//            }
//
//        } else if (cmd.equals("reload_shaders")) {
//            Shaders.reload();
//            ShaderTextureParticle.clearShaders();
//            ShaderTextureParticle.FBOShaderHandler.FBO.deleteFramebuffer();
//            ShaderTextureParticle.FBOShaderHandler.FBO.createFramebuffer(1024, 1024);
//        }
//        return null;
    }

}
