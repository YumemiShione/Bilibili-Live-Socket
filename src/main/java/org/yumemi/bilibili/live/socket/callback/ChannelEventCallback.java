package org.yumemi.bilibili.live.socket.callback;

import org.yumemi.bilibili.live.socket.SocketHandle;
import org.yumemi.bilibili.live.socket.SocketGroup;

public interface ChannelEventCallback{
    //网络链路连接成功时回调
    void onConnect(SocketGroup group,SocketHandle handle);
    //协议握手不成功时回调,原因各种各样...
    void onConnectFailure(SocketGroup group,SocketHandle handle);
    //协议握手成功时回调
    void onConnectSuccess(SocketGroup group,SocketHandle handle);
    //网络链路连接断开时回调,意外断网/主动关闭等都会回调
    void onDisconnected(SocketGroup group,SocketHandle handle);
    //当网络链路尝试重新连接时回调,仅在链路有成功建立过至少一次连接时才有可能回调(有回调过onConnect方法的链路)
    void onReconnect(SocketGroup group,SocketHandle handle);
    //接收到消息时回调
    void onMessageReceive(SocketGroup group,SocketHandle handle,byte[] bytes);
    //接收到人气值数据包时回调,这同时也是定期发送心跳包时得到心跳响应的一个结果
    void onViewerCountNotify(SocketGroup group,SocketHandle handle,int count);
}