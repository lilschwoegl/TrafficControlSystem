package observer;

import java.time.Instant;

import application.BulbColor;
import application.TrafficLight;

public interface TrafficLightObserver {
	
	public void update (TrafficLight light);
}
