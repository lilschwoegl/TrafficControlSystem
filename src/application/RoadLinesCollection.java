package application;

import java.util.Vector;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class RoadLinesCollection {

	private Vector<Lane> lanes = new Vector<Lane>();
	
	public void addLane(Point p1, Point p2)
	{
		lanes.add(new Lane(p1, p2));
	}
	
	public void removeLastLane()
	{
		if (lanes.size() > 0)
			lanes.removeElement(lanes.lastElement());
	}
	
	public void drawLanes(Mat frame)
	{
		for (Lane l : lanes)
		{
			l.drawLane(frame);
		}
	}
	
	public int isInLane(Point center)
	{
		
		if (lanes.size() == 1)
		{
			if (center.x < lanes.get(0).getCenter().x)
				return 0;
			else
				return 1;
		}
		else
		{
		
			for (int i = 1; i < lanes.size(); i++)
			{
				if (center.x > lanes.get(i-1).getCenter().x && center.x < lanes.get(i).getCenter().x)
					return i;
			}
		}
		
		return 0;
	}
}
