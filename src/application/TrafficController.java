package application;

import java.lang.String;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.lang.Double;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import simulator.Constants;
import simulator.Constants.Direction;
import javafx.util.Pair;
import observer.TrafficObserver;
import observer.TrafficUpdateObservable;
import tracking.Track;

import org.opencv.core.Point;
import org.opencv.core.Rect;

import application.BulbColor;
import simulator.MotorVehicle;

public class TrafficController implements TrafficObserver {
	
	// private subclass for keeping track of vehicles w/in range of intersection traffic cameras
	private class Vehicle {
		public int id = 0;
		public MotorVehicle vehicle = null;
		public Instant timestampFirstObserved = null;
		public Instant timestampLastObserved = null;
		public Direction direction = Direction.NORTH;
		public Vehicle(int trackId, MotorVehicle vehicle, Instant firstObserved) {
			this.id = trackId;
			this.vehicle = vehicle;
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
	
	private boolean isEmergencyVehicleControlled = false; // set to true while emergency vehicle takes control of intersection, set back to false when done
	
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
		this.sql = new SQLite(Config.databaseName, Config.databaseSchema);
		
		// record controller creation as event and metric entry
		log("System timestamp: " + now.toString());
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
				this.numLanesNS, this.laneWidthNS, this.numLanesEW, this.laneWidthEW, Config.pixelsCameraRange);
		
		log("Logic configuration: %s", signalLogicConfiguration.toString());
		if (signalLogicConfiguration == SignalLogicConfiguration.FixedTimers) {
			log("Starting SignalLogicConfiguration scheduled task");
			taskExecutor.scheduleAtFixedRate(() -> {
				CheckSignalStatusForChange();
			}, 0, 1, TimeUnit.SECONDS);
		}
		
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
	
 	public BulbColor RequestGreenLight(MotorVehicle car) {
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
		
		return BulbColor.Red;
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
		return dist > 0 && dist <= Config.pixelsCameraRange;
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
		// TODO Auto-generated method stub
		Instant now = Instant.now();
		boolean isInRange = IsInRangeOfCamera(vehicle);
		double distToCamera = GetDistanceToCamera(vehicle);		
		//log("Track %d is %f pixels from intersection, lane %d, inRange = %s", vehicle.getTrack().track_id, vehicle.distToIntersection(), vehicle.getLane(), isInRange);
		if (isInRange) {
			boolean isEmergencyVehicle = IsEmergencyVehicleWithActiveStrobe(vehicle);
			splat("update isEmergencyVehicle = %s", isEmergencyVehicle);
			if (Config.doTrafficControllerTrackEventLogging)
				log("Track %d is %f pixels from intersection, %f pixels from camera, inRange = %s", vehicle.getTrack().track_id, vehicle.distToIntersection(), distToCamera, isInRange);
			if (vehicles.containsKey(vehicle.getTrack().track_id)) { //update
				//vehicles.put(vehicle.getTrack().track_id, new Vehicle(vehicle.getTrack().track_id, vehicle, now));
				//log("Updating vehicle %d, timestampLastObserved = %s", vehicle.getTrack().track_id, now.toString());
				vehicles.get(vehicle.getTrack().track_id).timestampLastObserved = now;
			}
			else { // insert
				vehicles.put(vehicle.getTrack().track_id, new Vehicle(vehicle.getTrack().track_id, vehicle, Instant.now()));
				RecordEvent("New Vehicle Tracked", String.format("ID: %d, direction: %s", vehicle.getTrack().track_id, vehicle.getTrack().direction).toString());
				RecordMetric("New Vehicle", "New Track", vehicle.getTrack().track_id);
			}
			
			// emergency vehicle taking control of intersection?
			if (IsEmergencyVehicleWithActiveStrobe(vehicle) && !isEmergencyVehicleControlled) {
				RecordEvent("Emergency Vehicle", String.format("Vehicle is an emergency responder, ID=%d", vehicle.getTrack().track_id));
				RecordMetric("Emergency Vehicle", "Vehicle is an emergency responder", vehicle.getTrack().track_id);
				log("Emergency Vehicle with active strobe detected! Giving intersection override...");
				SetEmergencyVehicleControlled(vehicles.get(vehicle.getTrack().track_id).direction);
			}
		}
	}
		
	public String GetStatusForAllLights() {
		StringBuilder sb = new StringBuilder();
		
		//sb.append("TrafficLights Status: ");
		int numLights = 0;
		for (TrafficLight light : trafficLights) {
			if (++numLights > 1)
				sb.append(String.format("\t"));
			sb.append(String.format("%04d=%s", light.getID(), light.GetColor()));
		}
		
		return sb.toString();
	}
		
