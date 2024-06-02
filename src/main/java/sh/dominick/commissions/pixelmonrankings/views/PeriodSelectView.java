package sh.dominick.commissions.pixelmonrankings.views;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import sh.dominick.commissions.pixelmonrankings.PixelmonRankingsMod;
import sh.dominick.commissions.pixelmonrankings.support.arclight.ArcLightSupport;
import sh.dominick.commissions.pixelmonrankings.views.util.ActionHandler;
import sh.dominick.commissions.pixelmonrankings.views.util.BypassPacketHandler;
import sh.dominick.commissions.pixelmonrankings.views.util.SimpleDenyingPacketHandler;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static sh.dominick.commissions.pixelmonrankings.config.PixelmonRankingsLang.wrap;

public class PeriodSelectView extends Inventory implements ActionHandler {
    public static class Period {
        private final Instant start, end;
        private final ItemStack representation;
        private final int slot;

        public Period(Instant start, Instant end, ItemStack representation, int slot) {
            this.start = start;
            this.end = end;
            this.representation = representation;
            this.slot = slot;
        }

        public Instant start() {
            return start;
        }

        public Instant end() {
            return end;
        }
    }

    private SimpleChannelInboundHandler<IPacket<?>> packetHandler = new BypassPacketHandler();
    private Consumer<Period> onSelect = (it) -> {};

    private final Set<Period> periods;

    private PeriodSelectView(Set<Period> periods) {
        super(9);

        this.periods = periods;

        for (Period period : periods)
            setItem(period.slot, period.representation);
    }

    public void onSelect(Consumer<Period> onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    public void onLocalClick(int slot) {
        if (slot < 0 || slot > 8)
            return;

        Optional<Period> period = periods.stream()
                .filter((it) -> it.slot == slot)
                .findAny();

        if (!period.isPresent())
            return;

        onSelect.accept(period.get());
    }

    public static PeriodSelectView open(PixelmonRankingsMod mod, ServerPlayerEntity player, Set<Period> periods) {
        PeriodSelectView inventory = new PeriodSelectView(periods);

        ArcLightSupport.sync(() -> {
            player.closeContainer();

            player.openMenu(new SimpleNamedContainerProvider((a1, a2, a3) -> {
                ChestContainer container = new ChestContainer(ContainerType.GENERIC_9x1, a1, a2, inventory, 1);
                inventory.packetHandler = new SimpleDenyingPacketHandler(player, inventory, container.containerId, 0, 9 - 1);

                ChannelPipeline pipeline = player.connection.connection.channel().pipeline();

                try {
                    pipeline.remove(PixelmonRankingsMod.MOD_ID + "/inventory_handler");
                } catch (NoSuchElementException ex) { }

                pipeline.addBefore("packet_handler", PixelmonRankingsMod.MOD_ID + "/inventory_handler", inventory.packetHandler);

                return container;
            }, wrap(mod.lang().periodSelectView.title)));
        });

        return inventory;
    }
}
