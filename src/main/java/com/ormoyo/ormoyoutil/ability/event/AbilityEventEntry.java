package com.ormoyo.ormoyoutil.ability.event;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class AbilityEventEntry extends ForgeRegistryEntry<AbilityEventEntry>
{
    private final Class<? extends Event> event;
    private final AbilityEventPredicate<? extends Event> predicate;

    public <T extends Event> AbilityEventEntry(ResourceLocation name, Class<T> event, AbilityEventPredicate<T> predicate)
    {
        this.setRegistryName(name);

        this.event = event;
        this.predicate = predicate;
    }

    public Class<? extends Event> getEventClass()
    {
        return this.event;
    }

    public AbilityEventPredicate<? extends Event> getEventPredicate()
    {
        return this.predicate;
    }

    @Override
    public String toString()
    {
        return this.event.getSimpleName();
    }
}
