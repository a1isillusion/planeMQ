package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import common.Message;
import common.TopicRouteData;
import config.SystemConfig;
import remoting.CommandCode;
import remoting.NettyRemotingClient;
import remoting.RemotingCommand;
import store.MessageExtBrokerInner;

public class DefaultMQConsumer {
	private long defaultTimeoutMillis = 1000;
	public MQFaultStrategy mqFaultStrategy = new MQFaultStrategy();
	public LocalOffsetStore localOffsetStore = new LocalOffsetStore();
	public List<String> namesrvAddrList = new ArrayList<String>();
	public String namesrvAddrChoosed;
	public String topic;
	public MessageListener messageListener;
	public NettyRemotingClient remotingClient;

	public DefaultMQConsumer() {
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

	public void getOffset() {
		RemotingCommand request = new RemotingCommand(CommandCode.GET_OFFSET, null);
		List<String> brokerAddrs = this.mqFaultStrategy.getAllBrokerAddr();
		try {
			for (String addr : brokerAddrs) {
				RemotingCommand response = this.remotingClient.invokeSync(addr, request, this.defaultTimeoutMillis);
				this.localOffsetStore.updateOffsetTable(JSON.parseObject(new String(response.getBody()),
						new TypeReference<Map<String, Map<Integer, Long>>>() {
						}));
			}
		} catch (Exception e) {
			System.out.println("getOffset exception");
		}
	}

	public void subscribe(String topic) {
		this.topic = topic;
	}

	public void registerMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	public void start() {
		getAndUpdateRouteInto();
		getOffset();
		this.localOffsetStore.syncConsumeTable();
		int queueNums = this.mqFaultStrategy.getTopicQueueNums(this.topic);
		try {
			for (int queueId = 0; queueId < queueNums; queueId++) {
				long offset = this.localOffsetStore.getOffset(this.topic, queueId);
				while (!this.localOffsetStore.finishConsume(topic, queueId, offset)) {
					String addr = this.mqFaultStrategy.getAddrByQueueId(topic, queueId);
					RemotingCommand request = new RemotingCommand(CommandCode.PULL_MESSAGE, null);
					request.getExtFields().put("clusterName", SystemConfig.clusterName);
					request.getExtFields().put("topic", this.topic);
					request.getExtFields().put("queueId", "" + queueId);
					request.getExtFields().put("startOffset", "" + offset);
					request.getExtFields().put("maxMsgNums", "1");
					RemotingCommand response = this.remotingClient.invokeSync(addr, request, 1000);
					List<MessageExtBrokerInner> messageExtBrokerInners = JSON.parseObject(
							new String(response.getBody()), new TypeReference<List<MessageExtBrokerInner>>() {
							});
					List<Message> messages = new ArrayList<Message>();
					for (MessageExtBrokerInner msg : messageExtBrokerInners) {
						Message message = new Message(msg);
						messages.add(message);
					}
					this.messageListener.consumeMessage(messages);
					this.localOffsetStore.updateOffset(topic, queueId, ++offset);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {
		this.remotingClient.shutdown();
	}
}
