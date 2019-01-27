package remoting;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSON;

public class RemotingCommand {
public static final int RPC_TYPE=0;// 0, REQUEST_COMMAND
//1, RESPONSE_COMMAND
public static final int RPC_ONEWAY=1; // 0, RPC
//1, Oneway
public static AtomicInteger requestId=new AtomicInteger(0);
public int opaque=requestId.getAndIncrement();
public int code;
public byte[] body;
public HashMap<String, String> extFields;
public ByteBuffer encode() {
	int length =4;
	byte[] data=JSON.toJSONString(RemotingCommand.this).getBytes();
	ByteBuffer result=ByteBuffer.allocate(length+data.length);
	result.putInt(data.length);
	result.put(data);
	result.flip();
	return result;
}
public static RemotingCommand decode(ByteBuffer byteBuffer) {
	int dataLength=byteBuffer.getInt();
	byte[] data=new byte[dataLength];
	byteBuffer.get(data);
	RemotingCommand command=JSON.parseObject(new String(data),RemotingCommand.class);
	return command;
}
@Override
public String toString() {
	return "RemotingCommand [opaque=" + opaque + ", code=" + code + ", body=" + Arrays.toString(body) + ", extFields="
			+ extFields + "]";
}

}
