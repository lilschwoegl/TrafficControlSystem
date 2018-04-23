package observer;

import simulator.SimulatorManager;

import org.opencv.core.Point;

import observer.TrackUpdateObservable.TrackUpdate;
import simulator.Constants.Direction;
import simulator.MotorVehicle;
import tracking.SimulatedTrack;
import tracking.Track;
import tracking.Track.MOVEMENT_TYPE;


public class SimulatorObserver implements TrackObserver{

	SimulatorManager simulator;
	int simulatedCarsCounter = 6000;
	
	public SimulatorObserver(SimulatorManager sim)
	{
		simulator = sim;
	}

	@Override
	public void update(Track track, TrackUpdate updateType, Direction heading) {
		// TODO Auto-generated method stub
		
		//System.out.printf("Updated track %d, lane %d, changex %f changey %f\n", 
		//		track.track_id, 
		//		track.lane,
		//		track.getDistChange().x,
		//		track.getDistChange().y);
		
		SimulatedTrack strack;
		
		
		switch (updateType)
		{
			case ADDED:
				
				strack = new SimulatedTrack(  
						new Point(0,0),
						//simulatedCarsCounter++,
						track.track_id,
						heading,
						.05);
				
				simulator.addCar(
						track.lane, 
						heading, 
						strack, 
						true);
				
//				simulator.addCar(track.lane, Direction.SOUTH, track, false);
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
