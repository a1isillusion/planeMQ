package store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
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
public boolean putMessage(byte[] msg) {
	MappedFile mappedFile=getLastMappedFile(true);
	boolean result=mappedFile.appendMessage(msg);
	if(result==false) {
		mappedFile=createNewMappedFile();
		result=mappedFile.appendMessage(msg);
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
public ByteBuffer selectMappedBuffer(long offset) {
	ByteBuffer result=null;
	MappedFile mappedFile=findMappedFileByOffset(offset);
	if(mappedFile!=null) {
		result=mappedFile.selectMappedBuffer((int) (offset%mappedFileSize));
	}
	return result;
}
public void reput() {//待完善
	File dir=new File(storePath);
	if(dir.isDirectory()) {
		mappedFiles.clear();
		ArrayList<Long> fileNameList=new ArrayList<Long>();
		for(File file:dir.listFiles()) {
			fileNameList.add(Long.parseLong(file.getName()));
		}
		Collections.sort(fileNameList);
		for(Long name:fileNameList) {
			try {
				this.mappedFiles.add(new MappedFile(storePath, name, mappedFileSize));
			}catch (IOException e) {
				System.out.println("创建MappedFile失败！");
			}
		}
	}
}
}
