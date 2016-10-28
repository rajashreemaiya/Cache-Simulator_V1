import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import javax.sound.sampled.ReverbType;

import org.apache.commons.math3.linear.MatrixUtils;

import com.opencsv.CSVReader;

/**
 * This class initializes each client and it's operations
 */

/**
 * @author rmaiya
 *
 */
public class SimulatorClient extends Thread {
	int clientNumber;
	int dataInClient;
	int dataToLookUp;
	int LocalcacheHits = 0;
	int NeighborcacheHits = 0;
	int cacheMiss = 0;
	static int dataRequests = 0;
	Thread client;
	String clientName;
	int clientBlockSize;
	SimulatorCache clientCache;
	SimulatorTickCounts mytickCount;
	SimulatorLogger logFile;
	int[] clientTickCounts = new int[15];

	public SimulatorClient(int clientNumber, int blockSize) {
		this.clientBlockSize = this.setBlockSize(blockSize);
		this.clientCache = new SimulatorCache(blockSize);
		this.clientNumber = clientNumber;
		this.mytickCount = new SimulatorTickCounts();
		this.setBlockSize(blockSize);
		// this.fillInitialCache();
		SimulatorContentManager.lookUpTable
				.put(clientNumber, clientCache.cache);
	}

	private void fillInitialCache() {
		Random r = new Random();
		int i = 0;
		while (i < clientBlockSize) {
			int reqBlock = r.nextInt(20) + 1;
			if (!clientCache.cache.contains(reqBlock)) {
				clientCache.cache.add(reqBlock);
				SimulatorContentManager.recircArray.put(reqBlock, 3);
				i++;
			}
		}
	}

	public int setBlockSize(int blockSize) {
		return this.clientBlockSize = blockSize;
	}

	public int getBlockSize() {
		return clientBlockSize;
	}

	/**
	 * Helper method to for printing
	 * */
	public void printClientCache() {
		for (Integer j : clientCache.cache) {
			System.out.print(j + " ");
		}

	}

	public synchronized void run() {

		this.logFile = new SimulatorLogger(this.clientNumber);

		if (SimulatorConstants.ALGORITHM.equals("FIFO")) {
			this.simFIFO();
		}

		if (SimulatorConstants.ALGORITHM.equals("LRU")) {
			this.SimLRU();
		}

		if (SimulatorConstants.ALGORITHM.equals("Random")) {
			this.SimCacheRandomReplacement();
		}

		if (SimulatorConstants.ALGORITHM.equals("NChance")) {
			this.simNChance();
		}

		if (SimulatorConstants.ALGORITHM.equals("LFU")) {
			this.simLFU();
		}

	}

