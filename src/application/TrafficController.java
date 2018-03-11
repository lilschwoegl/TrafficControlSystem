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
import java.util.concurrent.locks.ReentrantLock;

//import application.Direction;
import observer.TrafficObserver;
import observer.TrafficUpdateObservable;
import simulator.Config;
import simulator.MotorVehicle;
import tracking.Track;

import org.opencv.core.Point;
import org.opencv.core.Rect;

import application.Direction;
import application.Color;

public class TrafficController implements TrafficObserver {
	// private subclass for keeping track of vehicles w/in range of intersection traffic cameras
	private class Vehicle {
		public int id = 0;
		public MotorVehicle vehicle = null;
		public Instant timestampLastObserved = null;
		public Direction direction = null;//Direction.North;
		public Vehicle(int trackId, MotorVehicle simVehicle, Instant firstObserved) {
			this.id = trackId;
			this.vehicle = simVehicle;
			this.timestampLastObserved = firstObserved;
			this.direction =
				  vehicle.getDirection() == simulator.MotorVehicle.Direction.NORTH ? Direction.North
				: vehicle.getDirection() == simulator.MotorVehicle.Direction.EAST ? Direction.East
				: vehicle.getDirection() == simulator.MotorVehicle.Direction.WEST ? Direction.West
				: Direction.South;
		}
	}
	
	//public enum TravelDirection { North, South, East, West }
	public enum SignalLogicConfiguration { FixedTimers, OnDemand }
	
	private ScheduledExecutorService  taskExecutor = Executors.newScheduledThreadPool(10);
	
	private static SignalLogicConfiguration signalLogicConfiguration = SignalLogicConfiguration.FixedTimers;
	private static double intersectionWidth = Config.simDisplayWidth - (2 * Config.roadStripLength);
	private static double intersectionHeight = Config.simDisplayHeight - (2 * Config.roadStripLength);
	private static double minDistanceEW = intersectionWidth; 
	private static double minDistanceNS = intersectionHeight;
	private static double maxDistanceEW = -intersectionWidth;
	private static double maxDistanceNS = -intersectionHeight;
	
	// settings for fixed-timer signal logic
	private static TimeUnit timeUnitForFixedTimerConfiguration = TimeUnit.SECONDS;
	private static long periodForFixedTimerConfiguration = 10; // green light duration, normally this would be 30s - 120s in real life
	private static boolean isNS = false; // true=North-South signals own green, false means East-West signals own green
	
	// system settings. TODO: move to config file
	private static int secondsYellowLightDuration = 3;		// duration for light to stay yellow when changing to red
	public static int GetSecondsYellowLightDuration() { return secondsYellowLightDuration; }
	
	private static int secondsMaxGreenLightDuration = 30;	// max duration a light may stay green if any are waiting on a green
	public static int GetSecondsMaxGreenLightDuration() { return secondsMaxGreenLightDuration; } 
	
	private static int maxFeetLightMonitoring = 200;		// max range a light is to monitor vehicles & pedestrians 
	public static int GetMaxFeetLightMonitoring() { return maxFeetLightMonitoring; } 
	
	private static long maxSecondsVehicleAgeToTrack = 5L; // N seconds: if vehicles collection contains any objects not updated any sooner, they're dropped
	
	// Traffic lights are assumed to be placed on the FAR side of an intersection for visibility reasons
	@SuppressWarnings("serial")
	private ArrayList<TrafficLight> trafficLights = new ArrayList<TrafficLight>() {{
		add(new TrafficLight(Direction.North));
		add(new TrafficLight(Direction.South));
		add(new TrafficLight(Direction.East));
		add(new TrafficLight(Direction.West));
	}};
	
	// vehicles currently using the intersection
	private HashMap<Integer,Vehicle> vehicles = new HashMap<Integer,Vehicle>();
	
