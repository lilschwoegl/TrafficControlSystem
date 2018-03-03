package observer;

import java.util.HashMap;

import simulator.MotorVehicle;
import tracking.Track;

public interface TrafficObservable {

	public void addObserver(TrafficObserver o);
	
	public void removeObserver(TrafficObserver o);
	
	public void notifyObserver(MotorVehicle motor);
	
}
