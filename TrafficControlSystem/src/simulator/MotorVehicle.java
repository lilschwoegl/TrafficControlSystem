package simulator;

import java.awt.Graphics;

import tracking.Track;

public abstract class MotorVehicle {
	protected int x;
	protected int y;
	protected int lane;
	protected double speed;
	public enum Route {STRAIGHT, LEFT, RIGHT};
	public enum Direction {NORTH, SOUTH, EAST, WEST};
	protected Direction direction;
	Track track;

	//constructor
	public MotorVehicle(int lane, Direction dir, Track track){
		this.lane = lane;
		this.direction = dir;
		this.track = track;

		speed = 0.5f;
	}
	
	//methods
	
	public void updateTrack(Track track)
	{
		this.track = track;
		
		updateLane(track.lane);
		
		tick();
	}
	
	public int getLane(){
		//return lane;	
		return lane;
	}
	
	
	
	public abstract void initLane(int lane);
	
	public abstract void updateLane(int lane);
	
	public Direction getDirection()
	{
		return direction;
	}
	
	public abstract void tick();
	
	public void leftTurn(){
		
	}
	
	public void rightTurn(){
		
	}
	
	public void render(Graphics g){
		g.drawImage(loadImage.upCarImage, x, y, 30, 45, null);
	}
}
