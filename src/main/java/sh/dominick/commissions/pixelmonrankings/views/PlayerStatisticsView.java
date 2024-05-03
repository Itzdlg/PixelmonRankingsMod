package sh.dominick.commissions.pixelmonrankings.views;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
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
import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsConfig;
import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;
import sh.dominick.commissions.pixelmonrankings.Statistic;
import sh.dominick.commissions.pixelmonrankings.data.IDataManager;
import sh.dominick.commissions.pixelmonrankings.util.ItemStackUtil;
import sh.dominick.commissions.pixelmonrankings.util.PlayerHeadUtil;
import sh.dominick.commissions.pixelmonrankings.views.util.ActionHandler;
import sh.dominick.commissions.pixelmonrankings.views.util.BypassPacketHandler;
import sh.dominick.commissions.pixelmonrankings.views.util.SimpleDenyingPacketHandler;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

public class PlayerStatisticsView extends Inventory implements ActionHandler {
    public static final int SLOT_BACK = 1;

    private SimpleChannelInboundHandler<IPacket<?>> packetHandler = new BypassPacketHandler();

    private Runnable onBack = null;

    private final PixelmonRankingsMod mod;
    private final PixelmonRankingsConfig config;
    private final IDataManager dataManager;
    private final UUID player;
    private final String playerName;
    private final Instant from, to;

    private PlayerStatisticsView(PixelmonRankingsMod mod, UUID player, String playerName, @Nullable Instant from, @Nullable Instant to) {
        super(27);

        this.mod = mod;
        this.config = mod.config();
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

        head.setHoverName(new StringTextComponent(playerName).withStyle(Style.EMPTY.withColor(TextFormatting.YELLOW).withItalic(false)));

        setItem(4, head);

        for (int i = 9; i < 18; i++) {
            ItemStack barrierItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            barrierItem.setHoverName(new StringTextComponent(""));

            setItem(i, barrierItem);
        }

        if (onBack != null) {
            ItemStack backHead = PlayerHeadUtil.getPlayerHead(UUID.randomUUID(), "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjBjZmI0ZjM3Y2NlZmQwNTg5YzU1NzhiNTQxZTdhZjkyM2UzZTY0MjBhZGE2YmU0NDNkZmFkY2IwNWJhZTE5NCJ9fX0=", 1);
            backHead.setHoverName(new StringTextComponent("Go Back").withStyle(TextFormatting.YELLOW));
            setItem(SLOT_BACK, backHead);
        }
    }

    private void writeStatistics() {
        for (Statistic statistic : Statistic.values()) {
            PixelmonRankingsConfig.StatisticConfig statisticConfig = config.statistic(statistic);

            int position = statisticConfig.itemPosition().get() + 18;

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(statisticConfig.itemMaterial().get()));
            ItemStack itemStack = new ItemStack(item);

            itemStack.setHoverName(new StringTextComponent(statisticConfig.itemDisplayName().get()).setStyle(Style.EMPTY.withColor(Color.fromLegacyFormat(TextFormatting.AQUA))));
            itemStack.setCount(statisticConfig.itemAmount().get());

            IDataManager.Key dataKey = new IDataManager.Key(player, statistic);

            long recordsTotal = dataManager.count(statistic, from, to);
            long ranking = dataManager.findPositionSorted(dataKey, from, to);
            if (ranking == -1) ranking = recordsTotal;

            String value = statistic.value(dataManager.aggregate(dataKey, from, to));

            ItemStackUtil.writeLore(itemStack,
                    new StringTextComponent(""),
                    new StringTextComponent("Ranked ").withStyle(TextFormatting.GRAY).append(new StringTextComponent(ranking + "").withStyle(TextFormatting.GOLD).append(new StringTextComponent(" of " + recordsTotal).withStyle(TextFormatting.GRAY))),
                    new StringTextComponent("with ").withStyle(TextFormatting.GRAY).append(new StringTextComponent(value).withStyle(TextFormatting.WHITE)));

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

        player.closeContainer();

        player.openMenu(new SimpleNamedContainerProvider((a1, a2, a3) -> {
            ChestContainer container = new ChestContainer(ContainerType.GENERIC_9x3, a1, a2, inventory, 3);
            inventory.packetHandler = new SimpleDenyingPacketHandler(player, inventory, container.containerId, 0, 27 - 1);
            return container;
        }, new StringTextComponent(targetName).withStyle(TextFormatting.BLUE)));

        ChannelPipeline pipeline = player.connection.connection.channel().pipeline();

        try {
            pipeline.remove(PixelmonRankingsMod.MOD_ID + "/inventory_handler");
        } catch (NoSuchElementException ex) { }

        pipeline.addBefore("packet_handler", PixelmonRankingsMod.MOD_ID + "/inventory_handler", inventory.packetHandler);

        return inventory;
    }
}
