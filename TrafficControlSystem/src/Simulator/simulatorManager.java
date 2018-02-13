package Simulator;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;

public class simulatorManager {
	private objectMotor motor;
	private NorthboundMotor nMotor;
	private EastboundMotor eMotor;
	private WestboundMotor wMotor;
	private long time = System.nanoTime();
	private long delay;
	
	//constructor
	public simulatorManager(){
		//motor = new objectMotor();		
		delay = 2000;
	}
	
	//methods
	//method init to initialize 
	public void init(){
		motor = new objectMotor();
		motor.init();
		nMotor = new NorthboundMotor(1);
		nMotor.init(nMotor.getLane());
		eMotor = new EastboundMotor(1);
		eMotor.init(eMotor.getLane());
		wMotor = new WestboundMotor(1);
		wMotor.init(wMotor.getLane());
		loadImage.init();
	}
	
	//method tick to move object across screen
	public void tick(){
		/*long elapsed = (System.nanoTime() - time)/1000000;
		if(elapsed > delay){
			nMotor.add(new NorthboundMotor());
			time = System.nanoTime();
		}*/
		
		
		/*for (int i = 0; i < 20; i++){
		nMotor.get(i).tick();
		}*/
		
		//Soutbound Motor
		motor.tick();
		//Northbound Motor
		nMotor.tick();
		//Eastbound Motor
		eMotor.tick();
		//Westbound Motor
		wMotor.tick();
		
	}
	
	//method render to set graphic location and size
	public void render(Graphics g){
		//render backgournd image
		g.drawImage(loadImage.fullImage,0,0,600,600,null);
		
		//render southbound vehicles
		motor.render(g);
		
		//render northbound vehicles
		nMotor.render(g);
		/*for(int i = 0; i < nMotor.size(); i++){
			nMotor.get(i).render(g);
		}*/
		eMotor.render(g);
		wMotor.render(g);
	}
}
