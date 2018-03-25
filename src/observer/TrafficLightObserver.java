package observer;

import java.time.Instant;

import application.Color;
import application.Direction;
import application.TrafficLight;
import simulator.paintTrafficLight;

public interface TrafficLightObserver {
	
	public void update (TrafficLight light);
}
