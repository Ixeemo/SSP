package pl.edu.agh.kt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IOFSwitch;
import pl.edu.agh.kt.StatisticsCollector.PortStatisticsPoller;

public class Hosts {
	protected static final Logger logger = LoggerFactory.getLogger(Hosts.class);
	private static List<String> switches = new ArrayList<String>();
	private static List<String> hosts = new ArrayList<String>();
	private static HashMap<String, String> neighbors = new HashMap<String, String>();
	static String addr = "10.0.0.";
	//static int i = 1;
	static String swID = "00:00:00:00:00:00:00:0";
	
	private Hosts(IOFSwitch sw) {
		
	}
	
	public static HashMap<String, String> pairHosts() {
		for(int i = 1; i < 5; i++) {
			hosts.add(addr + String.valueOf(i));
			switches.add(swID + String.valueOf(i));
			neighbors.put(hosts.get(i-1), switches.get(i-1));
		}
		
		//.debug("NEI: {}", neighbors);
		return neighbors;
	}
}
