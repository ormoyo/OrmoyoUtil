package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.ability.AbilityKeybindingBase;
import com.ormoyo.ormoyoutil.util.ASMUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class MessageSetAbilityKeys extends AbstractMessage<MessageSetAbilityKeys>
{
    private static final Consumer<Map<Integer, String>> ADD_KEY_IDS;

    private final Map<Integer, String> keys;
    public MessageSetAbilityKeys(Map<Integer, String> keys)
    {
        this.keys = keys;
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeVarInt(keys.size());
        for (Map.Entry<Integer, String> entry : keys.entrySet())
        {
            buffer.writeVarInt(entry.getKey());
            buffer.writeString(entry.getValue());
        }
    }

    @NetworkDecoder(MessageSetAbilityKeys.class)
    public static MessageSetAbilityKeys decode(PacketBuffer buffer)
    {
        int size = buffer.readVarInt();
        Map<Integer, String> map = new HashMap<>(size);

        for (int i = 0; i < size; i++)
        {
            int id = buffer.readVarInt();
            String desc = buffer.readString();

            map.put(id, desc);
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
        ADD_KEY_IDS.accept(this.keys);
    }

    static
    {
        Consumer<Map<Integer, String>> func = null;
        try
        {
            Method method = AbilityKeybindingBase.class.getDeclaredMethod("addKeybindIds", Map.class);
            func = ASMUtils.createLambdaFromMethod(Consumer.class, method);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        ADD_KEY_IDS = func;
    }
}
