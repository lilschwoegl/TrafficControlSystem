package application;

import java.util.ArrayList;

public class Config {
	// TrafficController settings
	public static boolean useSqlDatabase = true;						// true: use SQLite database; false: do not use database
	public static double pixelsCameraRange = 360;						// max range vehicles will be tracked for its facing cameras
	public static boolean doTrafficControllerLogging = false;			// if true, console text logging is enabled
	public static boolean doTrafficControllerTrackEventLogging = false;	// if true, observer events are logged to console
	public static boolean doMetricsLogging = true;						// set to true to record runtime metrics to persistent storage
	public static int secondsMaxGreenLightDuration = 30;				// max duration a light may stay green if any are waiting on a green
	public static long periodForFixedTimerConfiguration = 10;			// green light duration, normally this would be 30s - 120s in real life
	public static long maxSecondsVehicleAgeToTrack = 5;					// if vehicles collection contains any objects that haven't been updated in this amount of time, they're dropped
	public static long maxSecondsToWaitForEmergencyVehicles = 15;		// let emergency vehicles take control of intersection a max of this many seconds (NOTE: typically, this will be a longer wait time)
	
	// TrafficLight settings
	public static boolean doTrafficLightLogging = false;					// if true, runtime metrics are logged to console
	public static int secondsYellowLightDuration = 3;					// duration (seconds) for light to stay yellow when changing to red
	public static int minSecondsOwnershipUntilChangeAllowed = 15;		// light cannot be taken from a direction of travel until ownership has exceeded this duration
	
	// database schema
	public static String databaseName = "trafficController.db";
	public static String[] databaseSchema = {
		"create table if not exists Events(timestamp TEXT NOT NULL, name TEXT NOT NULL, value TEXT NOT NULL)",
		"create table if not exists Metrics(timestamp TEXT NOT NULL, name TEXT NOT NULL, description TEXT NULL, valueType TEXT NOT NULL, value BLOB NOT NULL)"
	};
}
