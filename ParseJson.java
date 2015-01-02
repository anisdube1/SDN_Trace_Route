/*
 * Copyright 2014, The Avengers 
 * @author - Nishant Gill
 * */

package net.floodlightcontroller.traceroute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ParseJson {
	private int numSwitch = 0;
	private int numLinks = 0;
	private ArrayList<SwitchLinks> links = new ArrayList<SwitchLinks>();

	public int getNumSwitch() {
		return numSwitch;
	}

	public int getNumLinks() {
		return numLinks;
	}

	public ArrayList<SwitchLinks> getLinks() {
		return links;
	}

	public void captureData() throws IOException, JSONException {
		String switchesCmd;
		String linksCmd;
		JSONObject obj;
		JSONArray arr;
		switchesCmd = "curl http://10.0.2.15:8080/wm/core/controller/switches/json";
		linksCmd = "curl http://10.0.2.15:8080/wm/topology/links/json";
		Runtime r = Runtime.getRuntime();
		Process p1, p2;
		// get the switch data
		p1 = r.exec(switchesCmd);
		BufferedReader in1 = new BufferedReader(new InputStreamReader(
				p1.getInputStream()));
		String str1;
		str1 = in1.readLine();
		arr = new JSONArray(str1);
		numSwitch = arr.length();
		// get the links data
		p2 = r.exec(linksCmd);
		BufferedReader in2 = new BufferedReader(new InputStreamReader(
				p2.getInputStream()));
		String str2 = in2.readLine();
		arr = new JSONArray(str2);
		numLinks = arr.length();
		for (int i = 0; i < numLinks; i++) {
			obj = arr.getJSONObject(i);
			links.add(new SwitchLinks(obj.getString("src-switch"), obj
					.getInt("src-port"), obj.getString("dst-switch"), obj
					.getInt("dst-port")));
		}
		in1.close();
		in2.close();
	}

	public static void main(String[] args) {
		ParseJson P = new ParseJson();
		try {
			P.captureData();
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(P.getNumSwitch());
		System.out.println(P.getNumLinks());
		ArrayList<SwitchLinks> L = P.getLinks();
		for (int i = 0; i < L.size(); i++)
			System.out.println(L.get(i).toString());
	}
}
