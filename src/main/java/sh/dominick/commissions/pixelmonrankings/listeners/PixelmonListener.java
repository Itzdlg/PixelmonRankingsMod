package sh.dominick.commissions.pixelmonrankings.listeners;

import com.pixelmonmod.pixelmon.api.daycare.event.DayCareEvent;
import com.pixelmonmod.pixelmon.api.events.BeatWildPixelmonEvent;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.events.EggHatchEvent;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sh.dominick.commissions.pixelmonrankings.Statistic;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;

import java.util.UUID;

public class PixelmonListener {
    private final IDataManager dataManager;

    public PixelmonListener(IDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onWildPokemonDefeated(BeatWildPixelmonEvent event) {
        UUID playerId = event.player.getUUID();
        dataManager.recordChange(new IDataManager.Key(playerId, Statistic.WILD_POKEMON_DEFEATED), 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onPokemonCaptured(CaptureEvent event) {
        UUID playerId = event.getPlayer().getUUID();

        boolean shiny = event.getPokemon().getPokemon().isShiny();
        boolean legendary = event.getPokemon().getPokemon().isLegendary();
        boolean ultraBeast = event.getPokemon().getPokemon().isUltraBeast();

        if (ultraBeast)
            dataManager.recordChange(new IDataManager.Key(playerId, Statistic.ULTRA_BEAST_CAPTURED), 1);

        if (legendary)
            dataManager.recordChange(new IDataManager.Key(playerId, Statistic.LEGENDARY_POKEMON_CAPTURED), 1);

        if (shiny)
            dataManager.recordChange(new IDataManager.Key(playerId, Statistic.POKEMON_SHINY_CAPTURED), 1);

        if (!ultraBeast && !legendary && !shiny)
            dataManager.recordChange(new IDataManager.Key(playerId, Statistic.NORMAL_POKEMON_CAPTURED), 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onMakeEgg(DayCareEvent.PostPokemonAdd event) {
        UUID playerId = event.getDayCare().getEgg().getOwnerPlayerUUID();
        dataManager.recordChange(new IDataManager.Key(playerId, Statistic.MADE_EGGS), 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onHatchEgg(EggHatchEvent event) {
        UUID playerId = event.getPokemon().getOwnerPlayerUUID();
        dataManager.recordChange(new IDataManager.Key(playerId, Statistic.MADE_EGGS), 1);
    }
}
