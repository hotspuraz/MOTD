package evo.motd;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(id = "motd", name = "MOTD", version = "1.1.0",
        description = "Configurable MOTD, player counts and maintenance mode", authors = {"evo"})
public final class MotdPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDir;
    private Settings settings;

    @Inject
    public MotdPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        try {
            settings = new Settings(dataDir);
        } catch (IOException e) {
            logger.error("Could not load config.yml; MOTD plugin disabled", e);
            return;
        }
        PingListener pings = new PingListener(this, proxy);
        proxy.getEventManager().register(this, pings);
        proxy.getEventManager().register(this, new MaintenanceListener(this));
        var meta = proxy.getCommandManager().metaBuilder("motd").plugin(this).build();
        proxy.getCommandManager().register(meta, new MotdCommand(this, pings, proxy));
        logger.info("MOTD loaded ({} motds, maintenance {})", settings.motds.size(), settings.maintenance ? "ON" : "off");
    }

    public Settings settings() {
        return settings;
    }
}
