package simulator;

import org.opencv.core.Point;

import simulator.Constants.Direction;
import tracking.SimulatedTrack;

public class SimulatedMotor extends MotorVehicle{

	public SimulatedMotor(int lane, Direction dir, SimulatedTrack track) {
		super(lane, dir, track);
		// TODO Auto-generated constructor stub
		//System.out.printf("Calling init lane Track %d\n", track.track_id);
		
		setSpeed(speed);
		initLane(lane);
	}

	@Override
	public void updateTrackPosition() {
		
		((SimulatedTrack)track).updateTrackPosition();
		x = ((SimulatedTrack)track).getBestPositionCenter().x;
		y = ((SimulatedTrack)track).getBestPositionCenter().y;
		
		//System.out.printf("Track %d new Y = %f\n", track.track_id, y);
	}

	@Override
	public void initLane(int lane) {
		// TODO Auto-generated method stub
		Point p = getLaneStartPoint();
		//System.out.printf("22222 Setting Track %d Y: %f\n", track.track_id, p.y);
		((SimulatedTrack)track).setPosition(p);
	}

	@Override
	public void updateLane(int lane) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tick() {
		// TODO Auto-generated method stub
		//((SimulatedTrack)track).updateTrackPosition();
		updateTrackPosition();
		notifyObservers();
		
		//System.out.printf("Track %d tick\n", track.track_id);
	}

}
