package observer;

import observer.TrackUpdateObservable.TrackUpdate;
import tracking.Track;

public interface TrackObserver {
	
	public void update(Track track, TrackUpdate updateType);
	
}
