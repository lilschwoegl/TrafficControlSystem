package Simulator;

import java.awt.Graphics;

public class EastboundMotor {
	private int x;
	private int y;
	private int lane;
	private double speed;
	private enum Route {STRAIGHT, LEFT, RIGHT};

	//constructor
	public EastboundMotor(int lane){
		this.lane = lane;
		x = 0;
		y = 0;
		speed = 0.5f;
	}
	
	//methods
	
	public void init(int lane){
		//position the object in a lane
		if(lane == 1){
			x = 0;
			y = 320;
		}
		if(lane == 2){
			x = 0;
			y = 355;
		}


	}
	
	public int getLane(){
		//return lane;	
		return 2;
	}
	
	public void tick(){
		x += 1;	
		
		/*if(up){
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
	
	public void render(Graphics g){
		g.drawImage(loadImage.rightCarImage, x, y, 45, 30, null);
	}
}
