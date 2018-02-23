package simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class traffic {

	public static ArrayList<String> nTraffic, sTraffic, eTraffic, wTraffic;
	public static ArrayList<String> nLane1;
	public static ArrayList<String> nLane2;
	public static ArrayList<String> sLane1;
	public static ArrayList<String> sLane2;
	public static ArrayList<String> eLane1;
	public static ArrayList<String> eLane2;
	public static ArrayList<String> wLane1;
	public static ArrayList<String> wLane2;
	
	public static void init(){
		nTraffic = loadTraffic("/northtraffic.txt");
		sTraffic = loadTraffic("/southtraffic.txt");
		eTraffic = loadTraffic("/easttraffic.txt");
		wTraffic = loadTraffic("/westtraffic.txt");
		nLane1 = getLaneData(nTraffic,0);
		nLane2 = getLaneData(nTraffic,1);
		sLane1 = getLaneData(sTraffic,0);
		sLane2 = getLaneData(sTraffic,1);
		eLane1 = getLaneData(eTraffic,0);
		eLane2 = getLaneData(eTraffic,1);
		wLane1 = getLaneData(wTraffic,0);
		wLane2 = getLaneData(wTraffic,1);
			
	}
	

	public static ArrayList<String> loadTraffic(String path){
		ArrayList<String> file = loadFile(path);
		return file;
	}
	
	public static ArrayList<String> loadFile(String path){
		//StringBuilder sr = new StringBuilder();
		ArrayList<String> sr = new ArrayList<String>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line;
			while((line = reader.readLine()) != null){	
				sr.add(line +"\n");
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sr;
	}
	
	public static ArrayList<String> getLaneData(ArrayList<String> traffic, int spaceIndex) {
		ArrayList<String> laneData = new ArrayList<String>();
		String line;
		int j=0;
		while (laneData.size() > j){
			line = laneData.get(j);
			String[] space = line.split("\\s+");
			laneData.add(space[spaceIndex]);
		}
		
		return laneData;
		
	}



}

