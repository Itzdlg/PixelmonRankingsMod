package sh.dominick.commissions.pixelmonrankings.views;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.registries.ForgeRegistries;
import sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsConfig;
import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;
import sh.dominick.commissions.pixelmonrankings.Statistic;
import sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsLang;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;
import sh.dominick.commissions.pixelmonrankings.util.ItemStackUtil;
import sh.dominick.commissions.pixelmonrankings.util.PlayerHeadUtil;
import sh.dominick.commissions.pixelmonrankings.views.util.ActionHandler;
import sh.dominick.commissions.pixelmonrankings.support.arclight.ArcLightSupport;
import sh.dominick.commissions.pixelmonrankings.views.util.BypassPacketHandler;
import sh.dominick.commissions.pixelmonrankings.views.util.SimpleDenyingPacketHandler;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

import static sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsLang.wrap;

public class PlayerStatisticsView extends Inventory implements ActionHandler {
    public static final int SLOT_BACK = 1;

    private SimpleChannelInboundHandler<IPacket<?>> packetHandler = new BypassPacketHandler();

    private Runnable onBack = null;

    private final PixelmonRankingsMod mod;
    private final IDataManager dataManager;
    private final UUID player;
    private final String playerName;
    private final Instant from, to;

    private PlayerStatisticsView(PixelmonRankingsMod mod, UUID player, String playerName, @Nullable Instant from, @Nullable Instant to) {
        super(27);

        this.mod = mod;
        this.dataManager = mod.dataManager();

        this.player = player;
        this.playerName = playerName;
        this.from = from;
        this.to = to;

        writeHeader();
        writeStatistics();
    }

    private void writeHeader() {
        ItemStack head = PlayerHeadUtil.getPlayerHead(
                player,
                dataManager.getGameProfile(player).texture(),
                1
        );

        head.setHoverName(wrap(mod.lang().playerStatisticsView.name,
                Placeholder.unparsed("player_name", playerName)));

        setItem(4, head);

        for (int i = 9; i < 18; i++) {
            ItemStack barrierItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            barrierItem.setHoverName(new StringTextComponent(""));

            setItem(i, barrierItem);
        }

        if (onBack != null) {
            ItemStack backHead = PlayerHeadUtil.getPlayerHead(UUID.randomUUID(), mod.lang().playerStatisticsView.backwardsItem.head, 1);
            backHead.setHoverName(wrap(mod.lang().playerStatisticsView.backwardsItem.name));
            ItemStackUtil.writeLore(backHead, wrap(mod.lang().playerStatisticsView.backwardsItem.lore));
            setItem(SLOT_BACK, backHead);
        }
    }

    private void writeStatistics() {
        for (Statistic statistic : Statistic.values()) {
            PixelmonRankingsLang.StatisticConfig statisticConfig = mod.lang().statistic(statistic);

            int position = statisticConfig.item.position + 18;

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(statisticConfig.item.material));
            ItemStack itemStack = new ItemStack(item);

            itemStack.setHoverName(wrap(mod.lang().playerStatisticsView.entryItem.name, Placeholder.unparsed("statistic_name", mod.lang().statistic(statistic).displayName)));
            itemStack.setCount(statisticConfig.item.amount);

            IDataManager.Key dataKey = new IDataManager.Key(player, statistic);

            long recordsTotal = dataManager.count(statistic, from, to);
            long ranking = dataManager.findPositionSorted(dataKey, from, to);
            if (ranking == -1) ranking = recordsTotal;

            String value = statistic.value(dataManager.aggregate(dataKey, from, to));

            ItemStackUtil.writeLore(itemStack, wrap(mod.lang().playerStatisticsView.entryItem.lore,
                    Placeholder.unparsed("position", ranking + ""),
                    Placeholder.unparsed("total", recordsTotal + ""),
                    Placeholder.unparsed("value", value)));

            setItem(position, itemStack);
        }
    }

    public void onBack(Runnable onBack) {
        this.onBack = onBack;
        writeHeader();
    }

    @Override
    public void onLocalClick(int slot) {
        if (slot != SLOT_BACK)
            return;

        onBack.run();
    }

    public static PlayerStatisticsView open(PixelmonRankingsMod mod, ServerPlayerEntity player, UUID target, String targetName, @Nullable Instant from, @Nullable Instant to) {
        PlayerStatisticsView inventory = new PlayerStatisticsView(mod, target, targetName, from, to);

        ArcLightSupport.sync(() -> {
            player.closeContainer();

            player.openMenu(new SimpleNamedContainerProvider((a1, a2, a3) -> {
                ChestContainer container = new ChestContainer(ContainerType.GENERIC_9x3, a1, a2, inventory, 3);
                inventory.packetHandler = new SimpleDenyingPacketHandler(player, inventory, container.containerId, 0, 27 - 1);
                return container;
            }, new StringTextComponent(targetName).withStyle(TextFormatting.BLUE)));
        });

        ChannelPipeline pipeline = player.connection.connection.channel().pipeline();

        try {
            pipeline.remove(PixelmonRankingsMod.MOD_ID + "/inventory_handler");
        } catch (NoSuchElementException ex) { }

        pipeline.addBefore("packet_handler", PixelmonRankingsMod.MOD_ID + "/inventory_handler", inventory.packetHandler);

        return inventory;
    }
}
