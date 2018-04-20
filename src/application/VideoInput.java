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
		"http://170.93.143.139:1935/rtplive/cf013c5801f700d700437a45351f0214/playlist.m3u8",
		"http://170.93.143.139:1935/rtplive/d6009a3500e50039004606363d235daa/playlist.m3u8",
		"http://170.93.143.139:1935/rtplive/dbff12ba0057008d004be2369e235daa/playlist.m3u8",
		"http://170.93.143.139:1935/rtplive/6001ce5800f700d700437a45351f0214/playlist.m3u8",
		"http://pa511wmedia101.ilchost.com/live/CAM-11-226.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8xMi8yMDE4IDk6MzA6NTAgUE0maGFzaF92YWx1ZT1aaEU5bXNTMnF5TmZaVFVzTDN2Y1dnPT0mdmFsaWRtaW51dGVzPTIwJmlkPTE3My42OS4xNzQuMTA3",
		"video/cars.mp4",
		"video/dog_people.mp4",
		"https://itsvideo.arlingtonva.us:8012/live/cam136.stream/playlist.m3u8",
		"https://itsvideo.arlingtonva.us:8011/live/cam74.stream/playlist.m3u8"
	};
	
	/**
	 * Names of the feeds that can be selected. These will be populated
	 * in the combobox on the mani form
	 */
	public static String[] feedNames = {
		"I-695 AT PULASKI HWY",
		"I-695 E of I-95",
		"I-695 AT PUTTY HILL AVE",
		"I-695 AT HARFORD RD",
		"Stump Road",
		"RAW VIDEO",
		"Dog and People",
		"Intersection",
		"Crashes"
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
	
}
