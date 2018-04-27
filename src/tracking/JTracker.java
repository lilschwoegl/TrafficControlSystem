package tracking;

import java.util.Vector;

import org.opencv.core.Mat;

import application.DetectedObject;

/**
 * JTracker.java
 * 
 * https://github.com/son-oh-yeah/Moving-Target-Tracking-with-OpenCV
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