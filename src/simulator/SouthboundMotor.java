package simulator;

import java.awt.Color;
import java.awt.Graphics;

import simulator.Constants.Direction;
import tracking.Track;

public class SouthboundMotor extends MotorVehicle{

	public SouthboundMotor(int lane, Track track){
		
		super(lane, Direction.SOUTH, track);
		
		initLane(lane);
	}
	
	@Override
	public void tick() {

	}

	@Override
	public void initLane(int lane) {
		// TODO Auto-generated method stub

		if(lane == 2){
			x = SimConfig.southBoundLane1.x;
			y = SimConfig.southBoundLane1.y;
		}
		if(lane == 1){
			x = SimConfig.southBoundLane2.x;
			y = SimConfig.southBoundLane2.y;
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

	@Override
	public void updateTrackPosition() {
		// TODO Auto-generated method stub
		
		y = (track.getBestPositionCenter().y * SimConfig.roadStripRatio);
	}
	
	/*public void render(Graphics g){
		g.drawImage(loadImage.downCarImage, (int)x, (int)y, 30, 45, null);
	}*/
	
}
