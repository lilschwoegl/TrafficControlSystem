package application;
import org.opencv.core.Point;

public class Line {
	double x1;
	double y1;
	double x2;
	double y2;
	
	public Line(double x1, double y1, double x2, double y2)
	{
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}
	
	public Line(double[] coords) throws Exception
	{
		this(coords[0], coords[1], coords[2], coords[3]);
		
	}
	
	public Line()
	{
		this(0,0,0,0);
	}
	
	public void setStartX(double x)
	{
		this.x1 = x;
	}
	
	public void setStartY(double y)
	{
		this.y1 = y;
	}
	
	public void setEndX(double x)
	{
		this.x2 = x;
	}
	
	public void setEndY(double y)
	{
		this.y2 = y;
	}
	
	public double getSlope()
	{
		return ((y2 - y1) / ((x2 - x1) + 0.000001));
	}
	
	public Point getStartPoint()
	{
		return new Point(x1, y1);
	}
	
	public Point getEndPoint()
	{
		return new Point(x2, y2);
	}
	
	public Point getCenter()
	{
		return new Point((x1 + x2) / 2, (y1 + y2) / 2);		
	}
	
	private double getB(Point p)
	{
		return (p.y / (getSlope() * p.x));
	}
	
	public boolean pointOnLine(Point p)
	{
		double b = getB(getCenter());
		
		return (p.y == ((getSlope() * p.x) + b));
	}
	
	public boolean pointNearLine(Point p, double distThresh)
	{
//		double b = getB(getCenter());
//		double y = (getSlope() * p.x) + b;
//		
//		double calcY = getCenter().y;
		
		return (p.x >= getCenter().x - distThresh) && (p.x <= getCenter().x + distThresh);
	}
}
