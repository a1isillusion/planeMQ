package namesrv;

import java.util.HashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import common.QueueData;
import common.TopicRouteData;
import io.netty.channel.ChannelHandlerContext;
import remoting.CommandCode;
import remoting.NettyRequestProcessor;
import remoting.RemotingCommand;

public class NameSrvRequestProcessor implements NettyRequestProcessor {
    public NamesrvController namesrvController;
    public NameSrvRequestProcessor(NamesrvController namesrvController) {
    	this.namesrvController=namesrvController;
    }
	public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
		switch (request.getCode()) {
        case CommandCode.PUT_KV_CONFIG:
            return this.putKVConfig(ctx, request);
        case CommandCode.GET_KV_CONFIG:
            return this.getKVConfig(ctx, request);
        case CommandCode.DELETE_KV_CONFIG:
            return this.deleteKVConfig(ctx, request);
        case CommandCode.REGISTER_BROKER:
            return this.registerBroker(ctx, request);
        case CommandCode.UNREGISTER_BROKER:
            return this.unregisterBroker(ctx, request);
        case CommandCode.GET_ROUTEINTO_BY_TOPIC:
            return this.getRouteInfoByTopic(ctx, request);
		default:
			break;
		}
		return null;
	}

	public boolean rejectRequest() {
		return false;
	}
    public RemotingCommand putKVConfig(ChannelHandlerContext ctx,
           RemotingCommand request) throws Exception {
           final RemotingCommand response = new RemotingCommand(CommandCode.SYSTEM_ERROR, null);

            this.namesrvController.getKvConfigManager().putKVConfig(
                request.getExtFields().get("namespace"),
                request.getExtFields().get("key"),
                request.getExtFields().get("value")
            );

            response.setCode(CommandCode.SUCCESS);
            return response;
     }
     public RemotingCommand getKVConfig(ChannelHandlerContext ctx,
            RemotingCommand request) throws Exception {
            final RemotingCommand response = new RemotingCommand(CommandCode.QUERY_NOT_FOUND, null);

            String value = this.namesrvController.getKvConfigManager().getKVConfig(
            	request.getExtFields().get("namespace"),
                request.getExtFields().get("key")
            );

            if (value != null) {
                response.getExtFields().put("value", value);
                response.setCode(CommandCode.SUCCESS);
            }
            return response;
     }
     public RemotingCommand deleteKVConfig(ChannelHandlerContext ctx,
            RemotingCommand request) throws Exception {
            final RemotingCommand response = new RemotingCommand(CommandCode.QUERY_NOT_FOUND, null);

    	    this.namesrvController.getKvConfigManager().deleteKVConfig(
    	         request.getExtFields().get("namespace"),
    	         request.getExtFields().get("key")
    	    );

    	    response.setCode(CommandCode.SUCCESS);
    	    return response;
     }
     public RemotingCommand registerBroker(ChannelHandlerContext ctx,
             RemotingCommand request) throws Exception {
             final RemotingCommand response = new RemotingCommand(CommandCode.SYSTEM_ERROR, null);
             HashMap<String, QueueData> topicQueueConfig=JSON.parseObject(new String(request.getBody()), new TypeReference<HashMap<String,QueueData>>() {});
             
    	       this.namesrvController.getRouteInfoManager().registerBroker(
    	      	    request.getExtFields().get("clusterName"),
    	    	    request.getExtFields().get("brokerAddr"),
    	    	    request.getExtFields().get("brokerName"),
    	    	    Long.parseLong(request.getExtFields().get("brokerId")),
    	    	    request.getExtFields().get("haServerAddr"),
    	            topicQueueConfig,
    	            ctx.channel()
    	        );

    	        response.setCode(CommandCode.SUCCESS);
    	        return response;
    	    }
     public RemotingCommand unregisterBroker(ChannelHandlerContext ctx,
            RemotingCommand request) throws Exception {
            final RemotingCommand response = new RemotingCommand(CommandCode.SYSTEM_ERROR, null);

    	    this.namesrvController.getRouteInfoManager().unregisterBroker(
    	         request.getExtFields().get("clusterName"),
    	         request.getExtFields().get("brokerAddr"),
    	         request.getExtFields().get("brokerName"),
    	         Long.parseLong(request.getExtFields().get("brokerId")));

    	    response.setCode(CommandCode.SUCCESS);
    	    return response;
     }
     public RemotingCommand getRouteInfoByTopic(ChannelHandlerContext ctx,
             RemotingCommand request) throws Exception {
             final RemotingCommand response = new RemotingCommand(CommandCode.SYSTEM_ERROR, null);

    	     TopicRouteData topicRouteData = this.namesrvController.getRouteInfoManager().pickupTopicRouteData(request.getExtFields().get("topic"));

    	     byte[] content = JSON.toJSONString(topicRouteData).getBytes();
    	     response.setBody(content);
    	     response.setCode(CommandCode.SUCCESS);
    	     return response;
     }
}
