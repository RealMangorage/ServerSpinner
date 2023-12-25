package org.mangorage.serverspinner.core.process;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import org.mangorage.serverspinner.core.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    private final LazyProcess process;
    private final int port;
    private final Path template;
    private final Path instance;
    private final RegisteredServer registeredServer;
    private boolean running = false;
    private boolean isDone = false;
    private Queue<Player> queue = new ConcurrentLinkedQueue<>();

    public static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public Server(String id, Path templateDirectory, Path instanceDirectory, int port, Player player, ProxyServer proxyServer) {
        this.process = LazyProcess.create(
                id,
                instanceDirectory.toString(),
                "java -jar server.jar nogui",
                out -> {
                    if (out.contains("Done")) {
                        this.isDone = true;
                        sendOver();
                        op(player);
                    }
                },
                new IStatus() {
                    @Override
                    public void stopped() {
                        running = false;
                        isDone = false;
                    }

                    @Override
                    public void running() {
                        running = true;
                        isDone = false;
                    }
                }
        );
        this.port = port;
        this.template = templateDirectory;
        this.instance = instanceDirectory;
        this.registeredServer = proxyServer.createRawRegisteredServer(new ServerInfo(id, new InetSocketAddress("localhost", port)));
        queue.add(player);
        setup();
    }

    private void op(Player player) {
        process.printInput("op %s".formatted(player.getUsername()));
    }

    public void setup() {
        try {
            deleteDirectory(instance);
            Util.copyDirectory(template, instance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File file = instance.resolve("server.properties").toFile();
        List<String> args = Util.readLinesFromFile(file);
        HashMap<String, String> properties = new HashMap<>();
        args.forEach(a -> {
            System.out.println(a);
            var result = a.split("=");
            if (result.length == 1) {
                properties.put(result[0], "");
            } else if (result.length == 2){
                properties.put(result[0], result[1]);
            }
        });

        properties.put("server-port", Integer.toString(port));

        try (var is = new FileWriter(file)) {
            properties.forEach((a, b) -> {
                try {
                    is.append("%s=%s".formatted(a, b)).append("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        start();
    }


    public void start() {
        process.start();
    }

    public void stop() {
        process.printInput("stop");
    }

    public void forceShutdown() {
        process.forceStopProcess();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void sendOver() {
        while (!queue.isEmpty()) {
            var player = queue.poll();
            sendOver(player, false);
        }
    }

    public void sendOver(Player player, boolean setup) {
        if (setup) {
            queue.add(player);
            setup();
        } else {
            if (!isDone) {
                if (queue.contains(player)) {
                    player.sendMessage(Component.text("Already connecting to server. Please wait"));
                } else {
                    queue.add(player);
                }
            } else {
                player.sendMessage(Component.text("Sending you to server now... Enjoy!"));

                player.createConnectionRequest(registeredServer).connect().whenComplete((a, t) -> {
                    a.getReasonComponent().ifPresent(player::sendMessage);
                });
            }
        }
    }

}
