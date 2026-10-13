package evo.motd;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class PingListener {

    private final MotdPlugin plugin;
    private final ProxyServer proxy;

    public PingListener(MotdPlugin plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy = proxy;
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        event.setPing(build(event.getPing()));
    }

    /** Builds the ping shown to clients, based on the proxy's own ping (used for protocol/version). */
    public ServerPing build(ServerPing base) {
        Settings s = plugin.settings();
        int real = proxy.getPlayerCount();
        int baseMax = base.getPlayers().map(ServerPing.Players::getMax).orElse(real);
        int online = switch (s.onlineMode) {
            case FIXED -> s.onlineValue;
            case OFFSET -> Math.max(0, real + s.onlineValue);
            default -> real;
        };
        int max = switch (s.maxMode) {
            case FIXED -> s.maxValue;
            case ONLINE_PLUS -> online + s.maxValue;
            case OFFSET -> baseMax + s.maxValue;
            default -> baseMax;
        };

        ServerPing.Builder b = base.asBuilder().onlinePlayers(online).maximumPlayers(max);

        Settings.Motd motd = s.maintenance ? s.maintenanceMotd : pick(s);
        String joined = motd.line2().isEmpty() ? motd.line1() : motd.line1() + "\n" + motd.line2();
        b.description(Text.parse(joined, proxy, online, max));

        List<String> hover = s.maintenance && !s.maintenanceHover.isEmpty() ? s.maintenanceHover : s.hover;
        if (!hover.isEmpty()) {
            List<ServerPing.SamplePlayer> sample = new ArrayList<>();
            for (String line : hover) {
                // The vanilla client only renders section-sign colours in the hover list.
                Component c = Text.parse(line, proxy, online, max);
                sample.add(new ServerPing.SamplePlayer(
                        LegacyComponentSerializer.legacySection().serialize(c), UUID.randomUUID()));
            }
            b.clearSamplePlayers().samplePlayers(sample.toArray(new ServerPing.SamplePlayer[0]));
        }

        if (s.maintenance) {
            String text = LegacyComponentSerializer.legacySection().serialize(Text.parse(s.maintenanceVersion));
            b.version(new ServerPing.Version(-1, text));
        }
        return b.build();
    }

    private Settings.Motd pick(Settings s) {
        List<Settings.Motd> list = s.motds;
        return switch (s.rotationMode) {
            case FIXED -> list.get(0);
            case RANDOM -> list.get(ThreadLocalRandom.current().nextInt(list.size()));
            case SEQUENTIAL -> list.get((int) ((System.currentTimeMillis() / 1000L / s.rotationIntervalSeconds) % list.size()));
        };
    }
}
