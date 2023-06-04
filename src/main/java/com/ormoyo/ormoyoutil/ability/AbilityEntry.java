package com.ormoyo.ormoyoutil.ability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.registries.IForgeRegistryEntry.Impl;

import java.util.function.Function;
import java.util.function.Predicate;

public class AbilityEntry extends Impl<AbilityEntry>
{
    Function<EntityPlayer, ? extends Ability> abilityConstructor;
    private final Class<? extends Ability> clazz;
    private final Class<? extends Event>[] conditionCheckingEvents;
    private final Predicate<EntityPlayer> condition;
    private final EnumVisability visability;
    private final int level;

    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz)
    {
        this(name, clazz, 1, null, EnumVisability.VISABLE);
    }

    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz, int level)
    {
        this(name, clazz, level, null, EnumVisability.VISABLE);
    }

    @SafeVarargs
    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz, Predicate<EntityPlayer> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this(name, clazz, condition, EnumVisability.VISABLE, conditionCheckingEvents);
    }

    @SafeVarargs
    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz, int level, Predicate<EntityPlayer> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this(name, clazz, level, condition, EnumVisability.VISABLE, conditionCheckingEvents);
    }

    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz, int level, EnumVisability visability)
    {
        this(name, clazz, level, null, visability);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz, Predicate<EntityPlayer> condition, EnumVisability visability, Class<? extends Event>... conditionCheckingEvents)
    {
        this.clazz = clazz;
        this.condition = condition != null ? condition : player -> false;
        this.conditionCheckingEvents = conditionCheckingEvents.length != 0 ? conditionCheckingEvents : (Class<? extends Event>[]) new Class<?>[]{PlayerLoggedInEvent.class};
        this.visability = visability;
        this.level = 0;
        this.setRegistryName(name);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz, int level, Predicate<EntityPlayer> condition, EnumVisability visability, Class<? extends Event>... conditionCheckingEvents)
    {
        this.clazz = clazz;
        this.condition = condition != null ? condition : player -> false;
        this.conditionCheckingEvents = conditionCheckingEvents.length != 0 ? conditionCheckingEvents : (Class<? extends Event>[]) new Class<?>[]{PlayerLoggedInEvent.class};
        this.visability = visability;
        this.level = Math.max(level, 1);
        this.setRegistryName(name);
    }

    public AbilityEntry(String name, Class<? extends Ability> clazz)
    {
        this(name, clazz, 1, null, EnumVisability.VISABLE);
    }

    public AbilityEntry(String name, Class<? extends Ability> clazz, int level)
    {
        this(name, clazz, level, null, EnumVisability.VISABLE);
    }

    @SafeVarargs
    public AbilityEntry(String name, Class<? extends Ability> clazz, Predicate<EntityPlayer> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this(name, clazz, condition, EnumVisability.VISABLE, conditionCheckingEvents);
    }

    @SafeVarargs
    public AbilityEntry(String name, Class<? extends Ability> clazz, int level, Predicate<EntityPlayer> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this(name, clazz, level, condition, EnumVisability.VISABLE, conditionCheckingEvents);
    }

    public AbilityEntry(String name, Class<? extends Ability> clazz, int level, EnumVisability visability)
    {
        this(name, clazz, level, null, visability);
    }

    @SafeVarargs
    public AbilityEntry(String name, Class<? extends Ability> clazz, Predicate<EntityPlayer> condition, EnumVisability visability, Class<? extends Event>... conditionCheckingEvents)
    {
        this.clazz = clazz;
        this.condition = condition;
        this.conditionCheckingEvents = conditionCheckingEvents;
        this.visability = visability;
        this.level = 0;
        this.setRegistryName(name);
    }

    @SafeVarargs
    public AbilityEntry(String name, Class<? extends Ability> clazz, int level, Predicate<EntityPlayer> condition, EnumVisability visability, Class<? extends Event>... conditionCheckingEvents)
    {
        this.clazz = clazz;
        this.condition = condition;
        this.conditionCheckingEvents = conditionCheckingEvents;
        this.visability = visability;
        this.level = Math.max(level, 1);
        this.setRegistryName(name);
    }

    public Class<? extends Ability> getAbilityClass()
    {
        return this.clazz;
    }

    public Ability newInstance(EntityPlayer player)
    {
        try
        {
            Ability ability = this.abilityConstructor.apply(player);
            return ability;
        }
        catch (IllegalArgumentException | SecurityException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public int getLevel()
    {
        return this.level;
    }

    public Predicate<EntityPlayer> getCondition()
    {
        return this.condition;
    }

    public Class<? extends Event>[] getConditionCheckingEvents()
    {
        return this.conditionCheckingEvents;
    }

    public EnumVisability getVisability()
    {
        return this.visability;
    }

    @Override
    public String toString()
    {
        return this.getRegistryName().toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj instanceof AbilityEntry)
        {
            AbilityEntry entry = (AbilityEntry) obj;
            return this.getRegistryName().equals(entry.getRegistryName());
        }
        return false;
    }
}
