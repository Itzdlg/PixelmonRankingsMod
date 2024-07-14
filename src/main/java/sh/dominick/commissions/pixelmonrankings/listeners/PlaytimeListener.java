package sh.dominick.commissions.pixelmonrankings.listeners;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sh.dominick.commissions.pixelmonrankings.Statistic;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;
import sh.dominick.commissions.pixelmonrankings.support.arclight.ArcLightSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeListener {
    private final IDataManager dataManager;
    private final Map<UUID, Long> loginTimes = new HashMap<>();

    public PlaytimeListener(IDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        UUID playerUUID = event.getPlayer().getUUID();
        loginTimes.put(playerUUID, System.currentTimeMillis());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerUUID = event.getPlayer().getUUID();
        if (loginTimes.containsKey(playerUUID)) {
            if (ArcLightSupport.hasPermission(playerUUID, "pixelmonrankings.bypass", false))
                return;

            long loginTimeMillis = loginTimes.get(playerUUID);
            long logoutTimeMillis = System.currentTimeMillis();
            long sessionDurationSeconds = (logoutTimeMillis - loginTimeMillis) / 1000;

            IDataManager.Key key = new IDataManager.Key(playerUUID, Statistic.PLAY_TIME);
            dataManager.recordChange(key, sessionDurationSeconds);

            // Remove the player from the login times map
            loginTimes.remove(playerUUID);
        }
    }
}