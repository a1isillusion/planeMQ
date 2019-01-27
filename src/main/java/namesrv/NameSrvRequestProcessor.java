package namesrv;

import io.netty.channel.ChannelHandlerContext;
import remoting.NettyRequestProcessor;
import remoting.RemotingCommand;

public class NameSrvRequestProcessor implements NettyRequestProcessor {

	public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean rejectRequest() {
		// TODO Auto-generated method stub
		return false;
	}

}
