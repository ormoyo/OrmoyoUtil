package com.ormoyo.ormoyoutil.ability;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class AbilityEntry extends ForgeRegistryEntry<AbilityEntry>
{
    private static final Class<? extends Event>[] EMPTY_CLASS_ARRAY = new Class[0];

    Function<AbilityHolder, ? extends Ability> abilityConstructor;

    private final Class<? extends Ability> clazz;
    private final Class<? extends Event>[] conditionCheckingEvents;

    private final Predicate<AbilityHolder> condition;
    private final int level;

    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz)
    {
        this(name, clazz, null);
    }

    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz, int level)
    {
        this(name, clazz, level, null);
    }

    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz, Predicate<AbilityHolder> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this(name, clazz, 0, condition, conditionCheckingEvents);
    }

    @SafeVarargs
    public AbilityEntry(ResourceLocation name, Class<? extends Ability> clazz, int level, Predicate<AbilityHolder> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this(name.toString(), clazz, level, condition, conditionCheckingEvents);
    }

    public AbilityEntry(String name, Class<? extends Ability> clazz)
    {
        this(name, clazz, null);
    }

    public AbilityEntry(String name, Class<? extends Ability> clazz, int level)
    {
        this(name, clazz, level, null);
    }

    public AbilityEntry(String name, Class<? extends Ability> clazz, Predicate<AbilityHolder> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this(name, clazz, 0, condition, conditionCheckingEvents);
    }

    @SafeVarargs
    public AbilityEntry(String name, Class<? extends Ability> clazz, int level, Predicate<AbilityHolder> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this.clazz = clazz;

        this.condition = condition;
        this.conditionCheckingEvents = conditionCheckingEvents == null ? EMPTY_CLASS_ARRAY : conditionCheckingEvents;

        this.level = Math.max(level, 0);

        this.setRegistryName(name);
    }

    public Class<? extends Ability> getAbilityClass()
    {
        return this.clazz;
    }

    public Ability newInstance(AbilityHolder abilityHolder)
    {
        try
        {
            return this.abilityConstructor.apply(abilityHolder);
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

    public Predicate<AbilityHolder> getCondition()
    {
        return this.condition;
    }

    public Class<? extends Event>[] getConditionCheckingEvents()
    {
        return this.conditionCheckingEvents;
    }

    @Override
    public String toString()
    {
        return String.valueOf(this.getRegistryName());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (obj instanceof AbilityEntry)
        {
            AbilityEntry entry = (AbilityEntry) obj;
            return Objects.equals(this.getRegistryName(), entry.getRegistryName());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.getRegistryName());
    }
}
