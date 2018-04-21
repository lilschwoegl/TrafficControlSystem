package simulator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;

import javafx.scene.text.Font;
import observer.TrafficUpdateObservable;
import simulator.Constants.Direction;
import tracking.SimulatedTrack;
import tracking.Track;

public abstract class MotorVehicle {
	protected double x = -100;
	protected double y = -100;
	protected int lane;
	protected double speed = SimConfig.speed;
	public enum Route {STRAIGHT, LEFT, RIGHT};
	protected Direction direction;
	Track track;
	BufferedImage vehicleImage;
	int imageWidth, imageHeight;

	//constructor
	public MotorVehicle(int lane, Direction dir, Track track){
		this.lane = lane;
		this.direction = dir;
		this.track = track;
		
		loadImage();
	}
	
	//methods
	
	private void loadImage()
	{
		if (direction == Direction.NORTH) {
			vehicleImage = loadImage.upCarImage;
			imageWidth = 30;
			imageHeight = 45;
		} else if (direction == Direction.SOUTH) {
			vehicleImage = loadImage.downCarImage;
			imageWidth = 30;
			imageHeight = 45;
		} else if (direction == Direction.EAST) {
			vehicleImage = loadImage.rightCarImage;
			imageWidth = 45;
			imageHeight = 30;
		} else if (direction == Direction.WEST) {
			vehicleImage = loadImage.leftCarImage;
			imageWidth = 45;
			imageHeight = 30;
		}
	}
	
	public void updateTrack(Track track)
	{
		this.track = track;
		
		updateLane(this.track.lane);
		
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
	
	
	
	public abstract void initLane(int lane);
	
	public abstract void updateLane(int lane);
	
	public double distToIntersection()
	{
		switch (direction)
		{
			case NORTH:
				return y - (SimConfig.simDisplayHeight - SimConfig.roadStripLength + 10);
			case SOUTH:
				return SimConfig.roadStripLength - y - 55;
			case EAST:
				return SimConfig.roadStripLength - x - 55;
			case WEST:
				return x - (SimConfig.simDisplayWidth - SimConfig.roadStripLength + 10);
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
		
		g.drawImage(vehicleImage, (int)x, (int)y, imageWidth, imageHeight, null);

		g.setColor(Color.WHITE);
		//g.drawString(String.format("%.0f", distToIntersection()), (int)x+5, (int)y+30);
		g.drawString(String.format("%d", track.track_id), (int)x+5, (int)y+30);
	}
	
	protected Point getLaneStartPoint()
	{
		Point p = new Point();// = SimConfig.laneStartPoints[direction.ordinal()][lane];
		
		p.x = SimConfig.laneStartPoints[direction.ordinal()][lane].x;
		p.y = SimConfig.laneStartPoints[direction.ordinal()][lane].y;	
		
		System.out.printf("11111 Setting Track %d Y: %f\n", track.track_id, p.y);
		
		return p;
	}
	
	public Rectangle getBoundingBox()
	{
		return new Rectangle((int)x, (int)y, imageWidth, imageHeight);
	}
	
	public boolean collisionDetected(MotorVehicle mv)
	{
		Rectangle myBB = getBoundingBox();
		Rectangle theirBB = mv.getBoundingBox();

		return myBB.intersects(theirBB);
	}
}
