package net.floodlightcontroller.traceroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import net.floodlightcontroller.traceroute.Switch.Color;

import org.json.JSONException;

public class TwoNodeColoring {
	// private Map mac_to_switch_object;
	public Map<String, Boolean> map_mac_to_visited = new HashMap<String, Boolean>();

	public TwoNodeColoring() {
		// TODO Auto-generated constructor stub
	}

	public void StartTwoNodeColoring(Graph G) {

		Iterator it = Graph.mac_to_switch_object.keySet().iterator();
		while (it.hasNext()) {

			String keyset = (String) it.next();
			map_mac_to_visited.put(keyset, false);
			// System.out.println(pairs.getKey() + " = " + pairs.getValue());
		}

		Iterator it1 = Graph.mac_to_switch_object.entrySet().iterator();
		Map.Entry<String, Switch> pairs = (Map.Entry<String, Switch>) it1
				.next();
		Switch s = pairs.getValue();
		s.setColor(Color.BLACK);

		dfs(G, s);

		Iterator it3 = Graph.mac_to_switch_object.keySet().iterator();
		while (it3.hasNext()) {
			String keyset = (String) it3.next();
			map_mac_to_visited.put(keyset, false);
		}

		Iterator it2 = Graph.mac_to_switch_object.entrySet().iterator();
		while (it2.hasNext()) {
			Map.Entry<String, Switch> pair = (Map.Entry<String, Switch>) it2
					.next();
			Switch s2 = pair.getValue();
			check_color(G, s2);
		}

		Iterator it5 = Graph.mac_to_switch_object.keySet().iterator();
		while (it5.hasNext()) {
			String keyset = (String) it5.next();
			map_mac_to_visited.put(keyset, false);
		}

		Iterator it4 = Graph.mac_to_switch_object.entrySet().iterator();
		Map.Entry<String, Switch> pair_print = (Map.Entry<String, Switch>) it4
				.next();
		Switch src_print = pair_print.getValue();
		print_color_graph(G, src_print);

	}

	public void dfs(Graph G, Switch src) {
		String mac = src.getMac();

		map_mac_to_visited.put(mac, true);

		LinkedList t = G.getadjSwitch(src);

		Iterator it = t.iterator();

		while (it.hasNext()) {

			// Map.Entry<String,Switch> pairs =
			// (Map.Entry<String,Switch>)it.next();
			HashMap<String, Switch> pairs = (HashMap<String, Switch>) it.next();
			Iterator it2 = pairs.entrySet().iterator();
			Map.Entry<String, Switch> pair2 = (Map.Entry<String, Switch>) it2
					.next();
			Switch s = pair2.getValue();
			String pmac = s.getMac();
			if (map_mac_to_visited.get(pmac).equals(false)) {
				if (src.getColor() == Color.BLACK)
					// s.setColor(Color.BLACK);
					// else
					// s.setColor(Color.WHITE);
					s.setColor(Color.WHITE);
				else
					s.setColor(Color.BLACK);

				// System.out.println("MAC---" +s.getMac());
				// System.out.println("Color --" +s.getColor());

				dfs(G, s);
			}

		}
	}

	public void check_color(Graph G, Switch src) {
		String mac = src.getMac();
		map_mac_to_visited.put(mac, true);

		LinkedList t = G.getadjSwitch(src);
		Iterator it = (Iterator) t.iterator();

		while (it.hasNext()) {
			HashMap<String, Switch> pairs = (HashMap<String, Switch>) it.next();
			Iterator it2 = pairs.entrySet().iterator();
			Map.Entry<String, Switch> pair2 = (Map.Entry<String, Switch>) it2
					.next();
			Switch s = pair2.getValue();
			String pmac = s.getMac();
			if (map_mac_to_visited.get(pmac).equals(false)) {
				if (src.getColor() == Color.WHITE
						&& s.getColor() == Color.WHITE)
					s.setColor(Color.BLACK);
			}

		}
	}

	public void print_color_graph(Graph G, Switch src) {
		String mac = src.getMac();
		System.out.println("MAC---" + src.getMac());
		System.out.println("Color --" + src.getColor());

		map_mac_to_visited.put(mac, true);

		LinkedList t = G.getadjSwitch(src);

		Iterator it = t.iterator();

		while (it.hasNext()) {

			HashMap<String, Switch> pairs = (HashMap<String, Switch>) it.next();
			Iterator it2 = pairs.entrySet().iterator();
			Map.Entry<String, Switch> pair2 = (Map.Entry<String, Switch>) it2
					.next();
			Switch s = pair2.getValue();
			String pmac = s.getMac();
			if (map_mac_to_visited.get(pmac).equals(false)) {

				print_color_graph(G, s);
			}

		}

	}

	public static void main(String[] args) {

		// create random bipartite graph with V vertices and E edges; then add F
		// random edges
		/*
		 * int V = Integer.parseInt(args[0]); int E = Integer.parseInt(args[1]);
		 * int F = Integer.parseInt(args[2]);
		 */

		Graph g = new Graph();

		/* Creating Switch objects */

		ParseJson P = new ParseJson();
		try {
			P.captureData();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		int num_links = P.getNumLinks();

		// System.out.println(L.get(i).toString());

		int number_of_switches = P.getNumSwitch();
		g.setswitches(number_of_switches);

		ArrayList<Switch> S = new ArrayList<Switch>();
		for (int i = 0; i < number_of_switches; i++) {
			S.add(new Switch());
		}

		/* Creating a Hash Map */

		int j = 0;
		ArrayList<SwitchLinks> L = P.getLinks();
		for (int i = 0; i < L.size(); i++) {
			String Source = L.get(i).getSrcMac();
			String Dest = L.get(i).getDestMac();
			int srcPort = L.get(i).getSrcPort();
			int destPort = L.get(i).getDestPort();
			if (!Graph.mac_to_switch_object.containsKey(Source)) {
				Graph.mac_to_switch_object.put(Source, S.get(j));
				j = j + 1;
			}
			if (!Graph.mac_to_switch_object.containsKey(Dest)) {
				Graph.mac_to_switch_object.put(Dest, S.get(j));
				j = j + 1;
			}
			Switch src = Graph.mac_to_switch_object.get(Source);
			Switch dst = Graph.mac_to_switch_object.get(Dest);
			src.setMac(Source);
			dst.setMac(Dest);
			g.addEdge_object(src, dst, srcPort);
			g.addEdge_object(dst, src, destPort);
			// g.print_adj_list(src);
			// g.print_adj_list(dst);
		}

		// TwoNodeColoring b = new TwoNodeColoring(g);

	}

}
