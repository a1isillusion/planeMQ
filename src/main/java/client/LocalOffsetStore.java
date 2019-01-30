package client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import common.MessageQueue;

public class LocalOffsetStore {
public ConcurrentMap<MessageQueue, AtomicLong> offsetTable =new ConcurrentHashMap<MessageQueue, AtomicLong>();
}
