package application;

import java.lang.String;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import simulator.Constants;
import simulator.Constants.Direction;
import observer.TrafficObserver;
import application.BulbColor;
import config.TrafficControllerConfig;
import simulator.MotorVehicle;

public class TrafficController implements TrafficObserver {
	
	// private subclass for keeping track of vehicles w/in range of intersection traffic cameras
	private class Vehicle {
		public int id = 0;
		public MotorVehicle vehicle = null;
		public boolean IsEmergencyVehicle = false;
		public Instant timestampFirstObserved = null;
		public Instant timestampLastObserved = null;
		public Direction direction = Direction.NORTH;
		public Vehicle(int trackId, MotorVehicle vehicle, Instant firstObserved) {
			this.id = trackId;
			this.vehicle = vehicle;
			this.IsEmergencyVehicle = GetObservableObjectValue(vehicle.getTrack().lastDetect.classId) == ObservableObject.EmergencyVehicle;
			this.timestampFirstObserved = firstObserved;
			this.timestampLastObserved = firstObserved;
			this.direction = this.vehicle.getDirection();
		}
	}
	
	// types of controller configurations, default = FixedTimers
	public enum SignalLogicConfiguration { FailSafe, FixedTimers, OnDemand }
	
	// types of objects monitored by the controller
	public enum ObservableObject { Vehicle, Bicycle, Motorcycle, EmergencyVehicle, Pedestrian }
	
	// thread pool
	private ScheduledExecutorService  taskExecutor = Executors.newScheduledThreadPool(10);
	
	// SQLite database connection for persistent metric storage
	public static SQLite sql = null;
	
	// intersection dimensions
	private SignalLogicConfiguration signalLogicConfiguration = SignalLogicConfiguration.FailSafe;
	private SignalLogicConfiguration prevSignalLogicConfiguration = SignalLogicConfiguration.FailSafe;
	private double intersectionWidthNS = 0;
	private double intersectionWidthEW = 0;
	private int numLanesNS = 0;
	private int numLanesEW = 0;
	private double laneWidthNS = 0;
	private double laneWidthEW = 0;
	
	private Instant lastSignalChange = Instant.MIN;
	private long SecondsSinceLastSignalChange() {
		return ChronoUnit.SECONDS.between(lastSignalChange, Instant.now());
	}
	
	// emergency vehicle settings
	private boolean isEmergencyVehicleControlled = false; // set to true while emergency vehicle takes control of intersection, set back to false when done
	private Direction directionOfEmergencyVehicle = null;
	private Instant timestampEmergencyVehicleDetected = Instant.MIN;
	
	// settings for fixed-timer signal logic
	private static boolean isNS = false; // true=North-South signals own green, false means East-West signals own green
	
	// Traffic lights are assumed to be placed on the FAR side of an intersection for visibility reasons
	private ArrayList<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
	
	// collection tracking vehicles currently using the intersection
	private ConcurrentHashMap<Integer,Vehicle> vehicles = new ConcurrentHashMap<Integer,Vehicle>();
	
	private String statusOfAllLights = null; // defined globally because Java doesn't permit lambdas modifying non-class properties (it's an "effective final" thing, grrr)
		
	// constructor: traffic cameras are assumed to be placed on the far side of the intersection to provide safest viewing angles by vehicles
	// note: if newLogicConfiguration is null, configuration will remain in FailSafe mode
	public TrafficController(SignalLogicConfiguration newLogicConfiguration, int numLanesNS, double laneWidthNS, int numLanesEW, double laneWidthEW)
	{
		Instant now = Instant.now();
		
		// create/open database
		this.sql = new SQLite(TrafficControllerConfig.databaseName, TrafficControllerConfig.databaseSchema);
		
		// record controller creation as event and metric entry
		log("System timestamp: " + now.toString());
		//splat("TrafficController Creation: Timestamp: %s, Logic Configuration: $s", now, newLogicConfiguration);
		RecordEvent("TrafficController Creation", String.format("Timestamp: %s, Logic Configuration: $s", now, newLogicConfiguration));
		RecordMetric("TrafficController", "Creation", String.format("Timestamp: %s, Logic Configuration: $s", now, newLogicConfiguration));
		
		this.signalLogicConfiguration = newLogicConfiguration;
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
				this.numLanesNS, this.laneWidthNS, this.numLanesEW, this.laneWidthEW, TrafficControllerConfig.pixelsCameraRange);
		
