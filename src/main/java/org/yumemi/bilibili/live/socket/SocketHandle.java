package org.yumemi.bilibili.live.socket;

import io.netty.channel.Channel;

public final class SocketHandle{
    final int roomId;
    final Object lock=new Object();
    Channel channel=null;
    boolean isClosed=false;
    SocketHandle(int roomId){
        this.roomId=roomId;
    }
    public void close(){
        synchronized(lock){
            if(isClosed){
                return;
            }else isClosed=true;
            channel.close();
        }
    }
    public int getRoomId(){
        return roomId;
    }
    public boolean isClosed(){
        synchronized(lock){
            return isClosed;
        }
    }
}