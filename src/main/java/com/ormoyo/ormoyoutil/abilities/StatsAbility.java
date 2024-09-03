package com.ormoyo.ormoyoutil.abilities;

import com.google.common.collect.Maps;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import com.ormoyo.ormoyoutil.event.AbilityEvents.StatsEvents.CalculateEntityExp;
import com.ormoyo.ormoyoutil.event.AbilityEvents.StatsEvents.LevelUpEvent;
import com.ormoyo.ormoyoutil.network.datasync.AbilityDataParameter;
import com.ormoyo.ormoyoutil.network.datasync.AbilitySyncManager;
import com.ormoyo.ormoyoutil.util.MathUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

public class StatsAbility extends Ability
{
    private static final AbilityDataParameter<Integer> LEVEL = AbilitySyncManager.createKey(StatsAbility.class, DataSerializers.VARINT);
    private static final AbilityDataParameter<Integer> EXP = AbilitySyncManager.createKey(StatsAbility.class, DataSerializers.VARINT);
    private static final AbilityDataParameter<Integer> REQUIRED_EXP = AbilitySyncManager.createKey(StatsAbility.class, DataSerializers.VARINT);

    public StatsAbility(AbilityHolder owner)
    {
        super(owner);
    }

    @Override
    public void abilityInit()
    {
        super.abilityInit();
        this.syncManager.register(LEVEL, 1);
        this.syncManager.register(EXP, 0);
        this.syncManager.register(REQUIRED_EXP, 20);
    }

    private static final Map<Class<? extends LivingEntity>, Integer> entityToExp = Maps.newHashMap();

