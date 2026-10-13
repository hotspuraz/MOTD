package evo.motd;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;

public final class MaintenanceListener {

    private final MotdPlugin plugin;

    public MaintenanceListener(MotdPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Settings s = plugin.settings();
        if (!s.maintenance || !event.getResult().isAllowed()) return;
        var player = event.getPlayer();
        if (player.hasPermission(s.bypassPermission)) return;
        for (String name : s.allowedPlayers) {
            if (name.equalsIgnoreCase(player.getUsername())) return;
        }
        event.setResult(ResultedEvent.ComponentResult.denied(Text.parse(s.kickMessage)));
    }
}
