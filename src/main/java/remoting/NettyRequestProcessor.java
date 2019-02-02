package remoting;

import io.netty.channel.ChannelHandlerContext;

public interface NettyRequestProcessor {
	public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception;

	public boolean rejectRequest();
}
