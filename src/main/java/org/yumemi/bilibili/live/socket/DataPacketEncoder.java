package org.yumemi.bilibili.live.socket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

final class DataPacketEncoder extends MessageToByteEncoder<DataPacket>{
    @Override
    protected void encode(ChannelHandlerContext context,DataPacket message,ByteBuf out){
        out.writeInt(message.length)
                .writeShort(message.headerLength)
                .writeShort(message.protocolVersion)
                .writeInt(message.operation)
                .writeInt(message.sequenceId)
                .writeBytes(message.body);
    }
}