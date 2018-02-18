package application;

import java.util.ArrayList;
import java.util.Vector;

import tracking.Track;
import tracking.Track.DIRECTION;

public class TrafficUpdateObserver implements Observable {

	private ArrayList<Observer> observers;
	private Vector<Track> tracks;
	
	// Singleton
	private static TrafficUpdateObserver instance;
	
	private TrafficUpdateObserver()
	{
		observers = new ArrayList<Observer>();
		tracks = new Vector<Track>();
	}
	
	public static TrafficUpdateObserver getInstance()
	{
		if (instance == null)
			instance = new TrafficUpdateObserver();
		
		return instance;
	}
	
	@Override
	public void addObserver(Observer o) {
		observers.add(o);
	}

	@Override
	public void removeObserver(Observer o) {
		observers.remove(o);
	}
	
	@Override
	public void notifyObserver(int numTracks, int numOncoming, int numOutgoing, int numUncertain) {
		for (Observer observer : observers)
		{
			observer.update(numTracks, numOncoming, numOutgoing, numUncertain);
		}
	}
	
	public void updateTracks(Vector<Track> updatedTracks)
	{
		if (updatedTracks.size() != tracks.size() ||
				tracksChangedDirection(updatedTracks))
		{
			tracks.clear();
			tracks.addAll(0, updatedTracks);
			
			int numOncoming = 0;
			int numOutgoing = 0;
			int numUncertain = 0;
			
			// count oncoming and outgoing in current list
			for (Track t : tracks)
			{
				if (t.direction == DIRECTION.ONCOMING)
					numOncoming++;
				else if (t.direction == DIRECTION.OUTGOING)
					numOutgoing++;
				else
					numUncertain++;
			}
			
			notifyObserver(updatedTracks.size(), numOncoming, numOutgoing, numUncertain);
		}		
	}
	
	private boolean tracksChangedDirection(Vector<Track> updatedTracks)
	{
		int numOncoming = 0;
		int numOutgoing = 0;
		int numUncertain = 0;
		
		// count oncoming and outgoing in current list
		for (Track t : tracks)
		{
			if (t.direction == DIRECTION.ONCOMING)
				numOncoming++;
			else if (t.direction == DIRECTION.OUTGOING)
				numOutgoing++;
		}
		
		// subtract from updatedTracks
		for (Track t : updatedTracks)
		{
			if (t.direction == DIRECTION.ONCOMING)
				numOncoming++;
			else if (t.direction == DIRECTION.OUTGOING)
				numOutgoing++;
			else
				numUncertain++;
		}
		
		return (numOncoming != 0 || numOutgoing != 0);
	}
	
}
