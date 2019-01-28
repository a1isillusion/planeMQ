package broker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import remoting.NettyRemotingServer;
import remoting.NettyRequestProcessor;
import store.MessageStore;

public class BrokerController {
public ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
public ExecutorService brokerExecutor;
public NettyRemotingServer remotingServer;
public MessageStore messageStore;
public NettyRequestProcessor requestProcessor;
public int port;
public BrokerController(int port) {
	this.port=port;
	this.messageStore=new MessageStore();
	this.brokerExecutor=Executors.newFixedThreadPool(4);
	this.remotingServer=new NettyRemotingServer(this.port);
	this.requestProcessor=new BrokerRequestProcessor(this);
	this.remotingServer.registerDefaultProcessor(this.requestProcessor, brokerExecutor);
}
}
