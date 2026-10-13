package evo.motd;

import com.velocitypowered.api.proxy.ProxyServer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** Placeholder substitution + MiniMessage parsing. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Text() {}

    public static String fill(String s, ProxyServer proxy, int shownOnline, int shownMax) {
        return s.replace("%online%", Integer.toString(shownOnline))
                .replace("%max%", Integer.toString(shownMax))
                .replace("%real_online%", Integer.toString(proxy.getPlayerCount()))
                .replace("%servers%", Integer.toString(proxy.getAllServers().size()))
                .replace("%date%", LocalDate.now().format(DATE))
                .replace("%time%", LocalTime.now().format(TIME));
    }

    public static Component parse(String s) {
        return MM.deserialize(s);
    }

    public static Component parse(String s, ProxyServer proxy, int shownOnline, int shownMax) {
        return MM.deserialize(fill(s, proxy, shownOnline, shownMax));
    }
}
