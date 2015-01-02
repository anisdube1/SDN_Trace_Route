package net.floodlightcontroller.traceroute;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IOFSwitch;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class TraceRouteResource extends ServerResource {
	@Get("json")
	public LinkedList retrieve() {
		TraceRouteService traceRouteService = (TraceRouteService) getContext()
				.getAttributes()
				.get(TraceRouteService.class.getCanonicalName());

		LinkedList map = traceRouteService.getPath();

		return map;
	}
}
