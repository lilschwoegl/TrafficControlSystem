package observer;

import java.util.HashMap;

import simulator.MotorVehicle;
import tracking.Track;

public interface TrafficObserver {

	public void update(MotorVehicle motor);
	
}
