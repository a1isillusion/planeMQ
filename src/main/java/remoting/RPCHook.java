package remoting;

public interface RPCHook {
	public void doBeforeRequest(String remoteAddr, RemotingCommand request);

	public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response);
}
