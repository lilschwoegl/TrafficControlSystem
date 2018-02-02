package application;

import java.util.Hashtable;

import org.opencv.videoio.VideoCapture;

public class VideoInput extends VideoCapture{
	
	// feeds from:
	// http://chart.maryland.gov/trafficcameras/index.php#

	private static String[] videoUrls = {
		"http://170.93.143.139:1935/rtplive/cf013c5801f700d700437a45351f0214/playlist.m3u8",
		"http://170.93.143.139:1935/rtplive/d6009a3500e50039004606363d235daa/playlist.m3u8",
		"http://170.93.143.139:1935/rtplive/dbff12ba0057008d004be2369e235daa/playlist.m3u8",
		"http://170.93.143.139:1935/rtplive/6001ce5800f700d700437a45351f0214/playlist.m3u8"
	};
	
	public static String[] feedNames = {
		"I-695 AT PULASKI HWY",
		"I-695 E of I-95",
		"I-695 AT PUTTY HILL AVE",
		"I-695 AT HARFORD RD"
	};
	
	private static Hashtable<String, String> videoFeeds;
	
	private String currentFeed = "";
	
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
	
	public VideoInput(String feedName)
	{
		this();
		this.open(videoFeeds.get(currentFeed));
	}
	
	public void selectCameraFeed(String feedName)
	{
		currentFeed = feedName;
		this.open(videoFeeds.get(currentFeed));
	}
	
	public String getCurrentFeed()
	{
		return currentFeed;
	}
	
}
