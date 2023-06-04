package com.ormoyo.ormoyoutil.mixin;

import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        this.playerAbilities.clear();
        this.playerAbilities.addAll(((IAbilityHolder) that).getAbilities());
    }
}
