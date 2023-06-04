package com.ormoyo.ormoyoutil.capability;

import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.util.Constants;

import java.util.Set;

public class AbiltyCapStorage implements IStorage<IAbilityCap>
{
    public NBTBase writeNBT(Capability<IAbilityCap> capability, IAbilityCap instance, EnumFacing side)
    {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        Set<Ability> unlockedAbilities = instance.getAbilities();
        for (Ability ability : unlockedAbilities)
        {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setString("id", ability.getName());
            ability.writeToNBT(compound);
            list.appendTag(compound);
        }
        nbt.setTag("unlockedAbilities", list);
        return nbt;
    }

    public void readNBT(Capability<IAbilityCap> capability, IAbilityCap instance, EnumFacing side, NBTBase nbt)
    {
        if (!(instance instanceof AbilityCap))
        {
            return;
        }
        NBTTagCompound tag = (NBTTagCompound) nbt;
        NBTTagList list = tag.getTagList("unlockedAbilities", Constants.NBT.TAG_COMPOUND);
        for (NBTBase n : list)
        {
            NBTTagCompound compound = (NBTTagCompound) n;
            String id = compound.getString("id");
            AbilityEntry entry = Ability.getAbilityRegistry().getValue(new ResourceLocation(id));
            if (entry != null)
            {
                CapabilityHandler.nbtData.put(entry, compound);
            }
        }
    }
}
