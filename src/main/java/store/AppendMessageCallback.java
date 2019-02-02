package store;

import java.nio.ByteBuffer;

import com.alibaba.fastjson.JSON;

public class AppendMessageCallback {
	public AppendMessageResult doAppend(long fileFromOffset, ByteBuffer byteBuffer, int leaveSize,
			MessageExtBrokerInner msg) {
		AppendMessageResult result = new AppendMessageResult();
		result.setFileOffset(fileFromOffset + byteBuffer.position());
		result.setOffset(byteBuffer.position());
		byte[] msgBytes = JSON.toJSONString(msg).getBytes();
		System.out.println(new String(msgBytes));
		if (msgBytes.length <= leaveSize) {
			byteBuffer.put(msgBytes);
			result.setStatus(AppendMessageResult.STATUS_SUCCESS);
			result.setPostion(byteBuffer.position());
			result.setSize(msgBytes.length);
		}
		System.out.println(JSON.toJSONString(result));
		return result;
	}
}
