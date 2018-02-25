package observer;

import java.util.ArrayList;

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
	public void notifyObserver(int trackId, double distToIntersection) {
		// TODO Auto-generated method stub
		for (TrafficObserver observer : observers)
		{
			observer.update(trackId, distToIntersection);
		}
	}
	
	public void updateTraffic(int trackId, double distToIntersection)
	{
		notifyObserver(trackId, distToIntersection);
	}
	
}
