package observer;

import application.BulbColor;
import application.Direction;
import application.TrafficLight;

public interface TrafficLightObservable {
	
	public void addObserver(TrafficLightObserver o);
	
	public void removeObserver(TrafficLightObserver o);
	
	public void notifyObservers();
	
}
