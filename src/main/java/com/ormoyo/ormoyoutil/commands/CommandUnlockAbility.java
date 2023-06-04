package com.ormoyo.ormoyoutil.commands;

import com.google.common.collect.Lists;
import com.ormoyo.ormoyoutil.OrmoyoUtil;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.capability.CapabilityHandler;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Collections;
import java.util.List;

public class CommandUnlockAbility extends CommandBase
{
    @Override
    public String getName()
    {
        return "unlockability";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/unlockability <Ability> [Player]";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    private final List<ResourceLocation> currentAbilities = Lists.newArrayList();

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
    {
        if (args.length == 1)
        {
            for (AbilityEntry entry : Ability.getAbilityRegistry().getValuesCollection())
            {
                try
                {
					if (this.currentAbilities.contains(entry.getRegistryName()))
					{
						continue;
					}

                    Ability ability = entry.newInstance(CommandBase.getCommandSenderAsPlayer(sender));
                    if (ability.isVisable())
                    {
                        this.currentAbilities.add(entry.getRegistryName());
                    }
                }
                catch (PlayerNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
            return CommandBase.getListOfStringsMatchingLastWord(args, this.currentAbilities);
        }
        if (args.length == 2)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    private static final String LANG_PREFIX = "command." + OrmoyoUtil.MODID + ".";

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length >= 1)
        {
            Ability ability = this.getAbilityByName(server, sender, args[0]);
            if (ability != null && ability.isVisable())
            {
                TextComponentString abilityTranslation = new TextComponentString(ability.getTranslatedName().getFormattedText().toLowerCase());
                if (args.length == 2)
                {
                    EntityPlayer player = CommandBase.getPlayer(server, sender, args[1]);
                    if (player.getCapability(CapabilityHandler.CAPABILITY_ABILITY_DATA, null).isAbilityUnlocked(ability))
                    {
                        sender.getCommandSenderEntity().sendMessage(new TextComponentTranslation(LANG_PREFIX + "already_unlocked"));
                        return;
                    }
                    if (OrmoyoUtil.PROXY.unlockAbility(ability))
                    {
                        sender.getCommandSenderEntity().sendMessage(new TextComponentString(String.format("The player %1$s unlocked the ability %2$s", player.getName(), abilityTranslation.getFormattedText())));
                        player.sendMessage(new TextComponentString(String.format("You unlocked the ablity %s", abilityTranslation.getFormattedText())));
                    }
                }
                else
                {
                    if (OrmoyoUtil.PROXY.isAbilityUnlocked(ability.getRegistryName(), ability.getOwner()))
                    {
                        sender.getCommandSenderEntity().sendMessage(new TextComponentTranslation(LANG_PREFIX + "already_unlocked"));
                        return;
                    }
                    if (OrmoyoUtil.PROXY.unlockAbility(ability))
                    {
                        sender.getCommandSenderEntity().sendMessage(new TextComponentString(String.format("You unlocked the ablity %s", abilityTranslation.getFormattedText())));
                    }
                }
            }
            else
            {
                sender.getCommandSenderEntity().sendMessage(new TextComponentTranslation(LANG_PREFIX + "ability_fail"));
            }
        }
        else
        {
            throw new WrongUsageException(this.getUsage(sender));
        }
    }

    private Ability getAbilityByName(MinecraftServer server, ICommandSender sender, String argument)
    {
        for (ResourceLocation location : Ability.getAbilityRegistry().getKeys())
        {
            if (location.equals(new ResourceLocation(argument)))
            {
                try
                {
                    return Ability.getAbilityRegistry().getValue(new ResourceLocation(argument)).newInstance(CommandBase.getCommandSenderAsPlayer(sender));
                }
                catch (PlayerNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
