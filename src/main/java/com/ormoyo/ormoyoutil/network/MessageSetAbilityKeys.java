package com.ormoyo.ormoyoutil.network;

import com.google.common.collect.BiMap;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.AbilityKeybindingBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
@NetworkMessage(modid = OrmoyoUtil.MODID, direction = NetworkDirection.PLAY_TO_SERVER)
public class MessageSetAbilityKeys extends AbstractMessage<MessageSetAbilityKeys>
{
    private static final BiMap<String, Integer> KEYBIND_IDS;
    private final Map<String, Integer> keys;

    public MessageSetAbilityKeys(Map<String, Integer> keys)
    {
        this.keys = keys;
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeVarInt(keys.size());
        for (Map.Entry<String, Integer> entry : keys.entrySet())
        {
            buffer.writeVarInt(entry.getValue());
            buffer.writeString(entry.getKey());
        }
    }

    @NetworkDecoder(MessageSetAbilityKeys.class)
    public static MessageSetAbilityKeys decode(PacketBuffer buffer)
    {
        int size = buffer.readVarInt();
        Map<String, Integer> map = new HashMap<>(size);

        for (int i = 0; i < size; i++)
        {
            int id = buffer.readVarInt();
            String desc = buffer.readString();

            map.put(desc, id);
        }

        return new MessageSetAbilityKeys(map);
    }

    @Override
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {
        for (Map.Entry<String, Integer> entry : keys.entrySet())
        {
            KEYBIND_IDS.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    static
    {
        BiMap<String, Integer> map = null;
        try
        {
            if (EffectiveSide.get().isServer())
            {
                Field mapField = AbilityKeybindingBase.class.getDeclaredField("KEYBIND_IDS");
                mapField.setAccessible(true);
                map = (BiMap<String, Integer>) mapField.get(null);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        KEYBIND_IDS = map;
    }
}
