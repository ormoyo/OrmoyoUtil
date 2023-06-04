package com.ormoyo.ormoyoutil.capability;

import com.google.common.collect.HashBiMap;
import com.ormoyo.ormoyoutil.ability.Ability;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AbilityCap implements IAbilityCap
{
    private final Map<ResourceLocation, Ability> unlockedAbilities = HashBiMap.create();

    private AbilitySet abilitySet;
    private EntityPlayer player;

    public AbilityCap()
    {
    }

    public AbilityCap(EntityPlayer player)
    {
        this.player = player;
    }

    @Override
    public boolean unlockAbility(Ability ability)
    {
        return !this.isAbilityUnlocked(ability) && this.unlockedAbilities.put(ability.getRegistryName(), ability) == null;
    }

    @Override
    public boolean isAbilityUnlocked(Ability ability)
    {
        return this.unlockedAbilities.containsKey(ability.getRegistryName());
    }

    @Override
    public boolean isAbilityUnlocked(ResourceLocation name)
    {
        return this.unlockedAbilities.containsKey(name);
    }

    @Override
    public boolean isAbilityUnlocked(Class<? extends Ability> clazz)
    {
        for (Ability ability : this.unlockedAbilities.values())
        {
            if (ability.getClass() == clazz)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Ability getAbility(ResourceLocation name)
    {
        return this.unlockedAbilities.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Ability> T getAbility(Class<T> clazz)
    {
        for (Ability ability : this.unlockedAbilities.values())
        {
            if (ability.getClass() == clazz)
            {
                return (T) ability;
            }
        }
        return null;
    }

    public Set<Ability> getAbilities()
    {
        Set<Ability> abilitySet;
        return (abilitySet = this.abilitySet) == null ? (this.abilitySet = new AbilitySet()) : abilitySet;
    }

    public EntityPlayer getPlayer()
    {
        return this.player;
    }

    class AbilitySet extends AbstractSet<Ability>
    {
        @Override
        public Iterator<Ability> iterator()
        {
            return new Iter();
        }

        @Override
        public int size()
        {
            return AbilityCap.this.unlockedAbilities.size();
        }

        class Iter implements Iterator<Ability>
        {
            final Iterator<Ability> iterator;

            public Iter()
            {
                this.iterator = AbilityCap.this.unlockedAbilities.values().iterator();
            }

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public Ability next()
            {
                return this.iterator.next();
            }

            @Override
            public void remove()
            {
                this.iterator.remove();
            }
        }
    }
}
