package com.ormoyo.ormoyoutil.proxy;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ormoyo.ormoyoutil.abilities.AbilityKeybindingBase;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.client.InjectRender;
import com.ormoyo.ormoyoutil.client.render.RenderHelper;
import com.ormoyo.ormoyoutil.event.AbilityEvents.OnAbilityUnlockedEvent;
import com.ormoyo.ormoyoutil.network.AbstractMessage;
import com.ormoyo.ormoyoutil.util.OrmoyoResourcePackListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    public static final Minecraft MINECRAFT = Minecraft.getMinecraft();
    private final BiMap<ResourceLocation, Ability> unlockedAbilities = HashBiMap.create();
    private AbilitySet abilitySet;

    @Override
    public <T extends AbstractMessage<T>> void handleMessage(final T message, final MessageContext messageContext)
    {
        if (messageContext.side.isServer())
        {
            super.handleMessage(message, messageContext);
        }
        else
        {
            MINECRAFT.addScheduledTask(() -> message.onClientReceived(MINECRAFT, MINECRAFT.player, messageContext));
        }
    }

    @Override
    public void preInit(FMLPreInitializationEvent event)
    {
        super.preInit(event);

        InjectRender.Handler.injectRender(event.getAsmData());
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

    @Override
    public void init(FMLInitializationEvent event)
    {
        super.init(event);
        this.registerKeybinds();

        try
        {
            Class.forName(RenderHelper.class.getName());
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }

        OrmoyoResourcePackListener listener = new OrmoyoResourcePackListener();
        if (MINECRAFT.getResourceManager() instanceof IReloadableResourceManager)
        {
            IReloadableResourceManager manager = (IReloadableResourceManager) MINECRAFT.getResourceManager();
            manager.registerReloadListener(listener);
        }
    }

    public void registerKeybinds()
    {
        for (AbilityEntry entry : Ability.getAbilityRegistry().getValuesCollection())
        {
            Ability ability = entry.newInstance(MINECRAFT.player);
            if (ability instanceof AbilityKeybindingBase)
            {
                AbilityKeybindingBase keybindBase = (AbilityKeybindingBase) ability;

                if (keybindBase.getKeybindCode() <= 0)
                {
                    return;
                }

                ResourceLocation location = ability.getRegistryName();
                ClientRegistry.registerKeyBinding(new KeyBinding(
                        "key." +
                                location.getNamespace() +
                                "." + location.getPath(),
                        Math.min(keybindBase.getKeybindCode(), Keyboard.KEYBOARD_SIZE),
                        "key." +
                                location.getNamespace() +
                                ".category"));
            }
        }
    }

    @Override
    public boolean isServerSide()
    {
        return MINECRAFT.isSingleplayer() && MINECRAFT.getIntegratedServer().isCallingFromMinecraftThread();
    }

    @Override
    protected boolean unlockAbility(Ability ability, boolean readFromNBT)
    {
        if (!ability.getOwner().getEntityWorld().isRemote)
        {
            return super.unlockAbility(ability, readFromNBT);
        }
        if (!readFromNBT && MinecraftForge.EVENT_BUS.post(new OnAbilityUnlockedEvent(ability)))
        {
            return false;
        }

        return !this.isAbilityUnlocked(ability.getRegistryName(), null) && this.unlockedAbilities.putIfAbsent(ability.getRegistryName(), ability) == null;
    }

    @Override
    public boolean isAbilityUnlocked(ResourceLocation name, EntityPlayer player)
    {
        return this.unlockedAbilities.containsKey(name);
    }

    @Override
    public boolean isAbilityUnlocked(Class<? extends Ability> clazz, EntityPlayer player)
    {
        for (Ability ability : this.unlockedAbilities.values())
        {
            if (ability.getClass() == clazz)
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<Ability> getAbilities(EntityPlayer player)
    {
        if (player != null && !player.world.isRemote)
        {
            return super.getAbilities(player);
        }

        Set<Ability> abilitySet;
        return (abilitySet = this.abilitySet) == null ? (this.abilitySet = new AbilitySet()) : abilitySet;
    }

    @Override
    public Ability getAbility(ResourceLocation name, EntityPlayer player)
    {
        if (player != null && !player.world.isRemote)
        {
            return super.getAbility(name, player);
        }

        return this.unlockedAbilities.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Ability> T getAbility(Class<T> clazz, EntityPlayer player)
    {
        if (player != null && !player.world.isRemote)
        {
            return super.getAbility(clazz, player);
        }

        AbilityEntry entry = Ability.getAbilityClassEntry(clazz);
        for (Ability ability : this.unlockedAbilities.values())
        {
            if (ability.getEntry().equals(entry))
            {
                return (T) ability;
            }
        }
        return null;
    }

    @Override
    public EntityPlayer getPlayerByUsername(String username)
    {
        if (this.isServerSide())
        {
            return super.getPlayerByUsername(username);
        }

        String name = MINECRAFT.player.getGameProfile().getName();
        return name.equals(username) ? MINECRAFT.player : MINECRAFT.world != null ? MINECRAFT.world.getPlayerEntityByName(username) : null;
    }

    @Override
    public EntityPlayer getPlayerByID(int id)
    {
        if (this.isServerSide())
        {
            return super.getPlayerByID(id);
        }

        return MINECRAFT.player.getEntityId() == id ? MINECRAFT.player : MINECRAFT.world != null ? (EntityPlayer) Minecraft.getMinecraft().world.getEntityByID(id) : null;
    }

    @Override
    public void openGui(Object gui)
    {
        if (gui == null || gui instanceof GuiScreen)
        {
            MINECRAFT.displayGuiScreen((GuiScreen) gui);
        }
    }

    @Override
    public EntityPlayer getClientPlayer()
    {
        return MINECRAFT.player;
    }

    private class EventHandler
    {
        @SubscribeEvent
        public void onDisconectingFromServer(ClientDisconnectionFromServerEvent event)
        {
            MINECRAFT.addScheduledTask(() ->
            {
                ClientProxy.this.unlockedAbilities.clear();
            });
        }
    }

    private class AbilitySet extends AbstractSet<Ability>
    {
        @Override
        public Iterator<Ability> iterator()
        {
            return new Iter();
        }

        @Override
        public int size()
        {
            return ClientProxy.this.unlockedAbilities.size();
        }

        class Iter implements Iterator<Ability>
        {
            final Iterator<Ability> iterator;

            public Iter()
            {
                this.iterator = ClientProxy.this.unlockedAbilities.values().iterator();
            }

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext();
            }

            @Override
            public Ability next()
            {
                return this.iterator.next();
            }

            @Override
            public void remove()
            {
                this.iterator.remove();
            }
        }
    }
}
