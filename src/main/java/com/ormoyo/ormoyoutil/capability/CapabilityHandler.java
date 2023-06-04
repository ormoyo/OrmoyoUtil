package com.ormoyo.ormoyoutil.capability;

import com.google.common.collect.Maps;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.proxy.CommonProxy;
import com.ormoyo.ormoyoutil.util.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

@EventBusSubscriber(modid = OrmoyoUtil.MODID)
public class CapabilityHandler
{

    @CapabilityInject(IAbilityCap.class)
    public static Capability<IAbilityCap> CAPABILITY_ABILITY_DATA = null;

    private static final Holder.Func3 unlockAbilityFunc;
    static final Map<AbilityEntry, NBTTagCompound> nbtData = Maps.newHashMap();

    public static void registerCapabilities()
    {
        CapabilityManager.INSTANCE.register(IAbilityCap.class, new AbiltyCapStorage(), AbilityCap::new);
    }

    @SubscribeEvent
    public static void AttachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) event.getObject();
            event.addCapability(new ResourceLocation(OrmoyoUtil.MODID, "ability_data"), new AbilityCapProvider<>(CAPABILITY_ABILITY_DATA, null, player));
        }
    }

    @SubscribeEvent
    public static void PlayerLoggedIn(EntityJoinWorldEvent event)
    {
        if (event.getEntity() instanceof EntityPlayerMP)
        {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            for (Entry<AbilityEntry, NBTTagCompound> entry : nbtData.entrySet())
            {
                Ability ability = entry.getKey().newInstance(player);
                unlockAbilityFunc.apply(OrmoyoUtil.PROXY, ability, true);
                ability.readFromNBT(entry.getValue());
            }
            for (AbilityEntry entry : Ability.getAbilityRegistry())
            {
                if (OrmoyoUtil.PROXY.isAbilityUnlocked(entry.getAbilityClass(), player))
                {
                    continue;
                }
                if (entry.getLevel() == 1)
                {
                    OrmoyoUtil.PROXY.unlockAbility(entry.newInstance(player));
                }
                else if (entry.getCondition() != null)
                {
                    if (ArrayUtils.contains(entry.getConditionCheckingEvents(), PlayerLoggedInEvent.class))
                    {
                        if (entry.getCondition().test(player))
                        {
                            Ability ability = entry.newInstance(player);
                            if (ability.isVisable())
                            {
                                OrmoyoUtil.PROXY.unlockAbility(ability);
                            }
                            else
                            {
                                unlockAbilityFunc.apply(OrmoyoUtil.PROXY, ability, true);
                            }
                        }
                    }
                }
            }
        }
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
