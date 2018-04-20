package simulator;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import application.BulbColor;
import simulator.Constants.Direction;

public class TrafficLight {
	Direction direction;
	BulbColor lightColor;
	org.opencv.core.Point lightPoint;
	int id;
	public static Graphics g1,g2,g3,g4,g5,g6,g7,g8,g9,g10,g11,g12;
	
	//Constructor
	public TrafficLight(int id, Direction direction, BulbColor lightColor) {
		this.id = id;
		this.direction = direction;
		this.lightColor = lightColor;
		lightPoint = getLightPoint(direction, lightColor);
	}
		
	//Methods
	public static void init (Graphics g){

			g1 = g;
			g2 = g;
			g3 = g;
			g4 = g;
			g5 = g;
			g6 = g;
			g7 = g;
			g8 = g;
			g9 = g;
			g10 = g;
			g11 = g;
			g12 = g;
		
			//north
			g1.drawImage(loadImage.greenLight, (int)SimConfig.northGLight.x, (int)SimConfig.northGLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			g2.drawImage(loadImage.yellowLight, (int)SimConfig.northYLight.x, (int)SimConfig.northYLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			g3.drawImage(loadImage.redLight, (int)SimConfig.northRLight.x, (int)SimConfig.northRLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			//south
			g4.drawImage(loadImage.greenLight, (int)SimConfig.southGLight.x, (int)SimConfig.southGLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			g5.drawImage(loadImage.yellowLight, (int)SimConfig.southYLight.x, (int)SimConfig.southYLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			g6.drawImage(loadImage.redLight, (int)SimConfig.southRLight.x, (int)SimConfig.southRLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			//east
			g7.drawImage(loadImage.greenLight, (int)SimConfig.eastGLight.x, (int)SimConfig.eastGLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			g8.drawImage(loadImage.yellowLight, (int)SimConfig.eastYLight.x, (int)SimConfig.eastYLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			g9.drawImage(loadImage.redLight, (int)SimConfig.eastRLight.x, (int)SimConfig.eastRLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			//west
			g10.drawImage(loadImage.greenLight, (int)SimConfig.westGLight.x, (int)SimConfig.westGLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			g11.drawImage(loadImage.yellowLight, (int)SimConfig.westYLight.x, (int)SimConfig.westYLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
			g12.drawImage(loadImage.redLight, (int)SimConfig.westRLight.x, (int)SimConfig.westRLight.y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
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
			g1.drawImage(loadImage.greenLight, (int)x, (int)y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
		} else if (lightColor == BulbColor.Yellow) {
			g2.drawImage(loadImage.yellowLight, (int)x, (int)y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
		} else if (lightColor == BulbColor.Red) {
			g3.drawImage(loadImage.redLight, (int)x, (int)y, (int)SimConfig.bulbWidth, (int)SimConfig.bulbHeight, null);
		}
	}
	
	
	
	
}
