package sh.dominick.commissions.pixelmonrankings.config;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import sh.dominick.commissions.pixelmonrankings.Statistic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class PixelmonRankingsLang {
    @Setting
    private final Map<String, StatisticConfig> statistics = new HashMap<String, StatisticConfig>() {{
        for (Statistic s : Statistic.values())
            put(s.name(), new StatisticConfig(s));
    }};

    public StatisticConfig statistic(Statistic statistic) {
        return statistics.get(statistic.name());
    }

    @ConfigSerializable
    public static class StatisticConfig {
        @Setting public ItemConfig item;
        @ConfigSerializable
        public static class ItemConfig {
            @Setting public int position;
            @Setting public String material;
            @Setting public int amount;

            @Setting public String displayName;
            @Setting public String[] lore;

            public ItemConfig() {}
            public ItemConfig(Statistic s) {
                this.position = s.ordinal();
                this.material = "minecraft:clay";
                this.amount = 1;

                this.displayName = "<italic:false><aqua>" + s.displayName() + "</aqua></italic>";
                this.lore = new String[] {};
            }
        }

        @Setting public String displayName;

        public StatisticConfig() {}
        public StatisticConfig(Statistic s) {
            item = new ItemConfig(s);
            displayName = s.displayName();
        }
    }

    @Setting public String unknownPlayerHead = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzhmM2Q3NjkxZDZkNWQ1NDZjM2NmMjIyNDNiM2U4MzA5YTEwNzAxMWYyZWU5Mzg0OGIxZThjNjU3NjgxYTU2ZCJ9fX0=";

    @Setting public PeriodSelectView periodSelectView = new PeriodSelectView();
    @ConfigSerializable
    public static class PeriodSelectView {
        @Setting public String title = "<blue>Select A Period</blue>";

        @Setting public ThisMonthItem thisMonthItem = new ThisMonthItem();
        @ConfigSerializable
        public static class ThisMonthItem {
            @Setting public String name = "<italic:false><yellow>This Month</yellow></italic>";
            @Setting public String[] lore = new String[] {};

            @Setting public String head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODc3MjFiZTM3ZjkzMzJhZDhiZTQzY2JkZjE1NmY0MzQ5MjY3ZWEyYzExMGZhZmVjMjM3NjhlNjhkODMwY2FmNiJ9fX0=";
        }

        @Setting public AllTimeItem allTimeItem = new AllTimeItem();
        @ConfigSerializable
        public static class AllTimeItem {
            @Setting public String name = "<italic:false><yellow>All Time</yellow></italic>";
            @Setting public String[] lore = new String[] {};

            @Setting public String head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmYyODNlNjExYTYzZWU4ZTJmNmYxZDJjZmNiN2U0YjVmM2I2N2E1YmE5NjQ4YzU2NGZlNGExZWE3NDFiY2FhMSJ9fX0=";
        }
    }

    @Setting public RankedStatisticView rankedStatisticView = new RankedStatisticView();
    @ConfigSerializable
    public static class RankedStatisticView {
        @Setting public String title = "<blue><statistic_name></blue>";

        @Setting public YouEntryItem youEntryItem = new YouEntryItem();
        @ConfigSerializable
        public static class YouEntryItem {
            @Setting public String name = "<italic:false><yellow>You</yellow></italic>";
            @Setting public String[] loreRanked = {
                    "",
                    "<gray>Ranked <gold><position></gold> of <total></gray>",
                    "<gray>with <white><value></gray>"
            };
            @Setting public String[] loreUnranked = {
                    "",
                    "<gray>Unranked with <white><value></white></gray>"
            };
        }

        @Setting public EntryItem entryItem = new EntryItem();
        @ConfigSerializable
        public static class EntryItem {
            @Setting public String name = "<italic:false><yellow><player_name></yellow></italic>";
            @Setting public String[] lore = {
                    "",
                    "<gray>Ranked <gold><position></gold> of <total></gray>",
                    "<gray>with <white><value></gray>"
            };
        }

        @Setting public BackwardsItem backwardsItem = new BackwardsItem();
        @ConfigSerializable
        public static class BackwardsItem {
            @Setting public String name = "<italic:false><yellow>Go Backwards</yellow></italic>";
            @Setting public String[] lore = new String[] {};

            @Setting public String head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjBjZmI0ZjM3Y2NlZmQwNTg5YzU1NzhiNTQxZTdhZjkyM2UzZTY0MjBhZGE2YmU0NDNkZmFkY2IwNWJhZTE5NCJ9fX0=";
        }

        @Setting public ForwardsItem forwardsItem = new ForwardsItem();
        @ConfigSerializable
        public static class ForwardsItem {
            @Setting public String name = "<italic:false><yellow>Go Forwards</yellow></italic>";
            @Setting public String[] lore = new String[] {};

            @Setting public String head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjM5NTExOWRkNTIwMWEyNDJiODZiNDg2NmQ2ZjA0NTQxYjAwYjkyZWJkZDU3Y2UyNzkxOWZiNWYxMDJhNmRkZCJ9fX0=";
        }
    }

    @Setting public PlayerStatisticsView playerStatisticsView = new PlayerStatisticsView();
    @ConfigSerializable
    public static class PlayerStatisticsView {
        @Setting public String name = "<italic:false><yellow><player_name></yellow></italic>";

        @Setting public BackwardsItem backwardsItem = new BackwardsItem();
        @ConfigSerializable
        public static class BackwardsItem {
            @Setting public String name = "<italic:false><yellow>Go Back</yellow></italic>";
            @Setting public String[] lore = new String[] {};

            @Setting public String head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjBjZmI0ZjM3Y2NlZmQwNTg5YzU1NzhiNTQxZTdhZjkyM2UzZTY0MjBhZGE2YmU0NDNkZmFkY2IwNWJhZTE5NCJ9fX0=";
        }

        @Setting public EntryItem entryItem = new EntryItem();
        @ConfigSerializable
        public static class EntryItem {
            @Setting public String name = "<italic:false><aqua><statistic_name></aqua></italic>";
            @Setting public String[] lore = {
                    "",
                    "<gray>Ranked <gold><position></gold> of <total></gray>",
                    "<gray>with <white><value></gray>"
            };
        }
    }

    @Setting public StatisticSelectView statisticSelectView = new StatisticSelectView();
    @ConfigSerializable
    public static class StatisticSelectView {
        @Setting public String title = "<blue>Select a Statistic</blue>";
    }

    public static ITextComponent wrap(String mm, TagResolver... tagResolvers) {
        Component component = MiniMessage.miniMessage().deserialize(mm, tagResolvers);
        JsonElement json = GsonComponentSerializer.gson().serializeToTree(component);
        return ITextComponent.Serializer.fromJson(json);
    }

    public static ITextComponent[] wrap(String[] mm, TagResolver... tagResolvers) {
        List<ITextComponent> results = new ArrayList<>();

        for (String s : mm)
            results.add(wrap(s, tagResolvers));

        return results.toArray(new ITextComponent[0]);
    }
}
