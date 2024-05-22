package sh.dominick.commissions.pixelmonrankings.views;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;
import sh.dominick.commissions.pixelmonrankings.Statistic;
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

public class RankedStatisticsView extends Inventory implements ActionHandler {
    private static final int PAGE_SIZE = 9 * 3;
    private static final int PAGE_START = 18;

    private static final int SLOT_SELF = 4;
    private static final int SLOT_PAGE_BACKWARD = 10;
    private static final int SLOT_PAGE_FORWARD = 16;

    private SimpleChannelInboundHandler<IPacket<?>> packetHandler = new BypassPacketHandler();

    private final PixelmonRankingsMod mod;
    private final IDataManager dataManager;

    private final ServerPlayerEntity self;
    private final Statistic statistic;
    private final Instant from, to;

    private final IDataManager.Entry[] records;
    private final long recordsTotal;

    private final int maxPage;
    private int page;

    private RankedStatisticsView(PixelmonRankingsMod mod, ServerPlayerEntity self, Statistic statistic, Instant from, Instant to) {
        super(9 * 5);

        this.mod = mod;
        this.dataManager = mod.dataManager();

        this.self = self;
        this.statistic = statistic;
        this.from = from;
        this.to = to;

        this.recordsTotal = dataManager.count(statistic, from, to);
        this.records = dataManager.sort(statistic, from, to, PAGE_SIZE * 5);

        this.maxPage = (int) Math.ceil((double) this.records.length / PAGE_SIZE) - 1;
        this.page = 0;

        writeHeader();
        writeCurrentPage();
        writePageControls();
    }

    public RankedStatisticsView atPage(int page) {
        this.page = page;

        writeHeader();
        writeCurrentPage();
        writePageControls();

        return this;
    }

    public int page() {
        return page;
    }

    private void writeHeader() {
        ItemStack selfHead = PlayerHeadUtil.getPlayerHead(
                self.getUUID(),
                dataManager.getGameProfile(self.getUUID()).texture(),
                1
        );

        IDataManager.Key dataKey = new IDataManager.Key(self.getUUID(), statistic);

        long selfRanking = dataManager.findPositionSorted(dataKey, from, to);
        if (selfRanking == -1) selfRanking = recordsTotal;

        String selfValue = statistic.value(dataManager.aggregate(dataKey, from, to));

        selfHead.setHoverName(wrap(mod.lang().rankedStatisticView.youEntryItem.name));

        ItemStackUtil.writeLore(selfHead, wrap(
                mod.lang().rankedStatisticView.youEntryItem.lore,
                Placeholder.unparsed("position", selfRanking + ""),
                Placeholder.unparsed("total", recordsTotal + ""),
                Placeholder.unparsed("value", selfValue)
        ));

        setItem(SLOT_SELF, selfHead);

        for (int i = 9; i < PAGE_START; i++) {
            ItemStack barrierItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            barrierItem.setHoverName(new StringTextComponent(""));

            setItem(i, barrierItem);
        }
    }

    private void writeCurrentPage() {
        for (int i = PAGE_START; i < PAGE_SIZE + PAGE_START; i++)
            setItem(i, new ItemStack(Items.AIR));

        int start = PAGE_SIZE * page;
        int end = Math.min(records.length, PAGE_SIZE * (page + 1));

        for (int i = start; i < end; i++) {
            int slot = (i % PAGE_SIZE) + PAGE_START;

            IDataManager.Entry record = records[i];
            IDataManager.CachedGameProfile gameProfile = dataManager.getGameProfile(record.key().player());

            ItemStack head = PlayerHeadUtil.getPlayerHead(
                    record.key().player(),
                    gameProfile.texture(),
                    1
            );

            head.setHoverName(wrap(mod.lang().rankedStatisticView.entryItem.name,
                    Placeholder.unparsed("player_name", gameProfile.playerName()))
            );

            String value = statistic.value(record.value());

            ItemStackUtil.writeLore(head, wrap(
                    mod.lang().rankedStatisticView.entryItem.lore,
                    Placeholder.unparsed("position", (i + 1) + ""),
                    Placeholder.unparsed("total", recordsTotal + ""),
                    Placeholder.unparsed("value", value)
            ));

            setItem(slot, head);
        }
    }

