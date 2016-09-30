import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 
 */

/**
 * @author rmaiya
 *
 */
public class SimulatorServer {
	static int serverCacheSize; //TODO: Will this be a fixed number?
	/* Make list synchronized object, ensures thread safety */
	static List<Integer> serverMemory = Collections.synchronizedList(new ArrayList<Integer>(serverCacheSize));
	
	public static void setServerMemory() {
		Random r = new Random();
		for(int i=0;i<serverCacheSize;i++) {
			int value = r.nextInt(10)+1;
			serverMemory.add(value);
		}
		
	}
	public static int searchMemory(int clientNum, int reqData, SimulatorLogger logFile, SimulatorTickCounts mytickCount) {
		if(serverMemory.contains(reqData)) {
			System.out.println("Data found on server");
			mytickCount.setTickCount(SimulatorConstants.SEARCH);
			logFile.writeToFile(clientNum, "Data found on server");
			return reqData;
		}
		
		else {
			System.out.println("Not in server memory");
			logFile.writeToFile(clientNum, "Not in server memory");
			logFile.writeToFile(clientNum, "Getting value from disk");
			System.out.println("Getting value from disk");
			mytickCount.setTickCount(SimulatorConstants.TODISK);
			mytickCount.setTickCount(SimulatorConstants.FROMDISK);
			int newValue = getFromDisk(reqData,mytickCount);
			writeToServer(newValue);
			return newValue;
		}
		
	}

	private synchronized static void writeToServer(int newValue) {
		synchronized(serverMemory) {
			serverMemory.set(0, newValue);
		}
	}

	private static int getFromDisk(int reqData, SimulatorTickCounts mytickCount) {
		return SimulatorDisk.getDataFromMemory(reqData);	
	}


}
