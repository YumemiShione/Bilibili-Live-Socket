package org.yumemi.bilibili.live.socket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class DataPacketDecoder extends ByteToMessageDecoder{
    private final Inflater inflater=new Inflater();
    private final byte[] bufferBytes=new byte[DataPacket.DEFAULT_HEADER_LENGTH];
    private final ByteBuf buffer=Unpooled.wrappedBuffer(bufferBytes);
    @Override
    protected void decode(ChannelHandlerContext context,ByteBuf in,List<Object> out){
        DataPacket packet=decode(in);
        in.readBytes(packet.body);
        if(packet.protocolVersion==DataPacket.PROTOCOL_BUFFER){
            decode(packet.body,out);
        }else out.add(packet);
    }
    private void decode(byte[] in,List<Object> out){
        inflater.setInput(in);
        try{
            while(!inflater.finished()){
                if(inflater.inflate(bufferBytes)<bufferBytes.length){
                    break;
                }
                DataPacket packet=decode(Unpooled.wrappedBuffer(buffer));
                buffer.resetReaderIndex();
                inflater.inflate(packet.body);
                out.add(packet);
            }
        }catch(DataFormatException exception){
            exception.printStackTrace();
        }finally{
            inflater.reset();
        }
    }
    private DataPacket decode(ByteBuf in){
        int length=in.readInt();
        short headerLength=in.readShort();
        short protocolVersion=in.readShort();
        int operation=in.readInt();
        int sequenceId=in.readInt();
        byte[] body=new byte[length-headerLength];
        return new DataPacket(length,headerLength,protocolVersion,operation,sequenceId,body);
    }
}