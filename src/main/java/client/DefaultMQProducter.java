package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import common.Message;
import common.MessageQueue;
import common.TopicRouteData;
import remoting.CommandCode;
import remoting.NettyRemotingClient;
import remoting.RemotingCommand;

public class DefaultMQProducter {
	private long defaultTimeoutMillis = 1000;
	public MQFaultStrategy mqFaultStrategy = new MQFaultStrategy();
	public List<String> namesrvAddrList = new ArrayList<String>();
	public String namesrvAddrChoosed;
	public NettyRemotingClient remotingClient;

	public DefaultMQProducter() {
		this.remotingClient = new NettyRemotingClient();
	}

	public void setNamesrvAddr(String addrs) {
		String[] spiltAddrs = addrs.split(";");
		for (String addr : spiltAddrs) {
			this.namesrvAddrList.add(addr);
		}
		this.namesrvAddrChoosed = this.namesrvAddrList.get(0);
	}

	public void getAndUpdateRouteInto() {
		RemotingCommand request = new RemotingCommand(CommandCode.GET_ROUTEINTO, null);
		try {
			RemotingCommand response = this.remotingClient.invokeSync(this.namesrvAddrChoosed, request,
					this.defaultTimeoutMillis);
			this.mqFaultStrategy.updateTopicRouteDataList(JSON.parseObject(new String(response.getBody()),
					new TypeReference<HashMap<String, TopicRouteData>>() {
					}));
		} catch (Exception e) {
			System.out.println("getAndUpdateRouteInto exception");
		}
	}

	public void sendMessage(Message message) throws Exception {
		MessageQueue messageQueue = this.mqFaultStrategy.selectOneMessageQueue(message.getTopic());
		if (messageQueue != null) {
			String addr = this.mqFaultStrategy.getMessageQueueAddr(messageQueue);
			RemotingCommand request = new RemotingCommand(CommandCode.SEND_MESSAGE, null);
			request.getExtFields().put("topic", messageQueue.getTopic());
			request.getExtFields().put("queueId", "" + messageQueue.getQueueId());
			request.setBody(message.getBody());
			this.remotingClient.invokeOneway(addr, request);
		}
	}

	public void shutdown() {
		this.remotingClient.shutdown();
	}
}
