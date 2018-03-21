package simulator;

import java.awt.Graphics;
import java.awt.Point;

import application.Color;
import application.Direction;
import observer.TrafficLightObserver;

//TODO
// Need to update observers classes so tha that this class is observering the trafficLight color states
// Need to somehow call the this class from the simulator manager to ensure that the lights are rendering upon state change


public class paintTrafficLight {
	
	//private TrafficLightObserver observer;
	Color light;
	org.opencv.core.Point lightPoint;
	
	//constructor
	public paintTrafficLight(Direction direction, Color color){
		light = color;

		
		//observer = new TrafficLightObserver(this);
		//TrafficLightObservable.addObserver(observer);
		
		switch (direction) {
			case North:
				if (light == Color.Green) {
					lightPoint = Config.northGLight;
				} else if (light == Color.Yellow) {
					lightPoint = Config.northYLight;
				} else {
					lightPoint = Config.northRLight;
				}
				break;
			case South:
				if (light == Color.Green) {
					lightPoint = Config.southGLight;
				} else if (light == Color.Yellow) {
					lightPoint = Config.southYLight;
				} else {
					lightPoint = Config.southRLight;
				}
				break;
			case East:
				if (light == Color.Green) {
					lightPoint = Config.eastGLight;
				} else if (light == Color.Yellow) {
					lightPoint = Config.eastYLight;
				} else {
					lightPoint = Config.eastRLight;
				}
				break;
			case West:
				if (light == Color.Green) {
					lightPoint = Config.westGLight;
				} else if (light == Color.Yellow) {
					lightPoint = Config.westYLight;
				} else {
					lightPoint = Config.westRLight;
				}	
				break;
		}

	}
	
	public void render (Graphics g){
		double x,y;
		x = lightPoint.x;
		y = lightPoint.y;	
		
		if (light == Color.Green) {
		g.drawImage(loadImage.greenLight, (int)x, (int)y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
		} else if (light == Color.Yellow) {
		g.drawImage(loadImage.yellowLight, (int)x, (int)y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
		} else if (light == Color.Red) {
		g.drawImage(loadImage.redLight, (int)x, (int)y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
		}
	}
}
