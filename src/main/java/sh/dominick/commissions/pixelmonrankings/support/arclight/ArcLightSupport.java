package sh.dominick.commissions.pixelmonrankings.support.arclight;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;

public class ArcLightSupport {
    public static Object PLUGIN = null;
    public static boolean IS_ARCLIGHT = false;

    static {
        try {
            Class.forName("org.bukkit.Bukkit");
            IS_ARCLIGHT = true;

            PLUGIN = new FakePlugin(PixelmonRankingsMod.MOD_ID, PixelmonRankingsMod.MOD_ID + " Mod");
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
}
