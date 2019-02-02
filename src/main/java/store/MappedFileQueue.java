package store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

public class MappedFileQueue {
	private ReentrantLock lock = new ReentrantLock();
	public String storePath;
	public int mappedFileSize;
	public CopyOnWriteArrayList<MappedFile> mappedFiles = new CopyOnWriteArrayList<MappedFile>();
	public long flushWhere = 0;
	public long committedWhere = 0;
	public volatile long storeTimestamp;

	public MappedFileQueue(String storePath, int mappedFileSize) {
		this.storePath = storePath;
		this.mappedFileSize = mappedFileSize;
	}

	public MappedFile getFirstMappedFile() {
		MappedFile mappedFileFirst = null;
		if (!this.mappedFiles.isEmpty()) {
			mappedFileFirst = this.mappedFiles.get(0);
		}
		return mappedFileFirst;
	}

	public MappedFile getLastMappedFile() {
		MappedFile mappedFileLast = null;
		if (!this.mappedFiles.isEmpty()) {
			mappedFileLast = this.mappedFiles.get(this.mappedFiles.size() - 1);
		}
		return mappedFileLast;
	}

	public MappedFile getLastMappedFile(boolean needCreate) {
		MappedFile mappedFileLast = getLastMappedFile();
		if (mappedFileLast == null && needCreate) {
			synchronized (this) {
				mappedFileLast = getLastMappedFile();
				if (mappedFileLast == null && needCreate) {
					try {
						mappedFileLast = new MappedFile(storePath, committedWhere, mappedFileSize);
					} catch (IOException e) {
						System.out.println("创建MappedFile失败!");
					}
					if (mappedFileLast != null) {
						this.mappedFiles.add(mappedFileLast);
					}
				}
			}
		}
		return mappedFileLast;
	}

	public void createNewMappedFile() {
		if (this.lock.tryLock()) {
			committedWhere += mappedFileSize;
			MappedFile mappedFile = null;
			try {
				mappedFile = new MappedFile(storePath, committedWhere, mappedFileSize);
			} catch (IOException e) {
				System.out.println("创建MappedFile失败！");
			}
			if (mappedFile != null) {
				this.mappedFiles.add(mappedFile);
			}
			this.lock.unlock();
		}
	}

	public MappedFile findMappedFileByOffset(long offset) {
		MappedFile mappedFile = null;
		int index = (int) (offset / mappedFileSize);
		if (index <= mappedFiles.size()) {
			mappedFile = mappedFiles.get(index);
		}
		return mappedFile;
	}

	public AppendMessageResult putMessage(MessageExtBrokerInner msg, AppendMessageCallback cb) {
		MappedFile mappedFile = getLastMappedFile(true);
		AppendMessageResult result = mappedFile.appendMessage(msg, cb);
		if (result.getStatus().equals(AppendMessageResult.STATUS_FAIL)) {
			createNewMappedFile();
			return putMessage(msg, cb);
		}
		return result;
	}

	public boolean putMessage(byte[] msg) {
		MappedFile mappedFile = getLastMappedFile(true);
		boolean result = mappedFile.appendMessage(msg);
		if (result == false) {
			createNewMappedFile();
			return putMessage(msg);
		}
		return result;
	}

	public MessageExtBrokerInner getMessage(long offset, int size) {
		MessageExtBrokerInner msg = null;
		MappedFile mappedFile = findMappedFileByOffset(offset);
		if (mappedFile != null) {
			msg = mappedFile.getMessage((int) (offset % mappedFileSize), size);
		}
		return msg;
	}

	public ByteBuffer selectMappedBuffer(long offset) {
		ByteBuffer result = null;
		MappedFile mappedFile = findMappedFileByOffset(offset);
		if (mappedFile != null) {
			result = mappedFile.selectMappedBuffer((int) (offset % mappedFileSize));
		}
		return result;
	}

	@SuppressWarnings("resource")
	public void recover() throws IOException {// 待完善
		RandomAccessFile raFile = new RandomAccessFile(new File(storePath, ".bak"), "rw");
		FileChannel fileChannel = raFile.getChannel();
		MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileChannel.size());
		int size = buffer.getInt();
		byte[] bs = new byte[size];
		buffer.get(bs);
		Map<String, Integer> positionMap = JSON.parseObject(new String(bs), new TypeReference<Map<String, Integer>>() {
		});
		File dir = new File(storePath);
		if (dir.isDirectory()) {
			mappedFiles.clear();
			ArrayList<Long> fileNameList = new ArrayList<Long>();
			for (File file : dir.listFiles()) {
				if (!file.getName().equals(".bak")) {
					fileNameList.add(Long.parseLong(file.getName()));
				}
			}
			Collections.sort(fileNameList);
			for (Long name : fileNameList) {
				try {
					MappedFile mappedFile = new MappedFile(storePath, name, mappedFileSize);
					mappedFile.wrotePosition = positionMap.get("" + name);
					this.mappedFiles.add(mappedFile);

				} catch (IOException e) {
					System.out.println("创建MappedFile失败！");
				}
			}
		}
	}

	public void backup() throws IOException {
		Map<String, Integer> positionMap = new HashMap<String, Integer>();
		for (MappedFile mappedFile : mappedFiles) {
			positionMap.put(mappedFile.name, mappedFile.wrotePosition);
		}
		byte[] bs = JSON.toJSONString(positionMap).getBytes();
		ByteBuffer byteBuffer = ByteBuffer.allocate(bs.length + 4);
		byteBuffer.putInt(bs.length);
		byteBuffer.put(bs);
		File file = new File(storePath, ".bak");
		if (!file.exists()) {
			file.createNewFile();
		}
		RandomAccessFile raFile = new RandomAccessFile(file, "rw");
		raFile.setLength(bs.length + 4);
		FileChannel fileChannel = raFile.getChannel();
		MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileChannel.size());
		buffer.put(byteBuffer.array());
		fileChannel.close();
		raFile.close();
	}
}