	// translate the enum to the int value used by the yolo class
	public int GetObservableObjectValue (ObservableObject e) {
		switch (e) {
		case Vehicle:
			return 1;
		case Bicycle:
			return 2;
		case Motorcycle:
			return 3;
		case EmergencyVehicle:
			return 4;
		case Pedestrian:
			return 5;
		default:
			return 1; // failsafe to vehicle
		}
	}
	
	// retrieve the enum by int value used by the yolo class
	public ObservableObject GetObservableObjectValue (int value) {
		splat("GetObservableObjectValue: value = %s", value);
		switch (value) {
		case 1:
			return ObservableObject.Vehicle;
		case 2:
			return ObservableObject.Bicycle;
		case 3:
			return ObservableObject.Motorcycle;
		case 4:
			return ObservableObject.EmergencyVehicle;
		case 5:
			return ObservableObject.Pedestrian;
		default:
			return ObservableObject.Vehicle; // failsafe to vehicle
		}
	}
	
	// change the signal logic configuration to a new type
	public void ChangeSignalLogicConfiguration(SignalLogicConfiguration newConfiguration) {
		if (prevSignalLogicConfiguration != newConfiguration) {
			
		} else {
			log("Warning, ChangeSignalLogicConfiguration: old and new signal configuration are the same (so no change made): %s", prevSignalLogicConfiguration);
		}
	}
	
	
	/****************************** PRIVATE METHODS *******************************/
	
	
	// check to see if signals need changing
	private void CheckSignalStatusForChange() {
		if (this.isEmergencyVehicleControlled) {
			log("Signals in Emergency Vehicle temp override mode");
		} else {
			switch (this.signalLogicConfiguration) {
			case FailSafe:
				break;
			case FixedTimers:
				ToggleTrafficLightsForFixedTimerConfig();
				break;
			case OnDemand:
				ChangeLightsIfObjectsAreWaiting();
				break;
			}
			log("Calling ToggleTrafficLightsForFixedTimerConfig...");
			ToggleTrafficLightsForFixedTimerConfig();
		}
		
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
				long age = ChronoUnit.SECONDS.between(light.getLastChanged(), Instant.now());
				if (age > oldest) {
					oldest = age;
					oldestLight = light;
				}
			}
		}
		
