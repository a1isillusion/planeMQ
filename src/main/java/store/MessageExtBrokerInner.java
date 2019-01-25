package store;


public class MessageExtBrokerInner {
public String topic;
public int QueueId;
public byte[] body;
public long bornTimeStamp;
public String bornHost;
public String storeHost;
public String getTopic() {
	return topic;
}
public void setTopic(String topic) {
	this.topic = topic;
}
public int getQueueId() {
	return QueueId;
}
public void setQueueId(int queueId) {
	QueueId = queueId;
}
public byte[] getBody() {
	return body;
}
public void setBody(byte[] body) {
	this.body = body;
}
public long getBornTimeStamp() {
	return bornTimeStamp;
}
public void setBornTimeStamp(long bornTimeStamp) {
	this.bornTimeStamp = bornTimeStamp;
}
public String getBornHost() {
	return bornHost;
}
public void setBornHost(String bornHost) {
	this.bornHost = bornHost;
}
public String getStoreHost() {
	return storeHost;
}
public void setStoreHost(String storeHost) {
	this.storeHost = storeHost;
}

}
