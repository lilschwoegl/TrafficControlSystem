package tracking;

import java.text.DateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Vector;

import org.opencv.core.Point;
import org.opencv.core.Rect;

import application.DetectedObject;

/**
 * Track.java TODO:
 * 
 * @author Kim Dinh Son Email:sonkdbk@gmail.com
 */

public class Track {

	public enum Aspect
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
	
	// aspect to the viewer
	public Aspect direction;

	// last successful detection
	public DetectedObject lastDetect;
	
	// lane that the track is in
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
		
		direction = Aspect.UNCERTAIN;
		
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
	
	public Rect getBestPositionRect()
	{
		Point lb, rt;

		// if there was a recent detect, use it to draw the bounding box
		// otherwise use the predicted position of the detect
		if (skipped_frames < 1)
		{

			lb = lastDetect.getLeftBot();
			rt = lastDetect.getRightTop();
		}
		else
		{
			lb = new Point(getLastCenter().x - lastDetect.getWidth() / 2, getLastCenter().y - lastDetect.getHeight() / 2);
			rt = new Point(getLastCenter().x + lastDetect.getWidth() / 2, getLastCenter().y + lastDetect.getHeight() / 2);
		}
		
		return new Rect(lb, rt);
	}
	
	public Point getBestPositionCenter()
	{
		Rect rect = getBestPositionRect();
		
		return new Point(rect.x + (rect.width) / 2, rect.y + (rect.height) / 2);
	}
	
	public long getSecSinceUpdate()
	{
		return Duration.between(lastUpdateTime, LocalDateTime.now()).getSeconds();
	}
	
	public boolean isTrackStale()
	{
		return (skipped_frames > TrackerConfig._maximum_allowed_skipped_frames || 
				getSecSinceUpdate() > TrackerConfig._max_sec_before_stale);
	}
}