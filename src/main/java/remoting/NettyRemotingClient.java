package remoting;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSON;

import common.Pair;
import common.QueueData;
import common.ResponseFuture;
import config.SystemConfig;
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
import util.RemotingUtil;

public class NettyRemotingClient {
public ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();	
public List<String> namesrvAddrList=new ArrayList<String>();
public String namesrvAddrChoosed;
public EventLoopGroup eventLoopGroupWorker;
public Bootstrap bootstrap=new Bootstrap();
public ConcurrentHashMap<String, Channel> channelTables=new ConcurrentHashMap<String, Channel>();
public ConcurrentHashMap<Integer, ResponseFuture> responseTables=new ConcurrentHashMap<Integer, ResponseFuture>();
public ExecutorService callbackExecutor;
public ChannelEventListrener channelEventListener;
public DefaultEventExecutorGroup defaultEventExecutorGroup;
public HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>> processorTable=new HashMap<Integer, Pair<NettyRequestProcessor,ExecutorService>>();
public Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;
public List<RPCHook> rpcHooks=new ArrayList<RPCHook>();
public NettyRemotingClient() {
	this.eventLoopGroupWorker=new NioEventLoopGroup(1, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, String.format("NettyClientSelector_%d", this.threadIndex.incrementAndGet()));
        }
    });
    this.callbackExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, "NettyClientCallbackExecutor_" + this.threadIndex.incrementAndGet());
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
        this.scheduledExecutorService.shutdown();
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
    if (this.callbackExecutor != null) {
        try {
            this.callbackExecutor.shutdown();
        } catch (Exception e) {
            System.out.println("NettyRemotingClient shutdown exception");
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
public void doBeforeRpcHooks(String addr, RemotingCommand request) {
    if (rpcHooks.size() > 0) {
        for (RPCHook rpcHook: rpcHooks) {
            rpcHook.doBeforeRequest(addr, request);
        }
    }
}

public void doAfterRpcHooks(String addr, RemotingCommand request, RemotingCommand response) {
    if (rpcHooks.size() > 0) {
        for (RPCHook rpcHook: rpcHooks) {
            rpcHook.doAfterResponse(addr, request, response);
        }
    }
}
public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
    ExecutorService executorThis = executor;
    if (null == executor) {
        executorThis = this.callbackExecutor;
    }

    Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<NettyRequestProcessor, ExecutorService>(processor, executorThis);
    this.processorTable.put(requestCode, pair);
}
public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
    this.defaultRequestProcessor = new Pair<NettyRequestProcessor, ExecutorService>(processor, executor);
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
public void registerBroker() {
	if (this.namesrvAddrList!=null&&this.namesrvAddrList.size()>0) {
		this.namesrvAddrChoosed=this.namesrvAddrList.get(0);
		final RemotingCommand command=new RemotingCommand(CommandCode.REGISTER_BROKER,null);
		command.getExtFields().put("clusterName", SystemConfig.clusterName);
		command.getExtFields().put("brokerAddr", SystemConfig.localAddr);
		command.getExtFields().put("brokerName", SystemConfig.brokerName);
		command.getExtFields().put("brokerId", ""+SystemConfig.brokerId);
		command.setBody(JSON.toJSONString(new HashMap<String, QueueData>()).getBytes());
	    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
	        public void run() {
	        	try {
					invokeOneway(NettyRemotingClient.this.namesrvAddrChoosed, command);
				} catch (Exception e) {
					System.out.println("register broker error");
				}
	        }
	    }, 0, 20, TimeUnit.SECONDS);
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
public void processRequestCommend(final ChannelHandlerContext ctx,final RemotingCommand cmd) {
	System.out.println("processRequest:"+cmd);
    final Pair<NettyRequestProcessor, ExecutorService> matched = this.processorTable.get(cmd.getCode());
    final Pair<NettyRequestProcessor, ExecutorService> pair = null == matched ? this.defaultRequestProcessor : matched;
    if(pair!=null) {
    	Runnable run=new Runnable() {		
			public void run() {
				try {
					RemotingCommand response=pair.getObject1().processRequest(ctx, cmd);
					if(cmd.getRPC_ONEWAY()==0) {
						if(response!=null) {
							response.setOpaque(cmd.getOpaque());
							response.setRPC_TYPE(1);
							ctx.writeAndFlush(response);
						}
					}
				} catch (Exception e) {
					System.out.println("process request over, but response failed");
				}
				
			}
		};
		if(pair.getObject1().rejectRequest()) {
			RemotingCommand response = new RemotingCommand(CommandCode.SYSTEM_BUSY,
                    "[REJECTREQUEST]system busy, start flow control for a while");
			response.setOpaque(cmd.getOpaque());
			ctx.writeAndFlush(response);
			return;
		}
		pair.getObject2().execute(run);
    }else {
    	String error = " request type " + cmd.getCode() + " not supported";
        final RemotingCommand response =new RemotingCommand(CommandCode.REQUEST_CODE_NOT_SUPPORTED, error);
        response.setOpaque(cmd.opaque);
        ctx.writeAndFlush(response);
	}
}
public void processResponseCommand(final ChannelHandlerContext ctx,final RemotingCommand cmd) {
	ExecutorService executor=this.callbackExecutor;
	if(executor!=null) {
        try {
            executor.submit(new Runnable() {
                public void run() {
                    try {
                        System.out.println(RemotingUtil.parseChannelRemoteAddr(ctx.channel())+" response:"+cmd);
                        ResponseFuture responseFuture=responseTables.get(cmd.getOpaque());
                        if (responseFuture!=null) {
							responseFuture.setResponse(cmd);
							responseTables.remove(cmd.getOpaque());
						}
                    } catch (Throwable e) {
                        System.out.println("execute callback in executor exception, and callback throw");
                    } 
                }
            });
        } catch (Exception e) {
            System.out.println("execute callback in executor exception, maybe executor busy");
        }
	}
}
public void invokeOneway(final String addr,final RemotingCommand request) throws Exception {
	Channel channel=channelTables.get(addr);
	if(channel==null) {
		channel=this.createChannel(addr);
	}
	invokeOneway(channel, request);
}
public void invokeOneway(final Channel channel, final RemotingCommand request) throws Exception {
     try {
           channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
               public void operationComplete(ChannelFuture f) throws Exception {
                   if (!f.isSuccess()) {
                          System.out.println("send a request command to channel <" + RemotingUtil.parseChannelRemoteAddr(channel) + "> failed.");
                        }
                    }
                });
          } catch (Exception e) {
            System.out.println("write send a request command to channel <" + RemotingUtil.parseChannelRemoteAddr(channel) + "> failed.");
          }    
}
public RemotingCommand invokeSync(final String addr,final RemotingCommand request,long timeoutMillis) throws Exception {
	Channel channel=channelTables.get(addr);
	if(channel==null) {
		channel=this.createChannel(addr);
	}
	return invokeSync(channel, request,timeoutMillis);
}
public RemotingCommand invokeSync(final Channel channel, final RemotingCommand request,final long timeoutMillis) throws Exception {
	ResponseFuture responseFuture=new ResponseFuture(request.getOpaque());
	responseTables.put(request.getOpaque(), responseFuture);
	try {
		request.setRPC_ONEWAY(0);
        channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture f) throws Exception {
                if (!f.isSuccess()) {
                       System.out.println("send a request command to channel <" + RemotingUtil.parseChannelRemoteAddr(channel) + "> failed.");
                     }
                 }
             });
       } catch (Exception e) {
         System.out.println("write send a request command to channel <" + RemotingUtil.parseChannelRemoteAddr(channel) + "> failed.");
       }
    return responseFuture.waitResponse(timeoutMillis);
	
}
public ChannelEventListrener getChannelEventListener() {
	return channelEventListener;
}
public void setChannelEventListener(ChannelEventListrener channelEventListener) {
	this.channelEventListener = channelEventListener;
}

class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        if(msg.RPC_TYPE==0) {
        	processRequestCommend(ctx, msg);
        }else if (msg.RPC_TYPE==1) {
			processResponseCommand(ctx, msg);
		}   	
    }
}

class NettyConnectManageHandler extends ChannelDuplexHandler {
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) throws Exception {
        super.connect(ctx, remoteAddress, localAddress, promise);
        if (NettyRemotingClient.this.channelEventListener != null) {
        	  NettyRemotingClient.this.channelEventListener.onChannelConnect(RemotingUtil.parseChannelRemoteAddr(ctx.channel()), ctx.channel());
        }
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        closeChannel(ctx.channel());
        super.disconnect(ctx, promise);

        if (NettyRemotingClient.this.channelEventListener != null) {
        	  NettyRemotingClient.this.channelEventListener.onChannelClose(remoteAddress, ctx.channel());
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
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
                final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
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
        final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        closeChannel(ctx.channel());
        if (NettyRemotingClient.this.channelEventListener != null) {
            NettyRemotingClient.this.channelEventListener.onChannelException(remoteAddress, ctx.channel());
        }
    }
}
}



