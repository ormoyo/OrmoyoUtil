package com.ormoyo.ormoyoutil.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ormoyo.ormoyoutil.ability.Ability;
import com.ormoyo.ormoyoutil.ability.AbilityEntry;
import com.ormoyo.ormoyoutil.ability.IAbilityHolder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.concurrent.CompletableFuture;

public class AcquireAbilityCommand
{
	private static final DynamicCommandExceptionType NO_ABILITIES_FOUND = new DynamicCommandExceptionType(str -> new TranslationTextComponent("argument.ability.notfound", str));
    private static final DynamicCommandExceptionType ABILITY_ALREADY_EXISTS = new DynamicCommandExceptionType(str -> new TranslationTextComponent("argument.ability.alreadyexists", str));

    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(Commands.literal("acquireAbility")
                .requires(commandSource -> commandSource.hasPermissionLevel(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("abilities", new AbilityParser())
                                .executes(context -> AcquireAbilityCommand.acquireAbility(context.getSource(), context.getArgument("abilities", AbilityEntry.class))))
                            )
                );
    }


    private static int acquireAbility(CommandSource entity, AbilityEntry entry) throws CommandSyntaxException
    {
        IAbilityHolder abilityHolder = (IAbilityHolder) entity.asPlayer();
        if (abilityHolder.getAbility(entry.getRegistryName()) != null)
            throw ABILITY_ALREADY_EXISTS.create(Ability.getAbilityDisplayName(entry.getAbilityClass()));

        boolean unlocked = abilityHolder.unlockAbility(entry);

        if(!unlocked)
            return -1;

        entity.sendFeedback(new TranslationTextComponent("commands.ormoyoutil.acquireability", Ability.getAbilityDisplayName(entry.getAbilityClass())), true);
        return 1;
    }


    private static class AbilityParser implements ArgumentType<AbilityEntry>
    {
        @Override
        public AbilityEntry parse(StringReader reader) throws CommandSyntaxException
        {
            ResourceLocation location = ResourceLocation.read(reader);

            if(!Ability.getAbilityRegistry().containsKey(location))
            {
                reader.setCursor(0);
                throw NO_ABILITIES_FOUND.create(String.valueOf(location));
            }


            return Ability.getAbilityRegistry().getValue(location);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
        {
            for (ResourceLocation registryName : Ability.getAbilityRegistry().getKeys())
            {
                builder.suggest(registryName.toString());
            }

            return builder.buildFuture();
        }
    }
}
