package com.ormoyo.ormoyoutil.abilities;

import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.util.AbilityMessage;
import com.ormoyo.ormoyoutil.ability.util.ClientAbility;
import com.ormoyo.ormoyoutil.capability.AbilityHolder;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@ClientAbility
public class DebugAbility extends Ability
{
    private static final NumberFormat formatter = new DecimalFormat("#0.000");
    private final List<DebugInfo> parameters = new ArrayList<>(4);

    public DebugAbility(AbilityHolder owner)
    {
        super(owner);
        this.addDebugInfoToDisplay(new DebugInfo("Production", () -> Boolean.toString(FMLEnvironment.production), DebugInfo.Visibility.DEV_ENV));
    }

    @SubscribeEvent
    public void onDebugHud(RenderGameOverlayEvent.Text event)
    {
        List<String> left = event.getLeft();
        List<String> right = event.getRight();
        for (DebugInfo info : this.parameters)
        {
            String value = info.value.get();
            List<String> debug = info.side == DebugInfo.Side.LEFT ?
                    left :
                    right;

            if (value == null)
                return;

            switch (info.visibility)
            {
                case DEV_ENV:
                case VISIBLE:
                    debug.add(info.name + ": " + value);
                    break;
                case DEV_DEBUG_MENU:
                case DEBUG_MENU:
                    if (!Minecraft.getInstance().gameSettings.showDebugInfo)
                        break;

                    debug.add(info.name + ": " + value);
                    break;
            }
        }
    }

    public void addDebugInfoToDisplay(DebugInfo info)
    {
        boolean dev = info.visibility == DebugInfo.Visibility.DEV_DEBUG_MENU || info.visibility == DebugInfo.Visibility.DEV_ENV;
        if (dev && FMLEnvironment.production)
            return;

        this.parameters.add(info);
    }

    @Override
    public void onMessageFromAbility(AbilityEntry<?> sender, AbilityMessage message)
    {
        super.onMessageFromAbility(sender, message);
        if ("addDebugInfo".equals(message.getKey()) && message.getValue() instanceof DebugInfo)
            this.addDebugInfoToDisplay((DebugInfo) message.getValue());
    }

    public List<DebugInfo> getDebugInfo()
    {
        return Collections.unmodifiableList(this.parameters);
    }

    public static class DebugInfo
    {
        public final String name;
        public final Supplier<String> value;
        public final Visibility visibility;
        public final Side side;

        public DebugInfo(String name, Supplier<String> value)
        {
            this(name, value, Visibility.DEBUG_MENU);
        }

        public DebugInfo(String name, Supplier<String> value, Visibility visibility)
        {
            this(name, value, visibility, Side.LEFT);
        }

        public DebugInfo(String name, Supplier<String> value, Visibility visibility, Side side)
        {
            this.name = name;
            this.value = value;
            this.visibility = visibility;
            this.side = side;
        }

        public enum Visibility
        {
            VISIBLE,
            DEBUG_MENU,
            DEV_ENV,
            DEV_DEBUG_MENU,
        }

        public enum Side
        {
            LEFT,
            RIGHT,
        }
    }
}
