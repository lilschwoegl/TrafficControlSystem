package application;

import java.lang.String;
import java.lang.Double;
import java.util.*;

import observer.TrafficObserver;
import observer.TrafficUpdateObservable;
import simulator.MotorVehicle;
import tracking.Track;

public class TrafficController implements TrafficObserver{
	public enum TravelDirection { North, South, East, West }
	
	// system settings. TODO: move to config file
	private static int secondsYellowLightDuration = 3;		// duration for light to stay yellow when changing to red
	public static int GetSecondsYellowLightDuration() { return secondsMaxGreenLightDuration; }
	
	private static int secondsMaxGreenLightDuration = 30;	// max duration a light may stay green if any are waiting on a green
	public static int GetSecondsMaxGreenLightDuration() { return secondsMaxGreenLightDuration; } 
	
	private static int maxFeetLightMonitoring = 200;		// max range a light is to monitor vehicles & pedestrians 
	public static int GetMaxFeetLightMonitoring() { return maxFeetLightMonitoring; } 
	
	// Traffic lights are assumed to be placed on the FAR side of an intersection for visibility reasons
	@SuppressWarnings("serial")
	private HashMap<TravelDirection,TrafficLight> trafficLights = new HashMap<TravelDirection,TrafficLight>() {{
		put(TravelDirection.North, new TrafficLight(TravelDirection.North));
		put(TravelDirection.South, new TrafficLight(TravelDirection.South));
		put(TravelDirection.East, new TrafficLight(TravelDirection.East));
		put(TravelDirection.West, new TrafficLight(TravelDirection.West));
	}};
	
	//private Stack q = new  
	
	// if feetInFrontOfTrafficLight is < 0, then it's behind the light
	public void Notify(String idVehicle, TravelDirection travelDirection, Double feetInFrontOfTrafficLight, Double speedMPH) {
		if (feetInFrontOfTrafficLight > maxFeetLightMonitoring)
			return; // too far away
		TrafficLight myLight = GetTrafficLight(travelDirection);
		if (myLight == null)
			return; // no light available (uh oh!)
		
		// process business rules to see if lights need to change
	}

	private boolean doesCurrTrafficHaveGreen(TravelDirection travelDirection) {
		return false;
	}
	
	public TrafficLight GetTrafficLight(TravelDirection forDirection) {
		return trafficLights.containsKey(forDirection)
			? trafficLights.get(forDirection)
			: null;
	}

	@Override
	public void update(MotorVehicle motor) {
		// TODO Auto-generated method stub
		if (motor.distToIntersection() > -50)
			System.out.printf("TrafficController: Track %d is %f pixels from intersection\n", motor.getTrack().track_id, motor.distToIntersection());
	}
}
