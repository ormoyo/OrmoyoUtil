package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.capability.AbilityHolder;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.Predicate;

public final class AbilityEntryBuilder<T extends Ability>
{
    private Class<T> clazz;
    private Predicate<AbilityHolder> condition;

    private Class<? extends Event>[] conditionCheckingEvents;
    private ResourceLocation location;

    private int level;

    public static<T extends Ability> AbilityEntryBuilder<T> create()
    {
        return new AbilityEntryBuilder<>();
    }

    /**
     * @param clazz The class of the ability
     * @return this
     */
    public AbilityEntryBuilder<T> ability(Class<T> clazz)
    {
        this.clazz = clazz;
        return this;
    }

    /**
     * @param location The id of the ability
     * @return this
     */
    public AbilityEntryBuilder<T> id(ResourceLocation location)
    {
        this.location = location;
        return this;
    }

    /**
     * @param level The level a player needs to level up to unlock the ability
     * @return this
     */
    public AbilityEntryBuilder<T> level(int level)
    {
        this.level = level;
        return this;
    }

    /**
     * If the default system isn't for you, you can use this
     * <p>
     * If an event that got fired is part of the conditionCheckingEvents it will test all players if on server side or <br> it will test the client player if on client side
     *
     * @param condition               The condition to test if a player should unlock the ability
     * @param conditionCheckingEvents The events where when fired the provided condition would be tested
     * @return this
     */
    @SafeVarargs
    public final AbilityEntryBuilder<T> condition(Predicate<AbilityHolder> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this.condition = condition;
        this.conditionCheckingEvents = conditionCheckingEvents;

        return this;
    }

    public AbilityEntry build()
    {
        if (this.location == null)
            return new AbilityEntry(this.clazz, this.level, this.condition, this.conditionCheckingEvents);
    public AbilityEntry<T> build()
    {
        AbilityEntry<T> entry = new AbilityEntry<>(this.clazz, this.level, this.condition, this.conditionCheckingEvents);
        if (location != null)
            entry.setRegistryName(this.location);

        return entry;
    }

        AbilityKeybinding(String name, int code, KeyModifier modifier, InputType type, KeyConflictContext conflictContext)
        {
            this.name = name;
            this.code = code;
            this.modifier = modifier;
            this.type = type;
            this.context = conflictContext;
        }
    }
}
