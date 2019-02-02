package common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class BrokerData implements Comparable<BrokerData> {
	private String cluster;
	private String brokerName;
	private HashMap<Long/* brokerId */, String/* broker address */> brokerAddrs;

	private final Random random = new Random();

	public BrokerData() {

	}

	public BrokerData(String cluster, String brokerName, HashMap<Long, String> brokerAddrs) {
		this.cluster = cluster;
		this.brokerName = brokerName;
		this.brokerAddrs = brokerAddrs;
	}

	/**
	 * Selects a (preferably master) broker address from the registered list. If the
	 * master's address cannot be found, a slave broker address is selected in a
	 * random manner.
	 *
	 * @return Broker address.
	 */
	public String selectBrokerAddr() {
		String addr = this.brokerAddrs.get(0l);
		if (addr == null) {
			List<String> addrs = new ArrayList<String>(brokerAddrs.values());
			return addrs.get(random.nextInt(addrs.size()));
		}

		return addr;
	}

	public HashMap<Long, String> getBrokerAddrs() {
		return brokerAddrs;
	}

	public void setBrokerAddrs(HashMap<Long, String> brokerAddrs) {
		this.brokerAddrs = brokerAddrs;
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((brokerAddrs == null) ? 0 : brokerAddrs.hashCode());
		result = prime * result + ((brokerName == null) ? 0 : brokerName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BrokerData other = (BrokerData) obj;
		if (brokerAddrs == null) {
			if (other.brokerAddrs != null)
				return false;
		} else if (!brokerAddrs.equals(other.brokerAddrs))
			return false;
		if (brokerName == null) {
			if (other.brokerName != null)
				return false;
		} else if (!brokerName.equals(other.brokerName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BrokerData [brokerName=" + brokerName + ", brokerAddrs=" + brokerAddrs + "]";
	}

	public int compareTo(BrokerData o) {
		return this.brokerName.compareTo(o.getBrokerName());
	}

	public String getBrokerName() {
		return brokerName;
	}

	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}
}
