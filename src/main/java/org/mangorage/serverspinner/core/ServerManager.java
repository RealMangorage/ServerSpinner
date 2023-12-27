package org.mangorage.serverspinner.core;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.mangorage.serverspinner.core.process.MinecraftServer;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class ServerManager {
    private final Map<String, MinecraftServer> servers = new ConcurrentHashMap<>();
    private final Queue<Runnable> runnables = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();
    private final ProxyServer server;
    private boolean running = true;

    private final Thread threadWorker = new Thread(() -> {
        System.out.println("Starting Server Manager Thread worker");
        while (running) {
            if (!runnables.isEmpty()) runnables.poll().run();
        }
    }, "ServerManager Thread Worker");

    private final Thread shutdownThread = new Thread(() -> {
        servers.forEach((id, server) -> server.forceShutdown());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    });

    public ServerManager(ProxyServer proxy) {
        Objects.requireNonNull(proxy);
        this.server = proxy;
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        threadWorker.start();
    }

    public int findPort() {
        return random.nextInt(30000, 40000);
    }

    public void enqueue(Runnable runnable) {
        runnables.add(runnable);
    }

    public void shutdown() {
        servers.forEach((id, server) -> server.stop());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void connect(Player player, String playerName) {
        UUID uuid = Util.getUUID(playerName);
        AtomicReference<MinecraftServer> pServer = new AtomicReference<>();

        if (uuid != null) {
            pServer.set(servers.get(playerName));
        } else {
            server.getPlayer(playerName).ifPresent(a -> {
                pServer.set(servers.get(a.getUniqueId().toString()));
            });
        }

        MinecraftServer playerServer = pServer.get();
        if (playerServer != null) {
            playerServer.sendOver(player, false);
        }
    }

    public void create(Player player) {
        String id = player.getUniqueId().toString();
        Path instancePath = Constants.getInstances().resolve(id + "/");

        if (!servers.containsKey(id)) {
            enqueue(() -> {
                MinecraftServer Pserver = servers.put(id, new MinecraftServer(
                        id,
                        Constants.getTemplate(),
                        instancePath,
                        findPort(),
                        player,
                        server
                ));

                Pserver.sendOver(player, !Pserver.isRunning());
            });
        } else {
            player.sendMessage(Component.text("Already created a server! Cannot create more then 1"));
        }
    }

    public void lobby(Player player) {
        var lobby = server.getServer("lobby");
        lobby.ifPresentOrElse(server -> {
            player.createConnectionRequest(server).connect().whenComplete((a, t) -> {
                a.getReasonComponent().ifPresent(player::sendMessage);
            });
        }, () -> {
            player.sendMessage(Component.text("Unable to send to lobby. lobby server doesn't exist"));
        });
    }
}
