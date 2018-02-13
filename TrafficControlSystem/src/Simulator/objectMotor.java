package Simulator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class objectMotor{
	private int x,y,r;
	private double speed;
	//private boolean left,right,up,down;
	
	//constructor
	public objectMotor(){
		x = 0;
		y = 0;
		speed = 0.3f;
	}
	
	//methods
	public void init(){
		//position the object in a lane
		x = 250;

	}
	
	public void tick(){
		y += 1;
		
		
		/*
		if(right){
			x += 1;
		}
		if(left){
			x -= 1;
		}
		if(up){
			speed += 0.3f;
			if (speed >= 7){
				speed = 7;
			}
		}
		if(down){
			speed -= 0.030f;
			if (speed <= 0) {
				speed = 0;
			}
		}
		*/
	}
	
	public void northStraightTick(){
		y -= 1;
	}
	
	public void northRightTurnTick(){
		
	}
	
	public void northLeftTurnTick(){
		
	}
	
	public void southStraightTick(){
		y += 1;
	}
	
	public void southRightTick(){
		
	}
	
	public void southLeftTick(){
		if (y == 210){
			//r = 45;
			x += 1;
		} else {
			//r = 0;
			y +=1;
		}
		
	}
	
	/*public void paintComponent(Graphics g){
	     Graphics2D g2d=(Graphics2D)g; // Create a Java2D version of g.
	     g2d.translate(170, 0); // Translate the center of our coordinates.
	     g2d.rotate(1);  // Rotate the image by 1 radian.
	     g2d.drawImage(image, 0, 0, 200, 200, this);
	}*/
	

	
	public void render(Graphics g){
		g.drawImage(loadImage.downCarImage,x,y,30,45,null);
		
	}
}
