package client;

import java.util.List;

import common.Message;

public interface MessageListener {
	public void consumeMessage(List<Message> msgs);
}
