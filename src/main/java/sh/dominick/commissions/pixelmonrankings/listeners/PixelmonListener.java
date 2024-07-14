package sh.dominick.commissions.pixelmonrankings.listeners;

import com.pixelmonmod.pixelmon.api.battles.BattleResults;
import com.pixelmonmod.pixelmon.api.daycare.event.DayCareEvent;
import com.pixelmonmod.pixelmon.api.events.BeatWildPixelmonEvent;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.events.EggHatchEvent;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sh.dominick.commissions.pixelmonrankings.Statistic;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;
import sh.dominick.commissions.pixelmonrankings.support.arclight.ArcLightSupport;

import java.util.Map;
import java.util.UUID;

public class PixelmonListener {
    private final IDataManager dataManager;

    public PixelmonListener(IDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onWildPokemonDefeated(BeatWildPixelmonEvent event) {
        UUID playerId = event.player.getUUID();
        if (ArcLightSupport.hasPermission(playerId, "pixelmonrankings.bypass", false))
            return;

        dataManager.recordChange(new IDataManager.Key(playerId, Statistic.WILD_POKEMON_DEFEATED), 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onPokemonCaptured(CaptureEvent.SuccessfulCapture event) {
        UUID playerId = event.getPlayer().getUUID();
        if (ArcLightSupport.hasPermission(playerId, "pixelmonrankings.bypass", false))
            return;

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
    public void onMakeEgg(DayCareEvent.PostCollect event) {
        UUID playerId = event.getPlayer().getUUID();
        if (ArcLightSupport.hasPermission(playerId, "pixelmonrankings.bypass", false))
            return;

        dataManager.recordChange(new IDataManager.Key(playerId, Statistic.MADE_EGGS), 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onHatchEgg(EggHatchEvent.Post event) {
        UUID playerId = event.getPokemon().getOwnerPlayerUUID();
        if (ArcLightSupport.hasPermission(playerId, "pixelmonrankings.bypass", false))
            return;

        dataManager.recordChange(new IDataManager.Key(playerId, Statistic.HATCHED_EGGS), 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onBattle(BattleEndEvent event) {
        if (!event.getBattleController().isPvP())
            return;

        for (Map.Entry<BattleParticipant, BattleResults> result : event.getResults().entrySet()) {
            if (result.getValue() != BattleResults.VICTORY)
                continue;

            LivingEntity battleEntity = result.getKey().getEntity();
            if (battleEntity.getType() != EntityType.PLAYER)
                continue;

            UUID playerId = battleEntity.getUUID();
            if (ArcLightSupport.hasPermission(playerId, "pixelmonrankings.bypass", false))
                continue;

            dataManager.recordChange(new IDataManager.Key(playerId, Statistic.PVP_BATTLES_WON), 1);
        }
    }
}
