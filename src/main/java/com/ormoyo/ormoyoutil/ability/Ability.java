package com.ormoyo.ormoyoutil.ability;

import com.google.common.collect.*;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Ints;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.abilities.AbilityStats;
import com.ormoyo.ormoyoutil.capability.AbilityCap;
import com.ormoyo.ormoyoutil.capability.CapabilityHandler;
import com.ormoyo.ormoyoutil.capability.IAbilityCap;
import com.ormoyo.ormoyoutil.client.GuiTest;
import com.ormoyo.ormoyoutil.commands.CommandUnlockAbility;
import com.ormoyo.ormoyoutil.event.AbilityEvents.OnAbilityUnlockedEvent;
import com.ormoyo.ormoyoutil.event.AbilityEvents.StatsEvents.LevelUpEvent;
import com.ormoyo.ormoyoutil.network.datasync.AbilityDataParameter;
import com.ormoyo.ormoyoutil.network.datasync.AbilitySyncManager;
import com.ormoyo.ormoyoutil.util.Utils;
import com.ormoyo.ormoyoutil.util.Utils.IInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IGenericEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

public abstract class Ability
{
    public Ability(EntityPlayer owner)
    {
        this.owner = owner;
        this.entry = Ability.getAbilityClassEntry(this.getClass());
        this.syncManager = new AbilitySyncManager(this);
        this.abilityInit();
    }

    private static final AbilityDataParameter<Boolean> IS_ENABLED = AbilitySyncManager.createKey(Ability.class, DataSerializers.BOOLEAN);
    private final AbilityEntry entry;
    protected AbilitySyncManager syncManager;
    protected final EntityPlayer owner;
    protected boolean isEnabled = true;

    /**
     * Called every tick
     */
    public void onUpdate()
    {
    }

    /**
     * Called for registering data parameters
     */
    public void abilityInit()
    {
        this.syncManager.register(IS_ENABLED, true);
    }

    /**
     * Called when a player unlocks this ability
     */
    public void onUnlock()
    {
    }

    public void writeToNBT(NBTTagCompound compound)
    {
    }

    public void readFromNBT(NBTTagCompound compound)
    {
    }

    public void setIsEnabled(boolean isEnabled)
    {
        this.syncManager.set(IS_ENABLED, isEnabled);
    }

    /**
     * If the ability is disabled all it's methods wouldn't get called
     */
    public boolean isEnabled()
    {
        return this.syncManager.get(IS_ENABLED);
    }

    public void onAbilityEnabled()
    {
    }

    public void onAbilityDisabled()
    {
    }

    public void notifySyncManagerChange(AbilityDataParameter<?> parameter)
    {
    }

    public EntityPlayer getOwner()
    {
        return this.owner;
    }

    public String getName()
    {
        return this.entry.toString();
    }

    public ITextComponent getTranslatedName()
    {
        return new TextComponentTranslation("ability." + this.entry.getRegistryName().getPath() + "." + this.getRegistryName().getPath() + ".name");
    }

    public final ResourceLocation getRegistryName()
    {
        return this.entry.getRegistryName();
    }

    public final AbilityEntry getEntry()
    {
        return this.entry;
    }

    public AbilitySyncManager getSyncManager()
    {
        return this.syncManager;
    }

    protected final void sendMessageToAbility(Ability ability, AbilityMessage message)
    {
        ability.getMessageFromAbility(this, message);
    }

    protected void getMessageFromAbility(Ability sender, AbilityMessage message)
    {
    }

    /**
     * Calls ability method by name on the specified side
     * <p>
     * This method doesn't use reflection
     *
     * @param <T>              The return type of the called method (if there is)
     * @param methodName       The name of the method to be called
     * @param argumentsClasses Names of the method arguments classes. <p> For Example: <pre>Arg1.class.getName() + Arg2.class.getName()</pre>
     * @param args             The arguments for the method to be called
     * @return What the called method returns or null if it doesn't return a value
     */
    @SuppressWarnings("unchecked")
    protected <T> T callMethodByName(String methodName, String argumentsClasses, Object... args)
    {
        IInvoker invoker = Handler.invokers.get(this.getRegistryName(), methodName + argumentsClasses);
        return (T) invoker.invoke(this, args);
    }

