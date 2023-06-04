package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

@NetworkMessage(modid = OrmoyoUtil.MODID, direction = NetworkDirection.PLAY_TO_CLIENT)
public class MessageUnlockAbility extends AbstractMessage<MessageUnlockAbility>
{
    private final AbilityEntry entry;

    public MessageUnlockAbility(AbilityEntry abilityEntry)
    {
        this.entry = abilityEntry;
    }

    @Override
    public void encode(PacketBuffer buf)
    {
        buf.writeRegistryId(this.entry);
    }

    @NetworkDecoder(MessageUnlockAbility.class)
    public static MessageUnlockAbility decode(PacketBuffer buf)
    {
        AbilityEntry entry = buf.readRegistryId();
        return new MessageUnlockAbility(entry);
    }

    @Override
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {
        IAbilityHolder abilityHolder = (IAbilityHolder) player;
        abilityHolder.unlockAbility(this.entry);
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {
    }
}
