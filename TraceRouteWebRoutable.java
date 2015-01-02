package net.floodlightcontroller.traceroute;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class TraceRouteWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/paths", TraceRouteResource.class);
		return router;
	}

	@Override
	public String basePath() {
		// TODO Auto-generated method stub
		return "/wm/traceroute";
	}

}
