package tracking;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import application.DetectedObject;
import config.TrackerConfig;
import simulator.Constants.Direction;

/**
 * Track.java TODO:
 * 
 * @author Kim Dinh Son Email:sonkdbk@gmail.com
 */

public class Track {

	public enum MOVEMENT_TYPE
	{
		ONCOMING,
		OUTGOING,
		STATIONARY,
		UNCERTAIN,
		NUM_VALUES
	};
	
	public Vector<Point> trace;
	public Vector<Point> history;
	public int track_id;
	public int skipped_frames;
	public int crossBorder;
	public Point prediction;
	public Kalman KF;
	public LocalDateTime lastUpdateTime;
	public boolean sentToSim = false;
	
	// aspect to the viewer
	public MOVEMENT_TYPE direction;
	public Direction oncomingHeading;

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
		
		direction = MOVEMENT_TYPE.UNCERTAIN;
		
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
			case STATIONARY:
				return "STATIONARY";
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
	
	public void drawDetect(Mat frame, boolean extrapDetects, boolean drawDetectInfo, boolean drawTrace)
	{
		try
		{
			// get the last detect for this track
			DetectedObject detect = this.lastDetect;

			Point lb, rt;

			// if there was a recent detect, use it to draw the bounding box
			// otherwise use the predicted position of the detect
			if (this.skipped_frames < 1)
			{

				lb = detect.getLeftBot();
				rt = detect.getRightTop();
			}
			else
			{
				if (extrapDetects)
				{
					float width = detect.getWidth();
					float height = detect.getHeight();
					Point lastCenter = this.getLastCenter();

					lb = new Point(lastCenter.x - width / 2, lastCenter.y - height / 2);
					rt = new Point(lastCenter.x + width / 2, lastCenter.y + height / 2);
				}
				else
				{
					lb = detect.getLeftBot();
					rt = detect.getRightTop();
				}
			}

			Scalar fontColor = new Scalar(255,255,255);
			Scalar boxColor = new Scalar(0,0,0);
			int boxThickness = 2;
			double fontScale = .3;
			int thickness = 1;
			int[] baseline = new int[] {0};
			Size fontSize = Imgproc.getTextSize(detect.getClassName() + " - ", 
					Core.FONT_HERSHEY_SIMPLEX, fontScale, thickness, baseline);
			Size boxSize = new Size(fontSize.width + 20, fontSize.height * 4);
			
			boolean drawBox = true;
			
			if (drawBox) 
			{
				// draw the bounding box around the detect
				Imgproc.rectangle(
						frame,
						lb,
						rt, 
						TrackerConfig.Colors[this.track_id % 9],
						boxThickness);
			}
			
			if (drawDetectInfo)
			{
				// draw the box to put info in
				Imgproc.rectangle(
						frame,
						lb,
						new Point(lb.x + boxSize.width, lb.y - boxSize.height),
						//CONFIG.Colors[tracker.tracks.get(i).track_id % 9],
						boxColor,
						Core.FILLED
						);
				
				// draw the class and probability of the detect
				Imgproc.putText(
						frame, 
						String.format("%s - %.0f%%", detect.getClassName(), detect.classProb * 100), 
						new Point(lb.x, lb.y - fontSize.height * 3), 
						Core.FONT_HERSHEY_SIMPLEX, 
						fontScale, 
						fontColor,
						thickness);
				
				// draw the direction the detected object is traveling
				Imgproc.putText(
						frame, 
						String.format("%s", this.getDirectionToString()), 
						new Point(lb.x, lb.y-fontSize.height * 2), 
						Core.FONT_HERSHEY_SIMPLEX, 
						fontScale, 
						fontColor,
						thickness);
				
				// draw the lane the car is in
				Imgproc.putText(
						frame, 
						String.format("Lane: %d", this.lane), 
						new Point(lb.x, lb.y-fontSize.height * 1), 
						Core.FONT_HERSHEY_SIMPLEX, 
						fontScale, 
						fontColor,
						thickness);
				
				// draw the track ID
				Imgproc.putText(
						frame, 
						String.format("ID: %d", this.track_id), 
						new Point(lb.x, lb.y), 
						Core.FONT_HERSHEY_SIMPLEX, 
						fontScale, 
						fontColor,
						thickness);
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}