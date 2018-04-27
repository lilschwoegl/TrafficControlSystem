package observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import simulator.Constants.Direction;
import tracking.Track;
import tracking.Track.MOVEMENT_TYPE;

public class TrackUpdateObservable implements TrackObservable {

	public enum TrackUpdate
	{
		ADDED,
		REMOVED,
		UPDATED
	};
	
	private ArrayList<TrackObserver> observers;
	private HashMap<Integer,Track> tracks;
	
	// Singleton
	private volatile static TrackUpdateObservable instance;
	
	/**
	 * Constructor
	 */
	private TrackUpdateObservable()
	{
		observers = new ArrayList<TrackObserver>();
		tracks = new HashMap<Integer,Track>();
	}
	
	/**
	 * Gets singleton instance
	 * @return Singleton instance
	 */
	public static TrackUpdateObservable getInstance()
	{
		if (instance == null)
			instance = new TrackUpdateObservable();
		
		return instance;
	}
	
	@Override
	/**
	 * Subscribes an observer
	 * @param o Observer to subscribe
	 */
	public void addObserver(TrackObserver o) {
		observers.add(o);
		System.out.println("Added observer " + o.getClass().toString() + ", total " + observers.size());
	}

	@Override
	/**
	 * Unsubscribes an observer
	 * @param o Observer to unsubscribe
	 */
	public void removeObserver(TrackObserver o) {
		observers.remove(o);
	}
	
	public void notifyObserver(Track track, TrackUpdate updateType, Direction heading)
	{
		for (TrackObserver observer : observers)
		{
			observer.update(track, updateType, heading);
		}
	}
	
	public void trackAdded(Track track, Direction heading)
	{
		tracks.put(track.track_id, track);
		notifyObserver(track, TrackUpdate.ADDED, heading);
	}
	
	public void trackRemoved(Track track, Direction heading)
	{
		tracks.remove(track.track_id);
		notifyObserver(track, TrackUpdate.REMOVED, heading);
	}
	
	public void trackUpdated(Track track, Direction heading)
	{
		tracks.remove(track.track_id);
		tracks.put(track.track_id, track);
		notifyObserver(track, TrackUpdate.UPDATED, heading);
	}
	
	public static String getUpdateToString(TrackUpdate type)
	{
		switch (type)
		{
		case ADDED:
			return "ADDED";
		case REMOVED:
			return "REMOVED";
		case UPDATED:
			return "UPDATED";
		}
		
		return "UNKNOWN";
	}
	
	public int getObserverCount()
	{
		return observers.size();
	}
	
}
