package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class RoadLinesCollection {

	Vector<Lane> lanes = new Vector<Lane>();
	double thresholdX = 200;
	double thresholdY = 2000;
	
	public void coorelateLine(Line line)
	{
		// if no lanes, add a new one
		if (lanes.size() == 0)
		{
			Lane l = new Lane();
			l.addLine(line);
			
			lanes.add(l);
		}
		else
		{
			// determine the lane to add the line to
			Point center, start, end;
			for (int i = 0; i < lanes.size(); i++)
			{
				center = lanes.get(i).getCenter();
				start = lanes.get(i).getStart();
				end = lanes.get(i).getEnd();
				
				double startDiff = Math.abs(start.x - line.getStartPoint().x);
				double endDiff = Math.abs(end.x - line.getEndPoint().x);
				double startEndDiff = Math.abs(start.x - line.getEndPoint().x);
				double endStartDiff = Math.abs(end.x - line.getStartPoint().x);
				
				if (startDiff < thresholdX ||
						endDiff < thresholdX ||
						startEndDiff < thresholdX ||
						endStartDiff < thresholdX)
				{
					lanes.get(i).addLine(line);
					return;
				}
				
			}
			
			// no good matches, add line as new lane
			Lane l = new Lane();
			l.addLine(line);
			
			// find where to insert lane
			for (int i = 0; i < lanes.size(); i++)
			{
				if (line.getCenter().x < lanes.get(i).getCenter().x)
				{
					lanes.add(i, l);
					return;
				}
			}
			
			lanes.add(lanes.size(), l);
		}
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
		if (lanes.size() < 2)
			return 0;
		
		for (int i = 1; i < lanes.size(); i++)
		{
			if (center.x > lanes.get(i-1).getCenter().x && center.x < lanes.get(i).getCenter().x)
				return i;
		}
		
		return 0;
	}
}
