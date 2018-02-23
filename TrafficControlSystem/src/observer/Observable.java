package observer;

import observer.TrafficUpdateObservable.TrackUpdate;
import tracking.Track;

public interface Observable {
	
	/**
	 * Subscribes an observer
	 * @param o Observer to subscribe
	 */
	public void addObserver(Observer o);
	
	/**
	 * Unsubscribes an observer
	 * @param o Observer to unsubscribe
	 */
	public void removeObserver(Observer o);
	
	/**
	 * Notifies all subscribers of a change
	 * @param numTracks Number of tracks currently detected
	 * @param numOncoming Number of oncoming tracks
	 * @param numOutgoing Number of outgoing tracks
	 * @param numUncertain Number of uncertain tracks
	 */
	public void notifyObserver(int numTracks, int numOncoming, int numOutgoing, int numUncertain);
	
	public void notifyObserver(Track track, TrackUpdate updateType);
	
}
