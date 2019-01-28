package namesrv;

import io.netty.channel.Channel;
import remoting.ChannelEventListrener;

public class BrokerHousekeepingService implements ChannelEventListrener {  
    private final NamesrvController namesrvController;

    public BrokerHousekeepingService(NamesrvController namesrvController) {
        this.namesrvController = namesrvController;
    }

	public void onChannelConnect(String remoteAddr, Channel channel) {
	}

	public void onChannelClose(String remoteAddr, Channel channel) {
		this.namesrvController.getRouteInfoManager().onChannelDestroy(remoteAddr, channel);
	}

	public void onChannelException(String remoteAddr, Channel channel) {
		this.namesrvController.getRouteInfoManager().onChannelDestroy(remoteAddr, channel);
	}

	public void onChannelIdle(String remoteAddr, Channel channel) {
		this.namesrvController.getRouteInfoManager().onChannelDestroy(remoteAddr, channel);
	}


}
