package store;

import java.util.HashMap;

public class CommitLog {
public MappedFileQueue mappedFileQueue;
public AppendMessageCallback cb;
public HashMap<String, Long>topicQueueTable;
public CommitLog() {
	mappedFileQueue=new MappedFileQueue("G://planeMQ//store", 1024);
	cb=new AppendMessageCallback();
	topicQueueTable=new HashMap<String, Long>();
}
public AppendMessageResult putMessage(MessageExtBrokerInner msg) {
	return mappedFileQueue.putMessage(msg, cb);
}
public MessageExtBrokerInner getMessage(long offset,int size) {
	return mappedFileQueue.getMessage(offset, size);
}
}
