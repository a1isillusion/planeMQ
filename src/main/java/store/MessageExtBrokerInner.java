package store;

import java.util.Arrays;

public class MessageExtBrokerInner {
	public String topic;
	public int queueId;
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
		return queueId;
	}

	public void setQueueId(int queueId) {
		this.queueId = queueId;
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

	@Override
	public String toString() {
		return "MessageExtBrokerInner [topic=" + topic + ", QueueId=" + queueId + ", body=" + Arrays.toString(body)
				+ ", bornTimeStamp=" + bornTimeStamp + ", bornHost=" + bornHost + ", storeHost=" + storeHost + "]";
	}
}
