package remoting;

public class CommandCode {
	public static final int SUCCESS = 0;
	public static final int SYSTEM_ERROR = 1;
	public static final int SYSTEM_BUSY = 2;
	public static final int REQUEST_CODE_NOT_SUPPORTED = 3;
	public static final int TRANSACTION_FAILED = 4;
	// namesrv
	public static final int QUERY_NOT_FOUND = 5;
	public static final int PUT_KV_CONFIG = 6;
	public static final int GET_KV_CONFIG = 7;
	public static final int DELETE_KV_CONFIG = 8;
	public static final int REGISTER_BROKER = 9;
	public static final int UNREGISTER_BROKER = 10;
	public static final int GET_ROUTEINTO_BY_TOPIC = 11;
	public static final int GET_ROUTEINTO = 12;
	// broker
	public static final int SEND_MESSAGE = 13;
	public static final int PULL_MESSAGE = 14;
	public static final int CREATE_TOPIC = 15;
	public static final int GET_OFFSET = 16;
}
