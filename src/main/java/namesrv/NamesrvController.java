package namesrv;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import remoting.NettyRemotingServer;

public class NamesrvController {
public RouteInfoManager routeInfoManager;
public KVConfigManager kvConfigManager;
public BrokerHousekeepingService brokerHousekeepingService;
public ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
public ExecutorService remotingExecutor;
public NettyRemotingServer remotingServer;
public int port;
public NamesrvController(int port) {
	this.port=port;
	this.routeInfoManager=new RouteInfoManager();
	this.kvConfigManager=new KVConfigManager();
	this.brokerHousekeepingService=new BrokerHousekeepingService(this);
    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
        public void run() {
            NamesrvController.this.routeInfoManager.scanNotActiveBroker();
        }
    }, 5, 10, TimeUnit.SECONDS);
    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
        public void run() {
            NamesrvController.this.kvConfigManager.printAllPeriodically();
        }
    }, 1, 10, TimeUnit.MINUTES);
    this.remotingExecutor =Executors.newFixedThreadPool(2);
    this.remotingServer=new NettyRemotingServer(port);
    this.remotingServer.registerDefaultProcessor(new NameSrvRequestProcessor(this), this.remotingExecutor);
    this.remotingServer.setChannelEventListener(this.brokerHousekeepingService);
}
public RouteInfoManager getRouteInfoManager() {
	return routeInfoManager;
}
public KVConfigManager getKvConfigManager() {
	return kvConfigManager;
}
public BrokerHousekeepingService getBrokerHousekeepingService() {
	return brokerHousekeepingService;
}
public ScheduledExecutorService getScheduledExecutorService() {
	return scheduledExecutorService;
}
public NettyRemotingServer getRemotingServer() {
	return remotingServer;
}
public int getPort() {
	return port;
}

}
