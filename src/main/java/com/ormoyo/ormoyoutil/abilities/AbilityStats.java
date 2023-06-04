package com.ormoyo.ormoyoutil.abilities;

import com.google.common.collect.Maps;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.config.ConfigHandler;
import com.ormoyo.ormoyoutil.event.AbilityEvents.StatsEvents.CalculateEntityExp;
import com.ormoyo.ormoyoutil.event.AbilityEvents.StatsEvents.LevelUpEvent;
import com.ormoyo.ormoyoutil.network.datasync.AbilityDataParameter;
import com.ormoyo.ormoyoutil.network.datasync.AbilitySyncManager;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.Map;

@EventBusSubscriber(modid = OrmoyoUtil.MODID)
public class AbilityStats extends Ability
{
    private static final AbilityDataParameter<Integer> LEVEL = AbilitySyncManager.createKey(AbilityStats.class, DataSerializers.VARINT);
    private static final AbilityDataParameter<Integer> EXP = AbilitySyncManager.createKey(AbilityStats.class, DataSerializers.VARINT);
    private static final AbilityDataParameter<Integer> REQUIRED_EXP = AbilitySyncManager.createKey(AbilityStats.class, DataSerializers.VARINT);

    public AbilityStats(EntityPlayer owner)
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

    private static final Map<Class<? extends EntityLivingBase>, Integer> entityToExp = Maps.newHashMap();

    @SubscribeEvent
    public void onDeathEvent(LivingDeathEvent event)
    {
        if (OrmoyoUtil.PROXY.isServerSide() && event.getSource().getTrueSource() != null)
        {
            if (event.getSource().getTrueSource().equals(this.owner))
            {
                int exp = 1;
                if (event.getEntityLiving() instanceof IMob)
                {
                    exp = 5;
                }
                if (entityToExp.containsKey(event.getEntityLiving().getClass()))
                {
                    exp = entityToExp.get(event.getEntityLiving().getClass());
                }
                CalculateEntityExp expEvent = new CalculateEntityExp(this, event.getEntityLiving(), exp);
                if (MinecraftForge.EVENT_BUS.post(expEvent))
                {
                    exp = 0;
                }
                exp = expEvent.getExp();
                if (!Arrays.asList(ConfigHandler.LEVELING_SYSTEM.entityExpCount).isEmpty())
                {
                    for (String string : ConfigHandler.LEVELING_SYSTEM.entityExpCount)
                    {
                        if (string.startsWith(EntityList.getKey(event.getEntityLiving()).toString()))
                        {
                            if (NumberUtils.isParsable(string.split("=")[1]))
                            {
                                exp = Integer.parseInt(string.split("=")[1]);
                            }
                        }
                    }
                }

                switch (this.owner.world.getDifficulty())
                {
                    case EASY:
                        if (this.owner.getRNG().nextDouble() > 0.01)
                        {
                            this.setEXP(this.getEXP() + exp);
                        }
                        break;
                    case NORMAL:
                        if (this.owner.getRNG().nextDouble() > 0.05)
                        {
                            this.setEXP(this.getEXP() + exp);
                        }
                        break;
                    case HARD:
                        if (this.owner.getRNG().nextDouble() > 0.25)
                        {
                            this.setEXP(this.getEXP() + exp);
                        }
                        break;
                    case PEACEFUL:
                        this.setEXP(this.getEXP() + exp);
                        break;
                }

                if (this.getEXP() >= this.getRequiredEXP())
                {
                    LevelUpEvent e = new LevelUpEvent(this, this.getLevel() + 1);
                    if (MinecraftForge.EVENT_BUS.post(e))
                    {
                        return;
                    }
                    this.levelUp();
                }
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        compound.setInteger("Level", this.getLevel());
        compound.setInteger("EXP", this.getEXP());
        compound.setInteger("requiredEXP", this.getRequiredEXP());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        this.setLevel(compound.getShort("Level"));
        this.setEXP(compound.getInteger("EXP"));
        this.setRequiredEXP(compound.getInteger("requiredEXP"));
    }

    public void setLevel(int level)
    {
        this.syncManager.set(LEVEL, MathHelper.clamp(level, 1, ConfigHandler.LEVELING_SYSTEM.maxLevel));
    }

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
            case PEACEFUL:
                multiplayer = 1;
                break;
        }
        this.setRequiredEXP(this.getRequiredEXP() + MathHelper.getInt(this.owner.getRNG(), this.getLevel(), MathHelper.clamp(multiplayer * this.getLevel(), this.getLevel(), Integer.MAX_VALUE - 1)));
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
}
