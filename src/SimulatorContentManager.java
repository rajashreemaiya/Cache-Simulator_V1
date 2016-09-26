import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 
 */

/**
 * @author rmaiya
 *
 */
public class SimulatorContentManager {

	/* Make the map synchronized object, ensures thread safety */
	static Map<Integer, ArrayList> lookUpTable = Collections.synchronizedMap(new HashMap<Integer, ArrayList>());
	static final int sysCacheSize = 7; //TODO: Will this be a fixed number?
	/* Make list synchronized object, ensures thread safety */
	static List<Integer> systemCache = Collections.synchronizedList(new ArrayList<Integer>(sysCacheSize));

	/**
	 * 
	 * @param clientNum unique number for each client
	 * @param req_data what we looking for
	 * @param logFile for writing to the log file
	 * @return true if found in neighbor's cache or system cache or false
	 */
	public static boolean searchContentManager(int clientNum, int req_data, SimulatorLogger logFile) {

		/* TODO: This will be a loop for n clients - look in all clients caches for value, 
		 * so you do not have to go to system cache*/
		logFile.writeToFile(clientNum,"Searching content manager");
		boolean found = false;
		logFile.writeToFile(clientNum, "Finding in neighbor cache...");
		for(Map.Entry<Integer, ArrayList> entry: lookUpTable.entrySet()) {
			ArrayList<Integer> values1 = entry.getValue();
			if(values1.contains(req_data) && clientNum != entry.getKey()) {
				System.out.println("Finding in neighbor cache...");	
				System.out.println("Found in neighbor: "+ entry.getKey());
				logFile.writeToFile(clientNum,"Found in neighbor: "+ entry.getKey());
				found = true;
			}
		}

		if(found == false) {
			System.out.println("Searching system cache...");
			logFile.writeToFile(clientNum,"Searching system cache...");
			if(systemCache.contains(req_data))
				found = true;
		}
		return found;
	}

	/* When a client's local cache is updated after fetch from server, 
	 * update me and also put that in the system cache
	 * so I have latest copies.
	 * */
	public synchronized static void updateContentManager(int clientNum,
			int reqData, ArrayList<Integer> localCacheUpdated, SimulatorLogger logFile) {
		synchronized(lookUpTable) {
			lookUpTable.put(clientNum, localCacheUpdated);	

			synchronized(systemCache) {
				System.out.println("Updating system cache");
				logFile.writeToFile(clientNum, "Updating system cache");
				if(systemCache.size() < sysCacheSize) {
					if(!(systemCache.contains(reqData))) {
						System.out.println("Adding to system cache...");
						logFile.writeToFile(clientNum,"Adding to system cache...");
						systemCache.add(reqData);
					}
				}
				else {
					// TODO: Random replacement algorithm
					Random random = new Random();
					int indexOfwhatToReplace = random.nextInt(systemCache.size()-1)+1;
					System.out.println("Replacing in cache....");
					logFile.writeToFile(clientNum,"Replacing in cache....");
					systemCache.set(indexOfwhatToReplace,reqData);
				}
			}
		}
	}
}
