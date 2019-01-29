package namesrv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import common.BrokerData;
import common.BrokerLiveInfo;
import common.QueueData;
import common.TopicRouteData;
import io.netty.channel.Channel;

public class RouteInfoManager {
private final static long BROKER_CHANNEL_EXPIRED_TIME = 1000 * 60 * 2;
private final ReadWriteLock lock = new ReentrantReadWriteLock();
private final HashMap<String/* topic */, List<QueueData>> topicQueueTable;
private final HashMap<String/* brokerName */, BrokerData> brokerAddrTable;
private final HashMap<String/* clusterName */, Set<String/* brokerName */>> clusterAddrTable;
private final HashMap<String/* brokerAddr */, BrokerLiveInfo> brokerLiveTable;
public RouteInfoManager() {
    this.topicQueueTable = new HashMap<String, List<QueueData>>(1024);
    this.brokerAddrTable = new HashMap<String, BrokerData>(128);
    this.clusterAddrTable = new HashMap<String, Set<String>>(32);
    this.brokerLiveTable = new HashMap<String, BrokerLiveInfo>(256);
}
public void deleteTopic(final String topic) {
    try {
        try {
            this.lock.writeLock().lockInterruptibly();
            this.topicQueueTable.remove(topic);
        } finally {
            this.lock.writeLock().unlock();
        }
    } catch (Exception e) {
    	System.out.println("deleteTopic Exception");
    }
}
public void registerBroker(
        final String clusterName,
        final String brokerAddr,
        final String brokerName,
        final long brokerId,
        final String haServerAddr,
        final HashMap<String,QueueData> topicConfig,
        final Channel channel) {
        try {
            try {
                this.lock.writeLock().lockInterruptibly();

                Set<String> brokerNames = this.clusterAddrTable.get(clusterName);
                if (null == brokerNames) {
                    brokerNames = new HashSet<String>();
                    this.clusterAddrTable.put(clusterName, brokerNames);
                }
                brokerNames.add(brokerName);

                boolean registerFirst = false;

                BrokerData brokerData = this.brokerAddrTable.get(brokerName);
                if (null == brokerData) {
                    registerFirst = true;
                    brokerData = new BrokerData(clusterName, brokerName, new HashMap<Long, String>());
                    this.brokerAddrTable.put(brokerName, brokerData);
                }
                String oldAddr = brokerData.getBrokerAddrs().put(brokerId, brokerAddr);
                registerFirst = registerFirst || (null == oldAddr);

                if (null != topicConfig
                    && 0l == brokerId) {
                    if (registerFirst) {
                        for (Map.Entry<String, QueueData> entry : topicConfig.entrySet()) {
                           List<QueueData> queueDatas=this.topicQueueTable.get(entry.getKey());
                           if(queueDatas==null) {
                        	   queueDatas=new ArrayList<QueueData>();
                        	   queueDatas.add(entry.getValue());
                        	   this.topicQueueTable.put(entry.getKey(), queueDatas);
                           }else {
							   queueDatas.add(entry.getValue());
						   }                           	                  	
                        }
                    }
                }

                BrokerLiveInfo prevBrokerLiveInfo = this.brokerLiveTable.put(brokerAddr,
                    new BrokerLiveInfo(
                        System.currentTimeMillis(),
                        1,
                        channel,
                        haServerAddr));
                if (null == prevBrokerLiveInfo) {
                	System.out.println("new broker registered, "+brokerAddr+" HAServer: "+haServerAddr+"");
                }

            } finally {
                this.lock.writeLock().unlock();
            }
        } catch (Exception e) {
        	System.out.println("registerBroker Exception");
        }
}
public void unregisterBroker(
        final String clusterName,
        final String brokerAddr,
        final String brokerName,
        final long brokerId) {
        try {
            try {
                this.lock.writeLock().lockInterruptibly();
                this.brokerLiveTable.remove(brokerAddr);

                boolean removeBrokerName = false;
                BrokerData brokerData = this.brokerAddrTable.get(brokerName);
                if (null != brokerData) {
                    brokerData.getBrokerAddrs().remove(brokerId);

                    if (brokerData.getBrokerAddrs().isEmpty()) {
                        this.brokerAddrTable.remove(brokerName);
                        removeBrokerName = true;
                    }
                }

                if (removeBrokerName) {
                    Set<String> nameSet = this.clusterAddrTable.get(clusterName);
                    if (nameSet != null) {
                        nameSet.remove(brokerName);

                        if (nameSet.isEmpty()) {
                            this.clusterAddrTable.remove(clusterName);
                        }
                    }
                    Iterator<Entry<String, List<QueueData>>> itMap = this.topicQueueTable.entrySet().iterator();
                    while (itMap.hasNext()) {
                        Entry<String, List<QueueData>> entry = itMap.next();
                        List<QueueData> queueDataList = entry.getValue();
                        Iterator<QueueData> it = queueDataList.iterator();
                        while (it.hasNext()) {
                            QueueData qd = it.next();
                            if (qd.getBrokerName().equals(brokerName)) {
                                it.remove();
                            }
                        }

                        if (queueDataList.isEmpty()) {
                            itMap.remove();
                        }
                    }
                }
            } finally {
                this.lock.writeLock().unlock();
            }
        } catch (Exception e) {
        	System.out.println("unregisterBroker Exception");
        }
}
@SuppressWarnings("unchecked")
public TopicRouteData pickupTopicRouteData(final String topic) {
    TopicRouteData topicRouteData = new TopicRouteData();
    boolean foundQueueData = false;
    boolean foundBrokerData = false;
    Set<String> brokerNameSet = new HashSet<String>();
    List<BrokerData> brokerDataList = new LinkedList<BrokerData>();
    topicRouteData.setBrokerDatas(brokerDataList);

    try {
        try {
            this.lock.readLock().lockInterruptibly();
            List<QueueData> queueDataList = this.topicQueueTable.get(topic);
            if (queueDataList != null) {
                topicRouteData.setQueueDatas(queueDataList);
                foundQueueData = true;

                Iterator<QueueData> it = queueDataList.iterator();
                while (it.hasNext()) {
                    QueueData qd = it.next();
                    brokerNameSet.add(qd.getBrokerName());
                }

                for (String brokerName : brokerNameSet) {
                    BrokerData brokerData = this.brokerAddrTable.get(brokerName);
                    if (null != brokerData) {
                        BrokerData brokerDataClone = new BrokerData(brokerData.getCluster(), brokerData.getBrokerName(), (HashMap<Long, String>) brokerData
                            .getBrokerAddrs().clone());
                        brokerDataList.add(brokerDataClone);
                        foundBrokerData = true;
                    }
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }
    } catch (Exception e) {
    	System.out.println("pickupTopicRouteData Exception");
    }

    if (foundBrokerData && foundQueueData) {
        return topicRouteData;
    }

    return null;
}
public void scanNotActiveBroker() {
    Iterator<Entry<String, BrokerLiveInfo>> it = this.brokerLiveTable.entrySet().iterator();
    while (it.hasNext()) {
        Entry<String, BrokerLiveInfo> next = it.next();
        long last = next.getValue().getLastUpdateTimestamp();
        if ((last + BROKER_CHANNEL_EXPIRED_TIME) < System.currentTimeMillis()) {
            next.getValue().getChannel().close();
            it.remove();
            this.onChannelDestroy(next.getKey(), next.getValue().getChannel());
        }
    }
}
public void onChannelDestroy(String remoteAddr, Channel channel) {
    String brokerAddrFound = null;
    if (channel != null) {
        try {
            try {
                this.lock.readLock().lockInterruptibly();
                Iterator<Entry<String, BrokerLiveInfo>> itBrokerLiveTable =
                    this.brokerLiveTable.entrySet().iterator();
                while (itBrokerLiveTable.hasNext()) {
                    Entry<String, BrokerLiveInfo> entry = itBrokerLiveTable.next();
                    if (entry.getValue().getChannel() == channel) {
                        brokerAddrFound = entry.getKey();
                        break;
                    }
                }
            } finally {
                this.lock.readLock().unlock();
            }
        } catch (Exception e) {
        	System.out.println("onChannelDestroy Exception");
        }
    }

    if (null == brokerAddrFound) {
        brokerAddrFound = remoteAddr;
    }

    if (brokerAddrFound != null && brokerAddrFound.length() > 0) {

        try {
            try {
                this.lock.writeLock().lockInterruptibly();
                this.brokerLiveTable.remove(brokerAddrFound);
                String brokerNameFound = null;
                boolean removeBrokerName = false;
                Iterator<Entry<String, BrokerData>> itBrokerAddrTable =
                    this.brokerAddrTable.entrySet().iterator();
                while (itBrokerAddrTable.hasNext() && (null == brokerNameFound)) {
                    BrokerData brokerData = itBrokerAddrTable.next().getValue();

                    Iterator<Entry<Long, String>> it = brokerData.getBrokerAddrs().entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<Long, String> entry = it.next();
                        String brokerAddr = entry.getValue();
                        if (brokerAddr.equals(brokerAddrFound)) {
                            brokerNameFound = brokerData.getBrokerName();
                            it.remove();
                            break;
                        }
                    }

                    if (brokerData.getBrokerAddrs().isEmpty()) {
                        removeBrokerName = true;
                        itBrokerAddrTable.remove();
                    }
                }

                if (brokerNameFound != null && removeBrokerName) {
                    Iterator<Entry<String, Set<String>>> it = this.clusterAddrTable.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<String, Set<String>> entry = it.next();
                        Set<String> brokerNames = entry.getValue();
                        boolean removed = brokerNames.remove(brokerNameFound);
                        if (removed) {
                            if (brokerNames.isEmpty()) {
                                it.remove();
                            }
                            break;
                        }
                    }
                }

                if (removeBrokerName) {
                    Iterator<Entry<String, List<QueueData>>> itTopicQueueTable =
                        this.topicQueueTable.entrySet().iterator();
                    while (itTopicQueueTable.hasNext()) {
                        Entry<String, List<QueueData>> entry = itTopicQueueTable.next();
                        List<QueueData> queueDataList = entry.getValue();
                        Iterator<QueueData> itQueueData = queueDataList.iterator();
                        while (itQueueData.hasNext()) {
                            QueueData queueData = itQueueData.next();
                            if (queueData.getBrokerName().equals(brokerNameFound)) {
                                itQueueData.remove();
                            }
                        }
                        if (queueDataList.isEmpty()) {
                            itTopicQueueTable.remove();
                        }
                    }
                }
            } finally {
                this.lock.writeLock().unlock();
            }
        } catch (Exception e) {
        	System.out.println("onChannelDestroy Exception");
        }
    }
}
public void printAllPeriodically() {
    try {
        try {
            this.lock.readLock().lockInterruptibly();
            System.out.println("--------------------------------------------------------");
            {
            	System.out.println("topicQueueTable SIZE: "+ this.topicQueueTable.size());
                Iterator<Entry<String, List<QueueData>>> it = this.topicQueueTable.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, List<QueueData>> next = it.next();
                    System.out.println("topicQueueTable Topic: "+next.getKey()+" "+next.getValue());
                }
            }

            {
            	System.out.println("brokerAddrTable SIZE: "+ this.brokerAddrTable.size());
                Iterator<Entry<String, BrokerData>> it = this.brokerAddrTable.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, BrokerData> next = it.next();
                    System.out.println("brokerAddrTable brokerName: "+next.getKey()+" "+next.getValue());
                }
            }

            {
            	System.out.println("brokerLiveTable SIZE: "+ this.brokerLiveTable.size());
                Iterator<Entry<String, BrokerLiveInfo>> it = this.brokerLiveTable.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, BrokerLiveInfo> next = it.next();
                    System.out.println("brokerLiveTable brokerAddr:"+next.getKey()+" "+next.getValue());
                }
            }

            {
            	System.out.println("clusterAddrTable SIZE: "+ this.clusterAddrTable.size());
                Iterator<Entry<String, Set<String>>> it = this.clusterAddrTable.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, Set<String>> next = it.next();
                    System.out.println("clusterAddrTable clusterName: "+next.getKey()+" "+next.getValue());
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }
    } catch (Exception e) {
    	System.out.println("printAllPeriodically Exception");
    }
}
}
