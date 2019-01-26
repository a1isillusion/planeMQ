package store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedFile {
public FileChannel fileChannel;	
public MappedByteBuffer buffer;
public String path;
public String name;
public File file;
public int fileSize;
public int wrotePosition=0;
public int committedPosition=0;
public int flushedPosition=0;
public int fileFromOffset;
@SuppressWarnings("resource")
public MappedFile(String storePath,int offset,int mappedFileSize) throws IOException {
	path=storePath;
	fileFromOffset=offset;
	fileSize=mappedFileSize;
	name=""+fileFromOffset;
	file=new File(path, name);
	if(!file.exists()) {
		file.createNewFile();
	}
	RandomAccessFile raFile=new RandomAccessFile(file, "rw");
	raFile.setLength(fileSize);
	fileChannel=raFile.getChannel();
	buffer=fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileChannel.size());
}
public boolean appendMessage(MessageExtBrokerInner msg,AppendMessageCallback cb) {
	boolean result=false;
	if(wrotePosition<fileSize) {
		ByteBuffer byteBuffer=buffer.slice();
		byteBuffer.position(wrotePosition);
		int offset=cb.doAppend(fileFromOffset, byteBuffer, fileSize-wrotePosition, msg);
		if(offset!=0) {
			wrotePosition=offset;
			result=true;
		}
	}
	return result;
}
}
