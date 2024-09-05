package com.ormoyo.ormoyoutil.capability;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AbilityHolderStorage implements Capability.IStorage<AbilityHolder>
{
    @Nullable
    @Override
    public INBT writeNBT(Capability<AbilityHolder> capability, AbilityHolder instance, Direction side)
    {
        CompoundNBT compound = new CompoundNBT();

        Collection<Ability> abilities = instance.getAbilities();
        ListNBT list = new ListNBT();

        for (Ability ability : abilities)
        {
            CompoundNBT abilityNBT = new CompoundNBT();

            abilityNBT.put("id", StringNBT.valueOf(ability.getRegistryName().toString()));
            ability.writeToNBT(abilityNBT);

            list.add(abilityNBT);
        }

        compound.put("abilities", list);
        return compound;
    }

    @Override
    public void readNBT(Capability<AbilityHolder> capability, AbilityHolder instance, Direction side, INBT nbt)
    {
        if (!(nbt instanceof CompoundNBT))
            return;

        CompoundNBT compound = (CompoundNBT) nbt;

        ListNBT list = compound.getCompound("abilities").getList("ormoyoutil", 10);
        List<Ability> abilities = new ArrayList<>(list.size());

        for (INBT data : list)
        {
            if (!(data instanceof CompoundNBT))
                continue;

            CompoundNBT abilityNBT = (CompoundNBT) data;

            ResourceLocation resourceLocation = new ResourceLocation(abilityNBT.getString("id"));
            AbilityEntry entry = Ability.getAbilityRegistry().getValue(resourceLocation);

            if (entry == null)
            {
                OrmoyoUtil.LOGGER.warn("Ability entry {} doesn't exist", resourceLocation);
                continue;
            }

            Ability ability = entry.newInstance(instance);
            ability.readFromNBT(abilityNBT);

            abilities.add(ability);
        }

        instance.setAbilities(abilities);
    }
}
