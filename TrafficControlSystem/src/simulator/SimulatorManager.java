package simulator;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import observer.SimulatorObserver;
import observer.TrafficUpdateObservable;
import simulator.MotorVehicle.Direction;
import tracking.Track;

public class SimulatorManager {
	
	private long time = System.nanoTime();
	private long delay;
	private SimulatorObserver observer;
	Graphics g;
	
	private HashMap<Integer,MotorVehicle> motors;
	
	//constructor
	public SimulatorManager(){
		//motor = new objectMotor();
		motors = new HashMap<Integer,MotorVehicle>();
		
		// delay = to 2secs
		delay = 2000;
		
		observer = new SimulatorObserver(this);
		TrafficUpdateObservable.getInstance().addObserver(observer);
	}
	
	//methods
	//method init to initialize 
	public void init(){
		loadImage.init();	
	}
	
	public void setGraphics(Graphics g)
	{
		this.g = g;
	}
	
	public void tick()
	{
		
	}
	
	public void addCar(int lane, Direction dir, Track track)
	{
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
	
	public void removeCar(Track track)
	{
		motors.remove(track.track_id);
	}
	
	public void updateCar(Track track)
	{
		motors.get(track.track_id).updateTrack(track);
	}
	
	
	//method render to set graphic location and size
	public void render(Graphics g){
		//render backgournd image
		g.drawImage(loadImage.fullImage,0,0,600,600,null);
		
		//render  vehicles
		for (MotorVehicle mv : motors.values())
		{
			mv.render(g);
		}
		
		
	}
}
