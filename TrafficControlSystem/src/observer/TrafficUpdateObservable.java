package observer;

import java.util.ArrayList;
import java.util.Vector;

import tracking.Track;
import tracking.Track.DIRECTION;

public class TrafficUpdateObservable implements Observable {

	private ArrayList<Observer> observers;
	private Vector<Track> tracks;
	
	// Singleton
	private static TrafficUpdateObservable instance;
	
	/**
	 * Constructor
	 */
	private TrafficUpdateObservable()
	{
		observers = new ArrayList<Observer>();
		tracks = new Vector<Track>();
	}
	
	/**
	 * Gets singleton instance
	 * @return Singleton instance
	 */
	public static TrafficUpdateObservable getInstance()
	{
		if (instance == null)
			instance = new TrafficUpdateObservable();
		
		return instance;
	}
	
	@Override
	/**
	 * Subscribes an observer
	 * @param o Observer to subscribe
	 */
	public void addObserver(Observer o) {
		observers.add(o);
	}

	@Override
	/**
	 * Unsubscribes an observer
	 * @param o Observer to unsubscribe
	 */
	public void removeObserver(Observer o) {
		observers.remove(o);
	}
	
	@Override
	/**
	 * Notifies all subscribers of a change
	 * @param numTracks Number of tracks currently detected
	 * @param numOncoming Number of oncoming tracks
	 * @param numOutgoing Number of outgoing tracks
	 * @param numUncertain Number of uncertain tracks
	 */
	public void notifyObserver(int numTracks, int numOncoming, int numOutgoing, int numUncertain) {
		for (Observer observer : observers)
		{
			observer.update(numTracks, numOncoming, numOutgoing, numUncertain);
		}
	}
	
	/**
	 * Updates tracks maintained by the observable
	 * @param updatedTracks
	 */
	public void updateTracks(Vector<Track> updatedTracks)
	{
		if (updatedTracks.size() != tracks.size() ||
				tracksChangedDirection(updatedTracks))
		{
			tracks.clear();
			tracks.addAll(0, updatedTracks);
			
			int numOncoming = 0;
			int numOutgoing = 0;
			int numUncertain = 0;
			
			// count oncoming and outgoing in current list
			for (Track t : tracks)
			{
				if (t.direction == DIRECTION.ONCOMING)
					numOncoming++;
				else if (t.direction == DIRECTION.OUTGOING)
					numOutgoing++;
				else
					numUncertain++;
			}
			
			notifyObserver(updatedTracks.size(), numOncoming, numOutgoing, numUncertain);
		}		
	}
	
	/**
	 * Determines if any tracks have changed directions
	 * @param updatedTracks
	 * @return
	 */
	private boolean tracksChangedDirection(Vector<Track> updatedTracks)
	{
		int numOncoming = 0;
		int numOutgoing = 0;
		int numUncertain = 0;
		
		// count oncoming and outgoing in current list
		for (Track t : tracks)
		{
			if (t.direction == DIRECTION.ONCOMING)
				numOncoming++;
			else if (t.direction == DIRECTION.OUTGOING)
				numOutgoing++;
			else
				numUncertain++;
		}
		
		// subtract from updatedTracks
		for (Track t : updatedTracks)
		{
			if (t.direction == DIRECTION.ONCOMING)
				numOncoming--;
			else if (t.direction == DIRECTION.OUTGOING)
				numOutgoing--;
			else
				numUncertain--;
		}
		
		return (numOncoming != 0 || numOutgoing != 0 || numUncertain != 0);
	}
	
}
