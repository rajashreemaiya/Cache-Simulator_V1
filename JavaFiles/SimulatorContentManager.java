import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
	static Map<Integer, List> lookUpTable = Collections
			.synchronizedMap(new HashMap<Integer, List>());
	static Map<Integer, Integer> recircArray; 
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


		logFile.writeToFile(clientNum, "Searching content manager");
		logFile.writeToFile(clientNum, "Finding in neighbor cache...");
		for (Map.Entry<Integer, List> entry : lookUpTable.entrySet()) {
			List<Integer> values1 = entry.getValue();
			if (values1.contains(req_data) && clientNum != entry.getKey()) {
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
			int reqData, List<Integer> localCacheUpdated,
			SimulatorLogger logFile) {
		synchronized (lookUpTable) {
			lookUpTable.put(clientNum, localCacheUpdated);

		}
	}
}
