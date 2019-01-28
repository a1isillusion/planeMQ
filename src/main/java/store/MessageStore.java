package store;

import java.util.concurrent.ConcurrentMap;

public class MessageStore {
public CommitLog commitLog;
public ConcurrentMap<String/* topic */, ConcurrentMap<Integer/* queueId */, ConsumeQueue>> consumeQueueTable;
}
