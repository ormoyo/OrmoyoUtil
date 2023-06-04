package com.ormoyo.ormoyoutil.util;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

import java.util.UUID;

public class ModifierHandler
{

    private static final UUID MODIFIER_ID_HEALTH = UUID.fromString("c0bef565-35f6-4dc5-bb4c-3644c382e6ce");
    private static final String MODIFIER_NAME_HEALTH = OrmoyoUtil.MODID + ".HealthModifier";

    private static void setModifier(IAttributeInstance attr, UUID id, String name, double amount, int op)
    {
        if (attr == null)
        {
            return;
        }

        double normalValue = attr.getBaseValue();
        double difference = amount - normalValue;

        AttributeModifier mod = attr.getModifier(id);
        AttributeModifier newMod = new AttributeModifier(id, name, difference, op);

        // Remove the old, apply the new.
        if (mod != null)
        {
            attr.removeModifier(mod);
        }
        attr.applyModifier(newMod);
    }

    public static void setMaxHealth(EntityLivingBase entity, double amount, int op)
    {
        if (amount <= 0)
        {
            OrmoyoUtil.LOGGER.warn("Cannot set entity health to 0");
            return;
        }

        float originalHealth = entity.getHealth();
        IAttributeInstance attr = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr != null)
        {
            setModifier(attr, MODIFIER_ID_HEALTH, MODIFIER_NAME_HEALTH, amount, op);
            entity.setHealth(originalHealth);
        }
    }
}
