package Simulator;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;

public class simulatorManager {
	private ArrayList<objectMotor> motor;
	private ArrayList<NorthboundMotor> nMotor;
	private ArrayList<EastboundMotor> eMotor;
	private ArrayList<WestboundMotor> wMotor;
	private traffic traffic;
	private long time = System.nanoTime();
	private long delay;
	
	//constructor
	public simulatorManager(){
		//motor = new objectMotor();
		motor = new ArrayList<objectMotor>();
		nMotor = new ArrayList<NorthboundMotor>();
		eMotor = new ArrayList<EastboundMotor>();
		wMotor = new ArrayList<WestboundMotor>();
		
		// delay = to 2secs
		delay = 2000;
	}
	
	//methods
	//method init to initialize 
	public void init(){
		loadImage.init();	
	}
	
	//method tick to move object across screen
	public void tick(){
		long elapsed = (System.nanoTime() - time)/1000000;
		if(elapsed > delay){
				nMotor.add(new NorthboundMotor(1));
				nMotor.add(new NorthboundMotor(2));
				motor.add(new objectMotor(1));
				motor.add(new objectMotor(2));
				eMotor.add(new EastboundMotor(1));
				eMotor.add(new EastboundMotor(2));
				wMotor.add(new WestboundMotor(1));
				wMotor.add(new WestboundMotor(2));
			
			time = System.nanoTime();
		}
		
		
		
		
		
		//Soutbound Motor
		for (int j = 0; j < motor.size(); j++) {
			motor.get(j).tick();
		}
		//Northbound Motor
		//nMotor.tick();
		for (int i = 0; i < nMotor.size(); i++){
		nMotor.get(i).tick();
		}
		//Eastbound Motor
		for (int k = 0; k < eMotor.size(); k++){
			eMotor.get(k).tick();
		}
		//Westbound Motor
		for (int l = 0; l < wMotor.size(); l++){
			wMotor.get(l).tick();
		}
		
	}
	
	//method render to set graphic location and size
	public void render(Graphics g){
		//render backgournd image
		g.drawImage(loadImage.fullImage,0,0,600,600,null);
		
		//render southbound vehicles
		for (int j = 0; j < motor.size(); j++) {
			motor.get(j).render(g);
		}
		
		//render northbound vehicles
		//nMotor.render(g);
		for(int i = 0; i < nMotor.size(); i++){
			nMotor.get(i).render(g);
		}
		
		for (int k = 0; k < eMotor.size(); k++){
			eMotor.get(k).render(g);
		}
		
		for (int l = 0; l < wMotor.size(); l++){
			wMotor.get(l).render(g);
		}
		
	}
}
