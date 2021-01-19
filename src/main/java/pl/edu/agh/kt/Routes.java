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
import java.util.HashSet;
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
	protected static List<IOFSwitch> switchesID;
	protected static HashMap<String, String> hosts = SdnLabListener.getHosts();
	protected static HashMap<Integer, Integer> val;
	protected static HashMap<Integer, Integer> activeFlowsPerPort;
	protected static HashMap<OFPort, OFPort> ports = new HashMap<OFPort, OFPort>();
	protected static HashMap<IOFSwitch, HashMap<OFPort, OFPort>> toAdd;


	
	public Routes(IRoutingService routingService) {
		super();
		this.routingService = routingService;
	}
	
	public static void calculatePath(IOFSwitch firstSW, Map<IOFSwitch, HashMap<Integer, Integer>> activeFlowsInSw, IPv4Address destIP, OFPacketIn pin, FloodlightContext cntx) {
		String dest = "";
		Set<Link> linkList = SdnLabListener.getLinks();
		int i = 0;
		IOFSwitch sw = firstSW;
		OFPort outPort = OFPort.of(0);
		OFPort inPort = pin.getInPort();
		List<Integer> portToRemove = new ArrayList<>();
		//activeFlowsPerPort 
		switchesID = new ArrayList<>();
		toAdd = new HashMap<IOFSwitch, HashMap<OFPort, OFPort>>();
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
			//logger.info("**************** {} ****************", sw.getId().toString());
			if (sw.getId().toString().contentEquals(dest)) {
				//logger.info("****************YES: ****************");
				outPort = OFPort.of(Integer.parseInt(dest.substring(dest.length()-1)));
				ports.put(outPort, inPort);
				toAdd.put(sw, ports);
				ports = new HashMap<OFPort, OFPort>();
				switchesID.add(sw);
				Collections.reverse(switchesID);
				for (IOFSwitch s: switchesID) {
					//logger.info("**************** {} ************ID****", s);

					HashMap<OFPort, OFPort> port = toAdd.get(s);
					for (OFPort p: port.keySet()) {
						if (s == firstSW) {
							Flows.simpleAdd(s, pin, cntx, p, port.get(p), true);
						}
						Flows.simpleAdd(s, pin, cntx, p, port.get(p), false);
					}
					//Flows.simpleAdd(sw, pin, cntx, outPort, inPort);
				}
				break;
				
			}else {
				
				activeFlowsPerPort = new HashMap<Integer, Integer>(activeFlowsInSw.get(sw));
				
				logger.info("**** Flows: **** {} ***** On: **** {} ", activeFlowsPerPort, sw.getId());
				
				if (activeFlowsPerPort.size() != 3) {
					HashSet<Integer> keys = new HashSet<>();
					String id = sw.getId().toString();
					for (int n=1; n<5; n++) {						
						if (n != Integer.parseInt(id.substring(id.length()-1))) {
							keys.add(n);
						}
					}
					HashSet<Integer> allKeys = new HashSet<>(activeFlowsPerPort.keySet());
					allKeys.addAll(keys);
					allKeys.removeAll(activeFlowsPerPort.keySet());
					for (Integer a: allKeys) {
						activeFlowsPerPort.put(a, 100);
					}
				}
				
				for (Integer p: portToRemove) {
					activeFlowsPerPort.remove(Integer.parseInt(p.toString()));
				}
				//logger.info("**************** {} ******PO USUNIECIU**********", activeFlowsPerPort);
				
				Integer minNum = Collections.min(activeFlowsPerPort.values());
				for (Map.Entry<Integer, Integer> hash: activeFlowsPerPort.entrySet()) {
					if(minNum.equals(hash.getValue())) {
						outPort = OFPort.of(hash.getKey());
						//Flows.simpleAdd(sw, pin, cntx, outPort, inPort);
						ports.put(outPort, inPort);
						toAdd.put(sw, ports);
						ports = new HashMap<OFPort, OFPort>();
						switchesID.add(sw);
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