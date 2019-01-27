package remoting;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {

	@Override
	protected void encode(ChannelHandlerContext ctx, RemotingCommand remotingCommand, ByteBuf out) throws Exception {
        try {
            ByteBuffer data = remotingCommand.encode();
            out.writeBytes(data);
        } catch (Exception e) {
        	System.out.println("encode exception");
            ctx.channel().close();
        }
		
	}

}
