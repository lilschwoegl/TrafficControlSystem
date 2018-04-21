package application;

import java.util.Hashtable;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;

public class VideoInput extends VideoCapture{
	
	// feeds from:
	// http://chart.maryland.gov/trafficcameras/index.php#
	
	// book
	// https://books.google.com/books?id=dMEyDwAAQBAJ&pg=PA58&lpg=PA58&dq=opencv+format+mat+for+processing&source=bl&ots=qvXcPpXOGC&sig=5pFEk9cYilEpidUXzYFJN0ltfK4&hl=en&sa=X&ved=0ahUKEwilr6Sa_o_ZAhXNVt8KHb6uDRUQ6AEISjAF#v=onepage&q=opencv%20format%20mat%20for%20processing&f=false

	// getting to work on windows:
	// https://stackoverflow.com/questions/23199886/opencv-java-binds-videocapture-from-file-failing-silently
	// http://kronoskoders.logdown.com/posts/256664-installing-opencv-and-ffmpeg-on-windows
	// https://transportation.arlingtonva.us/live-traffic-cameras/
	/**
	 * URLs that will be referenced when a feedName below is selected
	 */
	private static String[] videoUrls = {
		"https://itsvideo.arlingtonva.us:8012/live/cam136.stream/playlist.m3u8",
		"https://itsvideo.arlingtonva.us:8011/live/cam74.stream/playlist.m3u8",
		"https://itsvideo.arlingtonva.us:8012/live/cam120.stream/playlist.m3u8",
		"https://itsvideo.arlingtonva.us:8011/live/cam66.stream/playlist.m3u8",
		"https://itsvideo.arlingtonva.us:8011/live/cam77.stream/playlist.m3u8",
		"C:\\Temp\\intersection_recorded_1.avi",
		"C:\\Temp\\intersection_recorded_2.avi",
		"C:\\Temp\\intersection_recorded_3.avi"
	};
	
	/**
	 * Names of the feeds that can be selected. These will be populated
	 * in the combobox on the mani form
	 */
	public static String[] feedNames = {
		"Intersection",
		"10E",
		"EADS ST AT 15TH",
		"COL PK AT COURTHOUSE RD",
		"COL PK AT HIGHLAND",
		"Recorded 1",
		"Recorded 2",
		"Recorded 3"
	};
	
	private static long counter = 0;
	
	private static Hashtable<String, String> videoFeeds;
	
	private String currentFeed = "";
	
	/**
	 * Constructor
	 */
	public VideoInput()
	{
		// make sure the video links are in the hash table
		if (videoFeeds == null)
		{
			videoFeeds = new Hashtable<String, String>();
			
			// initialize videoFeeds table if null, one time thing
			for (int i = 0; i < videoUrls.length; i++)
			{
				videoFeeds.put(feedNames[i],  videoUrls[i]);
			}
		}
	}
	
	/**
	 * Constructor
	 * @param feedName Name of the feed to read from
	 */
	public VideoInput(String feedName)
	{
		this();
		this.open(videoFeeds.get(currentFeed));
	}
	
	/**
	 * Selects a camera feed based on the feed name
	 * @param feedName Name of the feed to read from
	 */
	public void selectCameraFeed(String feedName)
	{
		currentFeed = feedName;
		this.open(videoFeeds.get(currentFeed));
	}
	
	/**
	 * Gets the current feed that is being read from
	 * @return Name of the feed that is being read from
	 */
	public String getCurrentFeed()
	{
		return currentFeed;
	}
	
	/**
	 * Grabs a frame from the current video feed
	 * @return The next read frame
	 * @throws Exception Exception when next frame cannot be read
	 */
	public Mat grabFrame() throws Exception
	{
		return this.grabFrame(new Size(500,500));
	}
	
	/**
	 * Grabs a frame from the current video feed
	 * @param size Size to resize frame to
	 * @return The next read frame with the given size
	 * @throws Exception Exception when the next frame cannot be read
	 */
	public Mat grabFrame(Size size) throws Exception
	{
		Mat frame = new Mat();
		
		//System.out.printf("Img Counter: %d\n", counter);
		
		try {
			// read the current frame
			this.read(frame);
			
			//System.out.printf("Reading image: %d\n", counter-1);
			
			counter++;
			
			return frame;
		} catch (Exception e) {
			// log the (full) error
			//System.err.print("Exception during the image elaboration...");
			
			e.printStackTrace();
			
			// bubble the error up
			throw e;
		}
	}
	
	
	public void refresh()
	{
		release();
		selectCameraFeed(currentFeed);
	}
	
}