    /**
     * Calls ability method by name on the specified side
     * <p>
     * If the specified side is server it will call only the server including the integrated server
     * <p>
     * This method doesn't use reflection
     *
     * @param methodName       The name of the method to be called
     * @param side             The side where the method will be called
     * @param argumentsClasses Names of the method arguments classes. <p> For Example: <pre>Arg1.class.getName() + Arg2.class.getName()</pre>
     * @param args             The arguments for the method to be called
     * @param <T>              The return type of the called method (if there is)
     * @return What the called method returns or null if it doesn't return a value
     */
    @SuppressWarnings("unchecked")
    protected <T> T callMethodByName(String methodName, Side side, String argumentsClasses, Object... args)
    {
        IInvoker invoker = Handler.invokers.get(this.getRegistryName(), methodName + argumentsClasses);
        switch (side)
        {
            case CLIENT:
                return this.owner.world.isRemote ? (T) invoker.invoke(this, args) : null;
            case SERVER:
                return this.owner.world.isRemote ? null : (T) invoker.invoke(this, args);
            default:
                return (T) invoker.invoke(this, args);
        }
    }

    /**
     * If false the ability will not show up on {@link CommandUnlockAbility}
     */
    public boolean isVisable()
    {
        return this.entry.getLevel() > 1;
    }

