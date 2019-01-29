package store;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MessageStore {
public CommitLog commitLog;
public ConcurrentMap<String/* topic */, ConcurrentMap<Integer/* queueId */, ConsumeQueue>> consumeQueueTable;
public MessageStore() {
	this.commitLog=new CommitLog();
	this.consumeQueueTable=new ConcurrentHashMap<String, ConcurrentMap<Integer,ConsumeQueue>>();
}
public boolean putMessage(MessageExtBrokerInner msg) {
	boolean result=false;
	ConcurrentMap<Integer, ConsumeQueue>consumeQueues=consumeQueueTable.get(msg.getTopic());
	if (consumeQueues!=null) {
		ConsumeQueue consumeQueue=consumeQueues.get(msg.getQueueId());
		if(consumeQueue!=null) {
			AppendMessageResult appendMessageResult=this.commitLog.putMessage(msg);
			if(appendMessageResult.getStatus().equals(AppendMessageResult.STATUS_SUCCESS)) {
				result=consumeQueue.putMessagePositionInfo(appendMessageResult.getOffset(), appendMessageResult.getSize(), 0l);
			}
		}
	}
	return result;
}
public List<MessageExtBrokerInner> getMessage(String clusterName,String topic,int queueId,long startOffset,int maxMsgNums) {
	List<MessageExtBrokerInner> result=new ArrayList<MessageExtBrokerInner>();
	ConcurrentMap<Integer, ConsumeQueue>consumeQueues=consumeQueueTable.get(topic);
	if (consumeQueues!=null) {
		ConsumeQueue consumeQueue=consumeQueues.get(queueId);
		if(consumeQueue!=null) {
           for(int i=0;i<maxMsgNums;i++,startOffset++) {
        	   ByteBuffer buffer=consumeQueue.getIndexBuffer(startOffset);
        	   if(buffer==null) {
        		   break;
        	   }
        	   long offset=buffer.getLong();
        	   int size=buffer.getInt();
        	   MessageExtBrokerInner msg=this.commitLog.getMessage(offset, size);
        	   result.add(msg);
           }
		}
	}
	return result;
}
public void createTopic(String topic,int startQueueId,int queueNums) {
	ConcurrentMap<Integer, ConsumeQueue> consumeQueues=this.consumeQueueTable.get(topic);
	if(consumeQueues==null) {
		consumeQueues=new ConcurrentHashMap<Integer, ConsumeQueue>();
		for(int i=startQueueId;i<queueNums+startQueueId;i++) {
			consumeQueues.putIfAbsent(i, new ConsumeQueue(topic, i));
		}
		this.consumeQueueTable.put(topic, consumeQueues);
	}
}
}
