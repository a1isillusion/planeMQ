package nullguo.planeMQ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import broker.BrokerController;
import client.DefaultMQConsumer;
import client.DefaultMQProducter;
import client.LocalOffsetStore;
import client.MessageListener;
import common.Message;
import common.QueueData;
import common.ResponseFuture;
import config.StoreConfig;
import config.SystemConfig;
import io.netty.channel.Channel;
import namesrv.KVConfigManager;
import namesrv.NamesrvController;
import remoting.CommandCode;
import remoting.NettyRemotingClient;
import remoting.NettyRemotingServer;
import remoting.RemotingCommand;
import store.AppendMessageCallback;
import store.CommitLog;
import store.ConsumeQueue;
import store.MappedFile;
import store.MappedFileQueue;
import store.MessageExtBrokerInner;
import store.MessageStore;
import util.RemotingUtil;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws Exception {
		
/*		MessageExtBrokerInner inner = new MessageExtBrokerInner();
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
		System.out.println(command2);*/
		 
		/*
		 * NettyRemotingClient client=new NettyRemotingClient(); Channel
		 * channel=client.createChannel("localhost:8080"); RemotingCommand command=new
		 * RemotingCommand(1,"dsa"); client.invokeOneway(channel,command);
		 * Thread.sleep(3000); client.shutdown();
		 */
/*		NettyRemotingClient client=new NettyRemotingClient();
		Channel channel=client.createChannel("localhost:8080");
		HashMap<String, QueueData> topicQueueConfig=new HashMap<String, QueueData>();
		QueueData queueData=new QueueData();
		queueData.setBrokerName(SystemConfig.brokerName);
		queueData.setReadQueueNums(8);
		queueData.setWriteQueueNums(8);
		topicQueueConfig.put("i", queueData);
		RemotingCommand command=new RemotingCommand(CommandCode.REGISTER_BROKER,null);
		command.getExtFields().put("clusterName", SystemConfig.clusterName);
		command.getExtFields().put("brokerAddr", RemotingUtil.parseChannelLocalAddr(channel));
		command.getExtFields().put("brokerName", SystemConfig.brokerName);
		command.getExtFields().put("brokerId", ""+SystemConfig.brokerId);
		command.setBody(JSON.toJSONString(topicQueueConfig).getBytes());
		command.setRPC_ONEWAY(0);
		client.invokeOneway(channel, command);*/
/*		MessageExtBrokerInner message=new MessageExtBrokerInner();
		message.setTopic("i");
		message.setQueueId(6);
		message.setBornTimeStamp(System.currentTimeMillis());
		message.setBody(new String("test messagestore").getBytes());
		messageStore.putMessage(message);
		messageStore.putMessage(message);
		messageStore.putMessage(message);
		System.out.println(JSON.toJSONString(messageStore.getMessage("1", "i", 6, 0, 4)));	*/
/*		NettyRemotingClient client=new NettyRemotingClient();
		List<String> namesrvList=new ArrayList<String>();
		namesrvList.add("localhost:8080");
		client.updateNameServerAddressList(namesrvList);
		client.registerBroker();*/
/*		DefaultMQProducter producter=new DefaultMQProducter();
		producter.setNamesrvAddr("localhost:8038");
		producter.getAndUpdateRouteInto();
		for(int i=0;i<100;i++) {
			producter.sendMessage(new Message("i",new String("i:"+i).getBytes()));
			Thread.sleep(50);
		}
		
		DefaultMQConsumer consumer=new DefaultMQConsumer();
		consumer.subscribe("i");
		consumer.setNamesrvAddr("localhost:8038");
		consumer.registerMessageListener(new MessageListener() {			
			public void consumeMessage(List<Message> msgs) {
				for(Message message:msgs) {
				System.out.println("consuming message :"+new String(message.getBody()));		
				}						
			}
		});
		consumer.start();
		consumer.localOffsetStore.paintConsumeTable();
		consumer.shutdown();*/
/*		MessageStore store=new MessageStore();
		store.createTopic("i", 0, 8);
		MessageExtBrokerInner message=new MessageExtBrokerInner();
		message.setTopic("i");
		message.setQueueId(6);
		message.setBornTimeStamp(System.currentTimeMillis());
		message.setBody(new String("test messagestore").getBytes());
		store.putMessage(message);
		store.putMessage(message);
		message.setQueueId(2);
		store.putMessage(message);
		Map<String,Map<Integer,Long>> totalOffset1=store.getTotalOffset();
		Map<String,Map<Integer,Long>> totalOffset=JSON.parseObject(JSON.toJSONString(totalOffset1),new TypeReference<Map<String, Map<Integer,Long>>>() {});
        LocalOffsetStore offsetStore=new LocalOffsetStore();
        offsetStore.updateOffsetTable(totalOffset);
        offsetStore.paintOffsetTable();*/
/*		MessageExtBrokerInner message=new MessageExtBrokerInner();
		message.setTopic("i");
		message.setQueueId(6);
		message.setBornTimeStamp(System.currentTimeMillis());
		message.setBody(new String("test messagestore").getBytes());
		mappedFileQueue.putMessage(message, new AppendMessageCallback());
		mappedFileQueue.putMessage(message, new AppendMessageCallback());
		mappedFileQueue.backup();*/
/*		MessageExtBrokerInner message=new MessageExtBrokerInner();
		message.setTopic("i");
		message.setQueueId(6);
		message.setBornTimeStamp(System.currentTimeMillis());
		message.setBody(new String("test messagestore").getBytes());
        MessageStore store=new MessageStore();
        store.createTopic("i", 0, 10);
        store.putMessage(message);
        store.backup();*/
/*		args = new String[] { "-b", "8080", "-s", "G://planeMQ", "-n", "localhost:8037" };*/
		try {
			// create Options object
			Options options = new Options();
			options.addOption("n", true, "nameserver");
			options.addOption("b", true, "broker");
			options.addOption("s", true, "store");

			// create the command line parser
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

			// check the options have been set correctly
			if (cmd.hasOption("b")) {
				SystemConfig.brokerPort = cmd.getOptionValue("b");
				SystemConfig.namesrvAddr = cmd.getOptionValue("n");
				StoreConfig.storePath = cmd.getOptionValue("s");
				BrokerController brokerController = new BrokerController(Integer.parseInt(SystemConfig.brokerPort));
			} else if (cmd.hasOption("n")) {
				int port = Integer.parseInt(cmd.getOptionValue("n"));
				NamesrvController namesrvController = new NamesrvController(port);
			}
		} catch (Exception ex) {
			System.out.println("Unexpected exception:" + ex.getMessage());
		}
	}
}
