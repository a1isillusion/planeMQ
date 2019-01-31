package common;

import store.MessageExtBrokerInner;

public class Message {
public String topic;
public byte[] body;

public Message(String topic, byte[] body) {
	this.topic = topic;
	this.body = body;
}
public Message(MessageExtBrokerInner msg) {
	this.topic=msg.getTopic();
	this.body=msg.getBody();
}
public String getTopic() {
	return topic;
}
public void setTopic(String topic) {
	this.topic = topic;
}
public byte[] getBody() {
	return body;
}
public void setBody(byte[] body) {
	this.body = body;
}

}
