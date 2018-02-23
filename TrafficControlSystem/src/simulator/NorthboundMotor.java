package simulator;

import tracking.Track;

public class NorthboundMotor extends MotorVehicle{

	//constructor
	public NorthboundMotor(int lane, Track track){

		super(lane, Direction.NORTH, track);
		
		initLane(lane);

	}

	@Override
	public void tick() {
		y -= track.getDistChange().y;
	}

	@Override
	public void initLane(int lane) {
		// TODO Auto-generated method stub

		if(lane == 1){
			x = 320;
			y = 600;
		}
		if(lane == 2){
			x = 355;
			y = 600;
		}
	}

	@Override
	public void updateLane(int lane) {
		// TODO Auto-generated method stub
		if(lane == 1){
			x = 320;
		}
		if(lane == 2){
			x = 355;
		}
	}
	
	//methods
}
