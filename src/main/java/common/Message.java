package common;

public class Message {
public String topic;
public byte[] body;
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
