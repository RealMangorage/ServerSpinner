package org.mangorage.serverspinner;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.mangorage.serverspinner.core.ServerManager;
import org.mangorage.serverspinner.core.commands.ServerCommand;
import org.slf4j.Logger;

@Plugin(
        id = "serverspinner2",
        name = "ServerSpinner2",
        version = "1.0.0"
)
public class ServerSpinner {
    private static ServerManager serverManager;

    public static ServerManager getServerManager() {
        return serverManager;
    }

    @Inject
    private Logger logger;
    @Inject
    private CommandManager commandManager;
    @Inject
    private ProxyServer proxyServer;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        commandManager.register(ServerCommand.getCommand(proxyServer));

        serverManager = new ServerManager(proxyServer);
    }

    @Subscribe
    public void onReload(ProxyReloadEvent event) {

    }

    @Subscribe
    public void onExit(ProxyShutdownEvent event) {
        getServerManager().shutdown();
    }


}
