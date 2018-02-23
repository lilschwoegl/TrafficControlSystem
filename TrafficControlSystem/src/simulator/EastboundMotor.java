package simulator;

import tracking.Track;

public class EastboundMotor extends MotorVehicle{

	//constructor
	public EastboundMotor(int lane, Track track){
		
		super(lane, Direction.EAST ,track);
		
		initLane(lane);
	}
	
	@Override
	public void tick() {
		x += track.getDistChange().x;
	}

	@Override
	public void initLane(int lane) {
		// TODO Auto-generated method stub

		if(lane == 1){
			x = 0;
			y = 320;
		}
		if(lane == 2){
			x = 0;
			y = 355;
		}
	}

	@Override
	public void updateLane(int lane) {
		// TODO Auto-generated method stub
		if(lane == 1){
			y = 320;
		}
		if(lane == 2){
			y = 355;
		}
	}
	
	//methods
}
