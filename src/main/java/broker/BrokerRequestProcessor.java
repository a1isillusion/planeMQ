package broker;

import io.netty.channel.ChannelHandlerContext;
import remoting.NettyRequestProcessor;
import remoting.RemotingCommand;

public class BrokerRequestProcessor implements NettyRequestProcessor {
    public BrokerController brokerController;
    public BrokerRequestProcessor(BrokerController brokerController) {
    	this.brokerController=brokerController;
    }
	public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean rejectRequest() {
		// TODO Auto-generated method stub
		return false;
	}

}