    @SubscribeEvent
    public void onDeathEvent(LivingDeathEvent event)
    {
        System.out.println(Ability.getAbilityCapability());
        if (OrmoyoUtil.Config.LEVEL_SYSTEM.USE_MINECRAFT_LEVELING.get())
            return;

        if (EffectiveSide.get().isServer() && event.getSource().getTrueSource() != null)
        {
            if (!event.getSource().getTrueSource().equals(this.owner))
                return;

            int exp = 1;
            if (event.getEntityLiving() instanceof IMob)
                exp = 5;

            if (entityToExp.containsKey(event.getEntityLiving().getClass()))
                exp = entityToExp.get(event.getEntityLiving().getClass());

            CalculateEntityExp expEvent = new CalculateEntityExp(this, event.getEntityLiving(), exp);

            if (MinecraftForge.EVENT_BUS.post(expEvent))
                exp = 0;

            exp = expEvent.getExp();
            switch (this.owner.world.getDifficulty())
            {
                case EASY:
                    if (this.owner.getRNG().nextDouble() < OrmoyoUtil.Config.LEVEL_SYSTEM.EASY_SUCCESS_RATE.get())
                        this.setEXP(this.getEXP() + exp);

                    break;
                case NORMAL:
                    if (this.owner.getRNG().nextDouble() < OrmoyoUtil.Config.LEVEL_SYSTEM.NORMAL_SUCCESS_RATE.get())
                        this.setEXP(this.getEXP() + exp);

                    break;
                case HARD:
                    if (this.owner.getRNG().nextDouble() < OrmoyoUtil.Config.LEVEL_SYSTEM.HARD_SUCCESS_RATE.get())
                        this.setEXP(this.getEXP() + exp);

                    break;
                case PEACEFUL:
                    if (this.owner.getRNG().nextDouble() < OrmoyoUtil.Config.LEVEL_SYSTEM.PEACEFUL_SUCCESS_RATE.get())
                        this.setEXP(this.getEXP() + exp);

                    break;
            }

            if (this.getEXP() >= this.getRequiredEXP())
            {
                LevelUpEvent e = new LevelUpEvent(this, this.getLevel() + 1);
                if (MinecraftForge.EVENT_BUS.post(e))
                    return;

                this.levelUp();
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onLevelUp(PlayerXpEvent.LevelChange event)
    {
        if (!OrmoyoUtil.Config.LEVEL_SYSTEM.USE_MINECRAFT_LEVELING.get())
            return;

        AbilityHolder holder = Ability.getAbilityHolder(this.getOwner());

        for (AbilityEntry entry : Ability.getAbilityRegistry())
        {
            if (holder.getAbility(entry.getRegistryName()) != null)
                continue;

            if (event.getLevels() >= entry.getLevel())
            {
                holder.unlockAbility(entry);
                return;
            }
        }
    }

    @Override
    public void writeToNBT(CompoundNBT compound)
    {
        compound.putInt("Level", this.getLevel());
        compound.putInt("EXP", this.getEXP());

        compound.putInt("requiredEXP", this.getRequiredEXP());
    }

    @Override
    public void readFromNBT(CompoundNBT compound)
    {
        this.setLevel(compound.getInt("Level"));
        this.setEXP(compound.getInt("EXP"));

        this.setRequiredEXP(compound.getInt("requiredEXP"));
    }

    @SuppressWarnings("ConstantConditions")
    public void levelUp()
    {
        this.setLevel(this.getLevel() + 1);
        this.setEXP(Math.max(this.getEXP() - this.getRequiredEXP(), 0));

        int multiplayer = 1;
        switch (this.owner.world.getDifficulty())
        {
            case EASY:
                multiplayer = 2;
                break;
            case NORMAL:
                multiplayer = 3;
                break;
            case HARD:
                multiplayer = 5;
                break;
            default:
                break;
        }

        this.setRequiredEXP(this.getRequiredEXP() + MathUtils.randomInt(this.owner.getRNG(), this.getLevel(), MathHelper.clamp(multiplayer * this.getLevel(), this.getLevel(), Integer.MAX_VALUE - 1)));

        AbilityHolder holder = Ability.getAbilityHolder(this.getOwner());
        for (AbilityEntry entry : Ability.getAbilityRegistry())
        {
            if (holder.getAbility(entry.getRegistryName()) != null)
                continue;

            if (this.getLevel() >= entry.getLevel())
            {
                holder.unlockAbility(entry);
                return;
            }
        }
    }

    public void setLevel(int level)
    {
        this.syncManager.set(LEVEL, MathHelper.clamp(level, 1, 1));
    }

    public int getLevel()
    {
        return this.syncManager.get(LEVEL);
    }

    public void setEXP(int exp)
    {
        this.syncManager.set(EXP, exp);
    }

    public int getEXP()
    {
        return this.syncManager.get(EXP);
    }

    public void setRequiredEXP(int requiredEXP)
    {
        this.syncManager.set(REQUIRED_EXP, requiredEXP);
    }

    public int getRequiredEXP()
    {
        return this.syncManager.get(REQUIRED_EXP);
    }


    public static void onIMCHandle(InterModProcessEvent event)
    {
        event.enqueueWork(() ->
        {
            if (!EffectiveSide.get().isServer())
                return;

            Stream<InterModComms.IMCMessage> stream = event.getIMCStream(str -> str.equals("setEntityExp"));
            for (Iterator<InterModComms.IMCMessage> iterator = stream.iterator(); iterator.hasNext(); )
            {
                InterModComms.IMCMessage message = iterator.next();
                EntityXpEntry entry = (EntityXpEntry) message.getMessageSupplier().get();

                entityToExp.put(entry.getEntityClass(), entry.getXp());
            }
        });
    }

    public static class EntityXpEntry
    {
        private final Class<? extends LivingEntity> clazz;
        private final int xp;

        public EntityXpEntry(Class<? extends LivingEntity> clazz, int xp)
        {
            this.clazz = clazz;
            this.xp = xp;
        }

        public Class<? extends LivingEntity> getEntityClass()
        {
            return clazz;
        }

        public int getXp()
        {
            return xp;
        }
    }
}
