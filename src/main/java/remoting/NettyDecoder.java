package remoting;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class NettyDecoder extends LengthFieldBasedFrameDecoder {
private static final int FRAME_MAX_LENGTH =16777216;
public NettyDecoder() {
   super(FRAME_MAX_LENGTH, 0, 4, 0, 0);
}
@Override
public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
    ByteBuf frame = null;
    try {
        frame = (ByteBuf) super.decode(ctx, in);
        if (null == frame) {
            return null;
        }
        ByteBuffer byteBuffer = frame.nioBuffer();
        return RemotingCommand.decode(byteBuffer);
    } catch (Exception e) {
    	System.out.println("decode exception");
    	ctx.channel().close();
    } finally {
        if (null != frame) {
            frame.release();
        }
    }
    return null;
} 
}
