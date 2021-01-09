package pl.edu.agh.kt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.internal.OFSwitchHandshakeHandler;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.linkdiscovery.internal.LinkDiscoveryManager;


import net.floodlightcontroller.topology.ITopologyListener;

public class SdnLabTopologyListener implements ITopologyListener {
	protected static final Logger logger = LoggerFactory.getLogger(SdnLabTopologyListener.class);
	private List<DatapathId> swList = new ArrayList<>();
	protected Map<DatapathId, IOFSwitch> switches = new HashMap<DatapathId, IOFSwitch>();
	


	@Override
	public void topologyChanged(List<LDUpdate> linkUpdates) {
		logger.debug("Received topology status");

		for (LDUpdate update : linkUpdates) {
			switch (update.getOperation()) {
			case LINK_UPDATED:
				//logger.debug("Link updated. Update {}", update.toString());
				break;
			case LINK_REMOVED:
				//logger.debug("Link removed. Update {}", update.toString());
				//here could be garbage collector to remove sw singleton object
				break;
			case SWITCH_UPDATED:
				//logger.debug("Switch updated. Update {}", update.toString());
				//swList.add(update.getSrc());
				switches = SdnLabListener.getSwitches();
				//for (IOFSwitch s: switches.values()) {
				for (IOFSwitch s: switches.values()) {
					//logger.debug("******* {} *******", s.toString());
					StatisticsCollector collector = new StatisticsCollector(s);
				}
				//SdnLabListener.getRouting().calculateSpfTree(switches); //it should be when packet in arrives

				break;
			case SWITCH_REMOVED:
				//logger.debug("Switch removed. Update {}", update.toString());
				break;
			default:
				break;
		
	 }
	 }
	}
}

