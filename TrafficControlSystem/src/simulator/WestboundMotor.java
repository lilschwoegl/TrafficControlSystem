package simulator;

import java.awt.Color;
import java.awt.Graphics;

import tracking.Track;

public class WestboundMotor extends MotorVehicle{
	
	//constructor
	public WestboundMotor(int lane, Track track){
		
		super(lane, Direction.WEST, track);
	
		initLane(lane);
	}
	
	@Override
	public void tick() {
		
	}

	@Override
	public void initLane(int lane) {
		// TODO Auto-generated method stub

		if(lane == 1){
			x = Config.westBoundLane1.x;
			y = Config.westBoundLane1.y;
		}
		if(lane == 2){
			x = Config.westBoundLane2.x;
			y = Config.westBoundLane2.y;
		}
	}

	@Override
	public void updateLane(int lane) {
		// TODO Auto-generated method stub
		if(lane == 1){
			y = 245;
		}
		if(lane == 2){
			y = 210;
		}
	}

	@Override
	public void updateTrackPosition() {
		// TODO Auto-generated method stub
		x = Config.simDisplayWidth - (track.getBestPositionCenter().x * Config.roadStripRatio);
	}
	
	/*public void render(Graphics g){
		g.drawImage(loadImage.leftCarImage, (int)x, (int)y, 30, 45, null);
	}*/
	
	//methods
}


