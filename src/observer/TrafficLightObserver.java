package observer;

import application.Color;
import application.Direction;
import simulator.paintTrafficLight;

public class TrafficLightObserver {
	paintTrafficLight trafficLight;

	
	public TrafficLightObserver (paintTrafficLight paint) {
		trafficLight = paint;
	}
	
	public void update (Direction direction, Color color) {
		
	}
	
}
