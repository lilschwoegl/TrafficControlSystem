package simulator;

import java.awt.Color;
import java.awt.Graphics;

import org.opencv.core.Point;

import javafx.scene.text.Font;
import observer.TrafficUpdateObservable;
import tracking.Track;

public abstract class MotorVehicle {
	protected double x = -100;
	protected double y = -100;
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
	
	
	public abstract void initLane(int lane);
	
	public abstract void updateLane(int lane);
	
	public double distToIntersection()
	{
		switch (direction)
		{
			case NORTH:
				return y - (Config.simDisplayHeight - Config.roadStripLength);
			case SOUTH:
				return Config.roadStripLength - y;
			case EAST:
				return Config.roadStripLength - x;
			case WEST:
				return x - (Config.simDisplayWidth - Config.roadStripLength);
		}
		
		return -99999;
	}
	
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
		return Config.laneStartPoints[direction.ordinal()][lane];
	}
}
