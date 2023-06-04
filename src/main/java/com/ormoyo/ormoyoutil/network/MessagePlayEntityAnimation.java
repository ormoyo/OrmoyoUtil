package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.animation.AnimationHandler;
import com.ormoyo.ormoyoutil.entity.IAnimatedEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@NetworkMessage(modid = OrmoyoUtil.MODID, side = Side.CLIENT)
public class MessagePlayEntityAnimation extends AbstractMessage<MessagePlayEntityAnimation>
{
    Entity entity;
    String animation;

    public MessagePlayEntityAnimation()
    {
    }

    public <T extends Entity & IAnimatedEntity> MessagePlayEntityAnimation(T entity, String animation)
    {
        this.entity = entity;
        this.animation = animation;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.entity = OrmoyoUtil.PROXY.getClientPlayer().getEntityWorld().getEntityByID(buf.readInt());
        this.animation = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.entity.getEntityId());
        ByteBufUtils.writeUTF8String(buf, this.animation);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onClientReceived(Minecraft client, EntityPlayer player, MessageContext messageContext)
    {
        AnimationHandler.INSTANCE.setEntityAnimation((IAnimatedEntity) this.entity, this.animation);
    }

    @Override
    public void onServerReceived(MinecraftServer server, EntityPlayer player, MessageContext messageContext)
    {
    }
}
