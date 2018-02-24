package simulator;

import tracking.Track;

public class SouthboundMotor extends MotorVehicle{

	public SouthboundMotor(int lane, Track track){
		
		super(lane, Direction.SOUTH, track);
		
		initLane(lane);
	}
	
	@Override
	public void tick() {
		//y += track.getDistChange().y;
		
		y = (track.getBestPositionCenter().y * Config.roadStripRatio);
				
		// on frame, 500 is at the bottom, on image, 210 is at bottom
	}

	@Override
	public void initLane(int lane) {
		// TODO Auto-generated method stub

		if(lane == 2){
			x = Config.southBoundLane1.x;
			y = Config.southBoundLane1.y;
		}
		if(lane == 1){
			x = Config.southBoundLane2.x;
			y = Config.southBoundLane2.y;
		}
	}

	@Override
	public void updateLane(int lane) {
		// TODO Auto-generated method stub
		if(lane == 2){
			x = 250;
		}
		if(lane == 1){
			x = 210;
		}
	}
	
}
