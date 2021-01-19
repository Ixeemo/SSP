package pl.edu.agh.kt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.internal.OFSwitchHandshakeHandler;
import net.floodlightcontroller.core.internal.OFSwitchManager;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.core.IFloodlightProviderService;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Link;

public class SdnLabListener implements IFloodlightModule, IOFMessageListener {

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;
	protected ITopologyService topologyService;
	protected IRoutingService routingService;
	protected static ILinkDiscoveryService linkService;
	protected static Routes routing;
	protected static HashMap<String, String> hosts;
	protected OFSwitchManager swManager;
	protected IPv4Address destIP;
	//protected Iterable<IOFSwitch> swList = new ArrayList<>();
	protected static Map<DatapathId, IOFSwitch> swList = new HashMap<DatapathId, IOFSwitch>();
	protected Map<IOFSwitch, HashMap<Integer, Integer>> activeFlows;
	private static IOFSwitchService switchService;
	
	
	@Override
	public String getName() {
		return SdnLabListener.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		activeFlows = Collections.synchronizedMap(new HashMap<IOFSwitch, HashMap<Integer, Integer>>());
		logger.info("************* NEW PACKET IN *************");
		logger.info("**** Switch: {} ****", sw.getId());
		
		// TODO LAB 6
		OFPacketIn pin = (OFPacketIn) msg;
		
		//OFPort outPort = OFPort.of(0);
		
		PacketExtractor extractor = new PacketExtractor();
		extractor.packetExtract(cntx);
		destIP = extractor.getDestIP();
		//logger.info("***{} ***", extractor.getDestIP());
		
		activeFlows = StatisticsCollector.getFlows();
		
		//logger.info("**************** {} ************GET FLOWS****", activeFlowsInSw);

		Routes.calculatePath(sw, activeFlows, destIP, pin, cntx);
//		if (pin.getInPort() == OFPort.of(1)) {
//			outPort = OFPort.of(2);
//		} else
//			outPort = OFPort.of(1);		
		return Command.STOP;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRoutingService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(SdnLabListener.class);
		topologyService = context.getServiceImpl(ITopologyService.class);
		routingService = context.getServiceImpl(IRoutingService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		linkService = context.getServiceImpl(ILinkDiscoveryService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		logger.info("******************* START **************************");
		topologyService.addListener(new SdnLabTopologyListener());
		linkService.addListener(new SndLinkListener());
		routing = new Routes(routingService);	
		
	}
	public static Routes getRouting() {
		return routing;
		}
	
	public static HashMap<String, String> getHosts() {
		hosts = Hosts.pairHosts();
		return hosts;
	}
	
	public static Map<DatapathId, IOFSwitch> getSwitches() {
		swList = switchService.getAllSwitchMap();
		return swList;
		}
	
	public static Set<Link> getLinks() {
		Set<Link> linkList = linkService.getLinks().keySet();
		return linkList;
		}
	
}
