package simulator;

import java.awt.Color;
import java.awt.Graphics;

import org.opencv.core.Point;

import javafx.scene.text.Font;
import observer.TrafficUpdateObservable;
import tracking.SimulatedTrack;
import tracking.Track;

public abstract class MotorVehicle {
	protected double x = -100;
	protected double y = -100;
	protected int lane;
	protected double speed = 0.08f;
	public enum Route {STRAIGHT, LEFT, RIGHT};
	public enum Direction {NORTH, SOUTH, EAST, WEST};
	protected Direction direction;
	Track track;

	//constructor
	public MotorVehicle(int lane, Direction dir, Track track){
		this.lane = lane;
		this.direction = dir;
		this.track = track;

		((SimulatedTrack)track).setSpeed(speed);
	}
	
	//methods
	
	public void updateTrack(Track track)
	{
		this.track = track;
		
		updateLane(track.lane);
		
		updateTrackPosition();
	}
	
	public Track getTrack()
	{
		return track;
	}
	
	public abstract void updateTrackPosition();
	

	public int getLane(){
		//return lane;	
		return lane;
	}
	
	public void notifyObservers()
	{
		TrafficUpdateObservable.getInstance().updateTraffic(this);
	}
	
	// return (x,y) position of the vehicle
	public Point getPosition()
	{
		return new Point(x, y);
	}
	
	public void resetPosition(){
		switch(direction) {
		case NORTH:
			y = Config.simDisplayHeight + 100;			
		case SOUTH:
			y = -100;
		case EAST:
			x = -100;
		case WEST:
			x = Config.simDisplayWidth + 100;		
		}
	}
	
	public abstract void initLane(int lane);
	
	public abstract void updateLane(int lane);
	
	public double distToIntersection()
	{
		switch (direction)
		{
			case NORTH:
				return y - (Config.simDisplayHeight - Config.roadStripLength + 10);
			case SOUTH:
				return Config.roadStripLength - y - 55;
			case EAST:
				return Config.roadStripLength - x - 55;
			case WEST:
				return x - (Config.simDisplayWidth - Config.roadStripLength + 10);
		}
		
		return -99999;
	}
	
	public Direction getDirection()
	{
		return direction;
	}
	
	public double getSpeed()
	{
		return speed;
	}
	
	public void setSpeed(double s)
	{
		this.speed = s;
		((SimulatedTrack)track).setSpeed(s);
	}
	
	public abstract void tick();
	
	public void leftTurn(){
		
	}
	
	public void rightTurn(){
		
	}
	
	public void render(Graphics g){
		//if else statement to render the appropriate car per direction driving
		if (direction == Direction.NORTH) {
			g.drawImage(loadImage.upCarImage, (int)x, (int)y, 30, 45, null);
		} else if (direction == Direction.SOUTH) {
			g.drawImage(loadImage.downCarImage, (int)x, (int)y, 30, 45, null);
		} else if (direction == Direction.EAST) {
			g.drawImage(loadImage.rightCarImage, (int)x, (int)y, 45, 30, null);
		} else if (direction == Direction.WEST) {
			g.drawImage(loadImage.leftCarImage, (int)x, (int)y, 45, 30, null);
		}

		g.setColor(Color.WHITE);
		g.drawString(String.format("%.0f", distToIntersection()), (int)x+5, (int)y+30);
	}
	
	protected Point getLaneStartPoint()
	{
		Point p = null;
		
		//p = Config.laneStartPoints[direction.ordinal()][lane];
		//p = new Point (250,0);
		
		switch (direction) {
			case NORTH:
				if (lane == 2-1) {
					p = new Point (Config.northBoundLane2.x,Config.northBoundLane2.y);
				} else {
					p = new Point (Config.northBoundLane1.x,Config.northBoundLane1.y);
				}
				break;
			case SOUTH:
				if (lane == 2-1) {
					p = new Point (Config.southBoundLane2.x,Config.southBoundLane2.y);
				} else {
					p = new Point (Config.southBoundLane1.x,Config.southBoundLane1.y);
				}
				break;
			case EAST:
				if (lane == 2-1) {
					p = new Point (Config.eastBoundLane2.x,Config.eastBoundLane2.y);
				} else {
					p = new Point (Config.eastBoundLane1.x,Config.eastBoundLane1.y);
				}
				break;
			case WEST:
				if (lane == 2-1) {
					p = new Point (Config.westBoundLane2.x,Config.westBoundLane2.y);
				} else {
					p = new Point (Config.westBoundLane1.x,Config.westBoundLane1.y);
				}
				break;
		}
		
		return p;
		
	}
}
