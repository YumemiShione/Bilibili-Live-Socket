package org.yumemi.bilibili.live.socket.callback;

import org.yumemi.bilibili.live.socket.SocketHandle;
import org.yumemi.bilibili.live.socket.SocketGroup;

public class ChannelEventCallbackAdapter implements ChannelEventCallback{
    @Override
    public void onConnect(SocketGroup group,SocketHandle handle){}
    @Override
    public void onConnectFailure(SocketGroup group,SocketHandle handle){}
    @Override
    public void onConnectSuccess(SocketGroup group,SocketHandle handle){}
    @Override
    public void onDisconnected(SocketGroup group,SocketHandle handle){}
    @Override
    public void onMessageReceive(SocketGroup group,SocketHandle handle,byte[] bytes){}
    @Override
    public void onViewerCountNotify(SocketGroup group,SocketHandle handle,int count){}
}