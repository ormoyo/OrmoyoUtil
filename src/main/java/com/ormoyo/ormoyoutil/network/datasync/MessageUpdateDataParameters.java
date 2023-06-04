package com.ormoyo.ormoyoutil.network.datasync;

import com.google.common.collect.Lists;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.network.AbstractMessage;
import com.ormoyo.ormoyoutil.network.NetworkMessage;
import com.ormoyo.ormoyoutil.network.datasync.AbilitySyncManager.DataEntry;
import com.ormoyo.ormoyoutil.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.List;

@NetworkMessage(modid = OrmoyoUtil.MODID, side = Side.CLIENT)
public class MessageUpdateDataParameters extends AbstractMessage<MessageUpdateDataParameters>
{
    ResourceLocation location;
    List<DataEntry<?>> entries;

    public MessageUpdateDataParameters()
    {
    }

    public MessageUpdateDataParameters(Ability ability, List<DataEntry<?>> values)
    {
        this.location = ability.getRegistryName();
        this.entries = values;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = Utils.getPacketBuffer(buf);
        this.location = buffer.readResourceLocation();
        this.entries = this.readEntries(buffer);
    }

    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = Utils.getPacketBuffer(buf);
        buffer.writeResourceLocation(this.location);
        this.writeEntries(buffer, this.entries);
    }

    @Override
    public void onClientReceived(Minecraft client, EntityPlayer player, MessageContext messageContext)
    {
        Ability ability = OrmoyoUtil.PROXY.getAbility(this.location, player);
        ability.getSyncManager().setEntryValues(this.entries);
    }

    @Override
    public void onServerReceived(MinecraftServer server, EntityPlayer player, MessageContext messageContext)
    {
    }

    private void writeEntries(PacketBuffer buf, List<DataEntry<?>> values)
    {
        buf.writeVarInt(this.entries.size());
        for (DataEntry<?> value : this.entries)
        {
            this.writeEntry(buf, value);
        }
    }

    private <T> void writeEntry(PacketBuffer buf, DataEntry<T> entry)
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
    private List<DataEntry<?>> readEntries(PacketBuffer buf)
    {
        List<DataEntry<?>> list = null;
        try
        {
            int length = buf.readInt();
            for (int i = 0; i < length; i++)
            {
                if (list == null)
                {
                    list = Lists.newArrayList();
                }

                int id = buf.readUnsignedByte();
                int j = buf.readInt();
                DataSerializer<?> serializer = DataSerializers.getSerializer(j);

                if (serializer == null)
                {
                    throw new DecoderException("Unknown serializer type " + j);
                }

                list.add(new DataEntry(AbilitySyncManager.convertParameter(serializer.createKey(id)), serializer.read(buf)));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return list;
    }
}
