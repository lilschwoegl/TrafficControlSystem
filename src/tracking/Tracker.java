package tracking;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import application.DetectedObject;
import observer.TrackUpdateObservable;
import simulator.Constants.Direction;
import tracking.Track.MOVEMENT_TYPE;

/**
 * Tracker.java TODO:
 * 
 * @author Kim Dinh Son Email:sonkdbk@gmail.com
 */

public class Tracker extends JTracker {
	int nextTractID = 0;
	Vector<Integer> assignment = new Vector<>();
	private Direction oncomingHeading;

	public Tracker(float _dt, float _Accel_noise_mag, double _dist_thres,
			int _maximum_allowed_skipped_frames, int _max_trace_length, 
			int _max_sec_before_stale, Direction oncomingHeading) {
		tracks = new Vector<>();
		dt = _dt;
		Accel_noise_mag = _Accel_noise_mag;
		dist_thres = _dist_thres;
		maximum_allowed_skipped_frames = _maximum_allowed_skipped_frames;
		max_trace_length = _max_trace_length;
		max_sec_before_stale = _max_sec_before_stale;
		track_removed = 0;
		this.oncomingHeading = oncomingHeading;
	}

	static Scalar Colors[] = { new Scalar(255, 0, 0), new Scalar(0, 255, 0),
			new Scalar(0, 0, 255), new Scalar(255, 255, 0),
			new Scalar(0, 255, 255), new Scalar(255, 0, 255),
			new Scalar(255, 127, 255), new Scalar(127, 0, 255),
			new Scalar(127, 0, 127) };

	double euclideanDist(Point p, Point q) {
		Point diff = new Point(p.x - q.x, p.y - q.y);
		return Math.sqrt(diff.x * diff.x + diff.y * diff.y);
	}

