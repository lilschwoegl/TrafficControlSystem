package Simulator;

import java.awt.Graphics;

public class simulatorManager {
	private objectMotor motor;
	
	//constructor
	public simulatorManager(){
		
	}
	
	//methods
	//method init to initialize 
	public void init(){
		motor = new objectMotor();
		motor.init();
		loadImage.init();
	}
	
	//method tick to move object across screen
	public void tick(){
		motor.tick();
	}
	
	//method render to set graphic location and size
	public void render(Graphics g){
		g.drawImage(loadImage.fullImage,0,0,600,600,null);
		//g.drawImage(loadImage.subImage1, 150, 150, 100, 100, null);
		motor.render(g);
	}
}
