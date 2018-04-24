package simulator;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import application.BulbColor;
import config.SimConfig;
import simulator.Constants.Direction;

public class TrafficLightDisplay {
	Direction direction;
	BulbColor lightColor;
	org.opencv.core.Point lightPoint;
	int id;
	public static Graphics g1,g2,g3,g4,g5,g6,g7,g8,g9,g10,g11,g12;
	private static BufferedImage trafficLights[] = new BufferedImage[3];
	
	//Constructor
	public TrafficLightDisplay(int id, Direction direction, BulbColor lightColor) {
		this.id = id;
		this.direction = direction;
		this.lightColor = lightColor;
		lightPoint = getLightPoint(direction, lightColor);
		init();
	}
		
	//Methods
	public static void init (){
		trafficLights[BulbColor.Red.ordinal()] = ImageLoader.getTrafficLightImage(BulbColor.Red);
		trafficLights[BulbColor.Yellow.ordinal()] = ImageLoader.getTrafficLightImage(BulbColor.Yellow);
		trafficLights[BulbColor.Green.ordinal()] = ImageLoader.getTrafficLightImage(BulbColor.Green);
	}
	

	
	org.opencv.core.Point getLightPoint (Direction direction, BulbColor light) {
		
		switch (direction) {
		case NORTH:
			if (light == BulbColor.Green) {
				lightPoint = SimConfig.northGLight;
			} else if (light == BulbColor.Yellow) {
				lightPoint = SimConfig.northYLight;
			} else {
				lightPoint = SimConfig.northRLight;
			}
			break;
		case SOUTH:
			if (light == BulbColor.Green) {
				lightPoint = SimConfig.southGLight;
			} else if (light == BulbColor.Yellow) {
				lightPoint = SimConfig.southYLight;
			} else {
				lightPoint = SimConfig.southRLight;
			}
			break;
		case EAST:
			if (light == BulbColor.Green) {
				lightPoint = SimConfig.eastGLight;
			} else if (light == BulbColor.Yellow) {
				lightPoint = SimConfig.eastYLight;
			} else {
				lightPoint = SimConfig.eastRLight;
			}
			break;
		case WEST:
			if (light == BulbColor.Green) {
				lightPoint = SimConfig.westGLight;
			} else if (light == BulbColor.Yellow) {
				lightPoint = SimConfig.westYLight;
			} else {
				lightPoint = SimConfig.westRLight;
			}	
			break;
		}
		
		return lightPoint;
	}
	
	
	
	
	public void render(Graphics g){
		Graphics g1, g2, g3;
		g1 = g; g2 = g; g3 = g;
		double x,y;
		x = lightPoint.x;
		y = lightPoint.y;	
		
		if (lightColor == BulbColor.Green) {
			g1.drawImage(trafficLights[BulbColor.Green.ordinal()], (int)x, (int)y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
		} else if (lightColor == BulbColor.Yellow) {
			g2.drawImage(trafficLights[BulbColor.Yellow.ordinal()], (int)x, (int)y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
		} else if (lightColor == BulbColor.Red) {
			g3.drawImage(trafficLights[BulbColor.Red.ordinal()], (int)x, (int)y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
		}
	}
	
	
	
	
}
