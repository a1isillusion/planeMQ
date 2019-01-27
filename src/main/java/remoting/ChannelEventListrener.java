package remoting;

import io.netty.channel.Channel;

public interface ChannelEventListrener {
public void onChannelConnect(final String remoteAddr,Channel channel);
public void onChannelClose(final String remoteAddr, final Channel channel);
public void onChannelException(final String remoteAddr, final Channel channel);
public void onChannelIdle(final String remoteAddr, final Channel channel);
}
