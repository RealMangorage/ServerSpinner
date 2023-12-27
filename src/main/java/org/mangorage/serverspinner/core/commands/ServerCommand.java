package org.mangorage.serverspinner.core.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.mangorage.serverspinner.ServerSpinner;

import java.util.Collection;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public class ServerCommand {

    public static LiteralArgumentBuilder<CommandSource> literal(String id) {
        return LiteralArgumentBuilder.literal(id);
    }

    public static <T> RequiredArgumentBuilder<CommandSource, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static BrigadierCommand getCommand(ProxyServer server) {
        return new BrigadierCommand(literal("server")
                .then(literal("lobby")
                        .executes(context -> {
                            if (context.getSource() instanceof Player player) {
                                player.sendMessage(Component.text("Sending you to lobby now!"));
                                ServerSpinner.getServerManager().lobby(player);
                            }
                            return 1;
                        }))
                .then(literal("create")
                        .executes(context -> {
                            // Logic for /server create
                            if (context.getSource() instanceof Player player) {
                                player.sendMessage(Component.text("Creating your server now... Please wait up to 30 seconds..."));
                                ServerSpinner.getServerManager().create(player);
                            }
                            return 1; // Return a success code
                        }))
                .then(literal("join")
                        .executes(context -> {
                            // Logic for /server join
                            if (context.getSource() instanceof Player player) {
                                ServerSpinner.getServerManager().connect(player, player.getUniqueId().toString());
                            }
                            return 1; // Return a success code
                        }))
                .then(literal("join")
                        .then(argument("playername", word())
                                .suggests((context, builder) -> {
                                    for (Player onlinePlayer : server.getAllPlayers()) {
                                        builder.suggest(onlinePlayer.getUsername());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    if (context.getSource() instanceof Player player) {
                                        ServerSpinner.getServerManager().connect(player, getString(context, "playername"));
                                    }
                                    return 1; // Return a success code
                                })))
                .build());
    }
}
