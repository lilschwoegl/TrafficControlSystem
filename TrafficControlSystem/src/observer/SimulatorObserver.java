package observer;

import simulator.SimulatorManager;
import observer.TrafficUpdateObservable.TrackUpdate;
import simulator.MotorVehicle.Direction;
import tracking.Track;

public class SimulatorObserver implements Observer{

	SimulatorManager simulator;
	
	public SimulatorObserver(SimulatorManager sim)
	{
		simulator = sim;
	}
	
	@Override
	public void update(int numTracks, int numOncoming, int numOutgoing, int numUncertain) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(Track track, TrackUpdate updateType) {
		// TODO Auto-generated method stub
		
//		System.out.printf("Updated track %d, type %s, lane %d, changex %f changey %f\n", 
//				track.track_id, 
//				TrafficUpdateObservable.getUpdateToString(updateType), 
//				track.lane,
//				track.getDistChange().x,
//				track.getDistChange().y);
		
		switch (updateType)
		{
			case ADDED:
				simulator.addCar(track.lane, Direction.SOUTH, track);
				break;
			case REMOVED:
				simulator.removeCar(track);
				break;
			case UPDATED:
				simulator.updateCar(track);
				break;
			default:
				break;
		}
		
	}

}
