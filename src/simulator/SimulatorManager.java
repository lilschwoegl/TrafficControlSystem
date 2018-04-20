package simulator;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;

import org.opencv.core.Point;

import application.BulbColor;
import application.TrafficController;
import application.TrafficLight;
import observer.SimulatorObserver;
import observer.TrackUpdateObservable;
import observer.TrafficLightObserver;
import observer.TrafficUpdateObservable;
import simulator.MotorVehicle.Direction;
import tracking.SimulatedTrack;
import tracking.Track;

public class SimulatorManager implements TrafficLightObserver {
	
	private long time = System.nanoTime();
	private long delay;
	private SimulatorObserver observer;
	Graphics g;
	private int bulbID = 900;
	
	int simulatedCarsCounter = 5000;
	
	public HashMap<Integer,MotorVehicle> motors;
	public ArrayList<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
	public HashMap<Integer,simulator.TrafficLight> lights;
	
	public static TrafficController trafficController;
	
	//constructor
	public SimulatorManager(){
		motors = new HashMap<Integer,MotorVehicle>();
		lights = new HashMap<Integer, simulator.TrafficLight>();
		
		
		// delay = to 2secs
		delay = 2000;
		
		observer = new SimulatorObserver(this);
		TrackUpdateObservable.getInstance().addObserver(observer);
		
		trafficController = new TrafficController(3, 60, 3, 60); // 3 N-S lanes, 3 E-W lanes, 60 pixel lane width everywhere (based on calculations from simulator config file)
		trafficLights = trafficController.GetTrafficLights();
		
		System.out.println(String.format("SimulatorManager: %d Traffic Lights", trafficLights.size()));
		
		for (TrafficLight light : trafficLights) {
			light.addObserver(this);
			//add simulator traffic lights to hashmap
			lights.put(light.getID(), new simulator.TrafficLight(light.getID(), light.getTravelDirection(), light.GetColor()));
		}
		
		TrafficUpdateObservable.getInstance().addObserver(trafficController);
	}
	
	// receive updates on TrafficLight state changes (i.e. color changes)
	public void update (TrafficLight light) {
		System.out.println(String.format("Light %d, travel direction %s, changed to %s at %s",
			light.getID(), light.getTravelDirection().toString(), light.GetColor().toString(), light.getLastChanged().toString()
			));
		//updates lights in hashmap if same ID
		lights.put(light.getID(), new simulator.TrafficLight(light.getID(), light.getTravelDirection(), light.GetColor()));		
	}
	
	//methods
	//method init to initialize 
	public void init(){
		loadImage.init();	
		// TODO: Clean this up...
		//------------------------------------------------------------
		//START SIMULATED CARS
		//below the addCar functions will created simulated vehicles
		if (SimConfig.startSimulatedMotors)
		{
			addCar(
					1-1, 
					Direction.WEST, 
					new SimulatedTrack(
							new Point(SimConfig.simDisplayWidth,0), 
							simulatedCarsCounter++, 
							Direction.WEST,
							.01),
					true);
			
			addCar(
					2-1, 
					Direction.WEST, 
					new SimulatedTrack(
							new Point(SimConfig.simDisplayWidth,0), 
							simulatedCarsCounter++, 
							Direction.WEST,
							.05),
					true);
			
			addCar(
					2-1, 
					Direction.EAST, 
					new SimulatedTrack(
							new Point(0,0), 
							simulatedCarsCounter++, 
							Direction.EAST,
							.05),
					true);
			
			addCar(
					2-1, 
					Direction.NORTH, 
					new SimulatedTrack(
							new Point(0,SimConfig.simDisplayHeight), 
							simulatedCarsCounter++, 
							Direction.NORTH,
							.05),
					true);
			
			addCar(
					1-1, 
					Direction.NORTH, 
					new SimulatedTrack(
							new Point(0,SimConfig.simDisplayHeight), 
							simulatedCarsCounter++, 
							Direction.NORTH,
							.05),
					true);
			
			addCar(
					2-1, 
					Direction.SOUTH, 
					new SimulatedTrack(
							new Point(0,0), 
							simulatedCarsCounter++, 
							Direction.SOUTH,
							.05),
					true);
		}
		//END SIMULATED CARS
		//-----------------------------------------------------------------
	}
	
	public void setGraphics(Graphics g)
	{
		this.g = g;
	}
	
	
	public synchronized void tick()
	{
		for (MotorVehicle m : motors.values())
		{

			BulbColor l = trafficController.GetTrafficLight(m.getDirection()).GetColor();
			
			switch (l) {
				case Red:
					if (m.distToIntersection() >= 0 && m.distToIntersection() < 5) {
						continue;
					} else {
						break;
					}
				case Yellow:
					if (m.distToIntersection() < 0) {
						break;
					} else {
					m.setSpeed(m.speed-0.001); 
					break; }
				default:
					m.setSpeed(0.08f);
					break;
			}
			
			m.tick();
			
		}
	}
	
	public synchronized void addCar(int lane, Direction dir, Track track, boolean simulated)
	{

		if (simulated)
		{

			SimulatedTrack simTrack = (SimulatedTrack)track;
			
			System.out.printf("Added track %d, sim %d, lane %d, dir %d\n", 
					track.track_id,
					simTrack.track_id,
					lane,
					dir.ordinal());
			
			if (motors.containsKey(track.track_id))
			{
				System.out.println("Key already exists: " + track.track_id);
			}
			
			motors.put(simTrack.track_id, new SimulatedMotor(lane, dir, simTrack));

			return;
			
		}
		
		System.out.printf("Added track %d, lane %d, dir %d\n", 
				track.track_id,
				lane,
				dir.ordinal());
		
		switch (dir)
		{
			case NORTH:
				motors.put(track.track_id, new NorthboundMotor(lane, track));
				break;
			case SOUTH:
				motors.put(track.track_id, new SouthboundMotor(lane, track));
				break;
			case EAST:
				motors.put(track.track_id, new EastboundMotor(lane, track));
				break;
			case WEST:
				motors.put(track.track_id, new WestboundMotor(lane, track));
				break;
			default:
				break;
		}
	}
	
	//comment out if you do not want to remove cars from video feed
	public synchronized void removeCar(Track track)
	{
		motors.remove(track.track_id);
	}
	
	//comment out if you do not want to updatecar location from video feed
	public synchronized void updateCar(Track track)
	{
		
		motors.get(track.track_id).updateTrack(track);
		
		// notify observers of update
		motors.get(track.track_id).notifyObservers();
	}
	
	/*public Boolean trackClear(int lane, Direction dir) {
		Boolean trackClear = false;
		
		for (MotorVehicle m : motors.values())
		{
			while (trackClear = false) {
				if (lane == m.lane && dir == m.direction && m.distToIntersection() > 100){
					trackClear = false;
					continue;
				} else {
					trackClear = true;
					break;
				}

			}
			
			break;
						
		}
		
		return trackClear;
	}*/
	
	
	//method render to set graphic location and size
	public void render(Graphics g){
		
		//render background image
		g.drawImage(loadImage.fullImage,0,0,600,600,null);
		
		//renders initial traffic light bulbs

		//g.drawImage(loadImage.redLight,410,167,40,40,null);
		
		
		//render  vehicles
		synchronized(this){
			for (MotorVehicle mv : motors.values())
			{
				mv.render(g);
			}
			
		}
		
		//render traffic light
		synchronized(this){
			//simulator.TrafficLight.init(g);

			for (simulator.TrafficLight tl : lights.values())
			{
				tl.render(g);
			}
			
		}
		
		
	}
}
