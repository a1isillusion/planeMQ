package remoting;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import common.Pair;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

public class NettyRemotingClient {
public List<String> namesrvAddrList=new ArrayList<String>();
public String namesrvAddrChoosed;
public EventLoopGroup eventLoopGroupWorker;
public Bootstrap bootstrap=new Bootstrap();
public ConcurrentHashMap<String, Channel> channelTables=new ConcurrentHashMap<String, Channel>();
public Timer timer = new Timer("ClientHouseKeepingService", true);
public ExecutorService publicExecutor;
public ExecutorService callbackExecutor;
public DefaultChannelEventListener channelEventListener;
public DefaultEventExecutorGroup defaultEventExecutorGroup;
public HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>> processorTables=new HashMap<Integer, Pair<NettyRequestProcessor,ExecutorService>>();
public Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;
public List<RPCHook> rpcHooks=new ArrayList<RPCHook>();
public NettyRemotingClient() {
	this.channelEventListener=new DefaultChannelEventListener();
	this.eventLoopGroupWorker=new NioEventLoopGroup(1, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, String.format("NettyClientSelector_%d", this.threadIndex.incrementAndGet()));
        }
    });
    this.publicExecutor = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, "NettyClientPublicExecutor_" + this.threadIndex.incrementAndGet());
        }
    });
    this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(4, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
             return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
        }
    });
    this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_KEEPALIVE, false)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .option(ChannelOption.SO_SNDBUF, 1024*10)
            .option(ChannelOption.SO_RCVBUF, 1024*10)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(
                        defaultEventExecutorGroup,
                        new NettyEncoder(),
                        new NettyDecoder(),
                        new IdleStateHandler(0, 0, 60),
                        new NettyConnectManageHandler(),
                        new NettyClientHandler());
                }
    });

}
public void shutdown() {
    try {
        this.timer.cancel();
        for (Channel c : this.channelTables.values()) {
            this.closeChannel(c);
        }
        this.channelTables.clear();
        this.eventLoopGroupWorker.shutdownGracefully();
        if (this.defaultEventExecutorGroup != null) {
            this.defaultEventExecutorGroup.shutdownGracefully();
        }
    } catch (Exception e) {
        System.out.println("NettyRemotingClient shutdown exception");
    }

    if (this.publicExecutor != null) {
        try {
            this.publicExecutor.shutdown();
        } catch (Exception e) {
            System.out.println("NettyRemotingServer shutdown exception");
        }
    }
}
public void closeChannel(Channel channel) {
	if(channel==null) {
		return;
	}
	boolean removeItemFromTable=true;
	Channel c=null;
	String addrRemote=null;
	for(Map.Entry<String, Channel> entry:channelTables.entrySet()) {
		String key=entry.getKey();
		Channel value=entry.getValue();
		if(value!=null) {
			if(value==channel) {
				c=value;
				addrRemote=key;
				break;
			}
		}
	}
	if(null==c) {
		removeItemFromTable=false;
	}
	if(removeItemFromTable) {
		this.channelTables.remove(addrRemote);
		channel.close();
	}
}
public void registerRPCHook(RPCHook rpcHook) {
	if(rpcHook!=null&&!rpcHooks.contains(rpcHook)) {
		rpcHooks.add(rpcHook);
	}
}
public void updateNameServerAddressList(List<String> addrs) {
    List<String> old = this.namesrvAddrList;
    boolean update = false;
    if (!addrs.isEmpty()) {
        if (null == old) {
            update = true;
        } else if (addrs.size() != old.size()) {
            update = true;
        } else {
            for (int i = 0; i < addrs.size() && !update; i++) {
                if (!old.contains(addrs.get(i))) {
                    update = true;
                }
            }
        }
        if (update) {
            Collections.shuffle(addrs);
            this.namesrvAddrList=addrs;
        }
    }
}
public Channel createChannel(String addr) throws Exception{
	Channel channel=this.channelTables.get(addr);
	if(channel!=null) {
		channel.close();
		channelTables.remove(addr);
	}
	ChannelFuture channelFuture=this.bootstrap.connect(addr.split(":")[0], Integer.parseInt(addr.split(":")[1])).sync();
	channel=channelFuture.channel();
	this.channelTables.put(addr, channel);
	return channel;
}
public void processResponseCommand(ChannelHandlerContext ctx,RemotingCommand cmd) {
	System.out.println("processResponse:"+cmd);
}
public void invokeOneway(final Channel channel, final RemotingCommand request) throws Exception {
     try {
           channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
               public void operationComplete(ChannelFuture f) throws Exception {
                   if (!f.isSuccess()) {
                          System.out.println("send a request command to channel <" + channel.remoteAddress() + "> failed.");
                        }
                    }
                });
          } catch (Exception e) {
            System.out.println("write send a request command to channel <" + channel.remoteAddress() + "> failed.");
            throw new Exception(channel.remoteAddress().toString(), e);
          }    
}

class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        processResponseCommand(ctx, msg);
    }
}

class NettyConnectManageHandler extends ChannelDuplexHandler {
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) throws Exception {
        super.connect(ctx, remoteAddress, localAddress, promise);
        if (NettyRemotingClient.this.channelEventListener != null) {
        	  NettyRemotingClient.this.channelEventListener.onChannelConnect(remoteAddress.toString(), ctx.channel());
        }
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        final String remoteAddress = ctx.channel().remoteAddress().toString();
        closeChannel(ctx.channel());
        super.disconnect(ctx, promise);

        if (NettyRemotingClient.this.channelEventListener != null) {
        	  NettyRemotingClient.this.channelEventListener.onChannelClose(remoteAddress, ctx.channel());
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        final String remoteAddress = ctx.channel().remoteAddress().toString();
        closeChannel(ctx.channel());
        super.close(ctx, promise);
        if (NettyRemotingClient.this.channelEventListener != null) {
        	  NettyRemotingClient.this.channelEventListener.onChannelClose(remoteAddress, ctx.channel());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddress = ctx.channel().remoteAddress().toString();
                closeChannel(ctx.channel());
                if (NettyRemotingClient.this.channelEventListener != null) {
                	  NettyRemotingClient.this.channelEventListener.onChannelIdle(remoteAddress, ctx.channel());
                }
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String remoteAddress = ctx.channel().remoteAddress().toString();
        closeChannel(ctx.channel());
        if (NettyRemotingClient.this.channelEventListener != null) {
            NettyRemotingClient.this.channelEventListener.onChannelException(remoteAddress, ctx.channel());
        }
    }
}
}



