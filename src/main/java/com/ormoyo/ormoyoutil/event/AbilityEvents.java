package com.ormoyo.ormoyoutil.event;

import com.ormoyo.ormoyoutil.abilities.AbilityStats;
import com.ormoyo.ormoyoutil.ability.Ability;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

/**
 * AbilityEvent is fired when an event involving abilities occurs
 */
public class AbilityEvents extends PlayerEvent
{
    protected final Ability ability;

    public AbilityEvents(Ability ability)
    {
        super(ability.getOwner());
        this.ability = ability;
    }

    public static class StatsEvents extends AbilityEvents
    {
        private final AbilityStats stats;

        public StatsEvents(AbilityStats stats)
        {
            super(stats);
            this.stats = stats;
        }

        @Cancelable
        public static class LevelUpEvent extends StatsEvents
        {
            private final int level;

            public LevelUpEvent(AbilityStats stats, int level)
            {
                super(stats);
                this.level = level;
            }

            public int getLevel()
            {
                return this.level;
            }
        }

        /**
         * Called when calculating the exp of an entity that was killed by a player
         */
        @Cancelable
        public static class CalculateEntityExp extends StatsEvents
        {
            private final EntityLivingBase entity;
            private int exp;

            public CalculateEntityExp(AbilityStats stats, EntityLivingBase entity, int EXP)
            {
                super(stats);
                this.entity = entity;
                this.exp = EXP;
            }

            public void setExp(int EXP)
            {
                this.exp = EXP;
            }

            public int getExp()
            {
                return this.exp;
            }

            public EntityLivingBase getEntity()
            {
                return this.entity;
            }

            public EntityLivingBase getEntityLiving()
            {
                return this.entity;
            }
        }

        public AbilityStats getStats()
        {
            return this.stats;
        }
    }

    /**
     * OnAbilityUnlockedEvent is fired when a player unlocks an ability
     */
    @Cancelable
    public static class OnAbilityUnlockedEvent extends AbilityEvents
    {
        public OnAbilityUnlockedEvent(Ability ability)
        {
            super(ability);
        }
    }

    public Ability getAbility()
    {
        return this.ability;
    }
}
