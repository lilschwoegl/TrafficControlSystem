package application;
import org.opencv.core.Point;

public class Line {
	double x1;
	double y1;
	double x2;
	double y2;
	
	/**
	 * Constructor
	 * @param x1 Start X
	 * @param y1 Start Y
	 * @param x2 End X
	 * @param y2 End Y
	 */
	public Line(double x1, double y1, double x2, double y2)
	{
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}
	
	/**
	 * Constructor
	 * @param coords Start and End X,Y coords in format [x1,y1,x2,y2]
	 * @throws Exception
	 */
	public Line(double[] coords) throws Exception
	{
		this(coords[0], coords[1], coords[2], coords[3]);
	}
	
	/**
	 * Constructor
	 */
	public Line()
	{
		this(0,0,0,0);
	}
	
	/**
	 * Sets start X
	 * @param x X position
	 */
	public void setStartX(double x)
	{
		this.x1 = x;
	}
	
	/**
	 * Sets start y
	 * @param y Y position
	 */
	public void setStartY(double y)
	{
		this.y1 = y;
	}
	
	/**
	 * Sets end x
	 * @param x X position
	 */
	public void setEndX(double x)
	{
		this.x2 = x;
	}
	
	/**
	 * Sets end y
	 * @param y Y position
	 */
	public void setEndY(double y)
	{
		this.y2 = y;
	}
	
	/**
	 * Gets slope of the line
	 * @return Slope of the line
	 */
	public double getSlope()
	{
		return ((y2 - y1) / ((x2 - x1) + 0.000001));
	}
	
	/**
	 * Gets line start point
	 * @return Start point
	 */
	public Point getStartPoint()
	{
		return new Point(x1, y1);
	}
	
	/**
	 * Gets line end point
	 * @return End point
	 */
	public Point getEndPoint()
	{
		return new Point(x2, y2);
	}
	
	/**
	 * Gets center point of line
	 * @return Line center
	 */
	public Point getCenter()
	{
		return new Point((x1 + x2) / 2, (y1 + y2) / 2);		
	}
	
	/**
	 * Determines if a point is near the line given a distance threshold
	 * @param p Point to compare
	 * @param distThresh Distance threshold in pixels
	 * @return True if point is near the line
	 */
	public boolean pointNearLine(Point p, double distThresh)
	{
		return pointNearLine(p, distThresh, distThresh);
	}
	
	/**
	 * Determines if a point is near the line given a distance threshold
	 * @param p Point to compare
	 * @param xDistThresh Distance threshold of X in pixels
	 * @param yDistThresh Distance threshold of Y in pixels
	 * @return True if point is near the line
	 */
	public boolean pointNearLine(Point p, double xDistThresh, double yDistThresh)
	{
		return (p.x >= getCenter().x - xDistThresh) && (p.x <= getCenter().x + yDistThresh);
	}
}
