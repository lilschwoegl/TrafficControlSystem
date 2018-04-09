package simulator;

import org.opencv.core.Point;

import tracking.SimulatedTrack;
import tracking.Track;

public class SimulatedMotor extends MotorVehicle{

	public SimulatedMotor(int lane, Direction dir, SimulatedTrack track) {
		super(lane, dir, track);
		// TODO Auto-generated constructor stub
		
		initLane(lane);
	}

	@Override
	public void updateTrackPosition() {
		// TODO Auto-generated method stub
		x = track.getBestPositionCenter().x;
		y = track.getBestPositionCenter().y;
	}

	@Override
	public void initLane(int lane) {
		// TODO Auto-generated method stub
		Point p = getLaneStartPoint();
		((SimulatedTrack)track).setPosition(p);
	}

	@Override
	public void updateLane(int lane) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tick() {
		// TODO Auto-generated method stub
		((SimulatedTrack)track).updateTrackPosition();
		updateTrackPosition();
		//need to add a getlight status method call here... if green proceed, if red stop, if yellow slow down
		notifyObservers();
	}

}
