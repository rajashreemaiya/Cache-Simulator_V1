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
	static final int serverCacheSize = 7; //TODO: Will this be a fixed number?
	/* Make list synchronized object, ensures thread safety */
	static List<Integer> serverMemory = Collections.synchronizedList(new ArrayList<Integer>(serverCacheSize));
	
	public static void setServerMemory() {
		Random r = new Random();
		for(int i=0;i<serverCacheSize;i++) {
			int value = r.nextInt(10)+1;
			serverMemory.add(value);
		}
		
	}
	public static int searchMemory(int clientNum, int reqData, SimulatorLogger logFile) {
		if(serverMemory.contains(reqData)) {
			System.out.println("Data found on server");
			logFile.writeToFile(clientNum, "Data found on server");
			return reqData;
		}
		
		else {
			System.out.println("Not in server memory");
			logFile.writeToFile(clientNum, "Not in server memory");
			System.out.println("Getting value from disk");
			logFile.writeToFile(clientNum, "Not in server memory");
			int newValue = getFromDisk(reqData);
			writeToServer(newValue);
			return newValue;
		}
		
	}

	private synchronized static void writeToServer(int newValue) {
		synchronized(serverMemory) {
			serverMemory.set(0, newValue);
		}
	}

	private static int getFromDisk(int reqData) {
		return SimulatorDisk.getDataFromMemory(reqData);	
	}


}
