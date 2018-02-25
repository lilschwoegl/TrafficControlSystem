package observer;

import application.SystemUIController;
import application.Utils;
import observer.TrackUpdateObservable.TrackUpdate;
import tracking.Track;

public class UITrackObserver implements TrackObserver{

	@Override
	public void update(int numTracks, int numOncoming, int numOutgoing, int numUncertain) {
		String str = String.format(
				"%d tracks, %d oncoming, %d outgoing, %d uncertain\n",
				numTracks,
				numOncoming,
				numOutgoing,
				numUncertain);
		
		Utils.onFXThread(SystemUIController.trackLblProp, str);
	}

	@Override
	public void update(Track track, TrackUpdate updateType) {
		// TODO Auto-generated method stub
		
	}

}
