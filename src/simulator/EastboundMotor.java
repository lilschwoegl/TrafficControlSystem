package simulator;

import java.awt.Color;
import java.awt.Graphics;

import tracking.Track;

public class EastboundMotor extends MotorVehicle{

	//constructor
	public EastboundMotor(int lane, Track track){
		
		super(lane, Direction.EAST ,track);
		
		initLane(lane);
	}
	
	@Override
	public void tick() {
		
	}

	@Override
	public void initLane(int lane) {
		// TODO Auto-generated method stub

		if(lane == 1){
			//x = Config.eastBoundLane1.x;
			//y = Config.eastBoundLane1.y;
		}
		if(lane == 2){
			//x = Config.eastBoundLane2.x;
			//y = Config.eastBoundLane2.y;
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

	@Override
	public void updateTrackPosition() {
		// TODO Auto-generated method stub
		x = (track.getBestPositionCenter().x * SimConfig.roadStripRatio);
	}
	
	/*public void render(Graphics g){
		g.drawImage(loadImage.rightCarImage, (int)x, (int)y, 30, 45, null);
	}*/
	
	//methods
}
