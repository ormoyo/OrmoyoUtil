package com.ormoyo.ormoyoutil.network;

import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
import com.ormoyo.ormoyoutil.util.ASMUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@NetworkMessage(modid = OrmoyoUtil.MODID, direction = NetworkDirection.PLAY_TO_CLIENT)
public class MessageSetAbilities extends AbstractMessage<MessageSetAbilities>
{
    private static final BiConsumer<PlayerEntity, Collection<Ability>> SET_ABILITIES;
    private final Collection<AbilityEntry> entries;

    public MessageSetAbilities(Collection<AbilityEntry> entries)
    {
        this.entries = entries;
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeVarInt(this.entries.size());
        for (AbilityEntry entry : this.entries)
        {
            buffer.writeRegistryId(entry);
        }
    }

    @NetworkDecoder(MessageSetAbilities.class)
    public static MessageSetAbilities decode(PacketBuffer buffer)
    {
        int capacity = buffer.readVarInt();
        Collection<AbilityEntry> entries = new ArrayList<>(capacity);

        for (int i = 0; i < capacity; i++)
            entries.add(buffer.readRegistryId());

        return new MessageSetAbilities(entries);
    }

    @Override
    public void onClientReceived(PlayerEntity player, NetworkEvent.Context messageContext)
    {


        Collection<Ability> abilities = this.entries.stream().map(entry -> entry.newInstance((IAbilityHolder) player)).collect(Collectors.toList());
        SET_ABILITIES.accept(player, abilities);
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayerEntity player, NetworkEvent.Context messageContext)
    {

    }


    static
    {
        BiConsumer<PlayerEntity, Collection<Ability>> func = null;
        try
        {
            Method method = PlayerEntity.class.getDeclaredMethod("setPlayerAbilities", Collection.class);
            func = ASMUtils.createLambdaFromMethod(BiConsumer.class, method);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        SET_ABILITIES = func;
    }
}
