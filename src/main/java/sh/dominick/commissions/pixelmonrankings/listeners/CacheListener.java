package sh.dominick.commissions.pixelmonrankings.listeners;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;

import java.util.UUID;

public class CacheListener {
    private final IDataManager dataManager;
    public CacheListener(IDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

        UUID playerId = player.getUUID();
        String playerUsername = player.getName().getString();
        String texture = player.getGameProfile().getProperties().get("textures").stream().findFirst().get().getValue();

        dataManager.recordGameProfile(playerId, playerUsername, texture);
    }
}
