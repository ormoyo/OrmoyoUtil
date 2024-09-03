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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@NetworkMessage(modid = OrmoyoUtil.MODID, direction = NetworkDirection.PLAY_TO_CLIENT)
public class MessageSetAbilities extends AbstractMessage<MessageSetAbilities>
{
    private final int targetedPlayerId;
    private final Collection<AbilityEntry> entries;

    public MessageSetAbilities(AbilityHolder targetedPlayer, Collection<AbilityEntry> entries)
    {
        this(targetedPlayer.getPlayer().getEntityId(), entries);
    }

    private MessageSetAbilities(int targetedPlayerId, Collection<AbilityEntry> entries)
    {
        this.targetedPlayerId = targetedPlayerId;
        this.entries = entries;
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeVarInt(this.targetedPlayerId);
        buffer.writeVarInt(this.entries.size());

        for (AbilityEntry entry : this.entries)
            buffer.writeRegistryId(entry);
    }

    @NetworkDecoder(MessageSetAbilities.class)
    public static MessageSetAbilities decode(PacketBuffer buffer)
    {
        int playerId = buffer.readVarInt();
        int capacity = buffer.readVarInt();

        Collection<AbilityEntry> entries = new ArrayList<>(capacity);

        for (int i = 0; i < capacity; i++)
            entries.add(buffer.readRegistryId());

        return new MessageSetAbilities(playerId, entries);
    }

    @Override
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {
        PlayerEntity targetedPlayer = (PlayerEntity) player.getEntityWorld().getEntityByID(this.targetedPlayerId);
        AbilityHolder holder = Ability.getAbilityHolder(targetedPlayer);

        assert holder != null;
        Collection<Ability> abilities = this.entries.stream()
                .map(entry -> entry.newInstance(holder))
                .collect(Collectors.toList());

        holder.setAbilities(abilities);
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {
    }
}
