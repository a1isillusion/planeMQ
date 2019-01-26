package nullguo.planeMQ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import store.MappedFile;
import store.MessageExtBrokerInner;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {   File file=new File("G://planeMQ//00000000");
        RandomAccessFile file2=new RandomAccessFile(file, "rw");
        file2.setLength(1024);
        FileChannel channel=file2.getChannel();
        System.out.println(file.length());
        MappedByteBuffer buffer=channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
        buffer.put(new String("fds222").getBytes());
        System.out.println(new String("fds222dddddddddddddddddddd").getBytes().length);
        buffer.flip();
        byte[] bs=new byte[6];
        buffer.get(bs, 0, 6);
        System.out.println( new String(bs) );
        MappedFile file3=new MappedFile("G://planeMQ",0,1024);
        file3.buffer.put(new String("fdsfsdf22223dddddddddddd").getBytes());
        System.out.println( file3.buffer);
        System.out.println( new String(bs) );
        int p=file3.buffer.position();
        int l=file3.buffer.limit();
        file3.buffer.flip();
        file3.buffer.get(bs,0, 6);
        System.out.println( new String(bs) );
        file3.buffer.position(p);
        file3.buffer.limit(l);
        file3.buffer.put(new String("11111111111111111111111").getBytes());

    }
}
