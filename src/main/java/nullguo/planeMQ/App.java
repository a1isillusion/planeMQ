package nullguo.planeMQ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import common.QueueData;
import io.netty.channel.Channel;
import namesrv.KVConfigManager;
import namesrv.NamesrvController;
import remoting.NettyRemotingClient;
import remoting.NettyRemotingServer;
import remoting.RemotingCommand;
import store.AppendMessageCallback;
import store.CommitLog;
import store.ConsumeQueue;
import store.MappedFile;
import store.MessageExtBrokerInner;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws Exception {
		
		MessageExtBrokerInner inner = new MessageExtBrokerInner();
		inner.setBody("dsa".getBytes());
		inner.setBornHost("23.3.33.3");
		inner.setBornTimeStamp(System.currentTimeMillis());
		CommitLog log = new CommitLog();
		log.putMessage(inner);
		inner.setStoreHost("1.1.1.1");
		for (int i = 0; i < 100; i++) {
			if (i == 99) {
				inner.setStoreHost("2.2.2.2");
			}
			log.putMessage(inner);
		}
		MessageExtBrokerInner messageExtBrokerInner = log.getMessage(10032, 102);
		System.out.println(messageExtBrokerInner);
		ConsumeQueue queue = new ConsumeQueue("topic", 110);
		for (int i = 0; i < 100; i++) {
			System.out.println(queue.putMessagePositionInfo(i, i, i));
		}
		MappedFile file = queue.mappedFileQueue.getFirstMappedFile();
		ByteBuffer buffer = file.selectMappedBuffer(0);
		System.out.println(buffer.getLong());
		for (int i = 0; i < 100; i++) {
			System.out.println(queue.getIndexBuffer(i * 20).getLong());
		}
		RemotingCommand command = new RemotingCommand();
		HashMap<String, String> ext = new HashMap<String, String>();
		ext.put("dsa", "ds1a11");
		command.extFields = ext;
		command.body = new String("htr1").getBytes();
		ByteBuffer buffer2 = command.encode();
		RemotingCommand command2 = RemotingCommand.decode(buffer2);
		System.out.println(command2);
		 
		/*
		 * NettyRemotingClient client=new NettyRemotingClient(); Channel
		 * channel=client.createChannel("localhost:8080"); RemotingCommand command=new
		 * RemotingCommand(1,"dsa"); client.invokeOneway(channel,command);
		 * Thread.sleep(3000); client.shutdown();
		 */
/*		RemotingCommand command=new RemotingCommand(6,null);
		command.getExtFields().put("namespace", "g");
		command.getExtFields().put("key", "r");
		command.getExtFields().put("value", "e");
		command.setRPC_ONEWAY(0);
		NettyRemotingClient client=new NettyRemotingClient();
		Channel channel=client.createChannel("localhost:8080");
		client.invokeOneway(channel, command);*/
		
		
	}
}
