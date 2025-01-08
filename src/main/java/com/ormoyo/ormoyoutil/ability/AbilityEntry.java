package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.capability.AbilityHolder;
import net.minecraft.client.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class AbilityEntry<T extends Ability> extends ForgeRegistryEntry<AbilityEntry<?>>
{
    private static final Class<? extends Event>[] EMPTY_CLASS_ARRAY = new Class[0];

    Function<AbilityHolder, T> abilityConstructor;

    private final Class<T> clazz;
    private final Class<? extends Event>[] conditionCheckingEvents;

    private final Predicate<AbilityHolder> condition;
    private final int level;

    final List<Integer> keyBindingIndices;

    public AbilityEntry(Class<T> clazz)
    {
        this(clazz, null);
    }

    public AbilityEntry(Class<T> clazz, int level)
    {
        this(clazz, level, null);
    }

    public AbilityEntry(Class<T> clazz, Predicate<AbilityHolder> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this(clazz, 0, condition, conditionCheckingEvents);
    }

    @SafeVarargs
    public AbilityEntry(Class<T> clazz, int level, Predicate<AbilityHolder> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this(clazz, level, condition, 0, conditionCheckingEvents);
    }

    @SafeVarargs
    AbilityEntry(Class<T> clazz, int level, Predicate<AbilityHolder> condition, int keybindingsCount, Class<? extends Event>... conditionCheckingEvents)
    {
        this.clazz = clazz;

        this.condition = condition;
        this.conditionCheckingEvents = conditionCheckingEvents == null ? EMPTY_CLASS_ARRAY : conditionCheckingEvents;

        this.level = Math.max(level, 0);
        this.keyBindingIndices = new ArrayList<>(keybindingsCount);
    }

    public Class<T> getAbilityClass()
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
            OrmoyoUtil.LOGGER.catching(e);
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

    /**
     * @implNote If the ability doesn't extend {@link AbilityKeybindingBase} this will always be empty
     * @return The indices of the ability keybindings in {@link GameSettings#keyBindings}
     */
    public List<Integer> getKeyBindingIndices()
    {
        return Collections.unmodifiableList(this.keyBindingIndices);
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
            AbilityEntry<?> entry = (AbilityEntry<?>) obj;
            return Objects.equals(this.getRegistryName(), entry.getRegistryName());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        ResourceLocation name = this.getRegistryName();
        return name != null ? name.hashCode() : 0;
    }
}
