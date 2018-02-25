package observer;

public interface TrafficObservable {

	public void addObserver(TrafficObserver o);
	
	public void removeObserver(TrafficObserver o);
	
	public void notifyObserver(int trackId, double distToIntersection);
	
}
