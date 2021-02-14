package org.yumemi.bilibili.live.socket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.yumemi.bilibili.live.socket.callback.ChannelEventCallback;
import org.yumemi.bilibili.live.socket.callback.ChannelEventCallbackAdapter;

public class SocketGroup{
    abstract class BaseHandler extends SimpleChannelInboundHandler<DataPacket>{
        protected final SocketHandle handle;
        protected final ChannelEventCallback callback;
        protected BaseHandler(SocketHandle handle,ChannelEventCallback callback){
            this.handle=handle;
            this.callback=callback;
        }
        protected abstract void channelInactive0(ChannelHandlerContext context);
        @Override
        public void channelInactive(ChannelHandlerContext context){
            //在SocketGroup关闭后(或者正在关闭中),尽可能的设置连接的关闭标记
            if(isClosed){
                handle.isClosed=true;
            }
            try{
                channelInactive0(context);
            }finally{
                if(handle.isClosed){
                    return;
                }
                //链路重连
                EventExecutor executor=context.executor();
                executor.schedule(new Runnable(){
                    @Override
                    public void run(){
                        doReconnect(executor,handle,callback);
                    }
                },1,TimeUnit.SECONDS);
            }
        }
        private void doReconnect(EventExecutor executor,SocketHandle handle,ChannelEventCallback callback){
            ChannelFuture channelFuture=null;
            synchronized(handle.lock){
                if(handle.isClosed){
                    return;
                }
                channelFuture=doConnect(handle,callback);
            }
            channelFuture.addListener(new ChannelFutureListener(){
                @Override
                public void operationComplete(ChannelFuture future){
                    if(future.isSuccess()||future.isCancelled()){
                        return;
                    }
                    executor.schedule(new Runnable(){
                        @Override
                        public void run(){
                            doReconnect(executor,handle,callback);
                        }
                    },3,TimeUnit.SECONDS);
                }
            });
            callback.onReconnect(SocketGroup.this,handle);
        }
        @Override
        public void userEventTriggered(ChannelHandlerContext context,Object event){
            if(event instanceof IdleStateEvent){
                if(((IdleStateEvent)event).state()==IdleState.READER_IDLE){
                    context.close();
                }
            }
        }
        protected void writePacket(ChannelHandlerContext context,DataPacket packet){
            ChannelFuture future=context.writeAndFlush(packet);
            future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
    }
    private final class HandshakeHandler extends BaseHandler{
        private HandshakeHandler(SocketHandle handle,ChannelEventCallback callback){
            super(handle,callback);
        }
        @Override
        public void channelActive(ChannelHandlerContext context){
            //当网络链路连接成功时,将后续连接运作所需Handler组件都添加上
            context.pipeline().addFirst(new IdleStateHandler(40,0,0),
                    new LengthFieldBasedFrameDecoder(Short.MAX_VALUE,
                            0,
                            DataPacket.LENGTH_FIELD_LENGTH,
                            -DataPacket.LENGTH_FIELD_LENGTH,
                            0),
                    new DataPacketDecoder(),
                    new DataPacketEncoder());
            //组件添加后,发送协议握手包
            writePacket(context,DataPacket.buildEnterRoomPacket(handle.roomId));
            callback.onConnect(SocketGroup.this,handle);
        }
        @Override
        public void channelInactive0(ChannelHandlerContext context){
            callback.onConnectFailure(SocketGroup.this,handle);
        }
        @Override
        protected void channelRead0(ChannelHandlerContext context,DataPacket message){
            if(message.operation==DataPacket.OPERATION_HANDSHAKE_SUCCESS){
                //协议握手成功后,用MessageHandler取代HandshakeHandler去完成之后的消息处理(逻辑分离)
                context.pipeline().replace(this,null,new MessageHandler(handle,callback));
                callback.onConnectSuccess(SocketGroup.this,handle);
            }else context.close();
        }
    }
    private final class MessageHandler extends BaseHandler{
        private MessageHandler(SocketHandle handle,ChannelEventCallback callback){
            super(handle,callback);
        }
        @Override
        public void channelInactive0(ChannelHandlerContext context){
            callback.onDisconnected(SocketGroup.this,handle);
        }
        private ScheduledFuture<?> heartbeatTaskFuture=null;
        @Override
        public void handlerAdded(final ChannelHandlerContext context){
            heartbeatTaskFuture=context.executor().scheduleAtFixedRate(new Runnable(){
                @Override
                public void run(){
                    writePacket(context,DataPacket.HEARTBEAT_PACKET);
                }
            },0,30,TimeUnit.SECONDS);
        }
        @Override
        public void handlerRemoved(ChannelHandlerContext context){
            heartbeatTaskFuture.cancel(false);
        }
        @Override
        protected void channelRead0(ChannelHandlerContext context,DataPacket message){
            if(message.operation==DataPacket.OPERATION_DATA){
                callback.onMessageReceive(SocketGroup.this,handle,message.body);
            }else if(message.operation==DataPacket.OPERATION_HEARTBEAT_RESPONSE){
                int count=0;
                count+=(message.body[3]&0xFF);
                count+=((message.body[2]&0xFF)<<8);
                count+=((message.body[1]&0xFF)<<16);
                count+=((message.body[0]&0xFF)<<24);
                callback.onViewerCountNotify(SocketGroup.this,handle,count);
            }
        }
    }
    public static final class Builder{
        private static final SocketAddress DEFAULT_REMOTE_ADDRESS=new InetSocketAddress("broadcastlv.chat.bilibili.com",2243);
        private static final ChannelEventCallback DEFAULT_CHANNEL_EVENT_CALLBACK=new ChannelEventCallbackAdapter();
        private static final int DEFAULT_CONNECT_TIMEOUT=5000;
        private SocketAddress remoteAddress=DEFAULT_REMOTE_ADDRESS;
        private ChannelEventCallback channelEventCallback=DEFAULT_CHANNEL_EVENT_CALLBACK;
        private int connectTimeout=DEFAULT_CONNECT_TIMEOUT;
        public Builder(){}
        public Builder setChannelEventCallback(ChannelEventCallback callback){
            if(callback==null){
                channelEventCallback=DEFAULT_CHANNEL_EVENT_CALLBACK;
            }
            channelEventCallback=callback;
            return this;
        }
        public Builder setConnectTimeout(int connectTimeout){
            if(connectTimeout<0){
                throw new IllegalArgumentException("connectTimeout < 0");
            }
            this.connectTimeout=connectTimeout;
            return this;
        }
        public Builder setRemoteAddress(SocketAddress remoteAddress){
            if(remoteAddress==null){
                throw new NullPointerException("connectAddress == null");
            }
            this.remoteAddress=remoteAddress;
            return this;
        }
        public SocketGroup build(){
            return new SocketGroup(this);
        }
    }
    private final NioEventLoopGroup eventLoopGroup=new NioEventLoopGroup();
    private final Bootstrap bootstrap;
    private final ChannelEventCallback channelEventCallback;
    private final Object lock=new Object();
    private volatile boolean isClosed=false;
    private SocketGroup(Builder builder){
        bootstrap=new Bootstrap().channel(NioSocketChannel.class)
                .group(eventLoopGroup)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,builder.connectTimeout)
                .remoteAddress(builder.remoteAddress);
        channelEventCallback=builder.channelEventCallback;
    }
    public Future<?> close(){
        synchronized(lock){
            if(isClosed){
                return null;
            }else isClosed=true;
            Future<?> future=eventLoopGroup.shutdownGracefully();
            return future;
        }
    }
    public Future<SocketHandle> connect(int roomId){
        return connect(roomId,channelEventCallback);
    }
    public Future<SocketHandle> connect(int roomId,ChannelEventCallback callback){
        if(roomId<=0){
            throw new IllegalArgumentException("roomId <= 0");
        }
        synchronized(lock){
            if(isClosed){
                throw new IllegalStateException("Group closed");
            }
            SocketHandle handle=new SocketHandle(roomId);
            return new ConnectFuture(doConnect(handle,callback),handle);
        }
    }
    private ChannelFuture doConnect(final SocketHandle handle,final ChannelEventCallback callback){
        Bootstrap bootstrap=this.bootstrap.clone();
        ChannelFuture future=bootstrap.handler(new HandshakeHandler(handle,callback)).connect();
        handle.channel=future.channel();
        return future;
    }
}