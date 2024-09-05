package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;

@NetworkMessage(modid = OrmoyoUtil.MODID, direction = NetworkDirection.PLAY_TO_CLIENT)
public class MessageUnlockAbility extends AbstractMessage<MessageUnlockAbility>
{
    private final int targetedPlayerId;
    private final AbilityEntry entry;

    public MessageUnlockAbility(AbilityHolder holder, AbilityEntry abilityEntry)
    {
        this(holder.asPlayer().getEntityId(), abilityEntry);
    }

    private MessageUnlockAbility(int targetedPlayerId, AbilityEntry abilityEntry)
    {
        this.targetedPlayerId = targetedPlayerId;
        this.entry = abilityEntry;
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeVarInt(this.targetedPlayerId);
        buffer.writeRegistryId(this.entry);
    }

    @NetworkDecoder(MessageUnlockAbility.class)
    public static MessageUnlockAbility decode(PacketBuffer buffer)
    {
        int targetedPlayerId = buffer.readVarInt();
        AbilityEntry entry = buffer.readRegistryId();

        return new MessageUnlockAbility(targetedPlayerId, entry);
    }

    @Override
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {
        PlayerEntity targetedPlayer = (PlayerEntity) player.getEntityWorld().getEntityByID(this.targetedPlayerId);

        AbilityHolder abilityHolder = Ability.getAbilityHolder(targetedPlayer);
        Objects.requireNonNull(abilityHolder).unlockAbility(this.entry);
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {
    }
}
