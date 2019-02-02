package store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import config.StoreConfig;

public class MessageStore {
	public CommitLog commitLog;
	public ConcurrentMap<String/* topic */, ConcurrentMap<Integer/* queueId */, ConsumeQueue>> consumeQueueTable;

	public MessageStore() {
		this.commitLog = new CommitLog();
		this.consumeQueueTable = new ConcurrentHashMap<String, ConcurrentMap<Integer, ConsumeQueue>>();
	}

	public boolean putMessage(MessageExtBrokerInner msg) {
		boolean result = false;
		ConcurrentMap<Integer, ConsumeQueue> consumeQueues = consumeQueueTable.get(msg.getTopic());
		if (consumeQueues != null) {
			ConsumeQueue consumeQueue = consumeQueues.get(msg.getQueueId());
			if (consumeQueue != null) {
				AppendMessageResult appendMessageResult = this.commitLog.putMessage(msg);
				if (appendMessageResult.getStatus().equals(AppendMessageResult.STATUS_SUCCESS)) {
					result = consumeQueue.putMessagePositionInfo(appendMessageResult.getFileOffset(),
							appendMessageResult.getSize(), 0l);
				}
			}
		}
		return result;
	}

	public List<MessageExtBrokerInner> getMessage(String clusterName, String topic, int queueId, long startOffset,
			int maxMsgNums) {
		List<MessageExtBrokerInner> result = new ArrayList<MessageExtBrokerInner>();
		ConcurrentMap<Integer, ConsumeQueue> consumeQueues = consumeQueueTable.get(topic);
		if (consumeQueues != null) {
			ConsumeQueue consumeQueue = consumeQueues.get(queueId);
			if (consumeQueue != null) {
				for (int i = 0; i < maxMsgNums; i++, startOffset++) {
					ByteBuffer buffer = consumeQueue.getIndexBuffer(startOffset);
					if (buffer == null) {
						break;
					}
					long offset = buffer.getLong();
					int size = buffer.getInt();
					MessageExtBrokerInner msg = this.commitLog.getMessage(offset, size);
					result.add(msg);
				}
			}
		}
		return result;
	}

	public void createTopic(String topic, int startQueueId, int queueNums) {
		ConcurrentMap<Integer, ConsumeQueue> consumeQueues = this.consumeQueueTable.get(topic);
		if (consumeQueues == null) {
			consumeQueues = new ConcurrentHashMap<Integer, ConsumeQueue>();
			for (int i = startQueueId; i < queueNums + startQueueId; i++) {
				consumeQueues.putIfAbsent(i, new ConsumeQueue(topic, i));
			}
			this.consumeQueueTable.put(topic, consumeQueues);
		}
	}

	public Map<String, Map<Integer, Long>> getTotalOffset() {
		Map<String, Map<Integer, Long>> totalOffset = new HashMap<String, Map<Integer, Long>>();
		for (String topic : this.consumeQueueTable.keySet()) {
			HashMap<Integer, Long> queueOffset = new HashMap<Integer, Long>();
			ConcurrentMap<Integer, ConsumeQueue> queueMap = this.consumeQueueTable.get(topic);
			for (Integer queueId : queueMap.keySet()) {
				queueOffset.put(queueId, queueMap.get(queueId).getOffset());
			}
			totalOffset.put(topic, queueOffset);
		}
		return totalOffset;
	}

	public void backup() throws IOException {
		this.commitLog.mappedFileQueue.backup();
		for (String topic : this.consumeQueueTable.keySet()) {
			ConcurrentMap<Integer, ConsumeQueue> consumeQueueMap = this.consumeQueueTable.get(topic);
			for (Integer queueId : consumeQueueMap.keySet()) {
				consumeQueueMap.get(queueId).mappedFileQueue.backup();
			}
		}
	}

	public void recover() throws IOException {
		this.commitLog.mappedFileQueue.recover();
		File dir = new File(StoreConfig.storePath + "//ConsumeQueue");
		for (File topicDir : dir.listFiles()) {
			List<Integer> queueIdList = new ArrayList<Integer>();
			for (File queueDir : topicDir.listFiles()) {
				queueIdList.add(Integer.parseInt(queueDir.getName()));
			}
			int startQueueId = Collections.min(queueIdList);
			int queueNums = Collections.max(queueIdList) - startQueueId + 1;
			createTopic(topicDir.getName(), startQueueId, queueNums);
		}
		for (String topic : this.consumeQueueTable.keySet()) {
			ConcurrentMap<Integer, ConsumeQueue> consumeQueueMap = this.consumeQueueTable.get(topic);
			for (Integer queueId : consumeQueueMap.keySet()) {
				consumeQueueMap.get(queueId).mappedFileQueue.recover();
			}
		}
	}
}
