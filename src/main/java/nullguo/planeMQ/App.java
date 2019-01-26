package nullguo.planeMQ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import store.AppendMessageCallback;
import store.CommitLog;
import store.MappedFile;
import store.MessageExtBrokerInner;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {   
        MessageExtBrokerInner inner=new MessageExtBrokerInner();
        inner.setBody("dsa".getBytes());
        inner.setBornHost("23.3.33.3");
        inner.setBornTimeStamp(System.currentTimeMillis());
        CommitLog log=new CommitLog();
        log.putMessage(inner);
        inner.setStoreHost("1.1.1.1");
        for(int i=0;i<100;i++) {
         if(i==99) {
        	 inner.setStoreHost("2.2.2.2");
         }	
       	 log.putMessage(inner);
       }       
        MessageExtBrokerInner messageExtBrokerInner=log.getMessage(10032, 102);
        System.out.println(messageExtBrokerInner);
    }
}
