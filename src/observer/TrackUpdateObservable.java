package observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

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
	
	public void notifyObserver(Track track, TrackUpdate updateType)
	{
		for (TrackObserver observer : observers)
		{
			observer.update(track, updateType);
		}
	}
	
	public void trackAdded(Track track)
	{
		tracks.put(track.track_id, track);
		notifyObserver(track, TrackUpdate.ADDED);
	}
	
	public void trackRemoved(Track track)
	{
		tracks.remove(track.track_id);
		notifyObserver(track, TrackUpdate.REMOVED);
	}
	
	public void trackUpdated(Track track)
	{
		tracks.remove(track.track_id);
		tracks.put(track.track_id, track);
		notifyObserver(track, TrackUpdate.UPDATED);
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
	
	/**
	 * Updates tracks maintained by the observable
	 * @param updatedTracks
	 */
//	public void updateTracks(Vector<Track> updatedTracks)
//	{
//		if (updatedTracks.size() != tracks.size() ||
//				tracksChangedDirection(updatedTracks))
//		{
//			tracks.clear();
//			tracks.addAll(0, updatedTracks);
//			
//			int numOncoming = 0;
//			int numOutgoing = 0;
//			int numUncertain = 0;
//			
//			// count oncoming and outgoing in current list
//			for (Track t : tracks)
//			{
//				if (t.direction == DIRECTION.ONCOMING)
//					numOncoming++;
//				else if (t.direction == DIRECTION.OUTGOING)
//					numOutgoing++;
//				else
//					numUncertain++;
//			}
//			
//			notifyObserver(updatedTracks.size(), numOncoming, numOutgoing, numUncertain);
//		}		
//	}
	
	/**
	 * Determines if any tracks have changed directions
	 * @param updatedTracks
	 * @return
	 */
//	private boolean tracksChangedDirection(Vector<Track> updatedTracks)
//	{
//		int numOncoming = 0;
//		int numOutgoing = 0;
//		int numUncertain = 0;
//		
//		// count oncoming and outgoing in current list
//		for (Track t : tracks)
//		{
//			if (t.direction == DIRECTION.ONCOMING)
//				numOncoming++;
//			else if (t.direction == DIRECTION.OUTGOING)
//				numOutgoing++;
//			else
//				numUncertain++;
//		}
//		
//		// subtract from updatedTracks
//		for (Track t : updatedTracks)
//		{
//			if (t.direction == DIRECTION.ONCOMING)
//				numOncoming--;
//			else if (t.direction == DIRECTION.OUTGOING)
//				numOutgoing--;
//			else
//				numUncertain--;
//		}
//		
//		return (numOncoming != 0 || numOutgoing != 0 || numUncertain != 0);
//	}
	
}
