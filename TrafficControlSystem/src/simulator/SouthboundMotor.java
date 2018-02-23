package simulator;

import tracking.Track;

public class SouthboundMotor extends MotorVehicle{

	public SouthboundMotor(int lane, Track track){
		
		super(lane, Direction.SOUTH, track);
		
		initLane(lane);
	}
	
	@Override
	public void tick() {
		y += track.getDistChange().y;
	}

	@Override
	public void initLane(int lane) {
		// TODO Auto-generated method stub

		if(lane == 1){
			x = 250;
			y = 0;
		}
		if(lane == 2){
			x = 210;
			y = 0;
		}
	}

	@Override
	public void updateLane(int lane) {
		// TODO Auto-generated method stub
		if(lane == 1){
			x = 250;
		}
		if(lane == 2){
			x = 210;
		}
	}
	
}