	//constructor
	public TrafficController()
	{
		log("System timestamp: " + LocalDateTime.now());
		log("simDisplayWidth=" + Config.simDisplayWidth + ", simDisplayHeight="  + Config.simDisplayHeight);
		log("intersectionWidth=" + intersectionWidth + ", intersectionHeight=" + intersectionHeight);
		log("Distances: minDistanceEW=%s maxDistanceEW=%s minDistanceNS=%s maxDistanceNS=%s",
				minDistanceEW, maxDistanceEW, minDistanceNS, maxDistanceNS);
		
		log("Logic configuration: %s", signalLogicConfiguration.toString());
		if (signalLogicConfiguration == SignalLogicConfiguration.FixedTimers) {
			log("Starting SignalLogicConfiguration scheduled task");
			taskExecutor.scheduleAtFixedRate(() -> {
				log("Calling SignalLogicConfiguration...");
				ToggleTrafficLightsForFixedTimerConfig();
			}, 0, periodForFixedTimerConfiguration, timeUnitForFixedTimerConfiguration);
		}
		
		log("Starting DropOldVehiclesFromCollector scheduled task");
		taskExecutor.scheduleAtFixedRate(() -> {
			try {
				log("Calling DropOldVehiclesFromCollector...");
				DropOldVehiclesFromCollector();
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}, 0, 1, TimeUnit.SECONDS);
	
		log("Starting TrafficLight monitor scheduled task");
		taskExecutor.scheduleAtFixedRate(() -> {
			try {
				log(GetStatusForAllLights());
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}, 0, 1, TimeUnit.SECONDS);
	}
	
	/****************************** PRIVATE METHODS *******************************/
	
	// toggle traffic lights
	private void ToggleTrafficLightsForFixedTimerConfig() {
		if (signalLogicConfiguration != SignalLogicConfiguration.FixedTimers)
			return;
		
		try {
			//log("ToggleTrafficLightsForFixedTimerConfig (start): %s", GetStatusForAllLights());
			
			// change any green lights to red
			//log("ToggleTrafficLightsForFixedTimerConfig: triggering all green lights to change to red");
			for (TrafficLight light : trafficLights) {
				if (light.GetColor() == Color.Green) {
					taskExecutor.submit(() -> {
						light.TurnRed();
					});
				}
			}
			
			// wait sufficient time for lights to turn red
			//log("ToggleTrafficLightsForFixedTimerConfig: waiting for lights to finish cycling to red...");
			int numLights = trafficLights.size();
			int numRedLights = 0;
			int numChecks = 5;
			while (numChecks > 0 && numRedLights < numLights) {				
				numRedLights = 0;
				for (TrafficLight light : trafficLights) {
					if (light.GetColor() == Color.Red)
						numRedLights++;
				}
				//log("ToggleTrafficLightsForFixedTimerConfig: %d red lights", numRedLights);
				if (numRedLights < numLights)
					Thread.sleep(1000L);
				numChecks--;
			}
			
			// toggle direction change
			isNS = !isNS;
			
			// change signals in sets, simple version is 2 sets, toggling, seeding with North-South lights
			//log("ToggleTrafficLightsForFixedTimerConfig: triggering lights for new direction to change to green, isNS=%s", isNS);
			ArrayList<TrafficLight> lights = isNS ? GetTrafficLights(Direction.North) : GetTrafficLights(Direction.East);
			lights.addAll(isNS ? GetTrafficLights(Direction.South) : GetTrafficLights(Direction.West));
			for (TrafficLight light : lights) {
				if (light.GetColor() != Color.Green) {
					taskExecutor.submit(() -> {
						light.TurnGreen();
					});
				}
			}
		}
		catch (Exception ex) { ex.printStackTrace(); }
		finally {
		}
		
		//log("ToggleTrafficLightsForFixedTimerConfig (finish): %s", GetStatusForAllLights());
	}
	
	/****************************** PUBLIC METHODS *******************************/
	
	public Color RequestGreenLight(MotorVehicle car) {
		/*	- priority order: opposing traffic has green, oncoming left-turn is green
			- look at curr direction: is signal already green? Y -> grant green light
			- does cross traffic have green?
			- is opposing left-turn in progress? 
				Y -> wait up to 10s for opposing signal to change from green (look at its lastChanged time)
				N -> is opposing left-turn already waiting on green?
					Y -> give them green light
					N - grant a green light
		 	- 
		
		*/
		
		return Color.Red;
	}
	
	// returns all TrafficLight objects
	public ArrayList<TrafficLight> GetTrafficLights() {
		ArrayList<TrafficLight> list = new ArrayList<TrafficLight>();
		for (TrafficLight light : trafficLights) {
			list.add(light);
		}
		return list;
	}
	
	// returns 1st TrafficLight for the requested travel direction, uses MotorVehicle.Direction enum
	public TrafficLight GetTrafficLight(simulator.MotorVehicle.Direction forDirection) {
		for (TrafficLight light : trafficLights) {
			if (	(forDirection == simulator.MotorVehicle.Direction.NORTH	&& light.getTravelDirection() == Direction.North)
				||	(forDirection == simulator.MotorVehicle.Direction.SOUTH	&& light.getTravelDirection() == Direction.South)
				||	(forDirection == simulator.MotorVehicle.Direction.EAST	&& light.getTravelDirection() == Direction.East)
				||	(forDirection == simulator.MotorVehicle.Direction.WEST	&& light.getTravelDirection() == Direction.West)
			)
				return light;
		}
		return null;
	}
	
	// returns 1st TrafficLight for the requested travel direction, uses TrafficController.Direction enum
	public TrafficLight GetTrafficLight(Direction forDirection) {
		for (TrafficLight light : trafficLights) {
			if (light.getTravelDirection() == forDirection)
				return light;
		}
		return null;
	}
	
	// returns all TrafficLight objects for the requested travel direction, uses MotorVehicle.Direction enum
	public ArrayList<TrafficLight> GetTrafficLights(simulator.MotorVehicle.Direction forDirection) {
		ArrayList<TrafficLight> list = new ArrayList<TrafficLight>();
		for (TrafficLight light : trafficLights) {
			if (	(forDirection == simulator.MotorVehicle.Direction.NORTH	&& light.getTravelDirection() == Direction.North)
				||	(forDirection == simulator.MotorVehicle.Direction.SOUTH	&& light.getTravelDirection() == Direction.South)
				||	(forDirection == simulator.MotorVehicle.Direction.EAST	&& light.getTravelDirection() == Direction.East)
				||	(forDirection == simulator.MotorVehicle.Direction.WEST	&& light.getTravelDirection() == Direction.West)
			)
				list.add(light);
		}
		return list;
	}
	
	// returns all TrafficLight objects for the requested travel direction, uses TrafficController.Direction enum
	public ArrayList<TrafficLight> GetTrafficLights(Direction forDirection) {
		ArrayList<TrafficLight> list = new ArrayList<TrafficLight>();
		for (TrafficLight light : trafficLights) {
			if (light.getTravelDirection() == forDirection)
				list.add(light);
		}
		return list;
	}
	
	// return true if a vehicle is in the effective range of a camera
	public boolean IsInRangeOfCamera(MotorVehicle car) {
		boolean inRange = false;
		double dist = car.distToIntersection();
		simulator.MotorVehicle.Direction dir = car.getDirection();
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
	
	// record vehicle movement updates, ignore anything not in-range of cameras
	@Override
	public void update(MotorVehicle vehicle) {
		// TODO Auto-generated method stub
		boolean isInRange = IsInRangeOfCamera(vehicle);
		//log("Track %d is %f pixels from intersection, lane %d, inRange = %s", vehicle.getTrack().track_id, vehicle.distToIntersection(), vehicle.getLane(), isInRange);
		if (isInRange) {
			if (Config.doTrafficControllerTrackEventLogging)
				log("Track %d is %f pixels from intersection, inRange = %s", vehicle.getTrack().track_id, vehicle.distToIntersection(), isInRange);
			vehicles.put(vehicle.getTrack().track_id, new Vehicle(vehicle.getTrack().track_id, vehicle, Instant.now()));
		}
	}
		
	// fn to purge obsolete vehicles from the collection
	private void DropOldVehiclesFromCollector() {
		log("DropOldVehiclesFromCollector: %d vehicles to examine", vehicles.size());
		
		// nothing to track? exit early.
		if (vehicles.size() == 0)
			return;
		
		ReentrantLock lock = new ReentrantLock();
		try {
			lock.lock();
			
			Instant now = Instant.now();
			
			// list of vehicles to remove, do NOT remove items from collection while iterating thru it
			List<Integer> idsToRemove = new LinkedList<>();
			
			for (Entry<Integer,Vehicle> entry : vehicles.entrySet()) {
				Instant lastObserved = entry.getValue().timestampLastObserved;
				long age = ChronoUnit.SECONDS.between(lastObserved, now);
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
		catch (Exception ex) { ex.printStackTrace(); }
		finally {
			if (lock.isLocked())
				lock.unlock();
		}
	}
	
	public String GetStatusForAllLights() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("TrafficLights Status: ");
		int numLights = 0;
		for (TrafficLight light : trafficLights) {
			if (++numLights > 1)
				sb.append(String.format("\t"));
			sb.append(String.format("%04d=%s", light.getID(), light.GetColor()));
		}
		
		return sb.toString();
	}
	
	private void log(String format, Object ... args) {
		if (Config.doTrafficControllerLogging) {
			System.out.println(String.format("%s %s %s", "TrafficController:", Instant.now().toString(), String.format(format, args)));
		}
	}
}
