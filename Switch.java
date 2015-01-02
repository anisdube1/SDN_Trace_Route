package net.floodlightcontroller.traceroute;

import java.util.HashMap;
import java.util.LinkedList;

class Switch {
	public enum Color {
		NOCOLOR, WHITE, BLACK
	};

	public Color color;
	public String MAC;
	public HashMap hm = new HashMap<Integer, Switch>();
	public LinkedList adj_object = new LinkedList();

	public Switch() {
		color = Color.NOCOLOR;
		MAC = "";
	}

	public void setMac(String s) {
		MAC = s;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public String getMac() {
		return MAC;
	}

	@Override
	public String toString() {
		return ("Mac is - " + MAC);
	}

	public LinkedList getadj() {
		return adj_object;
	}

}
