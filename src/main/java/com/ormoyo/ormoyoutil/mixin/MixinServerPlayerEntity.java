package com.ormoyo.ormoyoutil.mixin;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
import com.ormoyo.ormoyoutil.network.MessageSetAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends MixinPlayerEntity
{
    protected MixinServerPlayerEntity(World world)
    {
        super(world);
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    protected void onCopyFrom(ServerPlayerEntity that, boolean keepEverything, CallbackInfo info)
    {
        Collection<Ability> abilities = ((IAbilityHolder)that).getAbilities();
        PlayerEntity player = (PlayerEntity)(Object)this;

        this.setPlayerAbilities(abilities);

        OrmoyoUtil.NETWORK_CHANNEL.send(PacketDistributor.PLAYER.with(() ->
                (ServerPlayerEntity)player),
                new MessageSetAbilities(abilities.stream()
                        .map(Ability::getEntry)
                        .collect(Collectors.toList())));
    }
}
