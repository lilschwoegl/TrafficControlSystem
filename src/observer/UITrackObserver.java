package observer;

import java.util.HashMap;

import application.SystemUIController;
import application.Utils;
import observer.TrackUpdateObservable.TrackUpdate;
import tracking.Track;
import tracking.Track.Aspect;

public class UITrackObserver implements TrackObserver{

	HashMap<Integer,Track> tracks = new HashMap<Integer,Track>();

	@Override
	public void update(Track track, TrackUpdate updateType) {
		// TODO Auto-generated method stub
		
		switch (updateType)
		{
			case ADDED:
				tracks.put(track.track_id,  track);
				break;
			case REMOVED:
				tracks.remove(track.track_id);
				break;
			case UPDATED:
				tracks.remove(track.track_id);
				tracks.put(track.track_id,  track);
				break;
		}
		
		int[] directions = new int[3];
		
		for (Track t : tracks.values())
		{
			directions[t.direction.ordinal()]++;
		}
		
		String str = String.format(
				"%d tracks, %d oncoming, %d outgoing, %d uncertain\n",
				tracks.size(),
				directions[Aspect.ONCOMING.ordinal()],
				directions[Aspect.OUTGOING.ordinal()],
				directions[Aspect.UNCERTAIN.ordinal()]);
		
		Utils.onFXThread(SystemUIController.trackLblProp, str);
	}

}
