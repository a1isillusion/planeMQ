package store;

import java.io.File;
import java.nio.ByteBuffer;

import config.StoreConfig;

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
	this.storePath=StoreConfig.storePath;
	File storeDir=new File(this.storePath+"//ConsumeQueue//"+topic+"//"+queueId);
	if(!storeDir.exists()) {
		storeDir.mkdirs();
	}
	mappedFileQueue=new MappedFileQueue(storeDir.getAbsolutePath(), 100);
}
public boolean putMessagePositionInfo(long offset,int size,long tagCode) {
	ByteBuffer byteBuffer=ByteBuffer.wrap(new byte[CQ_STORE_UNIT_SIZE]);
	byteBuffer.putLong(offset);
	byteBuffer.putInt(size);
	byteBuffer.putLong(tagCode);
	boolean result=mappedFileQueue.putMessage(byteBuffer.array());
	return result;
}
public ByteBuffer getIndexBuffer(long startIndex) {
	return mappedFileQueue.selectMappedBuffer(startIndex*CQ_STORE_UNIT_SIZE);
}
public long getOffset() {
	MappedFile mappedFileLast=this.mappedFileQueue.getLastMappedFile();
	return mappedFileLast==null?0l:(mappedFileLast.fileFromOffset+mappedFileLast.wrotePosition)/CQ_STORE_UNIT_SIZE;
}
}
