package sh.dominick.commissions.pixelmonrankings.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;
import sh.dominick.commissions.pixelmonrankings.data.facade.CachedDataManager;
import sh.dominick.commissions.pixelmonrankings.support.arclight.ArcLightSupport;
import sh.dominick.commissions.pixelmonrankings.util.ItemStackUtil;
import sh.dominick.commissions.pixelmonrankings.util.PlayerHeadUtil;
import sh.dominick.commissions.pixelmonrankings.util.TimeUtil;
import sh.dominick.commissions.pixelmonrankings.views.PeriodSelectView;
import sh.dominick.commissions.pixelmonrankings.views.RankedStatisticsView;
import sh.dominick.commissions.pixelmonrankings.views.StatisticSelectView;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsLang.wrap;

public class RankingsCommand {
    private final PixelmonRankingsMod mod;

    private RankingsCommand(PixelmonRankingsMod mod) {
        this.mod = mod;
    }

    public static void register(PixelmonRankingsMod mod, CommandDispatcher<CommandSource> dispatcher) {
        RankingsCommand instance = new RankingsCommand(mod);

        LiteralArgumentBuilder<CommandSource> builder = Commands.literal("rankings").executes(instance::executeRankings)
                .then(Commands.literal("this-month").executes((c) -> instance.executeRankings(c, TimeUtil.getStartOfMonth(), TimeUtil.getEndOfMonth())))
                .then(Commands.literal("all-time").executes((c) -> instance.executeRankings(c, null, null)));

        if (mod.config().devMode) {
            builder.then(Commands.literal("flood").executes(instance::executeFlood));
        }

        dispatcher.register(builder);
    }

    public int executeRankings(CommandContext<CommandSource> command) throws CommandSyntaxException {
        ItemStack monthlyItem = PlayerHeadUtil.getPlayerHead(UUID.randomUUID(), mod.lang().periodSelectView.thisMonthItem.head, 1);
        monthlyItem.setHoverName(wrap(mod.lang().periodSelectView.thisMonthItem.name));
        ItemStackUtil.writeLore(monthlyItem, wrap(mod.lang().periodSelectView.thisMonthItem.lore));
        ItemStackUtil.writeHideFlags(monthlyItem, ItemStackUtil.ALL_FLAGS);

        ItemStack allTimeItem = PlayerHeadUtil.getPlayerHead(UUID.randomUUID(), mod.lang().periodSelectView.allTimeItem.head, 1);
        allTimeItem.setHoverName(wrap(mod.lang().periodSelectView.allTimeItem.name));
        ItemStackUtil.writeLore(allTimeItem, wrap(mod.lang().periodSelectView.allTimeItem.lore));
        ItemStackUtil.writeHideFlags(allTimeItem, ItemStackUtil.ALL_FLAGS);

        Set<PeriodSelectView.Period> periods = new HashSet<>(Arrays.asList(
                new PeriodSelectView.Period(TimeUtil.getStartOfMonth(), TimeUtil.getEndOfMonth(), monthlyItem, 3),
                new PeriodSelectView.Period(null, null, allTimeItem, 5)
        ));

        ServerPlayerEntity player = command.getSource().getPlayerOrException();
        PeriodSelectView.open(mod, player, periods).onSelect((period) -> {
            StatisticSelectView.open(mod, player).onSelect((statistic) -> {
                RankedStatisticsView.open(mod, player, statistic, period.start(), period.end());
            });
        });

        return Command.SINGLE_SUCCESS;
    }

    public int executeRankings(CommandContext<CommandSource> command, @Nullable Instant start, @Nullable Instant end) throws CommandSyntaxException {
        ServerPlayerEntity player = command.getSource().getPlayerOrException();
        StatisticSelectView.open(mod, player).onSelect((statistic) -> {
            RankedStatisticsView.open(mod, player, statistic, start, end);
        });

        return Command.SINGLE_SUCCESS;
    }

    public int executeFlood(CommandContext<CommandSource> command) throws CommandSyntaxException {
        ServerPlayerEntity player = command.getSource().getPlayerOrException();
        StatisticSelectView.open(mod, player).onSelect((statistic) -> {
            ArcLightSupport.sync(player::closeContainer);

            int count = (int) (Math.random() * 1000);
            for (int i = 0; i < count; i++) {
                UUID randomId = UUID.randomUUID();
                int randomValue = (int) (Math.random() * 1000);
                mod.dataManager().recordChange(new IDataManager.Key(randomId, statistic), randomValue);

                String randomName = "Random Player " + i;
                String randomTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzhmM2Q3NjkxZDZkNWQ1NDZjM2NmMjIyNDNiM2U4MzA5YTEwNzAxMWYyZWU5Mzg0OGIxZThjNjU3NjgxYTU2ZCJ9fX0=";
                mod.dataManager().recordGameProfile(randomId, randomName, randomTexture);
            }

            if (mod.dataManager() instanceof CachedDataManager) {
                ((CachedDataManager) mod.dataManager()).clearAll();
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
