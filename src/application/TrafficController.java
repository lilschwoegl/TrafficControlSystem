package application;

import java.lang.String;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.lang.Double;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import observer.TrafficObserver;
import observer.TrafficUpdateObservable;
import simulator.Config;
import simulator.MotorVehicle;
import simulator.MotorVehicle.Direction;
import tracking.Track;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class TrafficController implements TrafficObserver {
	// private subclass for keeping track of vehicles w/in range of intersection traffic cameras
	private class Vehicle {
		public int id = 0;
		public MotorVehicle vehicle = null;
		public Instant timestamp = null;
		public Vehicle(int id, MotorVehicle v, Instant ts) {
			this.id = id;
			this.vehicle = v;
			this.timestamp = ts;
		}
	}
	private HashMap<Integer,Vehicle> vehicles = new HashMap<Integer,Vehicle>();
	
	public enum TravelDirection { North, South, East, West }
	
	private static double intersectionWidth = Config.simDisplayWidth - (2 * Config.roadStripLength);
	private static double intersectionHeight = Config.simDisplayHeight - (2 * Config.roadStripLength);
	private static double minDistanceEW = intersectionWidth; 
	private static double minDistanceNS = intersectionHeight;
	private static double maxDistanceEW = -intersectionWidth;
	private static double maxDistanceNS = -intersectionHeight;
	
	// system settings. TODO: move to config file
	private static int secondsYellowLightDuration = 3;		// duration for light to stay yellow when changing to red
	public static int GetSecondsYellowLightDuration() { return secondsMaxGreenLightDuration; }
	
	private static int secondsMaxGreenLightDuration = 30;	// max duration a light may stay green if any are waiting on a green
	public static int GetSecondsMaxGreenLightDuration() { return secondsMaxGreenLightDuration; } 
	
	private static int maxFeetLightMonitoring = 200;		// max range a light is to monitor vehicles & pedestrians 
	public static int GetMaxFeetLightMonitoring() { return maxFeetLightMonitoring; } 
	
	private static long maxSecondsVehicleAgeToTrack = 5L; // N seconds: if vehicles collection contains any objects not updated any sooner, they're dropped
	
	// Traffic lights are assumed to be placed on the FAR side of an intersection for visibility reasons
	@SuppressWarnings("serial")
	private HashMap<TravelDirection,TrafficLight> trafficLights = new HashMap<TravelDirection,TrafficLight>() {{
		put(TravelDirection.North, new TrafficLight(TravelDirection.North));
		put(TravelDirection.South, new TrafficLight(TravelDirection.South));
		put(TravelDirection.East, new TrafficLight(TravelDirection.East));
		put(TravelDirection.West, new TrafficLight(TravelDirection.West));
	}};
	
	private ScheduledExecutorService  taskExecutor = Executors.newScheduledThreadPool(1);
	
	//constructor
	public TrafficController()
	{
		log("System timestamp: " + LocalDateTime.now());
		log("simDisplayWidth=" + Config.simDisplayWidth + ", simDisplayHeight="  + Config.simDisplayHeight);
		log("intersectionWidth=" + intersectionWidth + ", intersectionHeight=" + intersectionHeight);
		log("Distances: minDistanceEW=%s maxDistanceEW=%s minDistanceNS=%s maxDistanceNS=%s",
				minDistanceEW, maxDistanceEW, minDistanceNS, maxDistanceNS);
		
		log("Starting DropOldVehiclesFromCollector scheduled task");
		taskExecutor.scheduleAtFixedRate(() -> {
			try {
				log("Calling DropOldVehiclesFromCollector...");
				DropOldVehiclesFromCollector();
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}, 0, 1000L, TimeUnit.MILLISECONDS);
	}
	
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
	
	// return true if a vehicle is in the effective range of a camera
	public boolean IsInRangeOfCamera(MotorVehicle car) {
		boolean inRange = false;
		double dist = car.distToIntersection();
		Direction dir = car.getDirection();
		switch (dir) {
			case WEST:
			case EAST:
				inRange = dist >= maxDistanceEW && dist <= minDistanceEW; 
				break;
			case NORTH:
			case SOUTH:
				inRange = dist >= maxDistanceNS && dist <= minDistanceNS; 
				break;
		}
		//log("Vehicle " + car.getTrack().track_id + " IsInRangeOfCamera: " + inRange);
		return inRange;
	}
	
	// fn to purge obsolete vehicles from the collection
	private void DropOldVehiclesFromCollector() {
		log("DropOldVehiclesFromCollector: %d vehicles to examine", vehicles.size());
		
		// nothing to track? exit early.
		if (vehicles.size() == 0)
			return;
		Instant now = Instant.now();
		
		// list of vehicles to remove, do NOT remove items from collection while iterating thru it
		List<Integer> idsToRemove = new LinkedList<>();
		
		for (Entry<Integer,Vehicle> entry : vehicles.entrySet()) {
			Instant then = entry.getValue().timestamp;
			long age = ChronoUnit.SECONDS.between(then, now);
			log("vehicle %d age = %ds", entry.getKey(), age);
			if (age > maxSecondsVehicleAgeToTrack) {
				idsToRemove.add(entry.getKey());
				log("Removing vehicle %d due to aging", entry.getKey());
			}
			else if (!IsInRangeOfCamera(entry.getValue().vehicle)) {
				idsToRemove.add(entry.getKey());
				log("Removing vehicle %d due to out-of-range condition", entry.getKey());
			}
		}
		if (!idsToRemove.isEmpty()) {
			idsToRemove.forEach(id -> vehicles.remove(id));
			log("DropOldVehiclesFromCollector: %d vehicles remain in collection", vehicles.size());
		}
	}
	
	// record vehicle movement updates, ignore anything not in-range of cameras
	@Override
	public void update(MotorVehicle vehicle) {
		// TODO Auto-generated method stub
		boolean isInRange = IsInRangeOfCamera(vehicle);
		log("Track %d is %f pixels from intersection, inRange = %s", vehicle.getTrack().track_id, vehicle.distToIntersection(), isInRange);
		if (isInRange) {
			//log("Track %d is %f pixels from intersection, inRange = %s", vehicle.getTrack().track_id, vehicle.distToIntersection(), isInRange);
			vehicles.put(vehicle.getTrack().track_id, new Vehicle(vehicle.getTrack().track_id, vehicle, Instant.now()));
		}
	}
	
	private void log(String format, Object ... args) {
		if (Config.doTrafficControllerLogging) {
			System.out.println(String.format("%s %s %s", "TrafficController:", Instant.now().toString(), String.format(format, args)));
		}
	}
}
