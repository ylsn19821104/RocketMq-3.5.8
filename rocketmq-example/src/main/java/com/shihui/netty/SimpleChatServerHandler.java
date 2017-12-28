package com.shihui.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Created by hongxp on 2017/12/18.
 */
public class SimpleChatServerHandler extends SimpleChannelInboundHandler<String> {
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel inComing = ctx.channel();
        for (Channel channel : channels) {
            channel.writeAndFlush("[SERVER]-" + inComing.remoteAddress() + "加入\n");
        }
        channels.add(inComing);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel inComing = ctx.channel();
        for (Channel channel : channels) {
            channel.writeAndFlush("[SERVER]-" + inComing.remoteAddress() + "离开\n");
        }
        channels.remove(inComing);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        Channel inComing = ctx.channel();
        for (Channel channel : channels) {
            if (channel != inComing) {
                channel.writeAndFlush("[" + inComing.remoteAddress() + "]" + msg + "\n");
            } else {
                channel.writeAndFlush("[you]-" + msg + "\n");
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel inComing = ctx.channel();
        System.err.println("SimpleChatClient:" + inComing.remoteAddress() + "在线");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel inComing = ctx.channel();
        System.err.println("SimpleChatClient:" + inComing.remoteAddress() + "掉线");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel inComing = ctx.channel();
        System.err.println("SimpleChatClient:" + inComing.remoteAddress() + "异常");
        cause.printStackTrace();
        ctx.close();
    }
}
