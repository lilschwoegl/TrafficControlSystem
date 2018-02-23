package observer;

import observer.TrafficUpdateObservable.TrackUpdate;
import tracking.Track;

public interface Observer {
	
	public void update(int numTracks, int numOncoming, int numOutgoing, int numUncertain);
	
	public void update(Track track, TrackUpdate updateType);
	
}
