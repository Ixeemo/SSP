package pl.edu.agh.kt;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.ListenableFuture;
import net.floodlightcontroller.core.IOFSwitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class StatisticsCollector {
	private static final Logger logger = LoggerFactory.getLogger(StatisticsCollector.class);
	private IOFSwitch sw;
	private Map<DatapathId, IOFSwitch> switches;
	long first = 0;
	long second = 0;
	long diff = 0;
	int interval = 3;
	long thr = 0;
	private HashMap<Integer, Long> bytes = new HashMap<Integer, Long>();
	private HashMap<Integer, Long> both = new HashMap<Integer, Long>();
	private HashMap<Integer, Integer> activeFlowsPerPort;
	//protected static HashMap<IOFSwitch, HashMap<Integer, Integer>> activeFlowsInSw = new HashMap<IOFSwitch, HashMap<Integer, Integer>>();
	protected static Map<IOFSwitch, HashMap<Integer, Integer>> activeFlowsInSw = new HashMap<IOFSwitch, HashMap<Integer, Integer>>();
	public class PortStatisticsPoller extends TimerTask {
		private final Logger logger = LoggerFactory.getLogger(PortStatisticsPoller.class);
		@Override
		public void run() {
			//activeFlowsPerPort = new HashMap<Integer, Integer>();
			//logger.debug("run() begin");
			synchronized (StatisticsCollector.this) {
				for (IOFSwitch sw: switches.values()) {
					activeFlowsPerPort = new HashMap<Integer, Integer>();
				if (sw == null) { // no switch
					logger.error("run() end (no switch)");
					return;
				}
				ListenableFuture<?> future;
				List<OFStatsReply> values = null;
				OFStatsRequest<?> req = null;
				OFStatsRequest<?> reqFlow = null;
				req = sw.getOFFactory().buildPortStatsRequest().setPortNo(OFPort.ANY).build(); 
				//reqFlow = sw.getOFFactory().buildFlowStatsRequest().setOutPort(OFPort.ANY).setTableId(TableId.ALL).build();
				//req = reqFlow;
				try {
					
					String id = sw.getId().toString();
					for (int i=1; i<5; i++) {
						Integer flows = 0;
						
						if (i != Integer.parseInt(id.substring(id.length()-1))) {
							reqFlow = sw.getOFFactory().buildFlowStatsRequest().setOutPort(OFPort.of(i)).setTableId(TableId.ALL).build();
							if (reqFlow != null) {
								future = sw.writeStatsRequest(reqFlow);
								values = (List<OFStatsReply>)
								future.get(PORT_STATISTICS_POLLING_INTERVAL * 1000 / 2, TimeUnit.MILLISECONDS);
							}
							OFFlowStatsReply psr = (OFFlowStatsReply) values.get(0);
							//logger.info("Switch id: {}", sw.getId());
						//logger.info("XID: {}", psr.getXid());
						//logger.info("Switch hash: {}", psr.hashCode());
						
							for (OFFlowStatsEntry pse : psr.getEntries()) {
								//logger.info("\tFlow Info: {}", pse.getMatch());
								flows = flows + 1;
							}
								activeFlowsPerPort.put(i, flows);
								//activeFlowsPerPort.put(i, psr.getEntries().size());
								
								//logger.info("Information: {}", activeFlowsPerPort);
							//}
					}
					}
				
					activeFlowsInSw.put(sw, activeFlowsPerPort);
			
					//logger.info("Information: {}", activeFlowsPerPort);
					//logger.info("Information: {}", activeFlowsInSw);
				} catch (InterruptedException | ExecutionException | TimeoutException ex) {
					logger.error("Error during statistics polling", ex);
				}
			}
			}
			//logger.debug("run() end");
			
		}
	}

	public static final int PORT_STATISTICS_POLLING_INTERVAL = 3000; // in ms
	private static StatisticsCollector singleton;
	//public StatisticsCollector(IOFSwitch sw) {
	public StatisticsCollector(Map<DatapathId, IOFSwitch> switches) {
		//this.sw = sw;
		this.switches = switches;
		//this.activeFlowsInSw = activeFlowsInSw;
		new Timer().scheduleAtFixedRate(new PortStatisticsPoller(), 0,
				PORT_STATISTICS_POLLING_INTERVAL);
	}
	
	public static Map<IOFSwitch, HashMap<Integer, Integer>> getFlows(){
		return activeFlowsInSw;	

	}

	
//	//public static StatisticsCollector getInstance(IOFSwitch sw) {
//		logger.debug("getInstance() begin");
//		synchronized (StatisticsCollector.class) {
//			if (singleton == null) {
//				logger.debug("Creating StatisticsCollector singleton");
//				singleton = new StatisticsCollector(sw);
//			}
//		}
//		singleton = null;
//		logger.debug("getInstance() end");
//		return singleton;
//	}
}