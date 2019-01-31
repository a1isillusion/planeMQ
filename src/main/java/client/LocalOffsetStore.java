package client;

import java.util.HashMap;
import java.util.Map;

public class LocalOffsetStore {
public Map<String,Map<Integer,Long>> offsetTable=new HashMap<String, Map<Integer,Long>>();
public Map<String,Map<Integer,Long>> consumeTable=new HashMap<String, Map<Integer,Long>>();
public synchronized void updateOffsetTable(Map<String,Map<Integer,Long>> offsetTable) {
	for(String topic:offsetTable.keySet()) {
		if(!this.offsetTable.containsKey(topic)) {
			this.offsetTable.put(topic, offsetTable.get(topic));	
		}else {
			Map<Integer, Long> queueOffset=offsetTable.get(topic);
			for(Integer queueId:queueOffset.keySet()) {
				this.offsetTable.get(topic).put(queueId, queueOffset.get(queueId));
			}
		}		
	}
}
public synchronized void syncConsumeTable() {
	this.consumeTable.clear();
	for(String topic:this.offsetTable.keySet()) {
		Map<Integer, Long> queueOffset=this.offsetTable.get(topic);
		Map<Integer, Long> consumeOffset=new HashMap<Integer, Long>();
		for(Integer queueId:queueOffset.keySet()) {
			consumeOffset.put(queueId, 0l);
		}
		this.consumeTable.put(topic, consumeOffset);
	}
}
public synchronized void updateOffset(String topic,int queueId,long offset) {
	this.consumeTable.get(topic).put(queueId, offset);
}
public long getOffset(String topic,int queueId) {
	return this.consumeTable.get(topic).get(queueId);
}
public boolean finishConsume(String topic,int queueId,long offset) {
	return this.offsetTable.get(topic).get(queueId)<=this.consumeTable.get(topic).get(queueId);
}
public void paintOffsetTable() {
	for(String topic:this.offsetTable.keySet()) {
		Map<Integer, Long> queueOffset=this.offsetTable.get(topic);
		for(Integer queueId:queueOffset.keySet()) {
			System.out.println("topic:"+topic+" queueId:"+queueId+" offset:"+queueOffset.get(queueId));
		}
	}
}
public void paintConsumeTable() {
	for(String topic:this.consumeTable.keySet()) {
		Map<Integer, Long> queueOffset=this.consumeTable.get(topic);
		for(Integer queueId:queueOffset.keySet()) {
			System.out.println("topic:"+topic+" queueId:"+queueId+" offset:"+queueOffset.get(queueId));
		}
	}
}
}
