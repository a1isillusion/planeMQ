package client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import common.BrokerData;
import common.MessageQueue;
import common.QueueData;
import common.TopicRouteData;

public class MQFaultStrategy {
public Map<String, TopicRouteData> topicRouteDataList;
public Map<String, Integer> topicQueueChoosed=new ConcurrentHashMap<String, Integer>();
public Map<String, Integer> topicQueueNums=new ConcurrentHashMap<String, Integer>();
public void updateTopicRouteDataList(Map<String, TopicRouteData> topicRouteDataList) {
	if (this.topicRouteDataList!=null) {
		this.topicRouteDataList.clear();
	}
	this.topicRouteDataList=topicRouteDataList;
	this.topicQueueChoosed.clear();
	this.topicQueueNums.clear();
	for(String topic:topicRouteDataList.keySet()) {
		this.topicQueueChoosed.put(topic, 0);
		this.topicQueueNums.put(topic, getTopicQueueNums(topic));
	}
}
public int getTopicQueueNums(String topic) {
	TopicRouteData topicRouteData=this.topicRouteDataList.get(topic);
	int queueNums=0;
	for(QueueData queueData:topicRouteData.getQueueDatas()) {
		queueNums+=queueData.getWriteQueueNums();
	}
	return queueNums;
}
public String getBrokerName(String topic,int queueId) {
	TopicRouteData topicRouteData=this.topicRouteDataList.get(topic);
	int queueNums=0;
	for(QueueData queueData:topicRouteData.getQueueDatas()) {
		queueNums+=queueData.getWriteQueueNums();
		if(queueId<=queueNums-1) {
			return queueData.getBrokerName();
		}
	}
	return null;
}
public void changeTopicQueueChoosed(String topic) {
	int queueChoosed=this.topicQueueChoosed.get(topic);
	int queueNums=this.topicQueueNums.get(topic);
	if(queueChoosed==queueNums-1) {
		this.topicQueueChoosed.put(topic, 0);
	}else {
		this.topicQueueChoosed.put(topic, ++queueChoosed);
	}
}
public MessageQueue selectOneMessageQueue(String topic) {
	MessageQueue messageQueue=null;
	if(this.topicQueueChoosed.get(topic)!=null) {
		int queueChoosed=this.topicQueueChoosed.get(topic);
		messageQueue=new MessageQueue();
	    messageQueue.setQueueId(queueChoosed);
	    messageQueue.setTopic(topic);
	    messageQueue.setBrokerName(getBrokerName(topic, queueChoosed));
	    changeTopicQueueChoosed(topic);
	}
	return messageQueue;	
}
public String getMessageQueueAddr(MessageQueue messageQueue) {
	List<BrokerData> brokerDatas= this.topicRouteDataList.get(messageQueue.getTopic()).getBrokerDatas();
	for(BrokerData brokerData:brokerDatas) {
		if (brokerData.getBrokerName().equals(messageQueue.getBrokerName())) {
			return brokerData.getBrokerAddrs().get(0l);
		}
	}
	return null;
}
public List<String> getAllBrokerAddr(){
	List<String> allBrokerAddr=new ArrayList<String>();
	for(String topic:this.topicRouteDataList.keySet()){
		TopicRouteData topicRouteData=this.topicRouteDataList.get(topic);
		List<BrokerData> brokerDatas=topicRouteData.getBrokerDatas();
		for(BrokerData brokerData:brokerDatas) {
			if(!allBrokerAddr.contains(brokerData.getBrokerAddrs().get(0l))) {
				allBrokerAddr.add(brokerData.getBrokerAddrs().get(0l));
			}
		}
	}
	return allBrokerAddr;
}
}
