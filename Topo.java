package net.floodlightcontroller.traceroute;

import java.util.Collection;
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

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.util.HexString;

public class Topo implements IFloodlightModule, IOFMessageListener {

	protected static Map<Switch, IOFSwitch> switchToIOFSwitchMap;
	protected static Map<String, String> hostMacToSwitchMacMap;

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		switchToIOFSwitchMap = new ConcurrentHashMap<Switch, IOFSwitch>();
		hostMacToSwitchMacMap = new ConcurrentHashMap<String, String>();
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub

	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		Long sourceMACHash = Ethernet.toLong(eth.getSourceMACAddress());

		if (eth.getEtherType() == Ethernet.TYPE_IPv4) {
			IPv4 ipPacket = (IPv4) eth.getPayload();
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
				String dpid = HexString.toHexString(sw.getId());
				String sMac = HexString.toHexString(sourceMACHash);
				Switch s = Graph.mac_to_switch_object.get(dpid);
				if (!switchToIOFSwitchMap.containsValue(sw)) {
					switchToIOFSwitchMap.put(s, sw);
				}

				if (!hostMacToSwitchMacMap.containsKey(sMac)) {
					hostMacToSwitchMacMap.put(sMac, dpid);
				}
			}
		}

		return net.floodlightcontroller.core.IListener.Command.CONTINUE;
	}

}
