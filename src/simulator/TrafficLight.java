package simulator;

import java.awt.Graphics;
import application.Color;
import application.Direction;

public class TrafficLight {
	public Direction direction;
	public Color lightColor;
	public org.opencv.core.Point lightPoint;
	
	//Constructor
	public TrafficLight(Direction direction, Color lightColor) {
		this.direction = direction;
		this.lightColor = lightColor;
		lightPoint = getLightPoint(direction, lightColor);
	}
		
	//Methods
	
	org.opencv.core.Point getLightPoint (Direction direction, Color light) {
		
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
		
		return lightPoint;
	}
	
	
	public void render (Graphics g){
		double x,y;
		x = lightPoint.x;
		y = lightPoint.y;	
		
		if (lightColor == Color.Green) {
		g.drawImage(loadImage.greenLight, (int)x, (int)y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
		} else if (lightColor == Color.Yellow) {
		g.drawImage(loadImage.yellowLight, (int)x, (int)y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
		} else if (lightColor == Color.Red) {
		g.drawImage(loadImage.redLight, (int)x, (int)y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
		}
	}
	
	
	
	
}
