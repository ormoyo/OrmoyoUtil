package com.ormoyo.ormoyoutil.ability;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.Predicate;

public final class AbilityEntryBuilder
{
    private Class<? extends Ability> clazz;
    private Predicate<AbilityHolder> condition;

    private Class<? extends Event>[] conditionCheckingEvents;
    private ResourceLocation location;

    private int level;

    public static <T extends Ability> AbilityEntryBuilder create()
    {
        return new AbilityEntryBuilder();
    }

    /**
     * @param clazz The class of the ability
     * @return this
     */
    public AbilityEntryBuilder ability(Class<? extends Ability> clazz)
    {
        this.clazz = clazz;
        return this;
    }

    /**
     * @param location The id of the ability
     * @return this
     */
    public AbilityEntryBuilder id(ResourceLocation location)
    {
        this.location = location;
        return this;
    }

    /**
     * @param level The level a player needs to level up to unlock the ability
     * @return this
     */
    public AbilityEntryBuilder level(int level)
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
    public final AbilityEntryBuilder condition(Predicate<AbilityHolder> condition, Class<? extends Event>... conditionCheckingEvents)
    {
        this.condition = condition;
        this.conditionCheckingEvents = conditionCheckingEvents;

        return this;
    }

    public AbilityEntry build()
    {
        return new AbilityEntry(this.location, this.clazz, this.level, this.condition, this.conditionCheckingEvents);
    }
}
