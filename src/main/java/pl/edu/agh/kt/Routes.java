package pl.edu.agh.kt;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.topology.TopologyInstance;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.internal.LinkDiscoveryManager;
import net.floodlightcontroller.linkdiscovery.internal.LinkInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Routes {
	private IRoutingService routingService;
	protected static java.util.Map<Link,LinkInfo>links;	
	private ITopologyService topoInstance;
	//protected List<Path> paths = new ArrayList<>();
	protected static final Logger logger = LoggerFactory.getLogger(Routes.class);
	protected List<Route> paths = new ArrayList<>();
	protected static List<DatapathId> switchesID = new ArrayList<>();
	protected static HashMap<String, String> hosts = SdnLabListener.getHosts();
	protected static HashMap<Integer, Integer> val;
	protected static HashMap<Integer, Integer> activeFlowsPerPort;

	
	public Routes(IRoutingService routingService) {
		super();
		this.routingService = routingService;
	}
	
	public static void calculatePath(IOFSwitch firstSW, HashMap<IOFSwitch, HashMap<Integer, Integer>> activeFlowsInSw, IPv4Address destIP, OFPacketIn pin, FloodlightContext cntx) {
		String dest = "";
		Set<Link> linkList = SdnLabListener.getLinks();
		int i = 0;
		IOFSwitch sw = firstSW;
		OFPort outPort = OFPort.of(0);
		OFPort inPort = pin.getInPort();
		List<Integer> portToRemove = new ArrayList<>();
		for (String h: hosts.keySet()) {
			if (destIP.toString().contentEquals(h)) {
				dest = hosts.get(h);
				break;
			}
		}		
		//for (IOFSwitch sw: activeFlowsInSw.keySet()) {
		for (int numSW; i<5; i++) {
			//			if (i == 0 && sw != firstSW) {
//				IOFSwitch s = sw;
//				val = activeFlowsInSw.get(sw);
//				sw = firstSW;
//				activeFlowsInSw.put(s, val);
//				i = 1;
//			}			
			logger.info("**************** {} ****************", sw.getId().toString());
			if (sw.getId().toString().contentEquals(dest)) {
				logger.info("****************YES: ****************");
				outPort = OFPort.of(Integer.parseInt(dest.substring(dest.length()-1)));
				Flows.simpleAdd(sw, pin, cntx, outPort, inPort);
				break;
			}else {
				
				activeFlowsPerPort = activeFlowsInSw.get(sw);
				logger.info("**************** {} ****************", activeFlowsPerPort);
							
					
//				if( activeFlowsPerPort.containsKey(Integer.parseInt(inPort.toString()))) {
//					portToRemove.add(inPort.getPortNumber());
//				}
				
				for (Integer p: portToRemove) {
					activeFlowsPerPort.remove(Integer.parseInt(p.toString()));
				}
				logger.info("**************** {} ****************", activeFlowsPerPort);
				
				Integer minNum = Collections.min(activeFlowsPerPort.values());
				for (Map.Entry<Integer, Integer> hash: activeFlowsPerPort.entrySet()) {
					if(minNum.equals(hash.getValue())) {
						outPort = OFPort.of(hash.getKey());
						Flows.simpleAdd(sw, pin, cntx, outPort, inPort);
						switchesID.add(sw.getId());
						//logger.info("**** {} *****", outPort);
						break;						
					}
				}
				for (Link link: linkList) {
					if (link.getSrc() == sw.getId()) {
						if (link.getSrcPort() == outPort) {
							inPort = link.getDstPort();
							sw = SdnLabListener.getSwitches().get(link.getDst());
							portToRemove.add(Integer.parseInt(link.getDstPort().toString()));
							//linkList.remove(link);
							break;
						}
					}
					
				}
				//inPort = outPort; // change to the port from nei switch 
				//activeFlowsPerPort.get(minNum);
				
			}
		}
	}
	
	public void calculateNei(DatapathId src, Map<DatapathId, IOFSwitch> swList) {
		//TODO lab9
			for (DatapathId dst: swList.keySet()) {
				if (src != dst) {
					Route route = calculateSpf(src, dst);
					
					//logger.info("Route: {}", route.getPath().toString());
				}
			}
		}
	
	public void calculateSpfTree(Map<DatapathId, IOFSwitch> swList) {
		//TODO lab9
		for (DatapathId src: swList.keySet()) {
			for (DatapathId dst: swList.keySet()) {
				if (src != dst) {
					Route route = calculateSpf(src, dst);
					//logger.info("Route: {}", route.getPath().toString());
				}
			}
		}
		//logger.info("Calculating SPF tree");
		
	}
	public Route calculateSpf(DatapathId src, DatapathId dst) {
		Route route = routingService.getRoute(src, dst, U64.of(0));
		paths = routingService.getRoutes(src, dst, false);
		
		
		//logger.info("***PATHS: {}***",  paths);
		//paths = routingService.getPathsFast(src, dst);
		
		//logger.info("**************** {} ****************", paths);
		return route;
	}
}