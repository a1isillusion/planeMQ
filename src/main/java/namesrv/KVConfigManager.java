package namesrv;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.alibaba.fastjson.JSON;


public class KVConfigManager {
private final ReadWriteLock lock = new ReentrantReadWriteLock();
private final HashMap<String/* Namespace */, HashMap<String/* Key */, String/* Value */>> configTable =new HashMap<String, HashMap<String, String>>();

	    public void putKVConfig(final String namespace, final String key, final String value) {
	        try {
	            this.lock.writeLock().lockInterruptibly();
	            try {
	                HashMap<String, String> kvTable = this.configTable.get(namespace);
	                if (null == kvTable) {
	                    kvTable = new HashMap<String, String>();
	                    this.configTable.put(namespace, kvTable);
	                }
	                kvTable.put(key, value);
	            } finally {
	                this.lock.writeLock().unlock();
	            }
	        } catch (InterruptedException e) {
	        	System.out.println("putKVConfig InterruptedException");
	        }
	    }

	    public void deleteKVConfig(final String namespace, final String key) {
	        try {
	            this.lock.writeLock().lockInterruptibly();
	            try {
	                HashMap<String, String> kvTable = this.configTable.get(namespace);
	                if (null != kvTable) {
	                    kvTable.remove(key);
	                }
	            } finally {
	                this.lock.writeLock().unlock();
	            }
	        } catch (InterruptedException e) {
	        	System.out.println("deleteKVConfig InterruptedException");
	        }
	    }

	    public byte[] getKVListByNamespace(final String namespace) {
	        try {
	            this.lock.readLock().lockInterruptibly();
	            try {
	                HashMap<String, String> kvTable = this.configTable.get(namespace);
	                if (null != kvTable) {
	                    return JSON.toJSONString(kvTable).getBytes();
	                }
	            } finally {
	                this.lock.readLock().unlock();
	            }
	        } catch (InterruptedException e) {
	        	System.out.println("getKVListByNamespace InterruptedException");
	        }

	        return null;
	    }

	    public String getKVConfig(final String namespace, final String key) {
	        try {
	            this.lock.readLock().lockInterruptibly();
	            try {
	                HashMap<String, String> kvTable = this.configTable.get(namespace);
	                if (null != kvTable) {
	                    return kvTable.get(key);
	                }
	            } finally {
	                this.lock.readLock().unlock();
	            }
	        } catch (InterruptedException e) {
	        	System.out.println("getKVConfig InterruptedException");
	        }

	        return null;
	    }

	    public void printAllPeriodically() {
	        try {
	            this.lock.readLock().lockInterruptibly();
	            try {
	                System.out.println("--------------------------------------------------------");
	                System.out.println("configTable SIZE: "+ this.configTable.size());
	                {
	                    Iterator<Entry<String, HashMap<String, String>>> it =
	                        this.configTable.entrySet().iterator();
	                    while (it.hasNext()) {
	                        Entry<String, HashMap<String, String>> next = it.next();
	                        Iterator<Entry<String, String>> itSub = next.getValue().entrySet().iterator();
	                        while (itSub.hasNext()) {
	                            Entry<String, String> nextSub = itSub.next();
	                            System.out.println("configTable NS: "+next.getKey()+" Key: "+nextSub.getKey()+" Value: "+nextSub.getValue());
	                        }
	                    }
	                }
	            } finally {
	                this.lock.readLock().unlock();
	            }
	        } catch (InterruptedException e) {
	        	System.out.println("printAllPeriodically InterruptedException");
	        }
	    }
}
