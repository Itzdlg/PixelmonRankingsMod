package sh.dominick.commissions.pixelmonrankings.views.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.network.play.server.SSetSlotPacket;

public class SimpleDenyingPacketHandler extends SimpleChannelInboundHandler<IPacket<?>> {
    protected final ServerPlayerEntity player;
    protected final Inventory inventory;
    protected final int containerId;
    protected final int minSlotNum, maxSlotNum;
    public SimpleDenyingPacketHandler(ServerPlayerEntity player, Inventory inventory, int containerId, int minSlotNum, int maxSlotNum) {
        this.player = player;
        this.inventory = inventory;
        this.containerId = containerId;
        this.minSlotNum = minSlotNum;
        this.maxSlotNum = maxSlotNum;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IPacket msg) {
        if (!(msg instanceof CClickWindowPacket)) {
            ctx.fireChannelRead(msg);
            return;
        }

        CClickWindowPacket packet = (CClickWindowPacket) msg;

        int slot = packet.getSlotNum();
        if (packet.getContainerId() != containerId
                || slot < minSlotNum || slot > maxSlotNum) {
            ctx.fireChannelRead(msg);
            return;
        }

        player.connection.send(new SSetSlotPacket(-1, -1, new ItemStack(Items.AIR)));
        player.connection.send(new SSetSlotPacket(containerId, slot, inventory.getItem(slot)));

        if (!(inventory instanceof ActionHandler)) {
            ctx.fireChannelRead(msg);
            return;
        }

        ActionHandler actionHandler = (ActionHandler) inventory;
        actionHandler.onLocalClick(slot);
    }
}
