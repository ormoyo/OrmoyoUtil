package com.ormoyo.ormoyoutil.mixin;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
import com.ormoyo.ormoyoutil.event.AbilityEvents;
import com.ormoyo.ormoyoutil.network.MessageUnlockAbility;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements IAbilityHolder
{
    protected final Collection<Ability> playerAbilities = new HashSet<>();

    protected MixinPlayerEntity(World world)
    {
        super(EntityType.PLAYER, world);
    }

    @Override
    public Collection<Ability> getAbilities()
    {
        return Collections.unmodifiableCollection(this.playerAbilities);
    }

    @Override
    public Ability getAbility(ResourceLocation resourceLocation)
    {
        for (Ability ability : this.playerAbilities)
        {
            if (Objects.equals(ability.getEntry().getRegistryName(), resourceLocation))
            {
                return ability;
            }
        }
        return null;
    }

    @Override
    public Ability getAbility(Class<? extends Ability> clazz)
    {
        for (Ability ability : this.playerAbilities)
        {
            if (ability.getClass() == clazz)
            {
                return ability;
            }
        }
        return null;
    }

    @Override
    public boolean unlockAbility(AbilityEntry entry)
    {
        Ability ability = entry.newInstance(this);

        boolean isUnlocked = this.getAbility(ability.getClass()) == null && this.playerAbilities.add(ability);
        if (isUnlocked)
        {
            if (MinecraftForge.EVENT_BUS.post(new AbilityEvents.OnAbilityUnlockedEvent(ability)))
                return false;

            if (EffectiveSide.get().isServer())
                OrmoyoUtil.NETWORK_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) this.getPlayer()), new MessageUnlockAbility(ability.getEntry()));

            ability.onUnlock();
        }

        return isUnlocked;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    protected void onTick(CallbackInfo info)
    {
        for (Ability ability : this.playerAbilities)
        {
            if (!ability.isEnabled())
                continue;

            ability.onUpdate();
        }
    }

    @Inject(method = "writeAdditional", at = @At("RETURN"))
    protected void onWriteAdditional(CompoundNBT compound, CallbackInfo info)
    {
        ListNBT list = new ListNBT();

        for (Ability ability : this.playerAbilities)
        {
            CompoundNBT abilityNBT = new CompoundNBT();

            abilityNBT.put("id", StringNBT.valueOf(ability.getRegistryName().toString()));
            ability.writeToNBT(abilityNBT);

            list.add(abilityNBT);
        }

        compound.getCompound("abilities").put("ormoyoutil", list);
    }

    @Inject(method = "readAdditional", at = @At("RETURN"))
    protected void onReadAdditional(CompoundNBT compound, CallbackInfo info)
    {
        ListNBT list = compound.getCompound("abilities").getList("ormoyoutil", 10);
        List<Ability> abilities = new ArrayList<>(list.size());

        for (INBT nbt : list)
        {
            if (!(nbt instanceof CompoundNBT))
                continue;

            CompoundNBT abilityNBT = (CompoundNBT) nbt;

            ResourceLocation resourceLocation = new ResourceLocation(abilityNBT.getString("id"));
            AbilityEntry entry = Ability.getAbilityRegistry().getValue(resourceLocation);

            if (entry == null)
            {
                OrmoyoUtil.LOGGER.warn("Ability entry {} doesn't exist", resourceLocation);
                continue;
            }

            Ability ability = entry.newInstance(this);
            ability.readFromNBT(abilityNBT);

            abilities.add(ability);
        }

        this.setPlayerAbilities(abilities);
    }

    private void setPlayerAbilities(Collection<Ability> abilities)
    {
        this.playerAbilities.clear();
        this.playerAbilities.addAll(abilities);
    }
}
