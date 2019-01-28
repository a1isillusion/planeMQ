package store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.alibaba.fastjson.JSON;

public class MappedFile {
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	public FileChannel fileChannel;
	public MappedByteBuffer buffer;
	public String path;
	public String name;
	public File file;
	public int fileSize;
	public volatile int wrotePosition = 0;
	public int committedPosition = 0;
	public int flushedPosition = 0;
	public long fileFromOffset;

	@SuppressWarnings("resource")
	public MappedFile(String storePath, long offset, int mappedFileSize) throws IOException {
		path = storePath;
		fileFromOffset = offset;
		fileSize = mappedFileSize;
		name = "" + fileFromOffset;
		file = new File(path, name);
		if (!file.exists()) {
			file.createNewFile();
		}
		RandomAccessFile raFile = new RandomAccessFile(file, "rw");
		raFile.setLength(fileSize);
		fileChannel = raFile.getChannel();
		buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileChannel.size());
	}

	public AppendMessageResult appendMessage(MessageExtBrokerInner msg, AppendMessageCallback cb) {
		AppendMessageResult result = new AppendMessageResult();
		try {
			this.lock.writeLock().lockInterruptibly();
			if (wrotePosition < fileSize) {
				ByteBuffer byteBuffer = buffer.slice();
				byteBuffer.position(wrotePosition);
				result = cb.doAppend(fileFromOffset, byteBuffer, fileSize - wrotePosition, msg);
				if (result.getStatus().equals(AppendMessageResult.STATUS_SUCCESS)) {
					wrotePosition = result.getPostion();
				}
			}
		} catch (InterruptedException e) {
			System.out.println("appendMessage error");
		} finally {
			this.lock.writeLock().unlock();
		}
		return result;
	}

	public boolean appendMessage(byte[] msg) {
		boolean result = false;
		try {
			this.lock.writeLock().lockInterruptibly();
			if (wrotePosition < fileSize && fileSize - wrotePosition >= msg.length) {
				ByteBuffer byteBuffer = buffer.slice();
				byteBuffer.position(wrotePosition);
				byteBuffer.put(msg);
				wrotePosition = byteBuffer.position();
				result = true;
			}
		} catch (InterruptedException e) {
			System.out.println("appendMessage error");
		} finally {
			this.lock.writeLock().unlock();
		}
		return result;
	}

	public MessageExtBrokerInner getMessage(int offset, int size) {
		MessageExtBrokerInner msg = null;
		try {
			this.lock.readLock().lockInterruptibly();
			if (offset + size <= wrotePosition) {
				ByteBuffer byteBuffer = buffer.slice();
				byteBuffer.position(wrotePosition);
				byteBuffer.flip();
				byteBuffer.position(offset);
				byte[] bs = new byte[size];
				byteBuffer.get(bs);
				msg = JSON.parseObject(new String(bs), MessageExtBrokerInner.class);
			}
		} catch (InterruptedException e) {
			System.out.println("getMessage error");
		} finally {
			this.lock.readLock().unlock();
		}
		return msg;
	}

	public ByteBuffer selectMappedBuffer(int offset) {
		ByteBuffer result = null;
		try {
			this.lock.readLock().lockInterruptibly();
			if (offset + ConsumeQueue.CQ_STORE_UNIT_SIZE <= wrotePosition) {
				ByteBuffer byteBuffer = buffer.slice();
				byteBuffer.position(wrotePosition);
				byteBuffer.flip();
				byteBuffer.position(offset);
				byte[] bs = new byte[ConsumeQueue.CQ_STORE_UNIT_SIZE];
				byteBuffer.get(bs);
				result = ByteBuffer.wrap(bs);
			}
		} catch (InterruptedException e) {
			System.out.println("seleMappedBuffer error");
		} finally {
			this.lock.readLock().unlock();
		}
		return result;
	}
}
