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
import net.minecraft.network.IPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.registries.ForgeRegistries;
import sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsConfig;
import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;
import sh.dominick.commissions.pixelmonrankings.Statistic;
import sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsLang;
import sh.dominick.commissions.pixelmonrankings.util.ItemStackUtil;
import sh.dominick.commissions.pixelmonrankings.views.util.ActionHandler;
import sh.dominick.commissions.pixelmonrankings.support.arclight.ArcLightSupport;
import sh.dominick.commissions.pixelmonrankings.views.util.BypassPacketHandler;
import sh.dominick.commissions.pixelmonrankings.views.util.SimpleDenyingPacketHandler;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsLang.wrap;

public class StatisticSelectView extends Inventory implements ActionHandler {
    public static final ITextComponent TITLE = new StringTextComponent("Select a Statistic").setStyle(
            Style.EMPTY.withColor(TextFormatting.BLUE)
    );

    private SimpleChannelInboundHandler<IPacket<?>> packetHandler = new BypassPacketHandler();

    private Statistic[] statisticPositions = new Statistic[64];
    private Consumer<Statistic> onSelect = (it) -> {};

    private StatisticSelectView(PixelmonRankingsMod mod) {
        super(9);

        for (Statistic statistic : Statistic.values()) {
            PixelmonRankingsLang.StatisticConfig config = mod.lang().statistic(statistic);

            int position = config.item.position;
            statisticPositions[position] = statistic;

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(config.item.material));
            ItemStack itemStack = new ItemStack(item);

            itemStack.setHoverName(wrap(config.item.displayName));
            itemStack.setCount(config.item.amount);

            ItemStackUtil.writeLore(itemStack, wrap(config.item.lore));

            setItem(position, itemStack);
        }
    }

    public void onSelect(Consumer<Statistic> onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    public void onLocalClick(int slot) {
        if (slot < 0 || slot > 8)
            return;

        onSelect.accept(statisticPositions[slot]);
    }

    public static StatisticSelectView open(PixelmonRankingsMod mod, ServerPlayerEntity player) {
        StatisticSelectView inventory = new StatisticSelectView(mod);

        ArcLightSupport.sync(() -> {
            player.closeContainer();

            player.openMenu(new SimpleNamedContainerProvider((a1, a2, a3) -> {
                ChestContainer container = new ChestContainer(ContainerType.GENERIC_9x1, a1, a2, inventory, 1);
                inventory.packetHandler = new SimpleDenyingPacketHandler(player, inventory, container.containerId, 0, 9 - 1);
                return container;
            }, wrap(mod.lang().statisticSelectView.title)));
        });

        ChannelPipeline pipeline = player.connection.connection.channel().pipeline();

        try {
            pipeline.remove(PixelmonRankingsMod.MOD_ID + "/inventory_handler");
        } catch (NoSuchElementException ex) { }

        pipeline.addBefore("packet_handler", PixelmonRankingsMod.MOD_ID + "/inventory_handler", inventory.packetHandler);

        return inventory;
    }
}
