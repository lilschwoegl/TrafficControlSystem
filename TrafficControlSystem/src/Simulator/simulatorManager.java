package Simulator;

import java.awt.Color;
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
	}
	
	//method tick to move object across screen
	public void tick(){
		motor.tick();
	}
	
	//method render to set graphic location and size
	public void render(Graphics g){
		motor.render(g);
	}
}
