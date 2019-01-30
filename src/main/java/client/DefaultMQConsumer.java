package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import common.TopicRouteData;
import remoting.CommandCode;
import remoting.NettyRemotingClient;
import remoting.RemotingCommand;

public class DefaultMQConsumer {
private long defaultTimeoutMillis=1000;
public List<String> namesrvAddrList=new ArrayList<String>();
public Map<String, TopicRouteData>topicRouteDataList;
public String namesrvAddrChoosed;
public String topic;
public MessageListener messageListener;
public NettyRemotingClient remotingClient;
public void setNamesrvAddr(String addrs) {
	String [] spiltAddrs=addrs.split(";");
	for(String addr:spiltAddrs) {
		this.namesrvAddrList.add(addr);
	}
	this.namesrvAddrChoosed=this.namesrvAddrList.get(0);
	
}
public void getAndUpdateRouteInto() {
	RemotingCommand request=new RemotingCommand(CommandCode.GET_ROUTEINTO,null);
	try {
		RemotingCommand response=this.remotingClient.invokeSync(this.namesrvAddrChoosed, request, this.defaultTimeoutMillis);
		this.topicRouteDataList=JSON.parseObject(new String(response.getBody()),new TypeReference<HashMap<String, TopicRouteData>>() {});
		
	} catch (Exception e) {
		System.out.println("getAndUpdateRouteInto exception");
	}
}
public void subscribe(String topic) {
	this.topic=topic;
}
public void registerMessageListener(MessageListener messageListener) {
	this.messageListener=messageListener;
}
public void start() {
	
}
public void shutdown() {
	this.remotingClient.shutdown();
}
}
