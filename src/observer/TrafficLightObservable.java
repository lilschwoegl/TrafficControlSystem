package observer;

import application.Color;
import application.Direction;

public interface TrafficLightObservable {
	
	public void addObserver(TrafficLightObserver o);
	
	public void removeObserver(TrafficLightObserver o);
	
	public void notifyObserver(int ID, Direction direction, Color light);
	
}
