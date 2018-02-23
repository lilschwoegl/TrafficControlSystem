package tracking;

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Vector;

import org.opencv.core.Point;

import application.DetectedObject;

/**
 * Track.java TODO:
 * 
 * @author Kim Dinh Son Email:sonkdbk@gmail.com
 */

public class Track {

	public enum DIRECTION
	{
		ONCOMING,
		OUTGOING,
		UNCERTAIN
	};
	
	public Vector<Point> trace;
	public Vector<Point> history;
	public int track_id;
	public int skipped_frames;
	public int crossBorder;
	public Point prediction;
	public Kalman KF;
	public LocalDateTime lastUpdateTime;
	
	public DIRECTION direction;

	public DetectedObject lastDetect;
	
	public int lane = 0;

	/**
	 * @param pt
	 * @param dt = 0.2
	 * @param Accel_noise_mag = 0.5
	 */
	public Track(Point pt, float dt, float Accel_noise_mag, int id, DetectedObject lastUpdate) {
		trace = new Vector<>();
		history = new Vector<>();
		track_id=id;
		// Every track have its own Kalman filter,
		// it user for next point position prediction.
		// KF = new Kalman(pt);
		KF = new Kalman(pt,dt,Accel_noise_mag);
		// Here stored points coordinates, used for next position prediction.
		prediction = pt;

		skipped_frames = 0;

		crossBorder = 0;

		lastDetect = lastUpdate;
		
		direction = DIRECTION.UNCERTAIN;
		
		lastUpdateTime = LocalDateTime.now();
	}
	
	public String getDirectionToString()
	{
		switch (direction)
		{
			case ONCOMING:
				return "ONCOMING";
			case OUTGOING:
				return "OUTGOING";
			case UNCERTAIN:
			default:
				return "UNCERTAIN";
			
		}
		
	}
	
	public Point getDistChange()
	{
		if (trace.size() == 0 || trace.size() == 1)
			return new Point(0,0);
		
		Point p1 = trace.get(trace.size()-1);
		Point p2 = trace.get(trace.size()-2);
		
		return new Point(p2.x - p1.x, p2.y - p1.y);
	}

	public Point getLastCenter()
	{
		return KF.getLastResult();
	}
}