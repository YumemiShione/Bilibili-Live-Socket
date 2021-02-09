package org.yumemi.bilibili.live.socket;

import io.netty.channel.ChannelFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

class ConnectFuture implements Future<SocketHandle>{
    private final ChannelFuture future;
    private final SocketHandle handle;
    ConnectFuture(ChannelFuture future,SocketHandle handle){
        this.future=future;
        this.handle=handle;
    }
    @Override
    public boolean cancel(boolean mayInterruptIfRunning){
        return future.cancel(mayInterruptIfRunning);
    }
    @Override
    public SocketHandle get() throws ExecutionException,InterruptedException{
        future.await();
        return getNow();
    }
    @Override
    public SocketHandle get(long timeout,TimeUnit unit) throws ExecutionException,InterruptedException,TimeoutException{
        if(future.await(timeout,unit)){
            return getNow();
        }else throw new TimeoutException();
    }
    private SocketHandle getNow() throws ExecutionException{
        Throwable cause=future.cause();
        if(cause==null){
            return handle;
        }else if(cause instanceof CancellationException){
            throw (CancellationException)cause;
        }else throw new ExecutionException(cause);
    }
    @Override
    public boolean isCancelled(){
        return future.isCancelled();
    }
    @Override
    public boolean isDone(){
        return future.isDone();
    }
}