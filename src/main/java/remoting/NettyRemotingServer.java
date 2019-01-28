package remoting;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import common.Pair;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import util.RemotingUtil;

public class NettyRemotingServer {
public ServerBootstrap serverBootstrap=new ServerBootstrap();
public EventLoopGroup eventLoopGroupSelector;
public EventLoopGroup eventLoopGroupBoss;
public ExecutorService publicExecutor;
public ChannelEventListrener channelEventListener;
public Timer timer=new Timer("ServerHouseKeepingService", true);
public DefaultEventExecutorGroup defaultEventExecutorGroup;
public HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>> processorTable=new HashMap<Integer, Pair<NettyRequestProcessor,ExecutorService>>();
public Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;
public int port=0;
public NettyRemotingServer(int port) {
	this.port=port;
    this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(4, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
             return new Thread(r, "NettyServerWorkerThread_" + this.threadIndex.incrementAndGet());
        }
    });
    this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, String.format("NettyNIOBoss_%d", this.threadIndex.incrementAndGet()));
        }
    });

    this.eventLoopGroupSelector = new NioEventLoopGroup(1, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        private int threadTotal = 1;
        public Thread newThread(Runnable r) {
            return new Thread(r, String.format("NettyServerNIOSelector_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
        }
    });
    this.publicExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, "NettyServerPublicExecutor_" + this.threadIndex.incrementAndGet());
        }
    });
	this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
    .channel(NioServerSocketChannel.class)
    .option(ChannelOption.SO_BACKLOG, 1024)
    .option(ChannelOption.SO_REUSEADDR, true)
    .option(ChannelOption.SO_KEEPALIVE, false)
    .childOption(ChannelOption.TCP_NODELAY, true)
    .childOption(ChannelOption.SO_SNDBUF, 1024*10)
    .childOption(ChannelOption.SO_RCVBUF, 1024*10)
    .localAddress(new InetSocketAddress(this.port))
    .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline()
                .addLast(defaultEventExecutorGroup,
                    new NettyEncoder(),
                    new NettyDecoder(),
                    new IdleStateHandler(0, 0, 3000),
                    new NettyConnectManageHandler(),
                    new NettyServerHandler()
                );
        }
    });
    try {
        this.serverBootstrap.bind().sync();
    } catch (InterruptedException e) {
        throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e);
    }
}
public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
    ExecutorService executorThis = executor;
    if (null == executor) {
        executorThis = this.publicExecutor;
    }

    Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<NettyRequestProcessor, ExecutorService>(processor, executorThis);
    this.processorTable.put(requestCode, pair);
}
public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
    this.defaultRequestProcessor = new Pair<NettyRequestProcessor, ExecutorService>(processor, executor);
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
public ChannelEventListrener getChannelEventListener() {
	return channelEventListener;
}
public void setChannelEventListener(ChannelEventListrener channelEventListener) {
	this.channelEventListener = channelEventListener;
}

class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        processRequestCommend(ctx, msg);
    }
}
class NettyConnectManageHandler extends ChannelDuplexHandler {
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress =RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        System.out.println("NETTY SERVER PIPELINE: channelRegistered, the channel["+remoteAddress+"]");
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        System.out.println("NETTY SERVER PIPELINE: channelUnregistered, the channel["+remoteAddress+"]");
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        System.out.println("NETTY SERVER PIPELINE: channelActive, the channel["+remoteAddress+"]");
        super.channelActive(ctx);

        if (NettyRemotingServer.this.channelEventListener != null) {
            NettyRemotingServer.this.channelEventListener.onChannelConnect(remoteAddress, ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        System.out.println("NETTY SERVER PIPELINE: channelInactive, the channel["+remoteAddress+"]");
        super.channelInactive(ctx);

        if (NettyRemotingServer.this.channelEventListener != null) {
            NettyRemotingServer.this.channelEventListener.onChannelClose(remoteAddress, ctx.channel());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
                System.out.println("NETTY SERVER PIPELINE: IDLE exception ["+remoteAddress+"]");
                ctx.channel().close();
                if (NettyRemotingServer.this.channelEventListener != null) {
                	NettyRemotingServer.this.channelEventListener.onChannelIdle(remoteAddress, ctx.channel());
                }
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        System.out.println("NETTY SERVER PIPELINE: exceptionCaught "+remoteAddress);
        System.out.println("NETTY SERVER PIPELINE: exceptionCaught exception.");

        if (NettyRemotingServer.this.channelEventListener != null) {
            NettyRemotingServer.this.channelEventListener.onChannelException(remoteAddress, ctx.channel());
        }
        ctx.channel().close();
    }
}
}
