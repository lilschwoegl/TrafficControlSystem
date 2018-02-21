package application;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
//import java.util.*;
//import java.lang.Runnable;

import application.TrafficController.TravelDirection;

public class TrafficLight {
	public enum SignalColor { Green, Yellow, Red }
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	public TravelDirection forDirection = TravelDirection.North;
	public SignalColor color = SignalColor.Red;
	
	public TrafficLight(TravelDirection forDirection) {
		this.forDirection = forDirection;
	}
	
	public void TurnRed() {
		ReentrantLock lock = new ReentrantLock();
		if (this.color == SignalColor.Green) {
			lock.lock();
			this.color = SignalColor.Yellow;
			lock.unlock();
			executor.submit(() -> {
				try {
					TimeUnit.SECONDS.sleep(1000 * (long)TrafficController.GetSecondsYellowLightDuration());
				}
				catch (InterruptedException e) { /*ignore*/ }
			    finally {
					lock.lock();
					this.color = SignalColor.Red;
					lock.unlock();
			    }
			});
		}
	}
}
