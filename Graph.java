package net.floodlightcontroller.traceroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import weiss.nonstandard.PriorityQueue;

// Used to signal violations of preconditions for
// various shortest path algorithms.
class GraphException extends RuntimeException {
	public GraphException(String name) {
		super(name);
	}
}

// Represents an edge in the graph.
class Edge {
	public Vertex dest; // Second vertex in Edge
	public double cost; // Edge cost, not currently used in the code

	public Edge(Vertex d, double c) {
		dest = d;
		cost = c;
	}
}

// Represents an entry in the priority queue for Dijkstra's algorithm.
class Path implements Comparable {
	public Vertex dest; // w
	public double cost; // d(w)

	public Path(Vertex d, double c) {
		dest = d;
		cost = c;
	}

	public int compareTo(Object rhs) {
		double otherCost = ((Path) rhs).cost;

		return cost < otherCost ? -1 : cost > otherCost ? 1 : 0;
	}
}

// Represents a vertex in the graph.
class Vertex {
	public String name; // Vertex name
	public List adj; // Adjacent vertices
	public double dist; // Cost
	public Vertex prev; // Previous vertex on shortest path
	public int scratch;// Extra variable used in algorithm

	public Vertex(String nm) {
		name = nm;
		adj = new LinkedList();
		reset();
	}

	public void reset() {
		dist = Graph.INFINITY;
		prev = null;
		pos = null;
		scratch = 0;
	}

	public PriorityQueue.Position pos; // Used for dijkstra2 (Chapter 23)
}

public class Graph {

	public static final double INFINITY = Double.MAX_VALUE;
	private Map vertexMap = new HashMap(); // Maps String to Vertex
	private Integer numberofswitches;
	public static Map<String, Switch> mac_to_switch_object = new HashMap<String, Switch>();
	public static int i = 0;

	/* Creating a Switch Class */

	public void setswitches(Integer v) {
		numberofswitches = v;
	}

	public Integer getswitches() {
		return numberofswitches;
	}

	/**
	 * Add a new edge to the graph.
	 */
	public void addEdge(String sourceName, String destName, double cost) {
		Vertex v = getVertex(sourceName);
		Vertex w = getVertex(destName);
		v.adj.add(new Edge(w, cost));
	}

	public void print_adj_list(Switch source) {

		System.out.println(source.hm.keySet().toString());
		System.out.println(source.hm.entrySet().toString());
		// System.out.println("Key = " + key);

	}

	public void addEdge_object(Switch source, Switch destination, int port) {

		// hm.put(port, destination);

		HashMap<Integer, Switch> hm = new HashMap<Integer, Switch>();

		hm.put(port, destination);

		source.adj_object.add(hm);
	}

	public LinkedList getadjSwitch(Switch src) {
		return src.adj_object;
	}

	/**
	 * If vertexName is not present, add it to vertexMap. In either case, return
	 * the Vertex.
	 */
	private Vertex getVertex(String vertexName) {
		Vertex v = (Vertex) vertexMap.get(vertexName);
		if (v == null) {
			v = new Vertex(vertexName);
			vertexMap.put(vertexName, v);
		}
		return v;
	}

	public void initializeTopology() {
		ParseJson P = new ParseJson();
		try {
			P.captureData();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		int num_links = P.getNumLinks();

		// System.out.println(L.get(i).toString());

		int number_of_switches = P.getNumSwitch();
		setswitches(number_of_switches);

		ArrayList<Switch> S = new ArrayList<Switch>();
		for (int i = 0; i < number_of_switches; i++) {
			S.add(new Switch());
		}

		// Creating a Hash Map

		int j = 0;
		ArrayList<SwitchLinks> L = P.getLinks();
		for (int i = 0; i < L.size(); i++) {
			String Source = L.get(i).getSrcMac();
			String Dest = L.get(i).getDestMac();
			int srcPort = L.get(i).getSrcPort();
			int destPort = L.get(i).getDestPort();
			if (!mac_to_switch_object.containsKey(Source)) {
				mac_to_switch_object.put(Source, S.get(j));
				j = j + 1;
			}
			if (!mac_to_switch_object.containsKey(Dest)) {
				mac_to_switch_object.put(Dest, S.get(j));
				j = j + 1;
			}
			Switch src = mac_to_switch_object.get(Source);
			Switch dst = mac_to_switch_object.get(Dest);
			src.setMac(Source);
			dst.setMac(Dest);
			addEdge_object(src, dst, srcPort);
			addEdge_object(dst, src, destPort);
			// g.print_adj_list(src);
			// g.print_adj_list(dst);

		}

		Iterator it = mac_to_switch_object.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Switch> pair2 = (Map.Entry<String, Switch>) it
					.next();
			Switch s = pair2.getValue();
			System.out.println("Printing adj list for " + s.getMac());
			LinkedList l = getadjSwitch(s);
			Iterator it2 = l.iterator();
			while (it2.hasNext()) {
				HashMap<String, Switch> pairs = (HashMap<String, Switch>) it2
						.next();
				Iterator it3 = pairs.entrySet().iterator();
				Map.Entry<String, Switch> pair3 = (Map.Entry<String, Switch>) it3
						.next();
				Switch s2 = pair3.getValue();
				String pmac = s2.getMac();
				System.out.println("MAC---" + pmac);
				// System.out.println("Color --" +s.getColor());

			}
		}

	}

	public static void main(String[] args) {
		Graph g = new Graph();

		// Creating Switch objects

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

		// Creating a Hash Map

		int j = 0;
		ArrayList<SwitchLinks> L = P.getLinks();
		for (int i = 0; i < L.size(); i++) {
			String Source = L.get(i).getSrcMac();
			String Dest = L.get(i).getDestMac();
			int srcPort = L.get(i).getSrcPort();
			int destPort = L.get(i).getDestPort();
			if (!mac_to_switch_object.containsKey(Source)) {
				mac_to_switch_object.put(Source, S.get(j));
				j = j + 1;
			}
			if (!mac_to_switch_object.containsKey(Dest)) {
				mac_to_switch_object.put(Dest, S.get(j));
				j = j + 1;
			}
			Switch src = mac_to_switch_object.get(Source);
			Switch dst = mac_to_switch_object.get(Dest);
			src.setMac(Source);
			dst.setMac(Dest);
			g.addEdge_object(src, dst, srcPort);
			g.addEdge_object(dst, src, destPort);
			// g.print_adj_list(src);
			// g.print_adj_list(dst);

		}

		Iterator it = mac_to_switch_object.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Switch> pair2 = (Map.Entry<String, Switch>) it
					.next();
			Switch s = pair2.getValue();
			System.out.println("Printing adj list for " + s.getMac());
			LinkedList l = g.getadjSwitch(s);
			Iterator it2 = l.iterator();
			while (it2.hasNext()) {
				HashMap<String, Switch> pairs = (HashMap<String, Switch>) it2
						.next();
				Iterator it3 = pairs.entrySet().iterator();
				Map.Entry<String, Switch> pair3 = (Map.Entry<String, Switch>) it3
						.next();
				Switch s2 = pair3.getValue();
				String pmac = s2.getMac();
				System.out.println("MAC---" + pmac);
				// System.out.println("Color --" +s.getColor());

			}
		}

	}
}
