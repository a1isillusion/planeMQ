package common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import remoting.RemotingCommand;

public class ResponseFuture {
private CountDownLatch countDownLatch =new CountDownLatch(1);	
public long createTimestamp=System.currentTimeMillis();
public int opaque;
public RemotingCommand response;
public ResponseFuture(int opaque) {
	this.opaque=opaque;
}
public void setResponse(RemotingCommand response) {
	this.response=response;
}
public long getCreateTimestamp() {
	return this.createTimestamp;
}
public RemotingCommand waitResponse(final long timeoutMillis) throws Exception{
	this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
	return this.response;
}
}
