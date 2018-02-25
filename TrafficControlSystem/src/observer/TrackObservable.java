package observer;

import observer.TrackUpdateObservable.TrackUpdate;
import tracking.Track;

public interface TrackObservable {
	
	/**
	 * Subscribes an observer
	 * @param o Observer to subscribe
	 */
	public void addObserver(TrackObserver o);
	
	/**
	 * Unsubscribes an observer
	 * @param o Observer to unsubscribe
	 */
	public void removeObserver(TrackObserver o);
	
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
