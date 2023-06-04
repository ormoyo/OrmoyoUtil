package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.proxy.CommonProxy;
import com.ormoyo.ormoyoutil.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Method;

@NetworkMessage(modid = OrmoyoUtil.MODID, side = Side.CLIENT)
public class MessageUnlockAbility extends AbstractMessage<MessageUnlockAbility>
{
    private static final Holder.Func3 unlockAbilityFunc;
    AbilityEntry abilityEntry;
    boolean readFromNBT;

    public MessageUnlockAbility()
    {
    }

    public MessageUnlockAbility(Ability ability, boolean readFromNBT)
    {
        this.abilityEntry = ability.getEntry();
        this.readFromNBT = readFromNBT;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.abilityEntry = ByteBufUtils.readRegistryEntry(buf, Ability.getAbilityRegistry());
        this.readFromNBT = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeRegistryEntry(buf, this.abilityEntry);
        buf.writeBoolean(this.readFromNBT);
    }

    @Override
    public void onClientReceived(Minecraft client, EntityPlayer player, MessageContext messageContext)
    {
        Ability ability = this.abilityEntry.newInstance(player);
        unlockAbilityFunc.apply(OrmoyoUtil.PROXY, ability, this.readFromNBT);
    }

    @Override
    public void onServerReceived(MinecraftServer server, EntityPlayer player, MessageContext messageContext)
    {
    }

    static
    {
        Holder.Func3 func = null;
        try
        {
            Method method = CommonProxy.class.getDeclaredMethod("unlockAbility", Ability.class, boolean.class);
            func = Utils.createLambdaFromMethod(Holder.Func3.class, method);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        unlockAbilityFunc = func;
    }

    private static class Holder
    {
        public interface Func3
        {
            boolean apply(CommonProxy instance, Ability ability, boolean b);
        }
    }
}
