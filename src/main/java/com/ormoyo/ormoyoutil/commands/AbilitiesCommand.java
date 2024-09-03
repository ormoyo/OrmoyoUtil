package com.ormoyo.ormoyoutil.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityHolder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Set;
import java.util.stream.Collectors;

public class AbilitiesCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(Commands.literal("abilities")
                .requires(commandSource -> commandSource.hasPermissionLevel(0))
                .executes(context -> AbilitiesCommand.printAbilities(context.getSource())));
    }

    private static int printAbilities(CommandSource source) throws CommandSyntaxException
    {
        AbilityHolder abilityHolder = Ability.getAbilityHolder(source.asPlayer());

        if (abilityHolder == null || abilityHolder.getAbilities().isEmpty())
        {
            source.sendFeedback(new TranslationTextComponent("commands.ormoyoutil.noabilities"), true);
            return -1;
        }

        Set<String> names = abilityHolder.getAbilities().stream().map(ability -> Ability.getAbilityDisplayName(ability.getClass()).getString()).collect(Collectors.toSet());
        source.sendFeedback(new TranslationTextComponent("commands.ormoyoutil.abilities", names), true);

        return 1;
    }
}
