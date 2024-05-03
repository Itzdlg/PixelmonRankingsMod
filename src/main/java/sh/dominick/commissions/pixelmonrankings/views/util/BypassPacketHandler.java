package sh.dominick.commissions.pixelmonrankings.views.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.IPacket;

public class BypassPacketHandler extends SimpleChannelInboundHandler<IPacket<?>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IPacket msg) {
        ctx.fireChannelRead(msg);
    }
}