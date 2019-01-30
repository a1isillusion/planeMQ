package client;

import java.util.ArrayList;
import java.util.List;

import common.Message;
import remoting.NettyRemotingClient;

public class DefaultMQProducter {
public List<String> namesrvAddrList=new ArrayList<String>();
public String namesrvAddrChoosed;
public NettyRemotingClient remotingClient;
public void setNamesrvAddr(String addrs) {
	String [] spiltAddrs=addrs.split(";");
	for(String addr:spiltAddrs) {
		this.namesrvAddrList.add(addr);
	}
}
public void sendMessage(Message message) {
	
}
}
