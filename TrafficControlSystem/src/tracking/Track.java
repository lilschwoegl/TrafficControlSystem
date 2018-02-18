package tracking;

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
	
	public DIRECTION direction;

	public DetectedObject lastDetect;

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

	public Point getLastCenter()
	{
		return KF.getLastResult();
	}
}