		return oldestLight;
	}
	
	private void ChangeLightsIfObjectsAreWaiting() {
		TrafficLight lightWithLongestWaitingObject = GetRedLightWithLongestWaitingObject();
		// if same signal type and no objects waiting for a green light, return immediately
		if (prevSignalLogicConfiguration == SignalLogicConfiguration.OnDemand && lightWithLongestWaitingObject == null)
			return;
		
		long oldestForRedLight = GetSecondsForOldestObjectAtLight(BulbColor.Red);
		long oldestForGreenLight = GetSecondsForOldestObjectAtLight(BulbColor.Green);
		// if same signal type and no valid objects waiting for a green light, no change possible, so return immediately
		if (prevSignalLogicConfiguration == SignalLogicConfiguration.OnDemand && (oldestForRedLight == 0 || oldestForGreenLight < Config.periodForFixedTimerConfiguration)) {
			if (prevSignalLogicConfiguration == SignalLogicConfiguration.OnDemand && oldestForRedLight > 0)
				log("ChangeLightsIfObjectsAreWaiting: Light change delayed, oldestForGreenLight = %d seconds", oldestForGreenLight);
			return;
		}
		
		try {
			// determine new direction
			Direction newDirection = null;
			
			// change current green lights to red
			ChangeLightsToRed();
			
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
		if (prevSignalLogicConfiguration == SignalLogicConfiguration.FixedTimers && SecondsSinceLastSignalChange() < Config.periodForFixedTimerConfiguration)
			return;
		
		try {
			//log("ToggleTrafficLightsForFixedTimerConfig (start): %s", GetStatusForAllLights());
			
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
		WaitForLightsToChangeFromColor(BulbColor.Yellow, Config.secondsYellowLightDuration);
		
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
		WaitForLightsToChangeFromColor(BulbColor.Yellow, Config.secondsYellowLightDuration);
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
	
	// wait up to N seconds for any lights of the requested color to change to another color, checking every 1/10th of a second 
	private void WaitForLightsToChangeFromColor(BulbColor color, long numSeconds) {
		ArrayList<TrafficLight> lights= GetTrafficLightsForColor(color);
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
				if (age > Config.maxSecondsVehicleAgeToTrack) {
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
	
	private void CheckAllLightSForWaitingTraffic() {
		if (vehicles.size() == 0)
			return;
		
		Integer i = 0;
		i++;
		
		
		int[] arrAllLights = new int[Direction.values().length];	// number of cars at every light
		Arrays.fill(arrAllLights, 0);
		int[] arrWaiting = new int[Direction.values().length];		// number of cars at red lights
		Arrays.fill(arrWaiting, 0);
		//int [] ra = arrWaiting.clone();
		
		//ArrayList<Pair<Direction,Integer>> lightsPerDirection = new ArrayList<Pair<Direction,Integer>>(); // cars per set of lights facing each direction
		for (Entry<Integer,Vehicle> entry : vehicles.entrySet()) {
			int indexForDirection = entry.getValue().direction.hashCode();
			arrAllLights[indexForDirection]++;
			TrafficLight light = GetTrafficLightForVehicle(entry.getValue().vehicle);
			if (light.GetColor() == application.BulbColor.Red)
				arrWaiting[indexForDirection]++;
		}
		//Arrays.sort(arrWaiting, Collections.reverseOrder());
		
		for (TrafficLight light : trafficLights) {
			
		}
	}
	
	// return True is a vehicle is an emergency vehicle
	private boolean IsEmergencyVehicleWithActiveStrobe(MotorVehicle vehicle) {
		splat("IsEmergencyVehicleWithActiveStrobe: classId = %s", vehicle.getTrack().lastDetect.classId);
		return GetObservableObjectValue(vehicle.getTrack().lastDetect.classId) == ObservableObject.EmergencyVehicle;
	}
	
	// usage: emergency vehicles needing all lights to go red except theirs (optionally)
	private void SetEmergencyVehicleControlled(Direction exceptForTravelDirection) {
		this.isEmergencyVehicleControlled = true;
		log("SetEmergencyVehicleControlled: turning all signals red (exception: %s)", exceptForTravelDirection == null ? "none" : exceptForTravelDirection.toString());
		for (TrafficLight light : trafficLights) {
			if (light.GetColor() != application.BulbColor.Red && (exceptForTravelDirection == null || light.getTravelDirection() != exceptForTravelDirection)) {
				log("TurnAllTrafficLightsRed: Changing light %d to Red", light.getID());
				light.TurnRed();
			}
		}
		// start sleep-looping until emergency vehicle has passed or max wait time is exceeded
		log("SetEmergencyVehicleControlled: Intersection under emergency vehicle control, waiting until vehicles have released control...");
		Instant startChecking = Instant.now();
		long age = 0;
		while (this.isEmergencyVehicleControlled || age > Config.maxSecondsToWaitForEmergencyVehicles) {
			try {
				log("SetEmergencyVehicleControlled: Wait time is no %d seconds", age);
				TimeUnit.SECONDS.sleep((long)5);
				age = ChronoUnit.SECONDS.between(startChecking, Instant.now());
			} catch (Exception ex) { ex.printStackTrace(); }
		}
		
		log("SetEmergencyVehicleControlled: Recovered from emergency vehicle control.");
		ResetFromEmergencyVehicleControlled();
		log("SetEmergencyVehicleControlled: Regular intersection management control resuming.");
	}
	
	// usage: after emergency vehicles have passed through the intersection, restart regular light logic
	private void ResetFromEmergencyVehicleControlled() {
		log("ResetFromEmergencyVehicleControlled: resetting North-South signals");
		for (TrafficLight light : trafficLights) {
			if (light.GetColor() == application.BulbColor.Red && (light.getTravelDirection() == Direction.NORTH || light.getTravelDirection() == Direction.SOUTH))
				light.TurnGreen();
		}
		this.isEmergencyVehicleControlled = false;
	}
	
	// record an event to the database in text format
	private void RecordEvent(String name, String value) {
		if (Config.doMetricsLogging ) {
			try {
				Instant now = Instant.now();
				String text = String.format("insert into Events (timestamp, name, value) values ('%s','%s','%s')", now.toString(), name, value).toString();
				//log("RecordEvent: SQL = %s", text);
				int result = sql.executeUpdate(text);
				//log("RecordEvent: result = %d", result);
				if (result < 1) {
					log("RecordEvent: Failed to store data in database");
				}
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}
	}
	
	// record a metric (as an Object) to the database 
	private void RecordMetric(String name, String description, Object value) {
		if (Config.doMetricsLogging ) {
			try {
				Instant now = Instant.now();
				
				// value needs SQL quotes?
				String valueAsText = (value instanceof String || value instanceof Instant) ? String.format("'%s'", value.toString()) : value.toString();
				
				String text = String.format("insert into Metrics (timestamp, name, description, valueType, value) values ('%s','%s','%s','%s', %s)",
					now.toString(), name, description, value.getClass().getName(), valueAsText).toString();
				log("RecordMetric: SQL = %s", text);
				int result = sql.executeUpdate(text);
				//log("RecordMetric: result = %d", result);
				if (result < 1) {
					log("RecordMetric: Failed to store data in database");
				}
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}
	}
	
	private void log(String format, Object ... args) {
		if (Config.doTrafficControllerLogging) {
			System.out.println(String.format("%s %s %s", "TrafficController:", Instant.now().toString(), String.format(format, args)));
		}
	}
	
	private void splat(String format, Object ... args) {
		System.out.println(String.format("%s %s %s", "TC:", Instant.now().toString(), String.format(format, args)));
	}
}
