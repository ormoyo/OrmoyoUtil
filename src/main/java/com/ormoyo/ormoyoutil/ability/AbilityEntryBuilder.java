package com.ormoyo.ormoyoutil.ability;

import com.ormoyo.ormoyoutil.capability.AbilityHolder;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.thread.EffectiveSide;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AbilityEntryBuilder<T extends Ability>
{
    private Class<T> clazz;
    private Predicate<AbilityHolder> condition;

    private Class<? extends Event>[] conditionCheckingEvents;
    private ResourceLocation location;

    private final List<AbilityEntry.KeyBinding> keyBindings = new ArrayList<>(3);

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

    public AbilityEntryBuilder<T> keybinding(String name, int code)
    {
        return this.keybinding(name, code, 0);
    }

    public AbilityEntryBuilder<T> keybinding(String name, int code, KeyModifier modifier)
    {
        return this.keybinding(name, code, modifier, 0);
    }

    public AbilityEntryBuilder<T> keybinding(String name, int code, KeyModifier modifier, InputType type)
    {
        return this.keybinding(name, code, modifier, type, 0);
    }

    public AbilityEntryBuilder<T> keybinding(String name, int code, KeyModifier modifier, InputType type, KeyConflictContext conflictContext)
    {
        return this.keybinding(name, code, modifier, type, conflictContext, 0);
    }

    public AbilityEntryBuilder<T> keybinding(String name, int code, int cooldown)
    {
        return this.keybinding(name, code, KeyModifier.NONE, 0);
    }

    public AbilityEntryBuilder<T> keybinding(String name, int code, KeyModifier modifier, int cooldown)
    {
        return this.keybinding(name, code, modifier, InputType.KEYBOARD, 0);
    }

    public AbilityEntryBuilder<T> keybinding(String name, int code, KeyModifier modifier, InputType type, int cooldown)
    {
        return this.keybinding(name, code, modifier, type, KeyConflictContext.IN_GAME, 0);
    }

    public AbilityEntryBuilder<T> keybinding(String name, int code, KeyModifier modifier, InputType type, KeyConflictContext conflictContext, int cooldown)
    {
        AbilityKeybinding abilityKeybinding = new AbilityKeybinding(name, code, modifier, type, conflictContext, cooldown);

        AbilityEntry.KeyBinding keybinding =
                EffectiveSide.get().isServer() ? new AbilityEntry.KeyBinding(-1, cooldown, null) :
                DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> new AbilityEntry.KeyBinding.ClientKeyBinding(-1, cooldown, null, abilityKeybinding));

        this.keyBindings.add(keybinding);
        return this;
    }

    public AbilityEntry<T> build()
    {
        AbilityEntry<T> entry = new AbilityEntry<>(this.clazz, this.level, this.condition, this.keyBindings, this.conditionCheckingEvents);
        if (location != null)
            entry.setRegistryName(this.location);

        return entry;
    }

    public enum InputType
    {
        KEYBOARD("KEYSYM"),
        MOUSE("MOUSE"),
        SCANCODE("SCANCODE");

        public final String desc;
        InputType(String desc)
        {
            this.desc = desc;
        }
    }

    static class AbilityKeybinding
    {
        final String name;
        final int code;
        final KeyModifier modifier;
        final InputType type;
        final KeyConflictContext context;

        AbilityKeybinding(String name, int code, KeyModifier modifier, InputType type, KeyConflictContext conflictContext, int cooldown)
        {
            this.name = name;
            this.code = code;
            this.modifier = modifier;
            this.type = type;
            this.context = conflictContext;
        }
    }
}
