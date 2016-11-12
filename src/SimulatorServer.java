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
	static int serverCacheSize;
	static ArrayList<Integer> indexArray = new ArrayList<Integer>();
	static int accessCounter = 0;

	/* Make list synchronized object, ensures thread safety */
	static List<Integer> serverMemory = Collections
			.synchronizedList(new ArrayList<Integer>(serverCacheSize));

	public static void setServerMemory() {
		Random r = new Random();
		for (int i = 0; i < serverCacheSize; i++) {
			int value = r.nextInt(20000) + 1;
			serverMemory.add(value);
			indexArray.add(0);
		}
	}

	public synchronized static int searchMemory(int clientNum, int reqData,
			SimulatorLogger logFile, SimulatorTickCounts mytickCount,
			String strategy) {
		if (serverMemory.contains(reqData)) {
			SimulatorConstants.FOUNDONSERVER++;
			mytickCount.setTickCount(SimulatorConstants.SEARCH);
			logFile.writeToFile(clientNum, "Data found on server");
			accessCounter = accessCounter + 1;
			int index = serverMemory.indexOf(reqData);
			indexArray.set(index, accessCounter);
			return reqData;
		}

		else {
			SimulatorConstants.DISKACCESSCOUNT++;
			logFile.writeToFile(clientNum, "Not in server memory");
			logFile.writeToFile(clientNum, "Getting value from disk");
			mytickCount.setTickCount(SimulatorConstants.TODISK);
			mytickCount.setTickCount(SimulatorConstants.FROMDISK);
			int newValue = getFromDisk(reqData, mytickCount);
			writeToServer(newValue, strategy);
			return newValue;
		}

	}

	private synchronized static void writeToServer(int newValue,
			String whatStrategy) {
		if (whatStrategy.equals("FIFO")) {
			writeToServerFIFO(newValue);
		}

		if (whatStrategy.equals("LRU")) {
			writeToServerLRU(newValue, accessCounter, indexArray);
		}

		if (whatStrategy.equals("Random")) {
			writeToServerRandom(newValue);
		}

	}

	private synchronized static void writeToServerRandom(int newValue) {
		Random r = new Random();
		int whatIndexToReplace = r.nextInt(serverCacheSize - 1) + 1;
		synchronized (serverMemory) {
			serverMemory.set(whatIndexToReplace, newValue);
		}
	}

	private synchronized static void writeToServerFIFO(int newValue) {
		synchronized (serverMemory) {
			serverMemory.remove(0);
			serverMemory.add(newValue);
		}
	}

	private synchronized static void writeToServerLRU(int newValue,
			int accCounter, ArrayList<Integer> indexArr) {
		synchronized (serverMemory) {
			int minIndex = indexArr.indexOf(Collections.min(indexArr));
			serverMemory.set(minIndex, newValue);
			accessCounter = accessCounter + 1;
			indexArray.set(minIndex, accessCounter);
		}
	}

	private static int getFromDisk(int reqData, SimulatorTickCounts mytickCount) {
		return SimulatorDisk.getDataFromMemory(reqData);
	}

}
