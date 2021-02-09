package org.yumemi.bilibili.live.socket.callback;

import org.yumemi.bilibili.live.socket.SocketHandle;
import org.yumemi.bilibili.live.socket.SocketGroup;

public interface ChannelEventCallback{
    void onConnect(SocketGroup group,SocketHandle handle);
    void onConnectFailure(SocketGroup group,SocketHandle handle);
    void onConnectSuccess(SocketGroup group,SocketHandle handle);
    void onDisconnected(SocketGroup group,SocketHandle handle);
    void onMessageReceive(SocketGroup group,SocketHandle handle,byte[] bytes);
    void onViewerCountNotify(SocketGroup group,SocketHandle handle,int count);
}