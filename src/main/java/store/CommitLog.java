package store;

import java.io.File;
import java.util.HashMap;

import config.StoreConfig;

public class CommitLog {
	public MappedFileQueue mappedFileQueue;
	public AppendMessageCallback cb;
	public HashMap<String, Long> topicQueueTable;

	public CommitLog() {
		File storeDir = new File(StoreConfig.storePath + "//Store");
		if (!storeDir.exists()) {
			storeDir.mkdirs();
		}
		mappedFileQueue = new MappedFileQueue(storeDir.getAbsolutePath(), 1024);
		cb = new AppendMessageCallback();
		topicQueueTable = new HashMap<String, Long>();
	}

	public AppendMessageResult putMessage(MessageExtBrokerInner msg) {
		return mappedFileQueue.putMessage(msg, cb);
	}

	public MessageExtBrokerInner getMessage(long offset, int size) {
		return mappedFileQueue.getMessage(offset, size);
	}
}
