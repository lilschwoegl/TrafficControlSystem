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

import application.Direction;
import observer.TrafficObserver;
import observer.TrafficUpdateObservable;
import tracking.Track;

import org.opencv.core.Point;
import org.opencv.core.Rect;

//import application.Direction;
//import application.Color;
import simulator.MotorVehicle;
//import simulator.Display;

public class TrafficController implements TrafficObserver {
	
	// private subclass for keeping track of vehicles w/in range of intersection traffic cameras
	private class Vehicle {
		public int id = 0;
		public MotorVehicle vehicle = null;
		public Instant timestampLastObserved = null;
		public Direction direction = application.Direction.North;
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
	
	// types of controller configurations, default = FixedTimers
	public enum SignalLogicConfiguration { FixedTimers, OnDemand }
	
	// thread pool
	private ScheduledExecutorService  taskExecutor = Executors.newScheduledThreadPool(10);
	
	// intersection dimensions
	private SignalLogicConfiguration signalLogicConfiguration = SignalLogicConfiguration.FixedTimers;
	private double intersectionWidthNS = 0;
	private double intersectionWidthEW = 0;
	private int numLanesNS = 0;
	private int numLanesEW = 0;
	private double laneWidthNS = 0;
	private double laneWidthEW = 0;
	
	// settings for fixed-timer signal logic
	private static TimeUnit timeUnitForFixedTimerConfiguration = TimeUnit.SECONDS;
	private static boolean isNS = false; // true=North-South signals own green, false means East-West signals own green
	
	// Traffic lights are assumed to be placed on the FAR side of an intersection for visibility reasons
	private ArrayList<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
	
	// collection tracking vehicles currently using the intersection
	private HashMap<Integer,Vehicle> vehicles = new HashMap<Integer,Vehicle>();
	
		
	// constructor: traffic cameras are assumed to be placed on the far side of the intersection to privide safest viewing angles by vehicles
	public TrafficController(int numLanesNS, double laneWidthNS, int numLanesEW, double laneWidthEW)
	{
		log("System timestamp: " + LocalDateTime.now());
		this.numLanesNS = numLanesNS;
		this.numLanesEW = numLanesEW;
		this.laneWidthNS = laneWidthNS;
		this.laneWidthEW = laneWidthEW;
		// intersection dimensions determined
		this.intersectionWidthEW = (double)this.numLanesNS * this.laneWidthNS;
		this.intersectionWidthNS = (double)this.numLanesEW * this.laneWidthEW;
		
		log("Intersection Dimensions:"
				+ "\n  Lanes NS      %d"
				+ "\n  Width NS      %f"
				+ "\n  Lanes EW      %d"
				+ "\n  Height EW     %f"
				+ "\n  Camera Range  %f (pixels)",
				this.numLanesNS, this.laneWidthNS, this.numLanesEW, this.laneWidthEW, Config.pixelsCameraRange);
		
		log("Logic configuration: %s", signalLogicConfiguration.toString());
		if (signalLogicConfiguration == SignalLogicConfiguration.FixedTimers) {
			log("Starting SignalLogicConfiguration scheduled task");
			taskExecutor.scheduleAtFixedRate(() -> {
				log("Calling SignalLogicConfiguration...");
				ToggleTrafficLightsForFixedTimerConfig();
			}, 0, Config.periodForFixedTimerConfiguration, timeUnitForFixedTimerConfiguration);
		}
		
		// add requested traffic lights
		if (numLanesNS > 0) {
			AddTrafficLight(application.Direction.North);
			AddTrafficLight(application.Direction.South);
		}
		if (numLanesEW > 0) {
			AddTrafficLight(application.Direction.East);
			AddTrafficLight(application.Direction.West);
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
	
	/****************************** PUBLIC METHODS *******************************/
	
	public TrafficLight AddTrafficLight(Direction directionOfTravel) {
		log("AddTrafficLight(%s)", directionOfTravel.toString());
		TrafficLight light = new TrafficLight(directionOfTravel); 
		trafficLights.add(light);
		return light;
	}
	
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
		double dist = GetDistanceToCamera(car);
		return dist > 0 && dist <= Config.pixelsCameraRange;
	}
	
	// record vehicle movement updates, ignore anything not in-range of cameras
	@Override
	public void update(MotorVehicle vehicle) {
		// TODO Auto-generated method stub
		boolean isInRange = IsInRangeOfCamera(vehicle);
		double distToCamera = GetDistanceToCamera(vehicle);		
		//log("Track %d is %f pixels from intersection, lane %d, inRange = %s", vehicle.getTrack().track_id, vehicle.distToIntersection(), vehicle.getLane(), isInRange);
		if (isInRange) {
			if (Config.doTrafficControllerTrackEventLogging)
				log("Track %d is %f pixels from intersection, %f pixes from camera, inRange = %s", vehicle.getTrack().track_id, vehicle.distToIntersection(), distToCamera, isInRange);
			vehicles.put(vehicle.getTrack().track_id, new Vehicle(vehicle.getTrack().track_id, vehicle, Instant.now()));
		}
	}
		
	// return distance of vehicle to its facing camera, considering vehicle's distance to intersection and intersection width
	public double GetDistanceToCamera(MotorVehicle car) {
		double distance = 0;
		switch (GetCarDirectionOfTravel(car)) {
			case West:
			case East:
				distance = car.distToIntersection() + this.intersectionWidthEW;
				break;
			case North:
			case South:
				distance = car.distToIntersection() + this.intersectionWidthNS;
				break;
		}
		return distance;
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
	
	// convert simulator vehicle direction into global application direction (for local calculations)
	private application.Direction GetCarDirectionOfTravel(MotorVehicle car) {
		return car.getDirection() == simulator.MotorVehicle.Direction.NORTH ? application.Direction.North
			: car.getDirection() == simulator.MotorVehicle.Direction.EAST ? application.Direction.East
			: car.getDirection() == simulator.MotorVehicle.Direction.WEST ? application.Direction.West
			: application.Direction.South;
	}
	
	// fn to purge obsolete vehicles from the collection, either because they are out of range of traffic light cameras or they haven't been updated for a long time
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
				if (age > Config.maxSecondsVehicleAgeToTrack) {
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
		if (Config.doTrafficControllerLsogging) {
			System.out.println(String.format("%s %s %s", "TrafficController:", Instant.now().toString(), String.format(format, args)));
		}
	}
}
