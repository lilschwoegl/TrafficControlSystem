package observer;

import simulator.SimulatorManager;

import org.opencv.core.Point;

import observer.TrackUpdateObservable.TrackUpdate;
import simulator.MotorVehicle.Direction;
import tracking.SimulatedTrack;
import tracking.Track;


public class SimulatorObserver implements TrackObserver{

	SimulatorManager simulator;
	int simulatedCarsCounter = 6000;
	
	public SimulatorObserver(SimulatorManager sim)
	{
		simulator = sim;
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
		SimulatedTrack strack;
		
		
		switch (updateType)
		{
			case ADDED:
				strack = new SimulatedTrack(  
						new Point(0,0),
						simulatedCarsCounter++,
						Direction.SOUTH,
						.05);
				//simulator.addCar(track.lane, Direction.SOUTH, track, false);
				simulator.addCar(
						track.lane, 
						Direction.SOUTH, 
						strack, 
						true);
				break;
			case REMOVED:
				//simulator.removeCar(strack);
				break;
			case UPDATED:
				//simulator.updateCar(strack);
				break;
			default:
				break;
		}
		
	}

}
