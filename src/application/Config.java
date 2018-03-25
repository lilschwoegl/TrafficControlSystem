package application;

public class Config {
	// TrafficController settings
	public static double pixelsCameraRange = 360;						// max range vehicles will be tracked for its facing cameras
	public static boolean doTrafficControllerLsogging = true;			// if true, runtime metrics are logged to console
	public static boolean doTrafficControllerTrackEventLogging = true;	// if true, observer events are logged to console
	public static boolean doMetricsLogging = true;						// set to true to record runtime metrics to persistent storage
	public static int secondsMaxGreenLightDuration = 30;				// max duration a light may stay green if any are waiting on a green
	public static long periodForFixedTimerConfiguration = 10; // green light duration, normally this would be 30s - 120s in real life
	public static int secondsYellowLightDuration = 3;					// duration (seconds) for light to stay yellow when changing to red
	public static long maxSecondsVehicleAgeToTrack = 5L; // N seconds: if vehicles collection contains any objects not updated any sooner, they're dropped
	
	// TrafficLight settings
	public static boolean doTrafficLightLogging = true;					// if true, runtime metrics are logged to console
}
