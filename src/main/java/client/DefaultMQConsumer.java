package client;

import java.util.ArrayList;
import java.util.List;

import remoting.NettyRemotingClient;

public class DefaultMQConsumer {
public List<String> namesrvAddrList=new ArrayList<String>();
public String namesrvAddrChoosed;
public String topic;
public MessageListener messageListener;
public NettyRemotingClient remotingClient;
public void setNamesrvAddr(String addrs) {
	String [] spiltAddrs=addrs.split(";");
	for(String addr:spiltAddrs) {
		this.namesrvAddrList.add(addr);
	}
}
public void subscribe(String topic) {
	this.topic=topic;
}
public void registerMessageListener(MessageListener messageListener) {
	this.messageListener=messageListener;
}
}
