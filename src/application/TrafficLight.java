package application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
//import java.util.*;
//import java.lang.Runnable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import application.TrafficController.TravelDirection;
import simulator.Config;

public class TrafficLight {
	public enum SignalColor { Green, Yellow, Red }
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private ReadWriteLock rwLock = new ReentrantReadWriteLock();
	
	private static int lightCounter = 0; // increment for each TrafficLight object created, 1st gets id of 1
	private int id = 0;
	public int getID() { return this.id; }
	
	private TravelDirection forTravelDirection = TravelDirection.North;
	public TravelDirection getTravelDirection() { return forTravelDirection; }
	
	private TravelDirection facingDirection = TravelDirection.North;
	public TravelDirection getFacingDirection() { return facingDirection; }
	
	private SignalColor color = SignalColor.Red;
	public SignalColor GetColor() { return this.color; }
	
	private Instant lastChanged = Instant.now();
	public Instant getLastChanged() { return lastChanged; }
	
	public TrafficLight(TravelDirection forTravelDirection) {
		this.id = ++lightCounter;
		this.color = SignalColor.Red;
		this.forTravelDirection = forTravelDirection;
		this.facingDirection = 
			  this.forTravelDirection == TravelDirection.North	? TravelDirection.South
			: this.forTravelDirection == TravelDirection.South	? TravelDirection.North
			: this.forTravelDirection == TravelDirection.East	? TravelDirection.West
			: TravelDirection.East;
		log("Light %04d created for travel direction %s, color %s", this.id, forTravelDirection.toString(), this.color.toString());
	}
	
	// change the light to green only if it's red
	public void TurnGreen() {
		if (this.color == SignalColor.Red) {
			try {
				rwLock.writeLock().lock();
				this.color = SignalColor.Green;
				logColorState();
				this.lastChanged = Instant.now();
			}
			catch (Exception ex) { ex.printStackTrace(); }
			finally {
				rwLock.writeLock().unlock();
			}
		}
	}
	
	// cycle the light from green to yellow, pause, then change to red
	public void TurnRed() {
		if (this.color == SignalColor.Green) {
			try {
				rwLock.writeLock().lock();
				this.color = SignalColor.Yellow;
				logColorState();
				this.lastChanged = Instant.now();
				TimeUnit.SECONDS.sleep((long)TrafficController.GetSecondsYellowLightDuration());
				this.color = SignalColor.Red;
				logColorState();
				this.lastChanged = Instant.now();
			}
			catch (Exception ex) { ex.printStackTrace(); }
			finally {
				rwLock.writeLock().unlock();
			}
		}
	}
	
	// return number of seconds since the light last changed color
	public long secondsSinceLastChange() {
		return ChronoUnit.SECONDS.between(lastChanged, Instant.now());
	}
	
	// log the current state of the traffic light  
	public void logColorState() {
		log("Light %04d: travel direction %s, is %s", this.id, this.forTravelDirection.toString(), this.color.toString());
	}
	
	private void log(String format, Object ... args) {
		if (Config.doTrafficControllerLogging) {
			System.out.println(String.format("%s %04d: %s %s", "TrafficLight", this.id, Instant.now().toString(), String.format(format, args)));
		}
	}
}
