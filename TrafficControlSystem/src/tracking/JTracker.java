package tracking;

import java.util.Vector;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import application.DetectedObject;

/**
 * JTracker.java TODO:
 * 
 * @author Kim Dinh Son Email:sonkdbk@gmail.com
 */

public abstract class JTracker {

	public float dt;

	public float Accel_noise_mag;

	public double dist_thres;

	public int maximum_allowed_skipped_frames;

	public int max_trace_length;
	
	public int max_sec_before_stale;

	public Vector<Track> tracks;
	
	public int track_removed;

	public abstract void update(Vector<DetectedObject> rectArray, Mat imag);

}