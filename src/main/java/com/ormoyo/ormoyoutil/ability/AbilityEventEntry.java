package com.ormoyo.ormoyoutil.ability;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.registries.IForgeRegistryEntry.Impl;

public class AbilityEventEntry extends Impl<AbilityEventEntry>
{
    private final Class<? extends Event> event;
    private final IAbilityEventPredicate<? extends Event> predicate;

    public <T extends Event> AbilityEventEntry(ResourceLocation name, Class<T> event, IAbilityEventPredicate<T> predicate)
    {
        this.setRegistryName(name);
        this.event = event;
        this.predicate = predicate;
    }

    public Class<? extends Event> getEventClass()
    {
        return this.event;
    }

    @SuppressWarnings("rawtypes")
    public IAbilityEventPredicate getEventPredicate()
    {
        return this.predicate;
    }

    @Override
    public String toString()
    {
        return this.event.getSimpleName();
    }
}