	public void update(Vector<DetectedObject> rectArray, Mat imag) {			
		if (tracks.size() == 0) {
			// If no tracks yet
			for (int i = 0; i < rectArray.size(); i++) {
				Track tr = new Track(rectArray.get(i).getObjectCenter(), dt,
						Accel_noise_mag, nextTractID++, rectArray.get(i));		
				tracks.add(tr);
				
				//TrackUpdateObservable.getInstance().trackAdded(tr, oncomingHeading);
			}
		}

		// -----------------------------------
		// Number of tracks and detections
		// -----------------------------------
		int N = tracks.size();
		int M = rectArray.size();

		// Cost matrix.
		double[][] Cost = new double[N][M]; // size: N, M
		// Vector<Integer> assignment = new Vector<>(); // assignment according to Hungarian algorithm
		assignment.clear();
		// -----------------------------------
		// Caculate cost matrix (distances)
		// -----------------------------------
		for (int i = 0; i < tracks.size(); i++) {
			for (int j = 0; j < rectArray.size(); j++) {
				Cost[i][j] = euclideanDist(tracks.get(i).prediction, rectArray.get(j).getObjectCenter());
			}
		}

		// -----------------------------------
		// Solving assignment problem (tracks and predictions of Kalman filter)
		// -----------------------------------
		// HungarianAlg APS = new HungarianAlg();
		// APS.Solve(Cost,assignment, HungarianAlg.TMethod.optimal);

		// HungarianAlg2 APS = new HungarianAlg2();
		// APS.Solve(Cost,assignment);
		
		AssignmentOptimal APS = new AssignmentOptimal();
		APS.Solve(Cost, assignment);
		// -----------------------------------
		// clean assignment from pairs with large distance
		// -----------------------------------
		// Not assigned tracks
		Vector<Integer> not_assigned_tracks = new Vector<>();

		for (int i = 0; i < assignment.size(); i++) {
			if (assignment.get(i) != -1) {
				if (Cost[i][assignment.get(i)] > dist_thres) {
					assignment.set(i, -1);
					// Mark unassigned tracks, and increment skipped frames
					// counter,
					// when skipped frames counter will be larger than
					// threshold, track will be deleted.
					not_assigned_tracks.add(i);
				}
			} else {
				// If track have no assigned detect, then increment skipped
				// frames counter.
				tracks.get(i).skipped_frames++;
				//not_assigned_tracks.add(i);
			}
		}

		// -----------------------------------
		// If track didn't get detects long time, remove it.
		// -----------------------------------

		checkForStaleTracks();

		// -----------------------------------
		// Search for unassigned detects
		// -----------------------------------
		Vector<Integer> not_assigned_detections = new Vector<>();
		for (int i = 0; i < rectArray.size(); i++) {
			if (!assignment.contains(i)) {
				not_assigned_detections.add(i);
			}
		}

		// -----------------------------------
		// and start new tracks for them.
		// -----------------------------------
		if (not_assigned_detections.size() > 0) {
			for (int i = 0; i < not_assigned_detections.size(); i++) {
				Track tr = new Track(rectArray.get(not_assigned_detections.get(i)).getObjectCenter(), dt,
						Accel_noise_mag, nextTractID++, rectArray.get(i));
				tracks.add(tr);
				
				//TrackUpdateObservable.getInstance().trackAdded(tr, oncomingHeading);
			}
		}


		// -----------------------------------
		// Update Kalman filter states
		// -----------------------------------
		updateKalman(imag,rectArray);

		for (int j = 0; j < assignment.size(); j++) {
			if (assignment.get(j) != -1) {
				Point pt1 = new Point(
						(int) ((rectArray.get(assignment.get(j)).xLeftBot + rectArray
								.get(assignment.get(j)).xRightTop) / 2), rectArray
						.get(assignment.get(j)).yLeftBot);
				Point pt2 = new Point(
						(int) ((rectArray.get(assignment.get(j)).xLeftBot + rectArray
								.get(assignment.get(j)).xRightTop) / 2), rectArray
						.get(assignment.get(j)).yRightTop);

				Imgproc.putText(imag, tracks.get(j).track_id + "", pt2,
						2 * Core.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255,
								255), 1);

				if (tracks.get(j).history.size() < 20)
					tracks.get(j).history.add(pt1);
				else {
					tracks.get(j).history.remove(0);
					tracks.get(j).history.add(pt1);
				}

			}
		}
	}
	
	public void checkForStaleTracks()
	{
		// -----------------------------------
		// If track didn't get detects long time, remove it.
		// -----------------------------------

		for (int i = 0; i < tracks.size(); i++) {
			if (tracks.get(i).isTrackStale()) {	
				
				TrackUpdateObservable.getInstance().trackRemoved(tracks.get(i), oncomingHeading);
				
				tracks.remove(i);
				assignment.remove(i);
				track_removed++;
				i--;
				
			}
		}
	}

	public void updateKalman(Mat imag, Vector<DetectedObject> detections) {
		// Update Kalman Filters state
		if(detections.size()==0)
			for(int i = 0; i < assignment.size(); i++)
				assignment.set(i, -1);

		for (int i = 0; i < assignment.size(); i++) {
			// If track updated less than one time, then filter state is not
			// correct.
			tracks.get(i).prediction=tracks.get(i).KF.getPrediction();

			if (assignment.get(i) != -1) // If we have assigned detect, then
				// update using its coordinates,
			{
				tracks.get(i).skipped_frames = 0;
				tracks.get(i).prediction = tracks.get(i).KF.update(
						detections.get(assignment.get(i)).getObjectCenter(), true);

				tracks.get(i).lastDetect = detections.get(assignment.get(i));
				tracks.get(i).lastUpdateTime = LocalDateTime.now();

			} else // if not continue using predictions
			{
				tracks.get(i).prediction = tracks.get(i).KF.update(new Point(0,
						0), false);
				
			}
			
			

			if (tracks.get(i).trace.size() > max_trace_length) {
				for (int j = 0; j < tracks.get(i).trace.size() - max_trace_length; j++)
					tracks.get(i).trace.remove(j);
			}

			tracks.get(i).trace.add(tracks.get(i).prediction);
			tracks.get(i).KF.setLastResult(tracks.get(i).prediction);

			// update track direction
			if (tracks.get(i).trace.size() > 1)
			{
				
				Point p1 = tracks.get(i).trace.get(tracks.get(i).trace.size() - 1);
				Point p2 = tracks.get(i).trace.get(0);
				
				double mag = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
				
				if (mag < TrackerConfig._min_dist_change) 
				{
					tracks.get(i).direction = MOVEMENT_TYPE.STATIONARY;
				}
				else if (tracks.get(i).trace.get(tracks.get(i).trace.size() - 1).y > 
					tracks.get(i).trace.get(0).y)
				{
					tracks.get(i).direction = MOVEMENT_TYPE.ONCOMING;
					
					if (!tracks.get(i).sentToSim)
					{
						// send to sim
						TrackUpdateObservable.getInstance().trackAdded(tracks.get(i), oncomingHeading);
						tracks.get(i).sentToSim = true;
					}
				}
				else
				{
					tracks.get(i).direction = MOVEMENT_TYPE.OUTGOING;
				}
			}
			else
			{
				tracks.get(i).direction = MOVEMENT_TYPE.UNCERTAIN;
			}
			
			TrackUpdateObservable.getInstance().trackUpdated(tracks.get(i), oncomingHeading);
		}
	}
}