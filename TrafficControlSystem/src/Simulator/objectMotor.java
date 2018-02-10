package Simulator;

import java.awt.Color;
import java.awt.Graphics;

public class objectMotor{
	private int x,y;
	private boolean left,right,up,down;
	
	//constructor
	public objectMotor(){
		
	}
	
	//methods
	public void init(){
		x = 0;
		y = 0;
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
			y -= 1;
		}
		if(down){
			y += 1;
		}*/
	}
	
	public void render(Graphics g){
		g.setColor(Color.red);
		g.fillRect(x, y, 40, 60);
	}
}
