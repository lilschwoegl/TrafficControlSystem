package observer;

import observer.TrackUpdateObservable.TrackUpdate;
import tracking.Track;

public interface TrackObserver {
	
	public void update(int numTracks, int numOncoming, int numOutgoing, int numUncertain);
	
	public void update(Track track, TrackUpdate updateType);
	
}