	private void simLFU() {

		BufferedReader reader;
		try {
			Map<Integer, Integer> freq = new HashMap<Integer, Integer>();
			int value = 0;
			for (int i = 0; i < clientBlockSize; i++) {
				int key = clientCache.cache.get(i);
				freq.put(key, value);
			}

			String line = null;
			Scanner scanner = null;
			reader = new BufferedReader(new FileReader("Input_traces/"
					+ SimulatorConstants.CLIENTS + "/"
					+ SimulatorConstants.FILEPREFIX
					+ SimulatorConstants.CLIENTS + "_" + this.clientNumber
					+ ".csv"));

			while ((line = reader.readLine()) != null) {

				updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String data = scanner.next();
					int reqBlock = Integer.parseInt(data);

					List<Integer> client_data = this.getClientCache();
					SimulatorContentManager.lookUpTable.put(this.clientNumber,
							client_data);

					if (clientCache.cache.size() < clientBlockSize) {
						logFile.writeToFile(this.clientNumber, "\n");
						if (clientCache.cache.contains(reqBlock)) {
							value = freq.get(reqBlock);
							freq.put(reqBlock, value + 1);
						}

						else {
							clientCache.cache.add(reqBlock);
							freq.put(reqBlock, value);
						}
					}

					else {
						if (searchLocalCache(reqBlock) == true) {
							value = freq.get(reqBlock);
							freq.put(reqBlock, value + 1);
							this.LocalcacheHits++;
							mytickCount.setTickCount(SimulatorConstants.SEARCH);
						}

						else {
							mytickCount
									.setTickCount(SimulatorConstants.TOCONTEXTMANAGER);
							mytickCount
									.setTickCount(SimulatorConstants.FROMCONTEXTMANAGER);
							int whoHasBlock = SimulatorContentManager
									.searchNeighborCache(this.clientNumber,
											reqBlock, logFile);
							if (whoHasBlock != -1) {
								int isBlockThere = checkForFalseHits(reqBlock,
										this.clientNumber, whoHasBlock);
								if (isBlockThere != -1) {
									getDataFromNeighbor(whoHasBlock,
											this.clientNumber, reqBlock);
									this.NeighborcacheHits++;
									Object[] localCacheUpdated = updateLocalCacheLFU(
											reqBlock, freq);
									freq = (Map<Integer, Integer>) localCacheUpdated[1];
								}

								else {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheLFU(
											reqBlock, freq);
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													(ArrayList<Integer>) localCacheUpdated[0],
													logFile);
									freq = (Map<Integer, Integer>) localCacheUpdated[1];
								}
							}

							else {

								int checkFalseMiss = checkForFalseMiss(
										reqBlock, this.clientNumber);
								if (checkFalseMiss == -1) {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheLFU(
											reqBlock, freq);
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													(ArrayList<Integer>) localCacheUpdated[0],
													logFile);
									freq = (Map<Integer, Integer>) localCacheUpdated[1];
								} else {
									this.NeighborcacheHits++;
									Object[] localCacheUpdated = updateLocalCacheLFU(
											reqBlock, freq);
									freq = (Map<Integer, Integer>) localCacheUpdated[1];
								}
							}
						}
					}
				}
			}
			reader.close();

		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	private Object[] updateLocalCacheLFU(int reqBlock,
			Map<Integer, Integer> freq) {

		ArrayList<Integer> minfreq = new ArrayList<Integer>();
		Object[] returnValues = new Object[2];
		if (freq.containsKey(reqBlock)) {
			int value = freq.get(reqBlock);
			freq.put(reqBlock, value + 1);
			// minfreq.add(freq.get(reqBlock));
		}

		else {
			int value = 0;
			freq.put(reqBlock, value);
			// minfreq.add(freq.get(reqBlock));
		}
		// System.out.println(freq);
		logFile.writeToFile(this.clientNumber, " --- " + clientCache.cache);
		logFile.writeToFile(this.clientNumber, "" + freq);
		for (int i = 0; i < clientCache.cache.size(); i++) {
			int cacheElement = clientCache.cache.get(i);
			// System.out.println(cacheElement);
			minfreq.add(freq.get(cacheElement));
		}
		// System.out.println(minfreq);
		int whatToReplace = minfreq.indexOf(Collections.min(minfreq));
		clientCache.cache.set(whatToReplace, reqBlock);
		logFile.writeToFile(this.clientNumber, " frequencies " + minfreq);
		logFile.writeToFile(this.clientNumber,
				" minimum " + minfreq.indexOf(Collections.min(minfreq)));
		logFile.writeToFile(this.clientNumber, " --- " + clientCache.cache);

		returnValues[0] = clientCache.cache;
		returnValues[1] = freq;
		return returnValues;

	}

	/**
	 * Implements LRU replacement policy
	 */
	private void SimLRU() {

		BufferedReader reader;
		try {
			int accessCounter = 0;
			ArrayList<Integer> indexArray = new ArrayList<Integer>();

			logFile.writeToFile(this.clientNumber, " " + this.getClientCache());

			int i = 0;
			String line = null;
			Scanner scanner = null;
			reader = new BufferedReader(new FileReader(
					"/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/Input_traces/"
							+ SimulatorConstants.CLIENTS + "/"
							+ SimulatorConstants.FILEPREFIX
							+ SimulatorConstants.CLIENTS + "_"
							+ this.clientNumber + ".csv"));
			//
			// reader = new BufferedReader(new FileReader("Input_traces/"
			// + SimulatorConstants.FILEPREFIX + this.clientNumber
			// + ".csv"));

			for (i = 0; i < clientBlockSize; i++) {
				indexArray.add(0);
			}

			while ((line = reader.readLine()) != null) {

				updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String data = scanner.next();
					int reqBlock = Integer.parseInt(data);

					logFile.writeToFile(this.clientNumber, "\n");
					logFile.writeToFile(this.clientNumber, "Client "
							+ this.clientNumber + " is requesting the data "
							+ reqBlock);

					int clientNum = this.clientNumber;
					List<Integer> client_data = this.getClientCache();
					SimulatorContentManager.lookUpTable.put(this.clientNumber,
							client_data);

					if (clientCache.cache.size() < clientBlockSize) {
						clientCache.cache.add(reqBlock);
					}

					else {

						if (searchLocalCache(reqBlock) == true) {
							int elmentIndex = clientCache.cache
									.indexOf(reqBlock);
							this.LocalcacheHits++;
							mytickCount.setTickCount(SimulatorConstants.SEARCH);
							accessCounter = accessCounter + 1;
							indexArray.set(elmentIndex, accessCounter);
							logFile.writeToFile(this.clientNumber,
									"Index array: " + indexArray);
						}

						else {
							mytickCount
									.setTickCount(SimulatorConstants.TOCONTEXTMANAGER);
							mytickCount
									.setTickCount(SimulatorConstants.FROMCONTEXTMANAGER);
							int whoHasBlock = SimulatorContentManager
									.searchNeighborCache(this.clientNumber,
											reqBlock, logFile);
							if (whoHasBlock != -1) {
								int isBlockThere = checkForFalseHits(reqBlock,
										this.clientNumber, whoHasBlock);
								if (isBlockThere != -1) {
									getDataFromNeighbor(whoHasBlock,
											this.clientNumber, reqBlock);
									this.NeighborcacheHits++;
									Object[] localCacheUpdated = updateLocalCacheLRU(
											reqBlock, indexArray, accessCounter);
									accessCounter = (int) localCacheUpdated[1];
									indexArray = (ArrayList<Integer>) localCacheUpdated[0];
								}

								else {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheLRU(
											reqBlock, indexArray, accessCounter);
									accessCounter = (int) localCacheUpdated[1];
									indexArray = (ArrayList<Integer>) localCacheUpdated[0];
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													(ArrayList<Integer>) localCacheUpdated[2],
													logFile);
								}
							}

							else {

								int checkFalseMiss = checkForFalseMiss(
										reqBlock, this.clientNumber);
								if (checkFalseMiss == -1) {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheLRU(
											reqBlock, indexArray, accessCounter);
									accessCounter = (int) localCacheUpdated[1];
									indexArray = (ArrayList<Integer>) localCacheUpdated[0];
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													(ArrayList<Integer>) localCacheUpdated[2],
													logFile);
								} else {
									this.NeighborcacheHits++;
									Object[] localCacheUpdated = updateLocalCacheLRU(
											reqBlock, indexArray, accessCounter);
									accessCounter = (int) localCacheUpdated[1];
									indexArray = (ArrayList<Integer>) localCacheUpdated[0];
								}
							}
						}
					}
				}
			}
			reader.close();

		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	private Object[] updateLocalCacheLRU(int reqBlock,
			ArrayList<Integer> indexArray, int accessCounter) {
		// System.out.println(clientCache.cache.size());

		Object[] returnValues = new Object[3];
		logFile.writeToFile(this.clientNumber, "Cache is:.."
				+ clientCache.cache);

		logFile.writeToFile(this.clientNumber, "Replacing..");

		int minIndex = -1;
		for (int i = 0; i < indexArray.size(); i++) {
			int min = Collections.min(indexArray);
			minIndex = indexArray.indexOf(min);
		}

		// System.out.println("----" + indexArray.size());
		// System.out.println(this.clientNumber + " ---- " + indexArray);
		// System.out.println(clientCache.cache.size());
		clientCache.cache.set(minIndex, reqBlock);
		accessCounter = accessCounter + 1;
		indexArray.set(minIndex, accessCounter);

		returnValues[0] = indexArray;
		returnValues[1] = accessCounter;
		returnValues[2] = clientCache.cache;

		logFile.writeToFile(this.clientNumber, "Index array: " + indexArray);

		logFile.writeToFile(this.clientNumber, "Replaced Cache is: "
				+ clientCache.cache);

		return returnValues;
	}

	private List<Integer> getClientCache() {
		return clientCache.cache;
	}

	/**
	 * Implements Random replacement policy
	 * */
	private void SimCacheRandomReplacement() {

		BufferedReader reader;
		try {
			String line = null;
			Scanner scanner = null;
			reader = new BufferedReader(new FileReader("Input_traces/"
					+ SimulatorConstants.CLIENTS + "/"
					+ SimulatorConstants.FILEPREFIX
					+ SimulatorConstants.CLIENTS + "_" + this.clientNumber
					+ ".csv"));

			while ((line = reader.readLine()) != null) {

				updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String data = scanner.next();
					int reqBlock = Integer.parseInt(data);

					int clientNum = this.clientNumber;
					List<Integer> client_data = this.getClientCache();
					SimulatorContentManager.lookUpTable.put(this.clientNumber,
							client_data);

					logFile.writeToFile(this.clientNumber, "lookup table: "
							+ SimulatorContentManager.lookUpTable);

					if (clientCache.cache.size() < clientBlockSize) {
						clientCache.cache.add(reqBlock);
					}

					else {

						logFile.writeToFile(this.clientNumber, "\n");
						logFile.writeToFile(this.clientNumber, "Client "
								+ this.clientNumber
								+ " is requesting the data " + reqBlock);

						if (clientCache.cache.size() < this.clientBlockSize
								&& (!clientCache.cache.contains(reqBlock))) {
							clientCache.cache.add(reqBlock);
						}

						else {

							if (searchLocalCache(reqBlock) == true) {
								this.LocalcacheHits++;
								mytickCount
										.setTickCount(SimulatorConstants.SEARCH);
							}

							else {
								mytickCount
										.setTickCount(SimulatorConstants.TOCONTEXTMANAGER);
								mytickCount
										.setTickCount(SimulatorConstants.FROMCONTEXTMANAGER);
								int whoHasBlock = SimulatorContentManager
										.searchNeighborCache(clientNum,
												reqBlock, logFile);
								if (whoHasBlock != -1) {
									logFile.writeToFile(clientNum,
											"Found in neighbor: " + whoHasBlock);
									int isBlockThere = checkForFalseHits(
											reqBlock, clientNum, whoHasBlock);
									if (isBlockThere != -1) {
										getDataFromNeighbor(whoHasBlock,
												clientNum, reqBlock);
										logFile.writeToFile(clientNum,
												"Got data from: " + whoHasBlock);
										this.NeighborcacheHits++;
										updateLocalCache(reqBlock);
									}

									else {
										goToServer(clientNum, reqBlock);
										List<Integer> localCacheUpdated = updateLocalCache(reqBlock);
										SimulatorContentManager
												.updateContentManager(
														clientNum, reqBlock,
														localCacheUpdated,
														logFile);
									}
								}

								else {

									int checkFalseMiss = checkForFalseMiss(
											reqBlock, clientNum);
									if (checkFalseMiss == -1) {
										goToServer(clientNum, reqBlock);
										List<Integer> localCacheUpdated = updateLocalCache(reqBlock);
										SimulatorContentManager
												.updateContentManager(
														clientNum, reqBlock,
														localCacheUpdated,
														logFile);
									} else {
										this.NeighborcacheHits++;
										updateLocalCache(reqBlock);
									}
								}
							}
						}
					}
				}

			}

			reader.close();
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	private List<Integer> updateLocalCache(int req_data) {
		Random random = new Random();
		int indexOfwhatToReplace = random.nextInt(clientBlockSize - 1) + 1;
		if (clientCache.cache.size() < clientBlockSize) {
			clientCache.cache.add(req_data);
		} else {
			clientCache.cache.set(indexOfwhatToReplace, req_data);
		}
		return clientCache.cache;
	}

	/**
	 * Search data in client's local cache
	 * */
	private boolean searchLocalCache(int req_data) {
		logFile.writeToFile(this.clientNumber, "Searching local cache");
		for (int i = 0; i < clientCache.cache.size(); i++) {
			if (clientCache.cache.get(i) == req_data) {
				return true;
			}
		}
		return false;
	}

	// ---------------------

	/**
	 * Implements FIFO replacement policy
	 */
	private void simFIFO() {

		BufferedReader reader;
		try {
			String line = null;
			Scanner scanner = null;

			reader = new BufferedReader(new FileReader(
					"/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/Input_traces/"
							+ SimulatorConstants.CLIENTS + "/"
							+ SimulatorConstants.FILEPREFIX
							+ SimulatorConstants.CLIENTS + "_"
							+ this.clientNumber + ".csv"));

			// reader = new BufferedReader(new FileReader("psuedodata.csv"));

			while ((line = reader.readLine()) != null) {

				updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String data = scanner.next();
					String[] parts = data.split(",");
					System.out.println(data);
					int reqBlock = Integer.parseInt(parts[0]);
					int pat_index = Integer.parseInt(parts[1]);
					// SimulatorContentManager.patterns.add(pat_index,);
					// System.out.println(pat_index);
					logFile.writeToFile(this.clientNumber, "\n");
					logFile.writeToFile(this.clientNumber, this.clientNumber
							+ " is requesting.. " + reqBlock);

					if (clientCache.cache.size() < clientBlockSize) {
						logFile.writeToFile(this.clientNumber, "Filling..");
						clientCache.cache.add(reqBlock);
						clientCache.pattern_index.add(pat_index);
					}

					else {
						if (searchLocalCache(reqBlock) == true) {
							this.LocalcacheHits++;
							mytickCount.setTickCount(SimulatorConstants.SEARCH);
						}

						else {
							mytickCount
									.setTickCount(SimulatorConstants.TOCONTEXTMANAGER);
							mytickCount
									.setTickCount(SimulatorConstants.FROMCONTEXTMANAGER);
							int whoHasBlock = SimulatorContentManager
									.searchNeighborCache(this.clientNumber,
											reqBlock, logFile);
							if (whoHasBlock != -1) {
								int isBlockThere = checkForFalseHits(reqBlock,
										this.clientNumber, whoHasBlock);
								if (isBlockThere != -1) {
									getDataFromNeighbor(whoHasBlock,
											this.clientNumber, reqBlock);
									this.NeighborcacheHits++;
									updateLocalCacheFIFO(reqBlock, pat_index);
								}

								else {
									goToServer(this.clientNumber, reqBlock);
									List<Integer> localCacheUpdated = updateLocalCacheFIFO(
											reqBlock, pat_index);
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													localCacheUpdated, logFile);
								}
							}

							else {

								int checkFalseMiss = checkForFalseMiss(
										reqBlock, this.clientNumber);
								if (checkFalseMiss == -1) {
									goToServer(this.clientNumber, reqBlock);
									List<Integer> localCacheUpdated = updateLocalCacheFIFO(
											reqBlock, pat_index);
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													localCacheUpdated, logFile);
								} else {
									this.NeighborcacheHits++;
									updateLocalCacheFIFO(reqBlock, pat_index);
								}
							}
						}
					}
				}
			}
			reader.close();

		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized void updateDataReqCount() {
		synchronized (SimulatorConstants.DATAREQUESTS) {
			SimulatorConstants.DATAREQUESTS += 1;
		}
	}

	private synchronized List<Integer> updateLocalCacheFIFO(int reqBlock,
			int pattern_index) {
		if (clientCache.cache.size() < clientBlockSize) {
			clientCache.cache.add(reqBlock);
		}

		else {
			logFile.writeToFile(this.clientNumber, "Indicies array:.."
					+ clientCache.pattern_index);
			logFile.writeToFile(this.clientNumber, "Cache is:.."
					+ clientCache.cache);

			logFile.writeToFile(this.clientNumber, "Replacing..");

			// logFile.writeToFile(this.clientNumber, "Removing..."
			// + clientCache.cache.get(0));

			int whatToRemove = clientCache.cache.get(0);
			int pat_index = clientCache.pattern_index.get(0);

			synchronized (clientCache.pattern_index) {
				Iterator<Integer> iter = clientCache.pattern_index.iterator();

				while (iter.hasNext()) {
					int p = iter.next();
					if (p == pat_index) {
						int partOfBlock = clientCache.pattern_index.indexOf(p);
						logFile.writeToFile(this.clientNumber, "Removing.."
								+ clientCache.cache.get(partOfBlock));
						clientCache.cache.remove(partOfBlock);
						iter.remove();
						logFile.writeToFile(this.clientNumber,
								"Now the size is: " + clientCache.cache.size());
					}

				}
				// clientCache.cache.remove(0);
				clientCache.cache.add(reqBlock);
				clientCache.pattern_index.add(pattern_index);
			}

			// logFile.writeToFile(this.clientNumber, "Now the size is: "
			// + clientCache.cache.size());

			// logFile.writeToFile(this.clientNumber, "Cache is: "
			// + clientCache.cache);

			logFile.writeToFile(this.clientNumber, "Replaced Cache is: "
					+ clientCache.cache);

		}
		return clientCache.cache;
	}

	/**
	 * 
	 * @param reqBlock
	 *            : Data I want
	 * @param whoAskedForBlock
	 *            : Client that asked for data
	 * @param whoHasBlock
	 *            : Client that has the data
	 * @return block if it exists (false miss) or -1 (CM is right)
	 */
	synchronized private int checkForFalseMiss(int reqBlock,
			int whoAskedForBlock) {
		for (int i = 0; i < SimulatorContentManager.allClients.length; i++) {
			if (SimulatorContentManager.allClients[i].getClientCache()
					.contains(reqBlock)) {
				logFile.writeToFile(whoAskedForBlock, "It exists here!");
				return reqBlock;
			}
		}
		logFile.writeToFile(whoAskedForBlock, "Ok, its not there!");
		return -1;
	}

	/**
	 * 
	 * @param reqBlock
	 *            : Data I want
	 * @param whoAskedForBlock
	 *            : Client that asked for data
	 * @param whoHasBlock
	 *            : Client that has the data
	 * @return block if it exists (CM is right) or -1 (false hit)
	 */
	synchronized public int checkForFalseHits(int reqBlock,
			int whoAskedForBlock, int whoHasBlock) {

		if (SimulatorContentManager.allClients[whoHasBlock].getClientCache()
				.contains(reqBlock)) {
			logFile.writeToFile(whoAskedForBlock, "OK Verified ");
			return reqBlock;
		}

		else {
			logFile.writeToFile(whoAskedForBlock, "Bad hit");
			cacheMiss++;
			return -1;
		}
	}

	/**
	 * 
	 * @param whoHasBlock
	 *            : Client that has the data
	 * @param whoAskedForBlock
	 *            : Client that asked for data
	 * @param reqBlock
	 *            : Data I want
	 * @return data if found else -1
	 */
	synchronized private int getDataFromNeighbor(int whoHasBlock,
			int whoAskedForBlock, int reqBlock) {
		mytickCount.setTickCount(SimulatorConstants.TONEIGHBOR);
		mytickCount.setTickCount(SimulatorConstants.FROMNEIGHBOR);
		logFile.writeToFile(whoAskedForBlock, "Getting from client: "
				+ whoHasBlock);
		mytickCount.setTickCount(SimulatorConstants.SEARCH);
		return reqBlock;

	}

	private void goToServer(int clientNum, int reqBlock) {
		cacheMiss++;
		mytickCount.setTickCount(SimulatorConstants.TOSERVER);
		mytickCount.setTickCount(SimulatorConstants.FROMSERVER);
		logFile.writeToFile(clientNum, "Not found in content manager: ");
		searchServerMemm(clientNum, reqBlock, logFile);
		logFile.writeToFile(clientNum, "serverMemory: "
				+ SimulatorServer.serverMemory);
	}

	/**
	 * Search server
	 * 
	 * @param logFile
	 * */
	private int searchServerMemm(int clientNum, int reqBlock,
			SimulatorLogger logFile) {
		return SimulatorServer.searchMemory(clientNum, reqBlock, logFile,
				mytickCount, SimulatorConstants.SERVER_ALGORITHM);
	}

	private void simNChance() {

		List<Integer> client_data = this.getClientCache();
		SimulatorContentManager.lookUpTable.put(this.clientNumber, client_data);
		BufferedReader reader;

		try {
			int c = 0;
			String line = null;
			Scanner scanner = null;
			reader = new BufferedReader(new FileReader(
					"/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/Input_traces/"
							+ SimulatorConstants.CLIENTS + "/"
							+ SimulatorConstants.FILEPREFIX
							+ SimulatorConstants.CLIENTS + "_"
							+ this.clientNumber + ".csv"));

			// reader = new BufferedReader(new FileReader("Input_traces/"
			// + SimulatorConstants.FILEPREFIX + this.clientNumber
			// + ".csv"));

			while ((line = reader.readLine()) != null) {

				updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String data = scanner.next();
					int reqBlock = Integer.parseInt(data);

					logFile.writeToFile(this.clientNumber, "\n");
					logFile.writeToFile(this.clientNumber, "Iteration: " + c);
					logFile.writeToFile(this.clientNumber, "Client "
							+ this.clientNumber + " is requesting the data "
							+ reqBlock);

					if (clientCache.cache.size() < clientBlockSize) {
						clientCache.cache.add(reqBlock);
					}

					else {

						if (SimulatorContentManager.recircArray
								.containsKey(reqBlock)) {
							if (SimulatorContentManager.recircArray
									.get(reqBlock) <= 0)
								SimulatorContentManager.recircArray.put(
										reqBlock, 3);
						} else {
							SimulatorContentManager.recircArray
									.put(reqBlock, 3);
						}

						if (searchLocalCache(reqBlock) == true) {
							this.LocalcacheHits++;
							mytickCount.setTickCount(SimulatorConstants.SEARCH);
						}

						else {
							mytickCount
									.setTickCount(SimulatorConstants.TOCONTEXTMANAGER);
							mytickCount
									.setTickCount(SimulatorConstants.FROMCONTEXTMANAGER);
							int whoHasBlock = SimulatorContentManager
									.searchNeighborCache(this.clientNumber,
											reqBlock, logFile);
							if (whoHasBlock != -1) {
								int isBlockThere = checkForFalseHits(reqBlock,
										this.clientNumber, whoHasBlock);
								if (isBlockThere != -1) {
									getDataFromNeighbor(whoHasBlock,
											this.clientNumber, reqBlock);
									this.NeighborcacheHits++;
									updateLocalCacheNChance(reqBlock);
								}

								else {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheNChance(reqBlock);
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													(ArrayList<Integer>) localCacheUpdated[1],
													logFile);
								}
							}

							else {

								int checkFalseMiss = checkForFalseMiss(
										reqBlock, this.clientNumber);
								if (checkFalseMiss == -1) {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheNChance(reqBlock);
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													(ArrayList<Integer>) localCacheUpdated[1],
													logFile);
								} else {
									this.NeighborcacheHits++;
									updateLocalCacheNChance(reqBlock);
								}
							}
						}
					}
				}
			}
			reader.close();

		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized Object[] updateLocalCacheNChance(int reqBlock) {

		ArrayList<Integer> allClients = new ArrayList<Integer>();

		int numClients = SimulatorContentManager.allClients.length;

		for (int i = 0; i < numClients; i++) {
			allClients.add(SimulatorContentManager.allClients[i].clientNumber);
		}

		int indexClient = allClients.indexOf(this.clientNumber);
		allClients.remove(indexClient);
		logFile.writeToFile(this.clientNumber, "Array of clients " + allClients);

		Object[] returnValues = new Object[3];
		boolean isUnique = false;
		int cacheElement = clientCache.cache.get(0);
		int index = clientCache.cache.indexOf(cacheElement);
		synchronized (SimulatorContentManager.recircArray) {
			logFile.writeToFile(this.clientNumber, ""
					+ SimulatorContentManager.lookUpTable);
			logFile.writeToFile(this.clientNumber, "arr "
					+ SimulatorContentManager.recircArray);

			for (int i = 0; i < SimulatorContentManager.allClients.length; i++) {
				if (SimulatorContentManager.allClients[i].clientNumber != this.clientNumber) {

					if (SimulatorContentManager.allClients[i].getClientCache()
							.contains(cacheElement)) {

						logFile.writeToFile(
								this.clientNumber,
								SimulatorContentManager.allClients[i].clientNumber
										+ " contains " + cacheElement);
						logFile.writeToFile(this.clientNumber, "Not unique");
						isUnique = false;
						break;
					}

					else {
						isUnique = true;
						logFile.writeToFile(this.clientNumber, cacheElement
								+ " unique");
					}
				}
			}

			logFile.writeToFile(this.clientNumber, cacheElement + " "
					+ isUnique);

			if (isUnique) {

				if (!SimulatorContentManager.recircArray.containsKey(reqBlock)) {
					SimulatorContentManager.recircArray.put(reqBlock, 3);
				}

				if ((SimulatorContentManager.recircArray.get(cacheElement)) > 0) {
					logFile.writeToFile(this.clientNumber, "Client list.."
							+ allClients);
					logFile.writeToFile(this.clientNumber, "Client list size.."
							+ allClients.size());
					int r = new Random().nextInt(allClients.size());
					int whereTosendTo = allClients.get(r);
					logFile.writeToFile(this.clientNumber, "Fowarding to..  "
							+ whereTosendTo);
					updateLocalCacheFIFO(whereTosendTo, cacheElement);
					logFile.writeToFile(this.clientNumber, "My cache is:  "
							+ clientCache.cache);
					logFile.writeToFile(
							this.clientNumber,
							"Neighborcache is "
									+ SimulatorContentManager.allClients[whereTosendTo]
											.getClientCache());
					int value = SimulatorContentManager.recircArray
							.get(cacheElement);
					SimulatorContentManager.recircArray.put(cacheElement,
							value - 1);
					clientCache.cache.set(index, reqBlock);
					logFile.writeToFile(this.clientNumber, "arr "
							+ SimulatorContentManager.recircArray);
					returnValues[0] = SimulatorContentManager.recircArray;
					returnValues[1] = clientCache.cache;
				}

				else {
					logFile.writeToFile(this.clientNumber,
							"Survived enough times!");
					clientCache.cache.remove(index);
					clientCache.cache.add(reqBlock);
					returnValues[0] = SimulatorContentManager.recircArray;
					returnValues[1] = clientCache.cache;
				}
			}

			else {
				logFile.writeToFile(this.clientNumber, "Not unique....removed");
				clientCache.cache.remove(index);
				clientCache.cache.add(reqBlock);
				returnValues[0] = SimulatorContentManager.recircArray;
				returnValues[1] = clientCache.cache;
			}
		}
		return returnValues;

	}

	synchronized private void updateLocalCacheFIFO1(int forwardTo, int reqBlock) {

		logFile.writeToFile(this.clientNumber, "Replacing in " + forwardTo);
		SimulatorContentManager.allClients[forwardTo].getClientCache()
				.remove(0);
		SimulatorContentManager.allClients[forwardTo].clientCache.cache
				.add(reqBlock);
		List<Integer> localCacheUpdated = SimulatorContentManager.allClients[forwardTo]
				.getClientCache();
		SimulatorContentManager.updateContentManager(forwardTo, reqBlock,
				localCacheUpdated, logFile);
	}

}
