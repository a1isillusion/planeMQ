package store;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class MappedFileQueue {
public String storePath;
public int mappedFileSize;
public CopyOnWriteArrayList<MappedFile> mappedFiles= new CopyOnWriteArrayList<MappedFile>();
public long flushWhere=0;
public long committedWhere=0;
public volatile long storeTimestamp;
public MappedFileQueue(String storePath,int mappedFileSize) {
	this.storePath=storePath;
	this.mappedFileSize=mappedFileSize;
}
public MappedFile getFirstMappedFile() {
	MappedFile mappedFileFirst=null;
	if(!this.mappedFiles.isEmpty()) {
		mappedFileFirst=this.mappedFiles.get(0);
	}
	return mappedFileFirst;
}
public MappedFile getLastMappedFile() {
	MappedFile mappedFileLast=null;
	if(!this.mappedFiles.isEmpty()) {
		mappedFileLast=this.mappedFiles.get(this.mappedFiles.size()-1);
	}
	return mappedFileLast;
}
public MappedFile getLastMappedFile(boolean needCreate) {
	MappedFile mappedFileLast=getLastMappedFile();
	if(mappedFileLast==null&&needCreate) {
		MappedFile mappedFile=null;
		try {
			mappedFile=new MappedFile(storePath, committedWhere, mappedFileSize);
		}catch (IOException e) {
			System.out.println("创建MappedFile失败！");
		}
		if(mappedFile!=null) {
			this.mappedFiles.add(mappedFile);
		}
		return mappedFile;
	}
	return mappedFileLast;
}
public MappedFile createNewMappedFile() {
	committedWhere+=mappedFileSize;
	MappedFile mappedFile=null;
	try {
		mappedFile=new MappedFile(storePath, committedWhere, mappedFileSize);
	}catch (IOException e) {
		System.out.println("创建MappedFile失败！");
	}
	if(mappedFile!=null) {
		this.mappedFiles.add(mappedFile);
	}	
	return mappedFile;
}
public MappedFile findMappedFileByOffset(long offset) {
	MappedFile mappedFile=null;
	int index=(int) (offset/mappedFileSize);
	if(index<=mappedFiles.size()) {
		mappedFile=mappedFiles.get(index);
	}
	return mappedFile;
}
public AppendMessageResult putMessage(MessageExtBrokerInner msg,AppendMessageCallback cb) {
	MappedFile mappedFile=getLastMappedFile(true);
	AppendMessageResult result=mappedFile.appendMessage(msg, cb);
	if(result.getStatus().equals(AppendMessageResult.STATUS_FAIL)) {
		mappedFile=createNewMappedFile();
		result=mappedFile.appendMessage(msg, cb);
	}
	return result;
}
public MessageExtBrokerInner getMessage(long offset,int size) {
	MessageExtBrokerInner msg=null;
	MappedFile mappedFile=findMappedFileByOffset(offset);
	if(mappedFile!=null) {
		msg=mappedFile.getMessage((int) (offset%mappedFileSize), size);
	}
	return msg;
}
}
