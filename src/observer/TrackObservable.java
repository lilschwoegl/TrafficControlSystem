package observer;

import observer.TrackUpdateObservable.TrackUpdate;
import simulator.Constants.Direction;
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
	
	public void notifyObserver(Track track, TrackUpdate updateType, Direction heading);
	
}
