package simulator;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.opencv.core.Point;

import application.BulbColor;
import application.TrafficController;
import application.TrafficLight;
import application.TrafficController.SignalLogicConfiguration;
import config.SimConfig;
import observer.SimulatorObserver;
import observer.TrackUpdateObservable;
import observer.TrafficLightObserver;
import observer.TrafficUpdateObservable;
import simulator.Constants.Direction;
import simulator.MotorVehicle;
import tracking.SimulatedTrack;
import tracking.Track;

public class SimulatorManager implements TrafficLightObserver {
	
	private SimulatorObserver observer;
	Graphics g;
	private static BufferedImage intersectionImage;
	
	
	static int simulatedCarsCounter = 5000;
	
	public static ConcurrentHashMap <Integer,MotorVehicle> motors;
	public ArrayList<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
	public HashMap<Integer,simulator.TrafficLightDisplay> lights;
	
	public static TrafficController trafficController;
	
	//constructor
	public SimulatorManager(){
		motors = new ConcurrentHashMap <Integer,MotorVehicle>();
		lights = new HashMap<Integer, simulator.TrafficLightDisplay>();
		
		
		observer = new SimulatorObserver(this);
		TrackUpdateObservable.getInstance().addObserver(observer);
		
		trafficController = new TrafficController(SimConfig.defaultTrafficControllerLogicConfiguration, 3, 60, 3, 60); // 3 N-S lanes, 3 E-W lanes, 60 pixel lane width everywhere (based on calculations from simulator config file)
		trafficLights = trafficController.GetTrafficLights();
		
		System.out.println(String.format("SimulatorManager: %d Traffic Lights", trafficLights.size()));
		
		for (TrafficLight light : trafficLights) {
			light.addObserver(this);
			//add simulator traffic lights to hashmap
			lights.put(light.getID(), new simulator.TrafficLightDisplay(light.getID(), light.getTravelDirection(), light.GetColor()));
		}
		
		TrafficUpdateObservable.getInstance().addObserver(trafficController);
	}
	
	// receive updates on TrafficLight state changes (i.e. color changes)
	public void update (TrafficLight light) {
		System.out.println(String.format("Light %d, travel direction %s, changed to %s at %s",
			light.getID(), light.getTravelDirection().toString(), light.GetColor().toString(), light.getLastChanged().toString()
			));
		//updates lights in hashmap if same ID
		lights.put(light.getID(), new simulator.TrafficLightDisplay(light.getID(), light.getTravelDirection(), light.GetColor()));		
	}
	
	//methods
	//method init to initialize 
	public void init(){
		ImageLoader.init();	
	    TrafficLightDisplay.init();
		intersectionImage = ImageLoader.getIntersectionImage();
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
							SimConfig.speed),
					true);
			
			addCar(
					2-1, 
					Direction.WEST, 
					new SimulatedTrack(
							new Point(SimConfig.simDisplayWidth,0), 
							simulatedCarsCounter++, 
							Direction.WEST,
							SimConfig.speed),
					true);
			
			addCar(
					2-1, 
					Direction.EAST, 
					new SimulatedTrack(
							new Point(0,0), 
							simulatedCarsCounter++, 
							Direction.EAST,
							SimConfig.speed),
					true);
			
			addCar(
					2-1, 
					Direction.NORTH, 
					new SimulatedTrack(
							new Point(0,SimConfig.simDisplayHeight), 
							simulatedCarsCounter++, 
							Direction.NORTH,
							SimConfig.speed),
					true);
			
			addCar(
					1-1, 
					Direction.NORTH, 
					new SimulatedTrack(
							new Point(0,SimConfig.simDisplayHeight), 
							simulatedCarsCounter++, 
							Direction.NORTH,
							SimConfig.speed),
					true);
			
			addCar(
					2-1, 
					Direction.SOUTH, 
					new SimulatedTrack(
							new Point(0,0), 
							simulatedCarsCounter++, 
							Direction.SOUTH,
							SimConfig.speed),
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
			Double dist = m.distToIntersection();
			
			switch (l)
			{
				case Red:
					if ((dist >= 0 && dist < 5)) 
						continue;
					break;
				case Yellow:
					if (dist > 0 && dist < 5)
						//if (m.speed > 0.005) 
							m.setSpeed(0.005); 
					break;
				case Green:
				default:
					m.setSpeed(SimConfig.speed);
					break;
			}
			
			// should the vehicle move?
			if (trackClear(m))
				m.tick();
		}
	}
	
	public synchronized static void addCar(int lane, Direction dir, Track track, boolean simulated)
	{

//			System.out.printf("Added track %d, lane %d, dir %d\n", 
//					track.track_id,
//					lane,
//					dir.ordinal());
			
		if (simulated)
		{
			SimulatedMotor simMotor = new SimulatedMotor(lane, dir, (SimulatedTrack)track);
			
			if (trackClear(simMotor))
			{
				motors.put(track.track_id, simMotor);
			}
			
			return;
		}
		
	}
	
	public static boolean trackClear(MotorVehicle mv) {
		
		for (MotorVehicle m : motors.values())
		{
			if (m.lane == mv.lane && m.direction == mv.direction && 
					m.track.track_id != mv.track.track_id && mv.track.track_id > m.track.track_id){
				if (mv.collisionDetected(m))
					return false;
			} 
		}
		
		return true;
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
	
	//method render to set graphic location and size
	public void render(Graphics g){
		
		//render background image
		g.drawImage(intersectionImage,0,0,600,600,null);
		
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

			for (simulator.TrafficLightDisplay tl : lights.values())
			{
				tl.render(g);
			}
			
		}
		
		
	}
}
