package store;

import java.nio.ByteBuffer;

import com.alibaba.fastjson.JSON;

public class AppendMessageCallback {
public int doAppend(int fileFromOffset,ByteBuffer byteBuffer,int leaveSize,MessageExtBrokerInner msg) {
	byte[] msgBytes=JSON.toJSONString(msg).getBytes();
	System.out.println(new String(msgBytes));
	if(msgBytes.length<=leaveSize) {
		byteBuffer.put(msgBytes);
		return byteBuffer.position();
	}  
	return 0;
}
}