		log("Logic configuration: %s", signalLogicConfiguration.toString());
		//splat("Starting SignalLogicConfiguration scheduled task");
		taskExecutor.scheduleAtFixedRate(() -> {
			CheckSignalStatusForChange();
		}, 0, 1, TimeUnit.SECONDS);
		
		// add requested traffic lights
		// TODO: for light-per-lane, change to loops: for N lanes, add new light
		if (numLanesNS > 0) {
			AddTrafficLight(Direction.NORTH);
			AddTrafficLight(Direction.SOUTH);
		}
		if (numLanesEW > 0) {
			AddTrafficLight(Direction.EAST);
			AddTrafficLight(Direction.WEST);
		}
		
		log("Starting DropOldVehiclesFromCollector scheduled task");
		taskExecutor.scheduleAtFixedRate(() -> {
			try {
				//log("Calling DropOldVehiclesFromCollector...");
				DropOldVehiclesFromCollector();
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}, 0, 1, TimeUnit.SECONDS);
		
		// TODO: probably not needed any longer unless for event logging, in which case control it with a configuration setting
		log("Starting TrafficLight monitor scheduled task");
		taskExecutor.scheduleAtFixedRate(() -> {
			try {
				String status = GetStatusForAllLights();
				log("TrafficLights Status: %s, statusOfAllLights: %s", status, statusOfAllLights);
				// record initial state and state changes only
				if (statusOfAllLights == null || !statusOfAllLights.equals(status)) {
					RecordEvent("Lights Changed", status);
					RecordMetric("Lights", "Lights Changed", status);
				}
				statusOfAllLights = status;
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}, 0, 1, TimeUnit.SECONDS);
		
		// set the signal logic configuration to the desired type
		if (newLogicConfiguration != null && newLogicConfiguration != signalLogicConfiguration)
			ChangeSignalLogicConfiguration(newLogicConfiguration);
	}
	
	/****************************** PUBLIC METHODS *******************************/
	
	public TrafficLight AddTrafficLight(Direction directionOfTravel) {
		log("AddTrafficLight(%s)", directionOfTravel.toString());
		TrafficLight light = new TrafficLight(directionOfTravel); 
		trafficLights.add(light);
		return light;
	}

	
	// returns all TrafficLight objects
	public ArrayList<TrafficLight> GetTrafficLights() {
		ArrayList<TrafficLight> list = new ArrayList<TrafficLight>();
		for (TrafficLight light : trafficLights) {
			list.add(light);
		}
		return list;
	}
	
	// returns all TrafficLight objects of the requested color
	public ArrayList<TrafficLight> GetTrafficLightsForColor(BulbColor color) {
		ArrayList<TrafficLight> list = new ArrayList<TrafficLight>();
		for (TrafficLight light : trafficLights) {
			if (light.GetColor() == color)
				list.add(light);
		}
		return list;
	}
	
	// returns all TrafficLight objects which are opposite of the requested direction
	private ArrayList<TrafficLight> GetTrafficLightsOfOppositeDirection(Direction forDirection) {
		Direction oppDirection = forDirection == Direction.NORTH ? Direction.SOUTH
			: forDirection == Direction.WEST ? Direction.EAST
			: forDirection == Direction.EAST ? Direction.WEST
			: Direction.NORTH;
		ArrayList<TrafficLight> list = new ArrayList<TrafficLight>();
		for (TrafficLight light : trafficLights) {
			if (light.getTravelDirection() == oppDirection)
				list.add(light);
		}
		return list;
	}
	
	// returns 1st TrafficLight for the requested travel direction, uses TrafficController.Direction enum
	public TrafficLight GetTrafficLight(Direction forDirection) {
		for (TrafficLight light : trafficLights) {
			if (light.getTravelDirection() == forDirection)
				return light;
		}
		return null;
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
		return dist > 0 && dist <= TrafficControllerConfig.pixelsCameraRange;
	}
	
	// return distance of vehicle to its facing camera, considering vehicle's distance to intersection and intersection width
	public double GetDistanceToCamera(MotorVehicle car) {
		double distance = 0;
		switch (GetCarDirectionOfTravel(car)) {
			case WEST:
			case EAST:
				distance = car.distToIntersection() + this.intersectionWidthEW;
				break;
			case NORTH:
			case SOUTH:
				distance = car.distToIntersection() + this.intersectionWidthNS;
				break;
		}
		return distance;
	}
	
	// convert simulator vehicle direction into global application direction (for local calculations)
	public Direction GetCarDirectionOfTravel(MotorVehicle car) {
		return car.getDirection();
	}
	
	public TrafficLight GetTrafficLightForVehicle(MotorVehicle car) {
		for (TrafficLight light : trafficLights) {
			if (light.getTravelDirection() == GetCarDirectionOfTravel(car))
				return light;
		}
		
		return null;
	}
	
	// record vehicle movement updates, ignore anything not in-range of cameras
	@Override
	public void update(MotorVehicle vehicle) {
		Instant now = Instant.now();
		boolean isInRange = IsInRangeOfCamera(vehicle);
		double distToCamera = GetDistanceToCamera(vehicle);		
		
		if (isInRange) {
			if (TrafficControllerConfig.doTrafficControllerTrackEventLogging)
				log("Track %d is %f pixels from intersection, %f pixels from camera, inRange = %s", vehicle.getTrack().track_id, vehicle.distToIntersection(), distToCamera, isInRange);
			if (vehicles.containsKey(vehicle.getTrack().track_id)) { //update
				vehicles.get(vehicle.getTrack().track_id).timestampLastObserved = now;
			}
			else { // insert
				vehicles.put(vehicle.getTrack().track_id, new Vehicle(vehicle.getTrack().track_id, vehicle, Instant.now()));
				RecordEvent("New Vehicle Tracked", String.format("ID: %d, direction: %s", vehicle.getTrack().track_id, vehicle.getTrack().direction).toString());
				RecordMetric("New Vehicle", "New Track", vehicle.getTrack().track_id);
				RecordTraffic(vehicle.getTrack().oncomingHeading.name());
			}
			
			// emergency vehicle taking control of intersection?
			if (IsEmergencyVehicleWithActiveStrobe(vehicle) && !this.isEmergencyVehicleControlled) {
				RecordEvent("Emergency Vehicle", String.format("Vehicle is an emergency responder, ID=%d", vehicle.getTrack().track_id));
				RecordMetric("Emergency Vehicle", "Vehicle is an emergency responder", vehicle.getTrack().track_id);
				log("update: Emergency Vehicle with active strobe detected! Giving intersection override...");
				SetEmergencyVehicleControlled(vehicles.get(vehicle.getTrack().track_id).direction);
			}
		}
	}
		
	public String GetStatusForAllLights() {
		StringBuilder sb = new StringBuilder();
		
		int numLights = 0;
		for (TrafficLight light : trafficLights) {
			if (++numLights > 1)
				sb.append(String.format("\t"));
			sb.append(String.format("%04d=%s", light.getID(), light.GetColor()));
		}
		
		return sb.toString();
	}
		
	// translate the enum to the int value used by the yolo class
	public int GetObservableObjectCode(ObservableObject e) {
		switch (e) {
		case Vehicle:
			return 0;
		case Bicycle:
			return 1;
		case Motorcycle:
			return 2;
		case EmergencyVehicle:
			return 3;
		case Pedestrian:
			return 4;
		default:
			return 0; // failsafe to vehicle
		}
	}
	
	// retrieve the enum by int value used by the yolo class
	public ObservableObject GetObservableObjectValue (int value) {
		switch (value) {
		case 0:
			return ObservableObject.Vehicle;
		case 1:
			return ObservableObject.Bicycle;
		case 2:
			return ObservableObject.Motorcycle;
		case 3:
			return ObservableObject.EmergencyVehicle;
		case 4:
			return ObservableObject.Pedestrian;
		default:
			return ObservableObject.Vehicle; // failsafe to vehicle
		}
	}
	
	// change the signal logic configuration to a new type
	public void ChangeSignalLogicConfiguration(SignalLogicConfiguration newConfiguration) {
		if (prevSignalLogicConfiguration != newConfiguration) {
			switch (newConfiguration) {
			case FailSafe:
				break;
			case FixedTimers:
				ToggleTrafficLightsForFixedTimerConfig();
				break;
			case OnDemand:
				ChangeLightsIfObjectsAreWaiting();
				break;
			default:
				log("ChangeSignalLogicConfiguration: Unknown configuration type: %s", newConfiguration);
				break;
			}
			prevSignalLogicConfiguration = newConfiguration;
		} else {
			log("Warning, ChangeSignalLogicConfiguration: old and new signal configuration are the same (so no change made): %s", prevSignalLogicConfiguration);
		}
	}
	
	
	/****************************** PRIVATE METHODS *******************************/
	
	
	// check to see if signals need changing
	private void CheckSignalStatusForChange() {
		Instant now = Instant.now();
		if (this.isEmergencyVehicleControlled) {
			long age = ChronoUnit.SECONDS.between(timestampEmergencyVehicleDetected, now);
			log("CheckSignalStatusForChange: Signals in Emergency Vehicle mode, duration %d seconds", age);
			if (!IsEmergencyVehicleAtIntersection()) {
				log("CheckSignalStatusForChange: Emergency vehicle no longer present at intersection, recovering from Emergency Vehicle mode");
				ResetFromEmergencyVehicleControlled();
			}
		}
		if (!this.isEmergencyVehicleControlled) {
			switch (this.signalLogicConfiguration) {
			case FailSafe:
				break;
			case FixedTimers:
				ToggleTrafficLightsForFixedTimerConfig();
				break;
			case OnDemand:
				ChangeLightsIfObjectsAreWaiting();
				break;
			default:
				log("CheckSignalStatusForChange: unknown traffic logic controller type: %s", this.signalLogicConfiguration);
			}
		}		
	}
	
	// return True if there are any emergency vehicles in range of any intersection traffic cameras
	private boolean IsEmergencyVehicleAtIntersection() {
		for (Vehicle vehicle : this.vehicles.values()) {
			if (vehicle.IsEmergencyVehicle)
				return true;
		}
		
		return false;
	}
	
	// compute max number of seconds an object has been waiting at any red light
	private long GetSecondsForOldestObjectAtLight(BulbColor color) {
		long oldest = 0;
		for (TrafficLight light : trafficLights) {
			if (light.GetColor() == color) {
				long age = ChronoUnit.SECONDS.between(light.getLastChanged(), Instant.now());
				if (age > oldest)
					oldest = age;
			}
		}
		
		return oldest;
	}
	
	// return red light with oldest waiting object, else null
	private TrafficLight GetRedLightWithLongestWaitingObject() {
		long oldest = 0;
		TrafficLight oldestLight = null;
		for (TrafficLight light : trafficLights) {
			if (light.GetColor() == BulbColor.Red) {
				for (Vehicle v : GetVehiclesForDirection(light.getTravelDirection())) {
					long age = ChronoUnit.SECONDS.between(v.timestampFirstObserved, Instant.now());
					if (age > oldest) {
						oldest = age;
						oldestLight = light;
					}
				}
			}
		}
		
		return oldestLight;
	}
	
	// get a list of vehicles for the requested travel direction
	private ArrayList<Vehicle> GetVehiclesForDirection(Direction dir) {
		ArrayList<Vehicle> list = new ArrayList<Vehicle>();
		
		for (Vehicle v : vehicles.values()) {
			if (v.direction == dir)
				list.add(v);
		}
		
		return list;
	}
	
	// get a list of vehicles for the requested travel direction
	private ArrayList<Vehicle> GetVehiclesForDirectionAndOpposingDirection(Direction direction) {
		ArrayList<Vehicle> list = new ArrayList<Vehicle>();		
		Direction oppDirection = GetOppositeDirection(direction);
		
		for (Vehicle v : vehicles.values()) {
			if (v.direction == direction || v.direction == oppDirection)
				list.add(v);
		}
		
		return list;
	}
	
	// get a count of vehicles for the requested travel direction
	private int CountVehiclesForDirection(Direction direction) {
		int count = 0;
		
		for (Vehicle v : vehicles.values()) {
			if (v.direction == direction)
				count++;
		}
		
		return count;
	}
	
	// get a count of vehicles for the requested travel direction or its opposite
	private int CountVehiclesForDirectionAndOpposingDirection(Direction direction) {
		int count = 0;		
		Direction oppDirection = GetOppositeDirection(direction);
		
		for (Vehicle v : vehicles.values()) {
			if (v.direction == direction || v.direction == oppDirection)
				count++;
		}
		
		return count;
	}
	
	// get a count of vehicles for all directions EXCEPT the requested travel direction and its opposite
	private int CountVehiclesForAllExceptDirectionAndOpposingDirection(Direction direction) {
		int count = 0;
		Direction oppDirection = GetOppositeDirection(direction);
		
		for (Vehicle v : vehicles.values()) {
			if (v.direction != direction && v.direction != oppDirection)
				count++;
		}
		
		return count;
	}
	
	// get a count of vehicles at all green lights
	private int CountVehiclesAtGreenLights() {
		int count = 0;
		
		ArrayList<Direction> dirs = new ArrayList<Direction>();
		for (TrafficLight light : trafficLights) {
			if (light.GetColor() == BulbColor.Green && !dirs.contains(light.getTravelDirection()))
				dirs.add(light.getTravelDirection());
		}
		
		for (Vehicle v : vehicles.values()) {
			if (dirs.contains(v.direction))
				count++;
		}
		
		return count;
	}
	
	private void ChangeLightsIfObjectsAreWaiting() {
		TrafficLight lightWithLongestWaitingObject = GetRedLightWithLongestWaitingObject();
		
		boolean oldMethod = true;
		if (oldMethod) {
			// if same signal type and no objects waiting for a green light, return immediately
			if (prevSignalLogicConfiguration == SignalLogicConfiguration.OnDemand && lightWithLongestWaitingObject == null) {
				//splat("ChangeLightsIfObjectsAreWaiting: return early 1");
				return;
			}
			
			if (lightWithLongestWaitingObject == null) {
				return;
			}
			
			long oldestForRedLight = GetSecondsForOldestObjectAtLight(BulbColor.Red);
			long oldestForGreenLight = GetSecondsForOldestObjectAtLight(BulbColor.Green);
			// if same signal type and no valid objects waiting for a green light, no change possible, so return immediately
			if (prevSignalLogicConfiguration == SignalLogicConfiguration.OnDemand && (oldestForRedLight == 0 || oldestForGreenLight < TrafficControllerConfig.periodForFixedTimerConfiguration)) {
				if (prevSignalLogicConfiguration == SignalLogicConfiguration.OnDemand && oldestForRedLight > 0)
					log("ChangeLightsIfObjectsAreWaiting: Light change delayed, oldestForGreenLight = %d seconds", oldestForGreenLight);
				return;
			}
		} else {
			int countVehiclesAtGreenLights = CountVehiclesAtGreenLights();
			
			if (lightWithLongestWaitingObject == null) {
				return;
			}
			Direction dirOfLongestWait = lightWithLongestWaitingObject.getTravelDirection();
			Direction dirOppositeOfLogestWait = GetOppositeDirection(dirOfLongestWait);
			int countOtherVehicles = CountVehiclesForAllExceptDirectionAndOpposingDirection(dirOfLongestWait); 
		}
		
		try {
			// determine new direction
			Direction newDirection = lightWithLongestWaitingObject.getTravelDirection();
			
			// change current green lights to red
			ChangeLightsToRedExceptDirection(newDirection);
			
			// wait for changing lights to go to red
			TimeUnit.MILLISECONDS.sleep((long)500);
			WaitForLightsToChangeFromColor(BulbColor.Yellow, 5);
			
			// change new lights to green
			ChangeLightsToGreen(newDirection);
	
			// record the change
			this.prevSignalLogicConfiguration = SignalLogicConfiguration.OnDemand;
			this.lastSignalChange = Instant.now();
		}
		catch (Exception ex) { ex.printStackTrace(); }
		finally {
		}
	}
	
	// toggle traffic lights	
	private void ToggleTrafficLightsForFixedTimerConfig() {
		if (prevSignalLogicConfiguration == SignalLogicConfiguration.FixedTimers && SecondsSinceLastSignalChange() < TrafficControllerConfig.periodForFixedTimerConfiguration)
			return;
		
		try {
			// change any green lights to red
			ChangeLightsToRed();
			
			// toggle direction change
			isNS = prevSignalLogicConfiguration != SignalLogicConfiguration.FixedTimers ? true : !isNS;
			
			// turn signals Green for the new direction and their opposite direction
			ChangeLightsToGreen(isNS ? Direction.NORTH : Direction.EAST);

			// record the change
			this.prevSignalLogicConfiguration = SignalLogicConfiguration.FixedTimers;
			this.lastSignalChange = Instant.now();
		}
		catch (Exception ex) { ex.printStackTrace(); }
		finally {
		}
		
		//log("ToggleTrafficLightsForFixedTimerConfig (finish): %s", GetStatusForAllLights());
	}

	// turn signals Green for the new travel direction, plus for the opposing direction
	private void ChangeLightsToGreen(Constants.Direction newDirection) {
		// wait for all lights to become non-yellow (they're changing to red on their own)
		WaitForLightsToChangeFromColor(BulbColor.Yellow, TrafficControllerConfig.secondsYellowLightDuration);
		
		// change signals in sets, simple version is 2 sets, e.g. North + South lights together
		ArrayList<TrafficLight> lights = GetTrafficLights(newDirection);
		lights.addAll(GetTrafficLightsOfOppositeDirection(newDirection));
		for (TrafficLight light : lights) {
			if (light.GetColor() != BulbColor.Green) {
				taskExecutor.submit(() -> {
					light.TurnGreen();
				});
			}
		}
	}

	// turn signals Green for the new travel direction, plus for the opposing direction
	private void ChangeLightsToGreenForOneDirection(Constants.Direction newDirection) {
		// wait for all lights to become non-yellow (they're changing to red on their own)
		WaitForLightsToChangeFromColor(BulbColor.Yellow, TrafficControllerConfig.secondsYellowLightDuration);
		
		// change signals in sets, simple version is 2 sets, e.g. North + South lights together
		ArrayList<TrafficLight> lights = GetTrafficLights(newDirection);
		lights.addAll(GetTrafficLightsOfOppositeDirection(newDirection));
		for (TrafficLight light : lights) {
			if (light.GetColor() != BulbColor.Green && light.getTravelDirection() == newDirection) {
				taskExecutor.submit(() -> {
					light.TurnGreen();
				});
			}
		}
	}

	// turn all green lights to red (yellow lights are in-process of going red, so ignore them)
	private void ChangeLightsToRed() {
		//log("ToggleTrafficLightsForFixedTimerConfig: triggering all green lights to change to red");
		for (TrafficLight light : trafficLights) {
			if (light.GetColor() == BulbColor.Green) {
				taskExecutor.submit(() -> {
					light.TurnRed();
				});
			}
		}
		
		// wait up to 1 second for all lights to become change to yellow
		WaitForLightsToChangeFromColor(BulbColor.Green, 1);
		
		// wait for all lights to become non-yellow (they're changing to red on their own)
		WaitForLightsToChangeFromColor(BulbColor.Yellow, TrafficControllerConfig.secondsYellowLightDuration);
	}
	
	// turn all green lights to red (yellow lights are in-process of going red, so ignore them) for a given direction
	private void ChangeLightsToRed(Constants.Direction newDirection) {
		// change signals in sets, simple version is 2 sets, e.g. North + South lights together
		ArrayList<TrafficLight> lights = GetTrafficLights(newDirection);
		lights.addAll(GetTrafficLightsOfOppositeDirection(newDirection));
		for (TrafficLight light : lights) {
			if (light.GetColor() == BulbColor.Green) {
				taskExecutor.submit(() -> {
					light.TurnRed();
				});
			}
		}
	}

	// turn all green lights to red (yellow lights are in-process of going red, so ignore them) for a given direction
	private void ChangeLightsToRedExceptDirection(Constants.Direction direction) {
		for (TrafficLight light : this.trafficLights) {
			// ignore yellow lights as they're already changing to red on their own
			if (light.GetColor() == BulbColor.Green && light.getTravelDirection() != direction) {
				log("ChangeLightsToRedExceptDirection: Changing light %d to red", light.getID());
				taskExecutor.submit(() -> {
					light.TurnRed();
				});
			}
		}
	}
	
	// wait up to N seconds for any lights of the requested color to change to another color, checking every 1/10th of a second 
	private void WaitForLightsToChangeFromColor(BulbColor color, long numSeconds) {
		ArrayList<TrafficLight> lights = GetTrafficLightsForColor(color);
		int numLights = lights.size();
		if (numLights > 0) {
			Instant loopStart = Instant.now();
			while (numLights > 0 && ChronoUnit.SECONDS.between(loopStart, Instant.now()) <= numSeconds) {
				try {
					TimeUnit.MILLISECONDS.sleep((long)100);
				} catch (InterruptedException e) { e.printStackTrace(); }
				numLights = 0;
				for (TrafficLight light : lights)
					if (light.GetColor() == color)
						numLights++;
			}
			if (numLights > 0)
				log("Warning, WaitForLightsToChangeFromColor: wait time for change of %s lights exceeded, possible stuck signals!", color);
		}
	}
	
	// wait up to maxSeconds for all lights of a given travel direction to change to a new color
	private void WaitForLightsToChangeToNewColor(Direction travelDirection, BulbColor color, long maxSeconds) {
		Direction oppDirection = GetOppositeDirection(travelDirection);
		ArrayList<TrafficLight> lights = new ArrayList<TrafficLight>();
		for (TrafficLight light : trafficLights) {
			if (light.getTravelDirection() == travelDirection || light.getTravelDirection() == oppDirection)
				lights.add(light);
		}
		
		// loop, counting lights not yet changed to the new color, up to maxSeconds
		int numLightsNotReady = 0;
		Instant loopStart = Instant.now();
		while (numLightsNotReady > 0 && ChronoUnit.SECONDS.between(loopStart, Instant.now()) <= maxSeconds) {
			try {
				TimeUnit.MILLISECONDS.sleep((long)100);
			} catch (InterruptedException e) { e.printStackTrace(); }
			numLightsNotReady = 0;
			for (TrafficLight light : lights)
				if (light.GetColor() == color)
					numLightsNotReady++;
		}
		if (numLightsNotReady > 0)
			log("Warning, WaitForLightsToChangeToNewColor: wait time for change of %s lights exceeded, possible stuck signals!", color);
	}
	
	
	
	// function to purge obsolete vehicles from the collection, either because they are out of range of traffic light cameras or they haven't been updated for a long time
	private void DropOldVehiclesFromCollector() {
		log("DropOldVehiclesFromCollector: %d vehicles to examine", vehicles.size());
		
		// nothing to track? exit early.
		if (vehicles.size() == 0) {
			// record metric
			RecordMetric("Vehicles", "0 Tracked", 0);
			return;
		}
		
		ReentrantLock lock = new ReentrantLock();
		try {
			lock.lock();
			
			Instant now = Instant.now();
			
			// list of vehicles to remove, do NOT remove items from collection while iterating thru it
			List<Integer> idsToRemove = new LinkedList<>();
			
			for (Entry<Integer,Vehicle> entry : vehicles.entrySet()) {
				Instant lastObserved = entry.getValue().timestampLastObserved;
				long age = ChronoUnit.SECONDS.between(lastObserved, now);
				log("vehicle %d age = %ds, lastObserved = %s", entry.getKey(), age, lastObserved.toString());
				if (age > TrafficControllerConfig.maxSecondsVehicleAgeToTrack) {
					idsToRemove.add(entry.getKey());
					log("Removing vehicle %d due to aging", entry.getKey());
					RecordEvent("Stop Tracking Vehicle", String.format("ID: %d, reason: aging", entry.getValue().id).toString());
				}
				else if (!IsInRangeOfCamera(entry.getValue().vehicle)) {
					idsToRemove.add(entry.getKey());
					log("Removing vehicle %d due to out-of-range condition", entry.getKey());
					RecordEvent("Stop Tracking Vehicle", String.format("ID: %d, reason: out-of-range", entry.getValue().id).toString());
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
		
		// record metric
		RecordMetric("Vehicles", "Currently Tracked", vehicles.size());
	}
	
	// return True is a vehicle is an emergency vehicle
	private boolean IsEmergencyVehicleWithActiveStrobe(MotorVehicle vehicle) {
		return GetObservableObjectValue(vehicle.getTrack().lastDetect.classId) == ObservableObject.EmergencyVehicle;
	}
	
	// usage: emergency vehicles needing all lights to go red except theirs (optionally)
	private void SetEmergencyVehicleControlled(Direction exceptForTravelDirection) {
		if (this.isEmergencyVehicleControlled) {
			log("SetEmergencyVehicleControlled, Warning: Emergency Vehicle mode already activated");
			return;
		}
		
		this.isEmergencyVehicleControlled = true;
		this.timestampEmergencyVehicleDetected = Instant.now();
		this.directionOfEmergencyVehicle = exceptForTravelDirection;
		log("SetEmergencyVehicleControlled: turning all signals red (exception: %s)", exceptForTravelDirection == null ? "none" : exceptForTravelDirection.toString());
		ChangeLightsToRedExceptDirection(exceptForTravelDirection);
		ChangeLightsToGreenForOneDirection(exceptForTravelDirection);
	}
	
	// usage: after emergency vehicles have passed through the intersection, restart regular light logic
	private void ResetFromEmergencyVehicleControlled() {
		Direction oppDirection = GetOppositeDirection(this.directionOfEmergencyVehicle);
		log("ResetFromEmergencyVehicleControlled: resetting signals to Green for direction %s", oppDirection);
		ChangeLightsToGreenForOneDirection(oppDirection);
		this.isEmergencyVehicleControlled = false;
	}
	
	private Direction GetOppositeDirection(Direction direction) {
		return direction == Direction.NORTH ? Direction.SOUTH
			: direction == Direction.EAST ? Direction.WEST
			: direction == Direction.WEST ? Direction.EAST
			: Direction.NORTH;
	}
	
	// record an event to the database in text format
	private void RecordEvent(String name, String value) {
		if (TrafficControllerConfig.doMetricsLogging ) {
			try {
				Instant now = Instant.now();
				String text = String.format("insert into Events (timestamp, name, value) values ('%s','%s','%s')", now.toString(), name, value).toString();
				int result = sql.executeUpdate(text);
				if (result < 1) {
					log("RecordEvent: Failed to store data in database");
				}
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}
	}
	
	// record a metric (as an Object) to the database 
	private void RecordMetric(String name, String description, Object value) {
		if (TrafficControllerConfig.doMetricsLogging ) {
			try {
				Instant now = Instant.now();
				
				// value needs SQL quotes?
				String valueAsText = (value instanceof String || value instanceof Instant) ? String.format("'%s'", value.toString()) : value.toString();
				
				String text = String.format("insert into Metrics (timestamp, name, description, valueType, value) values ('%s','%s','%s','%s', %s)",
					now.toString(), name, description, value.getClass().getName(), valueAsText).toString();
				log("RecordMetric: SQL = %s", text);
				int result = sql.executeUpdate(text);
				if (result < 1) {
					log("RecordMetric: Failed to store data in database");
				}
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}
	}
	
	// record a metric (as an Object) to the database 
		private void RecordTraffic(String direction) {
			if (TrafficControllerConfig.doMetricsLogging ) {
				try {
					Instant now = Instant.now();
					
					// value needs SQL quotes?
					String text = String.format("insert into Traffic (timestamp, direction) values ('%s', '%s')",
						now.toString(), direction).toString();
					log("RecordTraffic: SQL = %s", text);
					int result = sql.executeUpdate(text);
					if (result < 1) {
						log("RecordTraffic: Failed to store data in database");
					}
				}
				catch (Exception ex) { ex.printStackTrace(); }
			}
		}
	
	private void log(String format, Object ... args) {
		if (TrafficControllerConfig.doTrafficControllerLogging) {
			System.out.println(String.format("%s %s %s", "TrafficController:", Instant.now().toString(), String.format(format, args)));
		}
	}
}