    private void writePageControls() {
        ItemStack barrierItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        barrierItem.setHoverName(new StringTextComponent(""));

        ItemStack backwardHead = PlayerHeadUtil.getPlayerHead(UUID.randomUUID(), mod.lang().rankedStatisticView.backwardsItem.head, 1);
        backwardHead.setHoverName(wrap(mod.lang().rankedStatisticView.backwardsItem.name));
        ItemStackUtil.writeLore(backwardHead, wrap(mod.lang().rankedStatisticView.backwardsItem.lore));

        if (page - 1 >= 0) setItem(SLOT_PAGE_BACKWARD, backwardHead);
        else setItem(SLOT_PAGE_BACKWARD, barrierItem.copy());

        ItemStack forwardHead = PlayerHeadUtil.getPlayerHead(UUID.randomUUID(), mod.lang().rankedStatisticView.forwardsItem.head, 1);
        forwardHead.setHoverName(wrap(mod.lang().rankedStatisticView.forwardsItem.name));
        ItemStackUtil.writeLore(forwardHead, wrap(mod.lang().rankedStatisticView.forwardsItem.lore));

        if (page + 1 <= maxPage) setItem(SLOT_PAGE_FORWARD, forwardHead);
        else setItem(SLOT_PAGE_FORWARD, barrierItem.copy());
    }

    @Override
    public void onLocalClick(int slot) {
        if (slot == SLOT_PAGE_BACKWARD && page - 1 >= 0) {
            page -= 1;

            writeCurrentPage();
            writePageControls();
            return;
        }

        if (slot == SLOT_PAGE_FORWARD && page + 1 <= maxPage) {
            page += 1;

            writeCurrentPage();
            writePageControls();
            return;
        }

        if (slot == SLOT_SELF) {
            PlayerStatisticsView.open(mod, self, self.getUUID(), self.getName().getString(), from, to).onBack(() -> {
                self.closeContainer();
                RankedStatisticsView.open(mod, self, statistic, from, to).atPage(page);
            });

            return;
        }

        if (slot < PAGE_START)
            return;

        int recordIndex = (slot - PAGE_START) + (PAGE_SIZE * page);
        if (recordIndex < 0 || recordIndex >= records.length)
            return;

        IDataManager.Entry record = records[recordIndex];

        UUID player = record.key().player();
        String playerName = player.toString();
        if (dataManager.getGameProfile(player) != null)
            playerName = dataManager.getGameProfile(player).playerName();

        String finalPlayerName = playerName;

        ArcLightSupport.sync(() -> {
            self.closeContainer();
            PlayerStatisticsView.open(mod, self, player, finalPlayerName, from, to).onBack(() -> {
                self.closeContainer();
                RankedStatisticsView.open(mod, self, statistic, from, to).atPage(page);
            });
        });
    }

    public static RankedStatisticsView open(PixelmonRankingsMod mod, ServerPlayerEntity player, Statistic statistic, @Nullable Instant from, @Nullable Instant to) {
        RankedStatisticsView inventory = new RankedStatisticsView(mod, player, statistic, from, to);

        NetworkManager connection = player.connection.connection;
        ChannelPipeline pipeline = connection.channel().pipeline();

        ArcLightSupport.sync(() -> {
            player.closeContainer();

            player.openMenu(new SimpleNamedContainerProvider((a1, a2, a3) -> {
                ChestContainer container = new ChestContainer(ContainerType.GENERIC_9x5, a1, a2, inventory, 5);
                inventory.packetHandler = new SimpleDenyingPacketHandler(player, inventory, container.containerId, 0, 45 - 1);

                return container;
            }, wrap(mod.lang().rankedStatisticView.title, Placeholder.unparsed("statistic_name", mod.lang().statistic(statistic).displayName))));
        });

        try {
            pipeline.remove(PixelmonRankingsMod.MOD_ID + "/inventory_handler");
        } catch (NoSuchElementException ex) { }

        pipeline.addBefore("packet_handler", PixelmonRankingsMod.MOD_ID + "/inventory_handler", inventory.packetHandler);

        return inventory;
    }
}
