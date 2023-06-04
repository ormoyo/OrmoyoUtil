package com.ormoyo.ormoyoutil.network.datasync;

import com.google.common.collect.Lists;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
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

import java.util.List;

@NetworkMessage(modid = OrmoyoUtil.MODID, direction = NetworkDirection.PLAY_TO_CLIENT)
public class MessageUpdateDataParameters extends AbstractMessage<MessageUpdateDataParameters>
{
    AbilityEntry entry;
    List<AbilityDataEntry<?>> entries;

    public MessageUpdateDataParameters()
    {
    }

    public MessageUpdateDataParameters(AbilityEntry entry, List<AbilityDataEntry<?>> values)
    {
        this.entry = entry;
        this.entries = values;
    }

    public void encode(PacketBuffer buf)
    {
        buf.writeRegistryId(this.entry);
        this.writeEntries(buf, this.entries);
    }

    @NetworkDecoder(MessageUpdateDataParameters.class)
    public static MessageUpdateDataParameters decode(PacketBuffer buf)
    {
        AbilityEntry entry = buf.readRegistryId();
        List<AbilityDataEntry<?>> entries = readEntries(buf);

        return new MessageUpdateDataParameters(entry, entries);
    }

    @Override
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {
        Ability ability = ((IAbilityHolder) player).getAbility(this.entry.getAbilityClass());
        ability.getSyncManager().setEntryValues(this.entries);
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {
    }

    private void writeEntries(PacketBuffer buf, List<AbilityDataEntry<?>> values)
    {
        buf.writeVarInt(this.entries.size());
        for (AbilityDataEntry<?> value : this.entries)
        {
            this.writeEntry(buf, value);
        }
    }

    private <T> void writeEntry(PacketBuffer buf, AbilityDataEntry<T> entry)
    {
        AbilityDataParameter<T> parameter = entry.getKey();
        int i = DataSerializers.getSerializerId(parameter.getSerializer());
        if (i < 0)
        {
            throw new EncoderException("Unknown serializer type " + parameter.getSerializer());
        }
        else
        {
            buf.writeByte(parameter.getId());
            buf.writeVarInt(i);
            parameter.getSerializer().write(buf, entry.getValue());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<AbilityDataEntry<?>> readEntries(PacketBuffer buf)
    {
        List<AbilityDataEntry<?>> list = null;
        int length = buf.readInt();

        for (int i = 0; i < length; i++)
        {
            if (list == null)
                list = Lists.newArrayList();

            int id = buf.readUnsignedByte();
            int j = buf.readInt();

            IDataSerializer<?> serializer = DataSerializers.getSerializer(j);

            if (serializer == null)
                throw new DecoderException("Unknown serializer type " + j);

            list.add(new AbilityDataEntry(AbilitySyncManager.convertParameter(serializer.createKey(id)), serializer.read(buf)));
        }

        return list;
    }
}