    @Override
    public String toString()
    {
        return this.getName();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj instanceof Ability)
        {
            Ability ability = (Ability) obj;
            return this.entry.equals(ability.entry);
        }
        return false;
    }

    public static AbilityEntry getAbilityClassEntry(Class<? extends Ability> clazz)
    {
        for (Entry<ResourceLocation, AbilityEntry> entry : Ability.getAbilityRegistry().getEntries())
        {
            if (entry.getValue().getAbilityClass() == clazz)
            {
                return entry.getValue();
            }
        }
        return null;
    }

    private static IForgeRegistry<AbilityEntry> ABILITY_REGISTRY;

    public static IForgeRegistry<AbilityEntry> getAbilityRegistry()
    {
        return ABILITY_REGISTRY;
    }

    private static IForgeRegistry<AbilityEventEntry> ABILITY_EVENT_REGISTRY;

    public static IForgeRegistry<AbilityEventEntry> getAbilityEventRegistry()
    {
        return ABILITY_EVENT_REGISTRY;
    }

    @SuppressWarnings("unchecked")
    @EventBusSubscriber(modid = OrmoyoUtil.MODID)
    private static class Handler
    {
        private static final Table<Boolean, Integer, Class<?>> interfaces = ArrayTable.create(Booleans.asList(false, true), Ints.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
        private static final Table<ResourceLocation, String, IInvoker<?>> invokers = HashBasedTable.create();
        private static final Multimap<ResourceLocation, IAbilityEventListener> listeners = ArrayListMultimap.create();

        @SubscribeEvent
        public static void onEvent(Event event)
        {

            if (OrmoyoUtil.PROXY.isServerSide())
            {
                PlayerList list = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList();
                if (list == null)
                {
                    return;
                }
                for (EntityPlayerMP player : list.getPlayers())
                {
                    for (Ability ability : OrmoyoUtil.PROXY.getAbilities(player))
                    {
                        if (!ability.isEnabled())
                        {
                            continue;
                        }
                        Collection<IAbilityEventListener> listeners = Handler.listeners.get(ability.getRegistryName());
                        for (IAbilityEventListener listener : listeners)
                        {
                            if (listener.getEventClass() != event.getClass() || !listener.getEventPredicate().test(ability, event))
                            {
                                continue;
                            }
                            listener.invoke(ability, event);
                        }
                    }

                    if (OrmoyoUtil.PROXY.getAbilities(player).isEmpty())
                    {
                        continue;
                    }
                    for (AbilityEntry entry : Ability.getAbilityRegistry().getValuesCollection())
                    {
                        for (Class<? extends Event> clazz : entry.getConditionCheckingEvents())
                        {
                            if (clazz == event.getClass() && entry.getCondition().test(player))
                            {
                                OrmoyoUtil.PROXY.unlockAbility(entry.newInstance(player));
                            }
                        }
                    }
                }
            }
            else
            {
                Set<Ability> abilities = OrmoyoUtil.PROXY.getAbilities(null);
                if (abilities.isEmpty())
                {
                    return;
                }
                for (Ability ability : abilities)
                {
                    if (!ability.isEnabled())
                    {
                        continue;
                    }
                    Collection<IAbilityEventListener> listeners = Handler.listeners.get(ability.getRegistryName());
                    for (IAbilityEventListener listener : listeners)
                    {
                        if (listener.getEventClass() != event.getClass() || !listener.getEventPredicate().test(ability, event))
                        {
                            continue;
                        }
                        listener.invoke(ability, event);
                    }
                }

                for (AbilityEntry entry : Ability.getAbilityRegistry().getValuesCollection())
                {
                    for (Class<? extends Event> clazz : entry.getConditionCheckingEvents())
                    {
                        if (clazz == event.getClass() && entry.getCondition().test(OrmoyoUtil.PROXY.getClientPlayer()))
                        {
                            OrmoyoUtil.PROXY.unlockAbility(entry.newInstance(OrmoyoUtil.PROXY.getClientPlayer()));
                        }
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onAbilityUnlock(OnAbilityUnlockedEvent event)
        {
            event.getAbility().onUnlock();
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onLevelUp(LevelUpEvent event)
        {
            for (AbilityEntry entry : Ability.getAbilityRegistry())
            {
                if (OrmoyoUtil.PROXY.isAbilityUnlocked(entry.getRegistryName(), event.getEntityPlayer()))
                {
                    continue;
                }
                if (event.getLevel() == entry.getLevel())
                {
                    OrmoyoUtil.PROXY.unlockAbility(entry.newInstance(event.getEntityPlayer()));
                }
                else if (entry.getCondition() != null)
                {
                    if (entry.getCondition().test(event.getEntityPlayer()))
                    {
                        OrmoyoUtil.PROXY.unlockAbility(entry.newInstance(event.getEntityPlayer()));
                    }
                }
            }
        }

        @SuppressWarnings("unused")
        public static void onInit()
        {
            try
            {
                for (AbilityEntry entry : Ability.getAbilityRegistry())
                {
                    // || Constructor ||
                    Constructor<? extends Ability> constructor = entry.getAbilityClass().getConstructor(EntityPlayer.class);
                    entry.abilityConstructor = Utils.createLambdaFromConstructor(Function.class, constructor);

                    // || Methods ||
                    Collection<Method> methods = getAllMethods(entry.getAbilityClass());
                    Collection<Method> eventMethods = Stream.of(entry.getAbilityClass().getMethods()).filter(method -> method.isAnnotationPresent(SubscribeEvent.class)).collect(Collectors.toSet());
                    for (Method method : methods)
                    {
                        boolean isStatic = Modifier.isStatic(method.getModifiers());
                        boolean returnType = method.getReturnType() != void.class;
                        int paramCount = method.getParameterCount() + (isStatic ? 0 : 1);
                        Class<?> interfaceClass = interfaces.get(returnType, paramCount);
                        Object obj = Utils.createLambdaFromMethod(interfaceClass, method);
                        Method interfaceMethod = interfaceClass.getMethods()[0];
                        Class<?> invokerClass = createWrapper(interfaceMethod, isStatic);
                        Constructor<?> con = invokerClass.getConstructor(Object.class, int.class);
                        Utils.IInvoker invoker = (Utils.IInvoker) con.newInstance(obj, paramCount - (isStatic ? 0 : 1));
                        Handler.invokers.put(entry.getRegistryName(), method.getName() + toString(method.getParameterTypes()), invoker);
                    }
                    for (Method method : eventMethods)
                    {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length != 1)
                        {
                            throw new IllegalArgumentException(
                                    "Ability method " + method + " has @SubscribeEvent annotation, but requires " + parameterTypes.length +
                                            " arguments.  Event handler methods must require a single argument."
                            );
                        }
                        Class<?> eventT = parameterTypes[0];
                        if (!Event.class.isAssignableFrom(eventT))
                        {
                            throw new IllegalArgumentException("Ability method " + method + " has @SubscribeEvent annotation, but takes a argument that is not an Event " + eventT);
                        }
                        Class<? extends Event> eventType = (Class<? extends Event>) eventT;
                        for (AbilityEventEntry eventEntry : Ability.getAbilityEventRegistry().getValuesCollection())
                        {
                            if (eventType.isAssignableFrom(eventEntry.getEventClass()) || eventEntry.getEventClass().isAssignableFrom(eventType))
                            {
                                try
                                {
                                    listeners.entries().removeIf(cell -> cell.getValue().getEventClass().isAssignableFrom(eventType) && cell.getValue().getMethod().equals(method));
                                    listeners.put(entry.getRegistryName(), new AbilityEventListener(entry, method, Loader.instance().activeModContainer(), eventEntry.getEventPredicate(), eventType, IGenericEvent.class.isAssignableFrom(eventType)));
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }

        @SubscribeEvent
        public static void onA(BlockEvent.BreakEvent event)
        {
            if (!OrmoyoUtil.PROXY.isServerSide())
                return;

            Minecraft.getMinecraft().displayGuiScreen(new GuiTest());
        }

        @SubscribeEvent
        public static void onNewRegistry(RegistryEvent.NewRegistry event)
        {
            ABILITY_REGISTRY = new RegistryBuilder<AbilityEntry>().setName(new ResourceLocation(OrmoyoUtil.MODID, "ability")).setType(AbilityEntry.class).setIDRange(0, 2048).create();
            ABILITY_EVENT_REGISTRY = new RegistryBuilder<AbilityEventEntry>().setName(new ResourceLocation(OrmoyoUtil.MODID, "ability_event")).setType(AbilityEventEntry.class).setIDRange(0, 2048).create();
        }

        @SubscribeEvent
        public static void registerAbilities(RegistryEvent.Register<AbilityEntry> event)
        {
            event.getRegistry().register(new AbilityEntry(new ResourceLocation(OrmoyoUtil.MODID, "stats"), AbilityStats.class));
        }

        @SubscribeEvent
        public static void registerAbilityEventPredicates(RegistryEvent.Register<AbilityEventEntry> event)
        {
            register(event, EntityEvent.class, EventPredicates.ENTITY_EVENT);
            register(event, PlayerEvent.class, EventPredicates.PLAYER_EVENT);
            register(event, LivingAttackEvent.class, EventPredicates.LIVING_ATTACK_EVENT);
            register(event, LivingDeathEvent.class, EventPredicates.LIVING_DEATH_EVENT);
            register(event, LivingKnockBackEvent.class, EventPredicates.LIVING_KNOCKBACK_EVENT);
            register(event, ProjectileImpactEvent.class, EventPredicates.PROJECTILE_IMPACT_EVENT);
            register(event, ProjectileImpactEvent.Arrow.class, EventPredicates.PROJECTILE_IMPACT_ARROW_EVENT);
            register(event, ProjectileImpactEvent.Fireball.class, EventPredicates.PROJECTILE_IMPACT_FIREBALL_EVENT);
            register(event, ProjectileImpactEvent.Throwable.class, EventPredicates.PROJECTILE_IMPACT_THROWABLE_EVENT);
            register(event, ClientTickEvent.class, EventPredicates.CLIENT_TICK_EVENT);
            register(event, RenderTickEvent.class, EventPredicates.RENDER_TICK_EVENT);
            register(event, RenderGameOverlayEvent.class, EventPredicates.RENDER_GAME_OVERLAY_EVENT);
            register(event, RenderLivingEvent.class, EventPredicates.RENDER_LIVING_EVENT);
            register(event, InputUpdateEvent.class, EventPredicates.INPUT_UPDATE_EVENT);
            register(event, CameraSetup.class, EventPredicates.CAMERA_SETUP_EVENT);
            register(event, MouseEvent.class, EventPredicates.MOUSE_EVENT);
            register(event, MouseInputEvent.class, EventPredicates.MOUSE_INPUT_EVENT);
            register(event, KeyInputEvent.class, EventPredicates.KEY_INPUT_EVENT);
            register(event, RenderSpecificHandEvent.class, EventPredicates.RENDER_SPECIFIC_HAND_EVENT);
            register(event, ClientDisconnectionFromServerEvent.class, EventPredicates.CLIENT_DISCONNECTION_FROM_SERVER_EVENT);
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void registerAbilityEventPredicatesOnClient(RegistryEvent.Register<AbilityEventEntry> event)
        {
            register(event, GuiScreenEvent.class, EventPredicates.GUI_SCREEN_EVENT);
        }

        private static <T extends Event> void register(RegistryEvent.Register<AbilityEventEntry> event, Class<T> clazz, IAbilityEventPredicate<T> predicate)
        {
            StringBuilder name = new StringBuilder();
            char[] chars = clazz.getSimpleName().toCharArray();

            for (int i = 0; i < chars.length; i++)
            {
                if (i == 0)
                {
                    name.append(Character.toLowerCase(chars[i]));
                    continue;
                }

                char c = chars[i];
                if (Character.isUpperCase(c))
                {
                    name.append("_").append(Character.toLowerCase(c));
                }
            }
            event.getRegistry().register(new AbilityEventEntry(new ResourceLocation(OrmoyoUtil.MODID, name.toString()), clazz, predicate));
        }

        public static class EventPredicates
        {
            public static final IAbilityEventPredicate<EntityEvent>
                    ENTITY_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getEntity());

            public static final IAbilityEventPredicate<LivingAttackEvent>
                    LIVING_ATTACK_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getEntityLiving()) ||
                                    ability.owner.equals(event.getSource().getTrueSource());

            public static final IAbilityEventPredicate<LivingDeathEvent>
                    LIVING_DEATH_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getEntityLiving()) ||
                                    ability.owner.equals(event.getSource().getTrueSource());

            public static final IAbilityEventPredicate<LivingKnockBackEvent>
                    LIVING_KNOCKBACK_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getEntityLiving()) ||
                                    ability.owner.equals(event.getAttacker());

            public static final IAbilityEventPredicate<ProjectileImpactEvent>
                    PROJECTILE_IMPACT_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getRayTraceResult().entityHit);

            public static final IAbilityEventPredicate<ProjectileImpactEvent.Arrow>
                    PROJECTILE_IMPACT_ARROW_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getRayTraceResult().entityHit) ||
                                    ability.owner.equals(event.getArrow().shootingEntity);

            public static final IAbilityEventPredicate<ProjectileImpactEvent.Fireball>
                    PROJECTILE_IMPACT_FIREBALL_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getRayTraceResult().entityHit) ||
                                    ability.owner.equals(event.getFireball().shootingEntity);

            public static final IAbilityEventPredicate<ProjectileImpactEvent.Throwable>
                    PROJECTILE_IMPACT_THROWABLE_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.getRayTraceResult().entityHit) || ability.owner.equals(event.getThrowable().getThrower());

            public static final IAbilityEventPredicate<PlayerEvent>
                    PLAYER_EVENT =
                    (ability, event) ->
                            ability.owner.equals(event.player);

            public static final IAbilityEventPredicate<ClientTickEvent>
                    CLIENT_TICK_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<RenderTickEvent>
                    RENDER_TICK_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<RenderGameOverlayEvent>
                    RENDER_GAME_OVERLAY_EVENT =
                    (ability, event) ->
                            true;

            @SuppressWarnings("rawtypes")
            public static final IAbilityEventPredicate<RenderLivingEvent>
                    RENDER_LIVING_EVENT =
                    (ability, event) ->
                            !ability.owner.equals(event.getEntity());

            public static final IAbilityEventPredicate<GuiScreenEvent>
                    GUI_SCREEN_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<InputUpdateEvent>
                    INPUT_UPDATE_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<CameraSetup>
                    CAMERA_SETUP_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<MouseEvent>
                    MOUSE_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<MouseInputEvent>
                    MOUSE_INPUT_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<KeyInputEvent>
                    KEY_INPUT_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<RenderSpecificHandEvent>
                    RENDER_SPECIFIC_HAND_EVENT =
                    (ability, event) ->
                            true;

            public static final IAbilityEventPredicate<ClientDisconnectionFromServerEvent>
                    CLIENT_DISCONNECTION_FROM_SERVER_EVENT =
                    (ability, event) ->
                            true;
        }

        @SubscribeEvent
        public static void onPlayerTickEvent(PlayerTickEvent event)
        {
            if (event.phase == Phase.END)
            {
                for (Ability ability : OrmoyoUtil.PROXY.getAbilities(event.player))
                {
                    if (ability.isEnabled())
                    {
                        ability.onUpdate();
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event)
        {
            if (!event.isWasDeath())
            {
                return;
            }

            IAbilityCap o = event.getOriginal().getCapability(CapabilityHandler.CAPABILITY_ABILITY_DATA, null);
            IAbilityCap n = event.getEntityPlayer().getCapability(CapabilityHandler.CAPABILITY_ABILITY_DATA, null);

            if (n instanceof AbilityCap && o instanceof AbilityCap)
            {
                AbilityCap oldcap = (AbilityCap) o;
                AbilityCap newcap = (AbilityCap) n;

                BiMap<ResourceLocation, Ability> oldAbilities = ObfuscationReflectionHelper.getPrivateValue(AbilityCap.class, oldcap, "unlockedAbilities");

                ObfuscationReflectionHelper.setPrivateValue(AbilityCap.class, newcap, oldcap.getPlayer(), "player");
                ObfuscationReflectionHelper.setPrivateValue(AbilityCap.class, newcap, oldAbilities, "unlockedAbilities");
            }
        }

        private static final AbilityASMClassLoader LOADER = new AbilityASMClassLoader();
        private static final Map<Method, Class<?>> cache = Maps.newHashMap();
        private static int IDs;

        private static Class<?> createWrapper(Method callback, boolean isStatic)
        {
            if (cache.containsKey(callback))
            {
                return cache.get(callback);
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            GeneratorAdapter mv;

            boolean isMethodStatic = Modifier.isStatic(callback.getModifiers());
            boolean isInterface = callback.getDeclaringClass().isInterface();
            String name = String.format("%s_%d_%s", callback.getDeclaringClass().getSimpleName(),
                    IDs++,
                    callback.getName());
            String desc = name.replace('.', '/');
            String instType = Type.getInternalName(callback.getDeclaringClass());
            Type owner = Type.getObjectType(desc);
            Type objectType = Type.getType(Object.class);

            cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, objectType.getInternalName(), new String[]{Type.getInternalName(Utils.IInvoker.class)});

            cw.visitSource(".dynamic", null);
            {
                if (!isMethodStatic)
                {
                    cw.visitField(ACC_PUBLIC | ACC_FINAL, "instance", objectType.getDescriptor(), null, null).visitEnd();
                }
                cw.visitField(ACC_PUBLIC | ACC_FINAL, "paramCount", "I", null, null).visitEnd();
            }
            {
                String d = Type.getMethodDescriptor(Type.VOID_TYPE, objectType, Type.INT_TYPE);
                mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", d, null, null), ACC_PUBLIC, "<init>", d);
                mv.loadThis();
                mv.invokeConstructor(objectType, new org.objectweb.asm.commons.Method("<init>", "()V"));
                if (!isMethodStatic)
                {
                    mv.loadThis();
                    mv.loadArg(0);
                    mv.putField(owner, "instance", objectType);
                }
                mv.loadThis();
                mv.loadArg(1);
                mv.putField(owner, "paramCount", Type.INT_TYPE);
                mv.returnValue();
                mv.endMethod();
            }
            {
                Label exception = new Label();
                Label end = new Label();
                String d = Type.getMethodDescriptor(objectType, objectType, Type.getType(Object[].class));
                mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invoke", d, null, null), ACC_PUBLIC | ACC_VARARGS, "invoke", d);
                mv.loadArg(1);
                mv.arrayLength();
                mv.loadThis();
                mv.getField(owner, "paramCount", Type.INT_TYPE);
                mv.ifICmp(IFNE, exception);
                if (!isMethodStatic)
                {
                    mv.loadThis();
                    mv.getField(owner, "instance", objectType);
                    mv.checkCast(Type.getObjectType(instType));
                    if (!isStatic)
                    {
                        mv.loadArg(0);
                    }
                }
                Class<?>[] params = callback.getParameterTypes();
                for (int i = 0; i < params.length - (isStatic ? 0 : 1); i++)
                {
                    Class<?> parameter = params[i];
                    Type parameterType = Type.getType(parameter);
                    if (parameter.isPrimitive())
                    {
                        if (parameter == boolean.class)
                        {
                            mv.loadArg(1);
                            mv.push(i);
                            mv.arrayLoad(objectType);
                            mv.checkCast(Type.getType(Boolean.class));
                            mv.invokeVirtual(Type.getType(Boolean.class), new org.objectweb.asm.commons.Method("booleanValue", "()Z"));
                        }
                        else if (parameter == char.class)
                        {
                            mv.loadArg(1);
                            mv.push(i);
                            mv.arrayLoad(objectType);
                            mv.checkCast(Type.getType(Character.class));
                            mv.invokeVirtual(Type.getType(Character.class), new org.objectweb.asm.commons.Method("charValue", "()C"));
                        }
                        else
                        {
                            mv.loadArg(1);
                            mv.push(i);
                            mv.arrayLoad(objectType);
                            mv.checkCast(Type.getType(Number.class));
                            mv.invokeVirtual(Type.getType(Number.class), new org.objectweb.asm.commons.Method(parameter.getName() + "Value", Type.getMethodDescriptor(parameterType)));
                        }
                    }
                    else
                    {
                        mv.loadArg(1);
                        mv.push(i);
                        mv.arrayLoad(objectType);
                        mv.checkCast(parameterType);
                    }
                }

                mv.visitMethodInsn(isStatic && isMethodStatic ? INVOKESTATIC : isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL, instType, callback.getName(), Type.getMethodDescriptor(callback), isInterface);

                if (callback.getReturnType() == void.class)
                {
                    mv.visitInsn(ACONST_NULL);
                }
                mv.goTo(end);

                mv.visitLabel(exception);
                mv.newInstance(Type.getType(IllegalArgumentException.class));
                mv.dup();
                mv.push("Given arguments with length %s don't match method arguments with length %s");
                mv.push(2);
                mv.newArray(objectType);
                mv.dup();
                mv.push(0);
                mv.loadArg(1);
                mv.arrayLength();
                mv.invokeStatic(Type.getType(Integer.class), new org.objectweb.asm.commons.Method("valueOf", Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE)));
                mv.arrayStore(objectType);
                mv.dup();
                mv.push(1);
                mv.loadThis();
                mv.getField(owner, "paramCount", Type.INT_TYPE);
                mv.invokeStatic(Type.getType(Integer.class), new org.objectweb.asm.commons.Method("valueOf", Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE)));
                mv.arrayStore(objectType);
                mv.invokeStatic(Type.getType(String.class), new org.objectweb.asm.commons.Method("format", Type.getMethodDescriptor(Type.getType(String.class), Type.getType(String.class), Type.getType(Object[].class))));
                mv.invokeConstructor(Type.getType(IllegalArgumentException.class), new org.objectweb.asm.commons.Method("<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class))));
                mv.throwException();

                mv.visitLabel(end);
                mv.returnValue();
                mv.endMethod();
            }
            cw.visitEnd();
            Class<?> clazz = LOADER.define(name, cw.toByteArray());
            cache.put(callback, clazz);
            return clazz;
        }

        private static String toString(Class<?>[] a)
        {
            if (a == null)
            {
                return "null";
            }
            int iMax = a.length - 1;
            if (iMax == -1)
            {
                return "";
            }

            StringBuilder b = new StringBuilder();
            for (int i = 0; ; i++)
            {
                b.append(a[i]);
                if (i == iMax)
                {
                    return b.toString();
                }
            }
        }

        static
        {
            Class<?>[] interfaceArr = FunctionalInterfaces.class.getClasses();
            Arrays.sort(interfaceArr, (a, b) ->
            {
                int i = Integer.compare(Integer.parseInt(a.getSimpleName().replaceAll("\\D+", "")), Integer.parseInt(b.getSimpleName().replaceAll("\\D+", "")));
                if (a.getSimpleName().charAt(0) == 'V' && i == 0)
                {
                    return -1;
                }
                return i;
            });
            for (int i = 0; i < 21; i++)
            {
                for (int in = 0; in < 2; in++)
                {
                    Class<?> interfaceClass = interfaceArr[i * 2 + in];
                    interfaces.put(in != 0, i, interfaceClass);
                }
            }
        }

        public static Collection<Method> getAllMethods(Class<?> clazz)
        {
            Predicate<Method> include = m -> !m.isBridge() && !m.isSynthetic() && m.getDeclaringClass() != Object.class &&
                    Character.isJavaIdentifierStart(m.getName().charAt(0))
                    && m.getName().chars().skip(1).allMatch(Character::isJavaIdentifierPart);

            Set<Method> methods = Sets.newLinkedHashSet();
            Collections.addAll(methods, clazz.getMethods());
            methods.removeIf(include.negate());
            Stream.of(clazz.getDeclaredMethods()).filter(include).forEach(methods::add);

            final int access = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

            Package p = clazz.getPackage();
            int pass = Modifier.PROTECTED;
            include = include.and(m ->
            {
                int mod = m.getModifiers();
                return (mod & pass) != 0
                        || (mod & access) == 0 && m.getDeclaringClass().getPackage() == p;
            });
            for (clazz = clazz.getSuperclass(); clazz != null; clazz = clazz.getSuperclass())
            {
                Stream.of(clazz.getDeclaredMethods()).filter(include).forEach(methods::add);
            }
            return methods;
        }

        private static class AbilityASMClassLoader extends ClassLoader
        {
            private AbilityASMClassLoader()
            {
                super(AbilityASMClassLoader.class.getClassLoader());
            }

            public Class<?> define(String name, byte[] data)
            {
                return defineClass(name, data, 0, data.length);
            }
        }

        private interface FunctionalInterfaces
        {
            @FunctionalInterface
            interface V0Param
            {
                void accept();
            }

            @FunctionalInterface
            interface R0Param
            {
                Object apply();
            }

            @FunctionalInterface
            interface V1Param
            {
                void accept(Object paramObject);
            }

            @FunctionalInterface
            interface R1Param
            {
                Object apply(Object paramObject);
            }

            @FunctionalInterface
            interface V2Param
            {
                void accept(Object paramObject1, Object paramObject2);
            }

            @FunctionalInterface
            interface R2Param
            {
                Object apply(Object paramObject1, Object paramObject2);
            }

            @FunctionalInterface
            interface V3Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3);
            }

            @FunctionalInterface
            interface R3Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3);
            }

            @FunctionalInterface
            interface V4Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4);
            }

            @FunctionalInterface
            interface R4Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4);
            }

            @FunctionalInterface
            interface V5Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5);
            }

            @FunctionalInterface
            interface R5Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5);
            }

            @FunctionalInterface
            interface V6Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6);
            }

            @FunctionalInterface
            interface R6Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6);
            }

            @FunctionalInterface
            interface V7Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7);
            }

            @FunctionalInterface
            interface R7Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7);
            }

            @FunctionalInterface
            interface V8Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8);
            }

            @FunctionalInterface
            interface R8Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8);
            }

            @FunctionalInterface
            interface V9Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9);
            }

            @FunctionalInterface
            interface R9Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9);
            }

            @FunctionalInterface
            interface V10Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10);
            }

            @FunctionalInterface
            interface R10Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10);
            }

            @FunctionalInterface
            interface V11Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11);
            }

            @FunctionalInterface
            interface R11Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11);
            }

            @FunctionalInterface
            interface V12Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12);
            }

            @FunctionalInterface
            interface R12Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12);
            }

            @FunctionalInterface
            interface V13Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13);
            }

            @FunctionalInterface
            interface R13Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13);
            }

            @FunctionalInterface
            interface V14Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14);
            }

            @FunctionalInterface
            interface R14Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14);
            }

            @FunctionalInterface
            interface V15Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15);
            }

            @FunctionalInterface
            interface R15Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15);
            }

            @FunctionalInterface
            interface V16Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16);
            }

            @FunctionalInterface
            interface R16Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16);
            }

            @FunctionalInterface
            interface V17Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16, Object paramObject17);
            }

            @FunctionalInterface
            interface R17Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16, Object paramObject17);
            }

            @FunctionalInterface
            interface V18Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16, Object paramObject17, Object paramObject18);
            }

            @FunctionalInterface
            interface R18Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16, Object paramObject17, Object paramObject18);
            }

            @FunctionalInterface
            interface V19Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16, Object paramObject17, Object paramObject18, Object paramObject19);
            }

            @FunctionalInterface
            interface R19Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16, Object paramObject17, Object paramObject18, Object paramObject19);
            }

            @FunctionalInterface
            interface V20Param
            {
                void accept(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16, Object paramObject17, Object paramObject18, Object paramObject19, Object paramObject20);
            }

            @FunctionalInterface
            interface R20Param
            {
                Object apply(Object paramObject1, Object paramObject2, Object paramObject3, Object paramObject4, Object paramObject5, Object paramObject6, Object paramObject7, Object paramObject8, Object paramObject9, Object paramObject10, Object paramObject11, Object paramObject12, Object paramObject13, Object paramObject14, Object paramObject15, Object paramObject16, Object paramObject17, Object paramObject18, Object paramObject19, Object paramObject20);
            }
        }
    }
}
