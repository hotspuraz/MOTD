package evo.motd;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;

public final class MotdCommand implements SimpleCommand {

    private static final String PERM = "motd.admin";

    private final MotdPlugin plugin;
    private final PingListener pings;
    private final ProxyServer proxy;

    public MotdCommand(MotdPlugin plugin, PingListener pings, ProxyServer proxy) {
        this.plugin = plugin;
        this.pings = pings;
        this.proxy = proxy;
    }

    @Override
    public boolean hasPermission(Invocation inv) {
        return inv.source().hasPermission(PERM);
    }

    @Override
    public void execute(Invocation inv) {
        CommandSource src = inv.source();
        Settings s = plugin.settings();
        String[] a = inv.arguments();
        String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "reload" -> {
                    s.reload();
                    send(src, s.message("reloaded"));
                }
                case "maintenance" -> {
                    boolean next = a.length > 1 ? a[1].equalsIgnoreCase("on") : !s.maintenance;
                    s.setMaintenance(next);
                    send(src, s.message(next ? "maintenance-on" : "maintenance-off"));
                    if (next) kickNonBypass(s);
                }
                case "preview" -> {
                    // Synthetic base ping so the output matches what clients would see.
                    ServerPing base = new ServerPing(
                            new ServerPing.Version(0, ""),
                            new ServerPing.Players(proxy.getPlayerCount(), proxy.getConfiguration().getShowMaxPlayers(), List.of()),
                            Component.empty(), null);
                    src.sendMessage(pings.build(base).getDescriptionComponent());
                }
                default -> send(src, s.message("usage"));
            }
        } catch (IOException e) {
            src.sendMessage(Text.parse("<red>Failed: " + e.getMessage()));
        }
    }

    private void kickNonBypass(Settings s) {
        for (var p : proxy.getAllPlayers()) {
            boolean allowed = p.hasPermission(s.bypassPermission)
                    || s.allowedPlayers.stream().anyMatch(n -> n.equalsIgnoreCase(p.getUsername()));
            if (!allowed) p.disconnect(Text.parse(s.kickMessage));
        }
    }

    @Override
    public List<String> suggest(Invocation inv) {
        String[] a = inv.arguments();
        if (a.length <= 1) return List.of("reload", "maintenance", "preview");
        if (a.length == 2 && a[0].equalsIgnoreCase("maintenance")) return List.of("on", "off");
        return List.of();
    }

    private void send(CommandSource src, String msg) {
        src.sendMessage(Text.parse(plugin.settings().message("prefix") + msg));
    }
}
