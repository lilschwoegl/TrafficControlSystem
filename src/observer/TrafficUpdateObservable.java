package observer;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.MotorVehicle;
import tracking.Track;

public class TrafficUpdateObservable implements TrafficObservable {
	
	private ArrayList<TrafficObserver> observers;
	
	// Singleton
	private volatile static TrafficUpdateObservable instance;
	
	private TrafficUpdateObservable()
	{
		observers = new ArrayList<TrafficObserver>();
	}
	
	/**
	 * Gets singleton instance
	 * @return Singleton instance
	 */
	public static TrafficUpdateObservable getInstance()
	{
		if (instance == null)
			instance = new TrafficUpdateObservable();
		
		return instance;
	}

	@Override
	public void addObserver(TrafficObserver o) {
		// TODO Auto-generated method stub
		observers.add(o);
	}

	@Override
	public void removeObserver(TrafficObserver o) {
		// TODO Auto-generated method stub
		observers.remove(o);
	}

	@Override
	public void notifyObserver(MotorVehicle motor) {
		// TODO Auto-generated method stub
		for (TrafficObserver observer : observers)
		{
			observer.update(motor);
		}
	}
	
	public void updateTraffic(MotorVehicle motor)
	{
		notifyObserver(motor);
	}
	
	public int getObserverCount()
	{
		return observers.size();
	}
	
}
