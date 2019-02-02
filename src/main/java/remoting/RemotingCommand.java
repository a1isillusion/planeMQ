package remoting;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSON;

public class RemotingCommand {
	public int RPC_TYPE = 0;// 0, REQUEST_COMMAND
	// 1, RESPONSE_COMMAND
	public int RPC_ONEWAY = 1; // 0, RPC
	// 1, Oneway
	public static AtomicInteger requestId = new AtomicInteger(0);
	public int opaque = requestId.getAndIncrement();
	public int code;
	public byte[] body;
	public HashMap<String, String> extFields;

	public RemotingCommand(int code, String body) {
		this.code = code;
		if (body != null) {
			this.body = body.getBytes();
		} else {
			this.body = "".getBytes();
		}
		this.extFields = new HashMap<String, String>();
	}

	public RemotingCommand() {
	}

	public ByteBuffer encode() {
		int length = 4;
		byte[] data = JSON.toJSONString(RemotingCommand.this).getBytes();
		ByteBuffer result = ByteBuffer.allocate(length + data.length);
		result.putInt(data.length);
		result.put(data);
		result.flip();
		return result;
	}

	public static RemotingCommand decode(ByteBuffer byteBuffer) {
		int dataLength = byteBuffer.getInt();
		byte[] data = new byte[dataLength];
		byteBuffer.get(data);
		RemotingCommand command = JSON.parseObject(new String(data), RemotingCommand.class);
		return command;
	}

	public int getRPC_TYPE() {
		return RPC_TYPE;
	}

	public void setRPC_TYPE(int rPC_TYPE) {
		RPC_TYPE = rPC_TYPE;
	}

	public int getRPC_ONEWAY() {
		return RPC_ONEWAY;
	}

	public void setRPC_ONEWAY(int rPC_ONEWAY) {
		RPC_ONEWAY = rPC_ONEWAY;
	}

	public static AtomicInteger getRequestId() {
		return requestId;
	}

	public static void setRequestId(AtomicInteger requestId) {
		RemotingCommand.requestId = requestId;
	}

	public int getOpaque() {
		return opaque;
	}

	public void setOpaque(int opaque) {
		this.opaque = opaque;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public HashMap<String, String> getExtFields() {
		return extFields;
	}

	public void setExtFields(HashMap<String, String> extFields) {
		this.extFields = extFields;
	}

	@Override
	public String toString() {
		return "RemotingCommand [opaque=" + opaque + ", code=" + code + ", body=" + new String(body) + ", extFields="
				+ extFields + "]";
	}

}
