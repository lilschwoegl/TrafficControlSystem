package simulator;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import application.Color;
import application.Direction;

public class TrafficLight {
	Direction direction;
	Color lightColor;
	org.opencv.core.Point lightPoint;
	int id;
	public static Graphics g1,g2,g3,g4,g5,g6,g7,g8,g9,g10,g11,g12;
	
	//Constructor
	public TrafficLight(int id, Direction direction, Color lightColor) {
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
			g1.drawImage(loadImage.greenLight, (int)Config.northGLight.x, (int)Config.northGLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			g2.drawImage(loadImage.yellowLight, (int)Config.northYLight.x, (int)Config.northYLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			g3.drawImage(loadImage.redLight, (int)Config.northRLight.x, (int)Config.northRLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			//south
			g4.drawImage(loadImage.greenLight, (int)Config.southGLight.x, (int)Config.southGLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			g5.drawImage(loadImage.yellowLight, (int)Config.southYLight.x, (int)Config.southYLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			g6.drawImage(loadImage.redLight, (int)Config.southRLight.x, (int)Config.southRLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			//east
			g7.drawImage(loadImage.greenLight, (int)Config.eastGLight.x, (int)Config.eastGLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			g8.drawImage(loadImage.yellowLight, (int)Config.eastYLight.x, (int)Config.eastYLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			g9.drawImage(loadImage.redLight, (int)Config.eastRLight.x, (int)Config.eastRLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			//west
			g10.drawImage(loadImage.greenLight, (int)Config.westGLight.x, (int)Config.westGLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			g11.drawImage(loadImage.yellowLight, (int)Config.westYLight.x, (int)Config.westYLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
			g12.drawImage(loadImage.redLight, (int)Config.westRLight.x, (int)Config.westRLight.y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
	}
	

	
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
	
	
	
	
	public void render(Graphics g){
		Graphics g1, g2, g3;
		g1 = g; g2 = g; g3 = g;
		double x,y;
		x = lightPoint.x;
		y = lightPoint.y;	
		
		if (lightColor == Color.Green) {
			g1.drawImage(loadImage.greenLight, (int)x, (int)y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
		} else if (lightColor == Color.Yellow) {
			g2.drawImage(loadImage.yellowLight, (int)x, (int)y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
		} else if (lightColor == Color.Red) {
			g3.drawImage(loadImage.redLight, (int)x, (int)y, (int)Config.bulbWidth, (int)Config.bulbHeight, null);
		}
	}
	
	
	
	
}
