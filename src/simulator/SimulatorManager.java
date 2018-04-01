package simulator;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;

import org.opencv.core.Point;

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
	
	int simulatedCarsCounter = 5000;
	
	private HashMap<Integer,MotorVehicle> motors;
	private ArrayList<TrafficLight> trafficLights = new ArrayList<TrafficLight>();
	
	TrafficController trafficController;
	private simulator.TrafficLight simLight;
	
	//constructor
	public SimulatorManager(){
		//motor = new objectMotor();
		motors = new HashMap<Integer,MotorVehicle>();
		
		// delay = to 2secs
		delay = 2000;
		
		observer = new SimulatorObserver(this);
		TrackUpdateObservable.getInstance().addObserver(observer);
		
		trafficController = new TrafficController(3, 60, 3, 60); // 3 N-S lanes, 3 E-W lanes, 60 pixel lane width everywhere (based on calculations from simulator config file)
		trafficLights = trafficController.GetTrafficLights();
		
		System.out.println(String.format("SimulatorManager: %d Traffic Lights", trafficLights.size()));
		
		for (TrafficLight light : trafficLights) {
			light.addObserver(this);
		}
		
		TrafficUpdateObservable.getInstance().addObserver(trafficController);
	}
	
	// receive updates on TrafficLight state changes (i.e. color changes)
	public void update (TrafficLight light) {
		System.out.println(String.format("Light %d, travel direction %s, changed to %s at %s",
			light.getID(), light.getTravelDirection().toString(), light.GetColor().toString(), light.getLastChanged().toString()
			));
		
		simLight = new simulator.TrafficLight(light.getTravelDirection(), light.GetColor());
		simLight.render(g);
		
		
	}
	
	//methods
	//method init to initialize 
	public void init(){
		loadImage.init();	
		
		// TODO: Clean this up...
		//------------------------------------------------------------
		//START SIMULATED CARS
		//below the addCar functions will created simulated vehicles
		addCar(
				1-1, 
				Direction.WEST, 
				new SimulatedTrack(
						new Point(Config.simDisplayWidth,0), 
						simulatedCarsCounter++, 
						Direction.WEST,
						.01),
				true);
		
		addCar(
				2-1, 
				Direction.WEST, 
				new SimulatedTrack(
						new Point(Config.simDisplayWidth,0), 
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
						new Point(0,Config.simDisplayHeight), 
						simulatedCarsCounter++, 
						Direction.NORTH,
						.05),
				true);
		
		addCar(
				1-1, 
				Direction.NORTH, 
				new SimulatedTrack(
						new Point(0,Config.simDisplayHeight), 
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
			m.tick();
		}
	}
	
	public synchronized void addCar(int lane, Direction dir, Track track, boolean simulated)
	{
		if (simulated)
		{
			motors.put(track.track_id, new SimulatedMotor(lane, dir, (SimulatedTrack)track));
			return;
		}
		
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
	
	
	//method render to set graphic location and size
	public void render(Graphics g){
		
		//render background image
		g.drawImage(loadImage.fullImage,0,0,600,600,null);
		//g.drawImage(loadImage.redLight,410,167,40,40,null);
		
		
		//render  vehicles
		synchronized(this){
			for (MotorVehicle mv : motors.values())
			{
				mv.render(g);
			}
			
		}
		
		//render traffic light
	
		
		
	}
}
