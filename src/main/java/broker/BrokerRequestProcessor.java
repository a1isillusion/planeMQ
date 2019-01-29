package broker;

import java.util.List;

import com.alibaba.fastjson.JSON;

import io.netty.channel.ChannelHandlerContext;
import remoting.CommandCode;
import remoting.NettyRequestProcessor;
import remoting.RemotingCommand;
import store.MessageExtBrokerInner;
import util.RemotingUtil;

public class BrokerRequestProcessor implements NettyRequestProcessor {
    public BrokerController brokerController;
    public BrokerRequestProcessor(BrokerController brokerController) {
    	this.brokerController=brokerController;
    }
	public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
		switch (request.getCode()) {
        case CommandCode.SEND_MESSAGE:
            return this.sendMessage(ctx, request);
        case CommandCode.PULL_MESSAGE:
            return this.pullMessage(ctx, request);      
		default:
			break;
		}
		return null;
	}

	public boolean rejectRequest() {
		return false;
	}
    public RemotingCommand sendMessage(ChannelHandlerContext ctx,
           RemotingCommand request) throws Exception {
           final RemotingCommand response = new RemotingCommand(CommandCode.SYSTEM_ERROR, null);
           MessageExtBrokerInner msg=new MessageExtBrokerInner();
           msg.setTopic(request.getExtFields().get("topic"));
           msg.setQueueId(Integer.parseInt(request.getExtFields().get("queueId")));
           msg.setBody(request.getBody());
           msg.setBornHost(RemotingUtil.parseChannelRemoteAddr(ctx.channel()));
           msg.setStoreHost(RemotingUtil.parseChannelLocalAddr(ctx.channel()));
           msg.setBornTimeStamp(System.currentTimeMillis());
           boolean result=this.brokerController.getMessageStore().putMessage(msg);
           if(result==true) {
        	   response.setCode(CommandCode.SUCCESS);
           }
    	   return response;
    }
    public RemotingCommand pullMessage(ChannelHandlerContext ctx,
            RemotingCommand request) throws Exception {
            final RemotingCommand response = new RemotingCommand(CommandCode.SYSTEM_ERROR, null);
            List<MessageExtBrokerInner> msgList=this.brokerController.getMessageStore().getMessage(
            		request.getExtFields().get("clusterName"), 
            		request.getExtFields().get("topic"), 
            		Integer.parseInt(request.getExtFields().get("queueId")),
            		Long.parseLong(request.getExtFields().get("startOffset")),
            		Integer.parseInt(request.getExtFields().get("maxMsgNums"))
                    );
            if (msgList.size()!=0) {
				response.setBody(JSON.toJSONString(msgList).getBytes());
				response.setCode(CommandCode.SUCCESS);
			}
     	    return response;
    }
}
