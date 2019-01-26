package store;

import java.nio.ByteBuffer;

public class ConsumeQueue {
public static final int CQ_STORE_UNIT_SIZE=20;	
public String topic;
public int queueId;
public String storePath;
public MappedFileQueue mappedFileQueue;
public long maxPhysicOffset=-1;
public long minLogicOffset=0;
public ConsumeQueue(String topic,int queueId) {
	this.topic=topic;
	this.queueId=queueId;
	mappedFileQueue=new MappedFileQueue("G://planeMQ//consumequeue", 1000);
}
public boolean putMessagePositionInfo(long offset,int size,long tagCode) {
	ByteBuffer byteBuffer=ByteBuffer.wrap(new byte[CQ_STORE_UNIT_SIZE]);
	byteBuffer.putLong(offset);
	byteBuffer.putInt(size);
	byteBuffer.putLong(tagCode);
	System.out.println(new String(byteBuffer.array()));
	boolean result=mappedFileQueue.putMessage(byteBuffer.array());	
	return result;
}
public byte[] getIndexBuffer(long startIndex) {
	return null;
}
}
