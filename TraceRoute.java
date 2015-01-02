package net.floodlightcontroller.traceroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.traceroute.Switch.Color;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.Wildcards;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.HexString;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceRoute implements TraceRouteService, IOFMessageListener,
		IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;
	protected static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 6000; // in seconds
	protected static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	private static int count = 0;
	private static final short VLANID_WHITE = (short) -1;
	private static final short VLANID_BLACK = (short) 1;
	private static boolean isColoringInitialized = false;
	protected IRestApiService restApi;

	class HostInfo {
		int sourceIp;
		short port;
	}

	protected Map<Integer, String> ipToSwitchId;
	protected Map<Long, Map<Integer, HostInfo>> switchToHostsInfo;
	protected static Map<Switch, IOFSwitch> switchToIOFSwitchMap;
	protected static Map<String, String> hostMacToSwitchMacMap;
	protected static Map<Integer, Integer> ipToIdentification;
	protected static Map<String, LinkedList> tracepath;
	public static Map<String, Switch> mac_to_switch_object_copy;
	public long time_end = 0, time_start = 0;

	@Override
	public String getName() {
		return TraceRoute.class.getSimpleName();
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
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(TraceRouteService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(TraceRouteService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context
				.getServiceImpl(IFloodlightProviderService.class);
		ipToSwitchId = new HashMap<>();
		switchToHostsInfo = new HashMap<>();
		logger = LoggerFactory.getLogger(TraceRoute.class);
		switchToIOFSwitchMap = new ConcurrentHashMap<Switch, IOFSwitch>();
		hostMacToSwitchMacMap = new ConcurrentHashMap<String, String>();
		restApi = context.getServiceImpl(IRestApiService.class);
		tracepath = new ConcurrentHashMap<String, LinkedList>();
		Map<String, Switch> mac_to_switch_object_copy = new HashMap<String, Switch>();
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApi.addRestletRoutable(new TraceRouteWebRoutable());
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// logger.info("--------------------------------------------------------");
		switch (msg.getType()) {
		case PACKET_IN:
			// logger.info("PacketIn Path...");
			return processPacketInMessage(sw, (OFPacketIn) msg, cntx);
		default:
			break;
		}

		return Command.CONTINUE;
	}

	private net.floodlightcontroller.core.IListener.Command processPacketInMessage(
			IOFSwitch iofSwitch, OFPacketIn msg, FloodlightContext cntx) {
		System.out.println("-----------------------------Count:" + count++);

		if (!isColoringInitialized) {
			Graph g = new Graph();

			g.initializeTopology();
			mac_to_switch_object_copy = g.mac_to_switch_object;
			TwoNodeColoring b = new TwoNodeColoring();
			b.StartTwoNodeColoring(g);
			isColoringInitialized = true;
		}
		OFPacketIn pi = (OFPacketIn) msg;
		// parse the data in packetIn using match
		OFMatch match = new OFMatch();
		match.loadFromPacket(pi.getPacketData(), pi.getInPort());

		int destIP = match.getNetworkDestination();
		int sourceIP = match.getNetworkSource();
		Short inputPort = pi.getInPort();
		long swId = iofSwitch.getId();

		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		Long sourceMACHash = Ethernet.toLong(eth.getSourceMACAddress());

		if (match.getDataLayerType() == Ethernet.TYPE_IPv4
				&& match.getNetworkProtocol() == IPv4.PROTOCOL_ICMP) {
			String dpid = HexString.toHexString(iofSwitch.getId());
			String sMac = HexString.toHexString(sourceMACHash);
			System.out.println("dpid:" + dpid + " mac:" + sMac);
			Switch swtch = Graph.mac_to_switch_object.get(dpid);
			if (swtch == null) {
				System.out.println("WTF!!!!!!!!!!!!!!!!!!!");
			}
			if (!switchToIOFSwitchMap.containsValue(iofSwitch)) {
				switchToIOFSwitchMap.put(swtch, iofSwitch);
			}

			if (!hostMacToSwitchMacMap.containsKey(sMac)) {
				hostMacToSwitchMacMap.put(sMac, dpid);
			}

		}

		// If sourceIP is discovered for the first time, store in map
		// if (!ipToSwitchId.containsKey(sourceIP)) {
		// ipToSwitchId.put(sourceIP, iofSwitch.getId());
		// }

		if (!ipToSwitchId.containsKey(sourceIP)) {
			ipToSwitchId.put(sourceIP, iofSwitch.getStringId());
		}

		if (destIP != 0) {
			if (!switchToHostsInfo.containsKey(swId)) {
				Map<Integer, HostInfo> map = new HashMap<>();
				switchToHostsInfo.put(swId, map);
			}
			if (!switchToHostsInfo.get(swId).containsKey(sourceIP)) {
				HostInfo info = new HostInfo();
				info.sourceIp = sourceIP;
				info.port = inputPort;
				switchToHostsInfo.get(swId).put(sourceIP, info);
			}

			// If dest IP is already discovered then install the forward and
			// reverse rules
			if (switchToHostsInfo.get(swId).containsKey(destIP)) {

				OFMatch reverseMatch = match
						.clone()
						.setDataLayerSource(match.getDataLayerDestination())
						.setDataLayerDestination(match.getDataLayerSource())
						.setNetworkSource(match.getNetworkDestination())
						.setNetworkDestination(match.getNetworkSource())
						.setInputPort(
								switchToHostsInfo.get(iofSwitch.getId()).get(
										match.getNetworkDestination()).port);
				short outPort = switchToHostsInfo.get(iofSwitch.getId()).get(
						match.getNetworkDestination()).port;
				short revOutPort = switchToHostsInfo.get(iofSwitch.getId())
						.get(reverseMatch.getNetworkDestination()).port;
				if (match.getDataLayerType() == Ethernet.TYPE_ARP) {
					installRule(iofSwitch, match, outPort);
					installRule(iofSwitch, reverseMatch, revOutPort);
				} else if (match.getDataLayerType() == Ethernet.TYPE_IPv4
						&& match.getNetworkProtocol() == IPv4.PROTOCOL_ICMP) {

					String dpid = HexString.toHexString(iofSwitch.getId());
					Switch swtch = Graph.mac_to_switch_object.get(dpid);
					Color color = swtch.getColor();

					if (color.equals(Color.WHITE)) {
						System.out.println("WHITE SWITCH-----");
						installRule(iofSwitch, match, outPort);
						installRule(iofSwitch, reverseMatch, revOutPort);
						this.pushPacket(iofSwitch, match, pi, outPort);
						return Command.CONTINUE;
					} else if (color.equals(Color.BLACK)) {
						System.out.println("BLACK SWITCH-----");

						time_start = System.currentTimeMillis();
						installRule(iofSwitch, match,
								OFPort.OFPP_CONTROLLER.getValue());
						installRule(iofSwitch, reverseMatch,
								OFPort.OFPP_CONTROLLER.getValue());

						System.out.println("Generating path");
						LinkedList<String> path = generatePath(iofSwitch, msg,
								outPort, eth);
						if (path != null) {
							System.out.print("Hurray:");
							time_end = System.currentTimeMillis();
							System.out.println("Time consumed"
									+ (time_end - time_start) * 1000);
							for (int i = 0; i < path.size(); i++) {

								System.out.print(path.get(i) + "-->");
							}
							System.out.println();
						}
						System.out.println("Forwarding-----");
						this.pushPacket(iofSwitch, match, pi, outPort);
						return Command.CONTINUE;

					} else {
						System.out
								.println("Color not available on the node please check algorithm again");
						System.exit(1);
					}
				}
			}
		}
		// Flood the packet
		// System.out.println("flooding-----");
		this.pushPacket(iofSwitch, match, pi,
				(short) OFPort.OFPP_FLOOD.getValue());
		return Command.CONTINUE;
	}

	private void installRule(IOFSwitch sw, OFMatch match, short outPort) {
		// create the rule
		OFFlowMod rule = (OFFlowMod) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.FLOW_MOD);
		// set the Flow Removed bit
		rule.setFlags(OFFlowMod.OFPFF_SEND_FLOW_REM);

		// set of actions to apply to this rule
		int len = 0;
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		OFAction outputTo = new OFActionOutput(outPort);
		actions.add(outputTo);

		len = OFActionOutput.MINIMUM_LENGTH;
		setBasicPropForRule(rule, len);

		// If packet of type ICMP
		if (match.getDataLayerType() == Ethernet.TYPE_IPv4) {
			match.setWildcards(Wildcards.FULL.matchOn(Flag.DL_TYPE)
					.matchOn(Flag.IN_PORT).matchOn(Flag.NW_PROTO)
					.withNwSrcMask(32).withNwDstMask(32));
		}
		// If packet of type ARP
		else {
			match.setWildcards(Wildcards.FULL.matchOn(Flag.DL_TYPE)
					.matchOn(Flag.IN_PORT).withNwSrcMask(32).withNwDstMask(32));
		}
		sendFlowMod(sw, rule, actions, match);
	}

	private void sendFlowMod(IOFSwitch sw, OFFlowMod rule,
			ArrayList<OFAction> actions, OFMatch match) {
		rule.setMatch(match);
		rule.setActions(actions);

		try {
			sw.write(rule, null);
			logger.info("Rule installation successfull From:"
					+ match.getNetworkSource() + " to "
					+ match.getNetworkDestination() + " on sw:" + sw.getId());
		} catch (Exception e) {
			logger.error("Rule installation failed");
			e.printStackTrace();
		}
	}

	private void setBasicPropForRule(OFFlowMod rule, int len) {
		rule.setCommand(OFFlowMod.OFPFC_ADD);
		// specify timers for the life of the rule
		rule.setIdleTimeout(TraceRoute.FLOWMOD_DEFAULT_IDLE_TIMEOUT);
		rule.setHardTimeout(TraceRoute.FLOWMOD_DEFAULT_HARD_TIMEOUT);
		rule.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		// specify the length of the flow structure created
		rule.setLength((short) (OFFlowMod.MINIMUM_LENGTH + len));
	}

	private void pushPacket(IOFSwitch sw, OFMatch match, OFPacketIn pi,
			short outport) {

		// create an OFPacketOut for the pushed packet
		OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.PACKET_OUT);

		// update the inputPort and bufferID
		po.setInPort(pi.getInPort());
		po.setBufferId(pi.getBufferId());

		// define the actions to apply for this packet
		OFActionOutput action = new OFActionOutput();
		action.setPort(outport);
		po.setActions(Collections.singletonList((OFAction) action));
		po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

		// set data if it is included in the packet in but buffer id is NONE
		if (pi.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
			byte[] packetData = pi.getPacketData();
			po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
					+ po.getActionsLength() + packetData.length));
			po.setPacketData(packetData);
		} else {
			po.setLength(U16.t(OFPacketOut.MINIMUM_LENGTH
					+ po.getActionsLength()));
		}

		// push the packet to the switch
		try {
			sw.write(po, null);
		} catch (IOException e) {
			logger.error("failed to write packetOut: ", e);
		}
	}

	public LinkedList<String> generatePath(IOFSwitch iofSwitch, OFPacketIn msg,
			Short outputPort, Ethernet eth) {

		System.out.print("----1-----");
		OFPacketIn pi = (OFPacketIn) msg;
		// parse the data in packetIn using match

		// protected Map<Integer, Long> ipToSwitchId;

		OFMatch match = new OFMatch();
		match.loadFromPacket(pi.getPacketData(), pi.getInPort());

		int destIP = match.getNetworkDestination();
		int sourceIP = match.getNetworkSource();
		Short inputPort = pi.getInPort();
		long swId = iofSwitch.getId();

		Iterator it2 = ipToSwitchId.entrySet().iterator();

		String dst_sw_id = null;
		String scr_sw_id = null;
		while (it2.hasNext()) {
			Map.Entry pairs = (Map.Entry) it2.next();
			if (pairs.getKey().equals(destIP)) {
				dst_sw_id = (String) pairs.getValue();
			}
			if (pairs.getKey().equals(sourceIP)) {
				scr_sw_id = (String) pairs.getValue();
			}
		}

		IPv4 ip = (IPv4) eth.getPayload();
		int ident1 = ip.getIdentification();
		String id = sourceIP + ":" + ident1 + ":" + destIP;

		// String id = sourceIP + "";

		String dpid = HexString.toHexString(iofSwitch.getId());
		Switch swtch = Graph.mac_to_switch_object.get(dpid);

		// if(swtch.getMac().equals(scr_sw_id))
		// {
		// return null;
		// }

		LinkedList adj_switch = new LinkedList();
		adj_switch = swtch.getadj();

		Switch f_sw_append = null, f_sw_end = null;
		int flag = 0;
		for (int i = 0; i < adj_switch.size(); i++) {
			Map mp = (Map) adj_switch.get(i);
			Iterator it = mp.entrySet().iterator();
			Map.Entry pairs = (Map.Entry) it.next();
			System.out.println("Input port  " + inputPort);
			System.out.println("value for printing " + pairs.getKey());
			if (pairs.getKey().equals((new Integer(inputPort)))) {
				System.out.println("I entered into the object -1  ");

				f_sw_append = (Switch) pairs.getValue();
			}
			if (pairs.getKey().equals((new Integer(outputPort)))) {
				System.out.println("I entered into the object -2  ");
				f_sw_end = (Switch) pairs.getValue();
				if (f_sw_end.getMac().equals(new String(dst_sw_id))) {
					System.out.print("Setting flag as 1");
					flag = 1;
				}
			}
		}

		if (swtch.getMac().equals(new String(scr_sw_id))) {
			System.out.print("Did i come here");
			traceMap(id, dpid, null);
			return null;
		}

		if (f_sw_append == null) {
			System.out.println("Everytime i m coming here");
			return null;

		}
		if (flag == 0) {
			return (traceMap(id, dpid, f_sw_append));
		}

		if (flag == 1) {
			if (mac_to_switch_object_copy.containsKey(dst_sw_id)) {
				Switch sw = mac_to_switch_object_copy.get(dst_sw_id);
				if (sw.getColor().equals(Color.BLACK)) {
					return (traceMap(id, dpid, f_sw_append));
				}
			}
			LinkedList l_new = new LinkedList();
			l_new = traceMap(id, dpid, f_sw_append);
			l_new.add(dst_sw_id);
			return l_new;
		} else {
			return null;
		}
	}

	public LinkedList<String> traceMap(String id, String dpid, Switch neighbour) {

		Iterator it = tracepath.entrySet().iterator();

		// String[] parts = id.split(":");
		// String part2 = parts[1];

		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			if (pair.getKey().equals(id)) {
				LinkedList l = (LinkedList) pair.getValue();

				if (neighbour.getColor().equals(Color.BLACK)) {// point 3
					System.out.println("Entering point 3");
					System.out.print("----3-----");
					l.add(dpid);
					return l;
				} else {
					System.out.print("----4-----");
					l.add(neighbour.getMac());
					l.add(dpid);
					System.out
							.println("ADDED: point 4--------------------------------"
									+ neighbour.getMac() + "       " + dpid);
					return l;
				}
			}
		}
		LinkedList<String> path = new LinkedList<String>();
		if (neighbour == null) {// point 1
								// System.out.print("------");
			System.out.println("Entering point 1");
			path.add(dpid);
		} else {// point 2
			System.out.print("----5-----");
			System.out.println("Entering point 2");
			path.add(neighbour.getMac());
			path.add(dpid);
		}
		tracepath.put(id, path);
		return path;
	}

	@Override
	public LinkedList<String> getPath() {
		// TODO Auto-generated method stub
		return null;
	}
}
