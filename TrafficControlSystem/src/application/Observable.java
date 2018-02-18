package application;

public interface Observable {

	public void addObserver(Observer o);
	
	public void removeObserver(Observer o);
	
	public void notifyObserver(int numTracks, int numOncoming, int numOutgoing, int numUncertain);
	
}
