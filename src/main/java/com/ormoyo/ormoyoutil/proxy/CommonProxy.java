package com.ormoyo.ormoyoutil.proxy;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.capability.CapabilityHandler;
import com.ormoyo.ormoyoutil.event.AbilityEvents.OnAbilityUnlockedEvent;
import com.ormoyo.ormoyoutil.network.AbstractMessage;
import com.ormoyo.ormoyoutil.network.MessageUnlockAbility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Collections;
import java.util.Set;

public class CommonProxy
{
    public <T extends AbstractMessage<T>> void handleMessage(final T message, final MessageContext messageContext)
    {
        WorldServer world = (WorldServer) messageContext.getServerHandler().player.world;
        world.addScheduledTask(() -> message.onServerReceived(FMLCommonHandler.instance().getMinecraftServerInstance(), messageContext.getServerHandler().player, messageContext));
    }

    public void preInit(FMLPreInitializationEvent event)
    {
        CapabilityHandler.registerCapabilities();
    }

    public void init(FMLInitializationEvent event)
    {
        try
        {
            Ability.class.getDeclaredClasses()[0].getMethod("onInit").invoke(null);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    public void postInit(FMLPostInitializationEvent event)
    {
    }

    public boolean isServerSide()
    {
        return true;
    }

    /**
     * @param ability The ability to unlock
     * @return true if the ability owner didn't unlock it before
     */
    public boolean unlockAbility(Ability ability)
    {
        return this.unlockAbility(ability, false);
    }

    protected boolean unlockAbility(Ability ability, boolean readFromNBT)
    {
        if (!readFromNBT)
        {
            if (MinecraftForge.EVENT_BUS.post(new OnAbilityUnlockedEvent(ability)))
            {
                return false;
            }
        }
        boolean unlocked = ability.getOwner().getCapability(CapabilityHandler.CAPABILITY_ABILITY_DATA, null).unlockAbility(ability);
        if (unlocked)
        {
            OrmoyoUtil.NETWORK_WRAPPER.sendTo(new MessageUnlockAbility(ability, readFromNBT), (EntityPlayerMP) ability.getOwner());
        }
        return unlocked;
    }

    /**
     * @param name   The registry name of the ability
     * @param player The player who will be checked if they unlocked the ability
     * @return true if the player already unlocked the ability
     */
    public boolean isAbilityUnlocked(ResourceLocation name, EntityPlayer player)
    {
        return player.getCapability(CapabilityHandler.CAPABILITY_ABILITY_DATA, null).isAbilityUnlocked(name);
    }

    /**
     * @param clazz  The class of the ability
     * @param player The player who will be checked if they unlocked the ability
     * @return true if the player already unlocked the ability
     */
    public boolean isAbilityUnlocked(Class<? extends Ability> clazz, EntityPlayer player)
    {
        return player.getCapability(CapabilityHandler.CAPABILITY_ABILITY_DATA, null).isAbilityUnlocked(clazz);
    }

    public Set<Ability> getAbilities(EntityPlayer player)
    {
        if (player != null)
        {
            return player.getCapability(CapabilityHandler.CAPABILITY_ABILITY_DATA, null).getAbilities();
        }
        return Collections.emptySet();
    }

    public Ability getAbility(ResourceLocation name, EntityPlayer player)
    {
        if (player != null)
        {
            return player.getCapability(CapabilityHandler.CAPABILITY_ABILITY_DATA, null).getAbility(name);
        }
        return null;
    }

    public <T extends Ability> T getAbility(Class<T> clazz, EntityPlayer player)
    {
        if (player != null)
        {
            return player.getCapability(CapabilityHandler.CAPABILITY_ABILITY_DATA, null).getAbility(clazz);
        }
        return null;
    }

    public EntityPlayer getPlayerByUsername(String username)
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(username);
    }

    public EntityPlayer getPlayerByID(int id)
    {
        for (EntityPlayer player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers())
        {
            if (player.getEntityId() == id)
            {
                return player;
            }
        }
        return null;
    }

    public void openGui(Object gui)
    {
    }

    public EntityPlayer getClientPlayer()
    {
        return null;
    }
}
