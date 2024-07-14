package sh.dominick.commissions.pixelmonrankings.support.arclight;

import net.minecraft.entity.player.ServerPlayerEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;

import java.util.UUID;

public class ArcLightSupport {
    public static Object PLUGIN = null;
    public static boolean IS_ARCLIGHT = false;

    static {
        try {
            Class.forName("org.bukkit.Bukkit");
            IS_ARCLIGHT = true;

            PLUGIN = new FakePlugin(
                    PixelmonRankingsMod.MOD_ID,
                    PixelmonRankingsMod.MOD_ID + " Mod",
                    PixelmonRankingsMod.class,
                    null,
                    Bukkit.getLogger()
            );
        } catch (ClassNotFoundException e) {
            IS_ARCLIGHT = false;
        }
    }

    public static void sync(Runnable task) {
        if (!IS_ARCLIGHT) {
            task.run();
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin) PLUGIN, task);
    }

    public static boolean hasPermission(ServerPlayerEntity player, String permission, boolean fallback) {
        return hasPermission(player.getUUID(), permission, fallback);
    }

    public static boolean hasPermission(UUID player, String permission, boolean fallback) {
        if (IS_ARCLIGHT) {
            Player bukkitPlayer = Bukkit.getPlayer(player);
            if (bukkitPlayer == null)
                return false;

            return bukkitPlayer.hasPermission(permission);
        }

        return fallback;
    }
}
