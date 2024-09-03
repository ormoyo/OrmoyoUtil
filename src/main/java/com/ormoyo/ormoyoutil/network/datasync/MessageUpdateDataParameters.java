package com.ormoyo.ormoyoutil.network.datasync;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.network.AbstractMessage;
import com.ormoyo.ormoyoutil.network.NetworkDecoder;
import com.ormoyo.ormoyoutil.network.NetworkMessage;
import com.ormoyo.ormoyoutil.network.datasync.AbilitySyncManager.AbilityDataEntry;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.IDataSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@NetworkMessage(modid = OrmoyoUtil.MODID, direction = NetworkDirection.PLAY_TO_CLIENT)
public class MessageUpdateDataParameters extends AbstractMessage<MessageUpdateDataParameters>
{
    private final int targetedPlayerId;

    private final AbilityEntry entry;
    private final Collection<AbilityDataEntry<?>> entries;

    public MessageUpdateDataParameters(Ability ability)
    {
        this(ability.getOwner().getEntityId(), ability.getEntry(), ability.getSyncManager().getDirty());
    }

    private MessageUpdateDataParameters(int targetedPlayerId, AbilityEntry entry, Collection<AbilityDataEntry<?>> values)
    {
        this.targetedPlayerId = targetedPlayerId;

        this.entry = entry;
        this.entries = values;
    }

    public void encode(PacketBuffer buffer)
    {
        buffer.writeVarInt(this.targetedPlayerId);

        buffer.writeRegistryIdUnsafe(Ability.getAbilityRegistry(), this.entry);
        this.writeEntries(buffer, this.entries);
    }

    @NetworkDecoder(MessageUpdateDataParameters.class)
    public static MessageUpdateDataParameters decode(PacketBuffer buffer)
    {
        int targetedPlayerId = buffer.readVarInt();

        AbilityEntry entry = buffer.readRegistryIdUnsafe(Ability.getAbilityRegistry());
        Collection<AbilityDataEntry<?>> entries = readEntries(buffer);

        return new MessageUpdateDataParameters(targetedPlayerId, entry, entries);
    }

    @Override
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {
        PlayerEntity targetedPlayer = (PlayerEntity) player.getEntityWorld().getEntityByID(this.targetedPlayerId);
        Ability ability = Objects.requireNonNull(Ability.getAbilityHolder(targetedPlayer)).getAbility(this.entry.getAbilityClass());

        ability.getSyncManager().setEntryValues(this.entries);
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {
    }

    private void writeEntries(PacketBuffer buffer, Collection<AbilityDataEntry<?>> values)
    {
        buffer.writeVarInt(this.entries.size());
        for (AbilityDataEntry<?> value : this.entries)
        {
            this.writeEntry(buffer, value);
        }
    }

    private <T> void writeEntry(PacketBuffer buffer, AbilityDataEntry<T> entry)
    {
        AbilityDataParameter<T> parameter = entry.getKey();
        int serializerId = DataSerializers.getSerializerId(parameter.getSerializer());

        if (serializerId < 0)
            throw new EncoderException("Unknown serializer type " + parameter.getSerializer());

        buffer.writeByte(parameter.getId());
        buffer.writeVarInt(serializerId);

        parameter.getSerializer().write(buffer, entry.getValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Collection<AbilityDataEntry<?>> readEntries(PacketBuffer buffer)
    {
        int length = buffer.readVarInt();
        Collection<AbilityDataEntry<?>> list = new ArrayList<>(length);

        for (int i = 0; i < length; i++)
        {
            int id = buffer.readUnsignedByte();
            int serializerId = buffer.readVarInt();

            IDataSerializer<?> serializer = DataSerializers.getSerializer(serializerId);

            if (serializer == null)
                throw new DecoderException("Unknown serializer type " + serializerId);

            list.add(new AbilityDataEntry(AbilitySyncManager.convertParameter(serializer.createKey(id)), serializer.read(buffer)));
        }

        return list;
    }
}
