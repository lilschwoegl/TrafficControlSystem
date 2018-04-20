package observer;

import observer.TrackUpdateObservable.TrackUpdate;
import simulator.Constants.Direction;
import tracking.Track;

public interface TrackObserver {
	
	public void update(Track track, TrackUpdate updateType, Direction heading);
	
}
