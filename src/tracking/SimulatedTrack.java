package tracking;

import java.util.ArrayList;

import org.opencv.core.Point;
import org.opencv.core.Rect;

import application.BulbColor;
import application.DetectedObject;
import application.TrafficController;
import application.TrafficLight;
import simulator.Constants.Direction;
import simulator.MotorVehicle;
import simulator.SimulatorManager;

public class SimulatedTrack extends Track{

	private Point lastPosition;
	private Point newPosition;
	private double speed = 0.05;
	private double elapsedTime = 0.0;
	private Direction travelDirection;

	
	public SimulatedTrack(Point pt, float dt, float Accel_noise_mag, int id, DetectedObject lastUpdate) {
		super(pt, dt, Accel_noise_mag, id, lastUpdate);
		// TODO Auto-generated constructor stub
	}
	
	public SimulatedTrack(Point pt, int id, Direction direction, double speed)
	{
		this(pt, id, direction, speed, 0);		
	}
	
	public SimulatedTrack(Point pt, int id, Direction direction, double speed, int classId)
	{
		super(pt, 0, 0, 0, new DetectedObject(classId));
		
		track_id = id;
		travelDirection = direction;
		
		this.speed = speed;
		
		lastPosition = pt;
		newPosition = pt;
		
	}
	
	public Point getDistChange()
	{
		return new Point(lastPosition.x - newPosition.x,
				lastPosition.y - newPosition.y);
	}

	public Point getLastCenter()
	{
		return lastPosition;
	}
	
	public Rect getBestPositionRect()
	{
		Rect r = new Rect();
		
		r.x      = (int)newPosition.x;
		r.y      = (int)newPosition.y;
		r.width  = (int)newPosition.x + 20;
		r.height = (int)newPosition.y + 20;
		
		return r;
	}
	
	public Point getBestPositionCenter()
	{
		return newPosition;
	}
	
	public void setSpeed(double speed)
	{
		this.speed = speed;
	}
	
	public void setPosition(Point p)
	{
		newPosition = p;
		lastPosition = p;
		
		//System.out.printf("33333 Setting Track %d Y: %f\n", track_id, p.y);
	}
	
	public void updateTrackPosition()
	{
		Point temp = newPosition;
	
		switch (travelDirection)
		{
		case NORTH:
			newPosition.y = lastPosition.y - (speed * getSecSinceUpdate());
			break;
		case SOUTH:
			newPosition.y = lastPosition.y + (speed * getSecSinceUpdate());
			break;
		case EAST:
			newPosition.x = lastPosition.x + (speed * getSecSinceUpdate());
			break;
		case WEST:
			newPosition.x = lastPosition.x - (speed * getSecSinceUpdate());
			break;
		}
		
		//System.out.printf("Track %d new X=%f Y=%f, Sec=%d, Speed=%f\n", 
		//		track_id, newPosition.x, newPosition.y, getSecSinceUpdate(), speed);
		
		lastPosition = temp;
	}

}
