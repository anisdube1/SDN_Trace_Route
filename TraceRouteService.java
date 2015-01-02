package net.floodlightcontroller.traceroute;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openflow.protocol.OFPacketIn;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;

public interface TraceRouteService extends IFloodlightService {
	public LinkedList<String> getPath();
}
