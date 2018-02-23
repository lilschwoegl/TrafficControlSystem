package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Lane {

	//Vector<Line> lines = new Vector<Line>();
	Line line = new Line();
	Point centerPoint;
	
	double xTake = 0.005;
	
	public void addLine(Line line)
	{
		if (this.line.getStartPoint().x == 0 || this.line.getEndPoint().x == 0)
		{
			// first line to be added
//			lines.add(line);
//			calculateCenter();
			
			this.line.setStartY(100000);
			this.line.setEndY(-100000);
		}
//		else
//		{
//			// find the right spot for this line, this will be where the center
//			// point is less than the center point of the other lines in the y axis
//			for (int i = 0; i < lines.size(); i++)
//			{
//				if (line.getEndPoint().y > lines.get(i).getEndPoint())
//				
//				
//				if (line.getEndPoint().y > lines.get(i).getEndPoint().y &&
//						line.getStartPoint().y < lines.get(i).getStartPoint().y)
//				{
//					lines.remove(i);
//					lines.add(i, line);
//					calculateCenter();
//					return;
//				}
//			}
//			
//			lines.add(lines.size(), line);
//			calculateCenter();
//		}
		
		if (line.getStartPoint().y < this.line.getStartPoint().y)
			this.line.setStartY(line.getStartPoint().y);
		
		if (line.getEndPoint().y > this.line.getEndPoint().y)
			this.line.setEndY(line.getEndPoint().y);
		
		if (this.line.getStartPoint().x == 0 || this.line.getEndPoint().x == 0)
		{
			this.line.setStartX(line.getStartPoint().x);
			this.line.setEndX(line.getEndPoint().x);
		}
		else
		{	
			double xStartDiff = line.getStartPoint().x - this.line.getStartPoint().x;
			double xEndDiff = line.getEndPoint().x - this.line.getEndPoint().x;
			
			//System.out.printf("X Start Diff: %f, X End Diff: %f, Takeaway Start: %f, Takeaway End: %f\n", 
			//		xStartDiff, xEndDiff, xStartDiff * xTake, xEndDiff * xTake);
			
			this.line.setStartX(this.line.getStartPoint().x + (xStartDiff * xTake));
			this.line.setEndX(this.line.getEndPoint().x + (xEndDiff * xTake));
		}
		
	}
	
	public Point getCenter()
	{
		return line.getCenter();
	}
	
	public Point getStart()
	{
		return line.getStartPoint();
	}
	
	public Point getEnd()
	{
		return line.getEndPoint();
	}
	
	private void calculateCenter()
	{
//		double avgX = 0;
//		double avgY = 0;
//		double size = lines.size();
//		
//		for (Line l : lines)
//		{
//			avgX += l.getCenter().x;
//			avgY += l.getCenter().y;
//		}
//		
//		avgX /= size;
//		avgY /= size;
//		
//		centerPoint = new Point(avgX, avgY);
		
		centerPoint = this.line.getCenter();
	}
	
	public void drawLane(Mat frame)
	{
//		if (lines.size() == 0)
//		{
//			Imgproc.line(frame, 
//					lines.get(0).getStartPoint(), 
//					lines.get(0).getEndPoint(), 
//					new Scalar(0,0,255),
//					15);
//		}
//		else
//		{
//			//for (int i = 1; i < lines.size(); i++)
//			//{
//				Imgproc.line(frame, 
//						lines.get(0).getCenter(), 
//						lines.get(lines.size()-1).getCenter(), 
//						new Scalar(0,255,0),
//						15);
//			//}
//		}
		
		Imgproc.line(frame, 
				line.getStartPoint(), 
				line.getEndPoint(), 
				new Scalar(0,255,0),
				15);
		
	}
	
}
