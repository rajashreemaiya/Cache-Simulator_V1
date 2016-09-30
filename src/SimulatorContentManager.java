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
	static Map<Integer, ArrayList> lookUpTable = Collections
			.synchronizedMap(new HashMap<Integer, ArrayList>());
	static SimulatorClient[] allClients;

	/**
	 * 
	 * @param clientNum
	 *            unique number for each client
	 * @param req_data
	 *            what we looking for
	 * @param logFile
	 *            for writing to the log file
	 * @return which client has the data if found in neighbor's cache else -1
	 */
	public synchronized static int searchNeighborCache(int clientNum,
			int req_data, SimulatorLogger logFile) {

		/*
		 * TODO: This will be a loop for n clients - look in all clients caches
		 * for value, so you do not have to go to system cache
		 */
		logFile.writeToFile(clientNum, "Searching content manager");
		logFile.writeToFile(clientNum, "Finding in neighbor cache...");
		for (Map.Entry<Integer, ArrayList> entry : lookUpTable.entrySet()) {
			ArrayList<Integer> values1 = entry.getValue();
			if (values1.contains(req_data) && clientNum != entry.getKey()) {
				System.out.println("Finding in neighbor cache...");
				System.out.println("Found in neighbor: " + entry.getKey());
				return entry.getKey();
			}
		}
		return -1;
	}

	/*
	 * When a client's local cache is updated after fetch from server, update me
	 * and also put that in the system cache so I have latest copies.
	 */
	public synchronized static void updateContentManager(int clientNum,
			int reqData, ArrayList<Integer> localCacheUpdated,
			SimulatorLogger logFile) {
		synchronized (lookUpTable) {
			lookUpTable.put(clientNum, localCacheUpdated);

		}
	}
}
