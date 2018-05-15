package com.directions.route;

import java.util.List;
import com.google.android.gms.maps.model.LatLng;

public class Maneuver {
	
	private String maneuver;
	private LatLng location;
	
	public Maneuver(String maneuver, LatLng location){
		this.maneuver = maneuver;
		this.location = location;	
	}
	
	public Maneuver getManeuver(){
		return this;
	}
	
	public String getMove(){
		return maneuver;
	}
	
	public LatLng getLoc(){
		return location;
	}
	
	void setManeuver(String maneuver, LatLng location){
		this.maneuver = maneuver;
		this.location = location;
	}
}