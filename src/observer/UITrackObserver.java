package observer;

import java.util.HashMap;

import application.SystemUIController;
import application.Utils;
import observer.TrackUpdateObservable.TrackUpdate;
import tracking.Track;
import tracking.Track.MOVEMENT_TYPE;

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
		
		int[] directions = new int[MOVEMENT_TYPE.NUM_VALUES.ordinal()];
		
		for (Track t : tracks.values())
		{
			directions[t.direction.ordinal()]++;
		}
		
		String str = String.format(
				"%d tracks, %d oncoming, %d outgoing, %d uncertain\n",
				tracks.size(),
				directions[MOVEMENT_TYPE.ONCOMING.ordinal()],
				directions[MOVEMENT_TYPE.OUTGOING.ordinal()],
				directions[MOVEMENT_TYPE.UNCERTAIN.ordinal()]);
		
		Utils.onFXThread(SystemUIController.trackLblProp, str);
	}

}
