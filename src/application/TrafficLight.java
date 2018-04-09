package application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
//import java.util.*;
//import java.lang.Runnable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import application.Direction;
import application.Color;

import observer.SimulatorObserver;
import observer.TrackUpdateObservable;
import observer.TrafficLightObservable;
import observer.TrafficLightObserver;
import observer.TrafficObserver;
import observer.TrafficUpdateObservable;

import simulator.MotorVehicle;

public class TrafficLight implements TrafficLightObservable {
	//public enum SignalColor { Green, Yellow, Red }
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private ReadWriteLock rwLock = new ReentrantReadWriteLock();
	
	private static int lightCounter = 0; // increment for each TrafficLight object created, 1st gets id of 1
	private int id = 0;
	public int getID() { return this.id; }
	
	private Direction forTravelDirection = Direction.North;
	public Direction getTravelDirection() { return forTravelDirection; }
	
	private Direction facingDirection = Direction.North;
	public Direction getFacingDirection() { return facingDirection; }
	
	private Color color = Color.Red;
	public Color GetColor() { return this.color; }
	
	private Instant lastChanged = Instant.now();
	public Instant getLastChanged() { return lastChanged; }
	
	private ArrayList<TrafficLightObserver> observers = new ArrayList<TrafficLightObserver>();
	
	public TrafficLight(Direction forTravelDirection) {
		this.id = ++lightCounter;
		this.color = Color.Red;
		this.forTravelDirection = forTravelDirection;
		this.facingDirection = 
			  this.forTravelDirection == Direction.North	? Direction.South
			: this.forTravelDirection == Direction.South	? Direction.North
			: this.forTravelDirection == Direction.East	? Direction.West
			: Direction.East;
		
		//create simulator light
		log("Light %04d created for travel direction %s, color %s", this.id, forTravelDirection.toString(), this.color.toString());
	}
	
	/* observeable methods */
	public void addObserver(TrafficLightObserver o) {
		observers.add(o);
		log("Light %04d added observer %s", this.id, o.toString());
	}
	
	public void removeObserver(TrafficLightObserver o) {
		observers.remove(o);
		log("Light %04d removed observer %s", this.id, o.toString());
	}
	
	public void notifyObservers() {
		for (TrafficLightObserver observer : observers) {
			try {
				observer.update(this);
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}
	}
	/* end of observable methods */
	
	// change the light to green only if it's red
	public void TurnGreen() {
		if (this.color == Color.Red) {
			try {
				rwLock.writeLock().lock();
				this.color = Color.Green;
				logColorState();
				this.lastChanged = Instant.now();
			}
			catch (Exception ex) { ex.printStackTrace(); }
			finally {
				rwLock.writeLock().unlock();
			}
			notifyObservers();
		}
	}
	
	// cycle the light from green to yellow, pause, then change to red
	/*TODO (DONE): downgrade writelock to readlock to allow clients to query yellow status. Because you can't upgrade a lock from read to write, would
	 * need to release read lock grab new write lock, change to red, then unlock.*/
	public void TurnRed() {
		if (this.color == Color.Green) {
			try {
				// grab writelock for light change
				rwLock.writeLock().lock();
				this.color = Color.Yellow;
				logColorState();
				this.lastChanged = Instant.now();
				// downgrade to readlock, lets clients view signal color change
				rwLock.readLock().lock();
				rwLock.writeLock().unlock();
				notifyObservers();
				try {
					TimeUnit.SECONDS.sleep((long)Config.secondsYellowLightDuration);
				}
				catch (Exception ex2) { ex2.printStackTrace(); }
				// upgrade to writelock for light change
				rwLock.readLock().unlock();
				rwLock.writeLock().lock();
				this.color = Color.Red;
				logColorState();
				this.lastChanged = Instant.now();
			}
			catch (Exception ex) { ex.printStackTrace(); }
			finally {
				rwLock.writeLock().unlock();
			}
			notifyObservers();
		}
	}
	
	// return true if this light's color has remained unchanged (i.e. owned) for its min duration
	public boolean IsMinOwnershipDurationElapsed() {
		return secondsSinceLastChange() >= Config.minSecondsOwnershipUntilChangeAllowed;
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
		if (Config.doTrafficLightLogging) {
			System.out.println(String.format("%s %04d: %s %s", "TrafficLight", this.id, Instant.now().toString(), String.format(format, args)));
		}
	}
}
