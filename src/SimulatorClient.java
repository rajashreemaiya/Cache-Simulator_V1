import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 
 * This class initializes each client and its operations.
 * 
 * @author Rajashree K Maiya
 *
 */
public class SimulatorClient extends Thread {
	int clientNumber;
	int LocalcacheHits = 0;
	int NeighborcacheHits = 0;
	int cacheMiss = 0;
	static int dataRequests = 0;
	Thread client;
	int clientBlockSize;
	SimulatorCache clientCache;
	SimulatorTickCounts mytickCount;
	SimulatorLogger logFile;

	public SimulatorClient(int clientNumber, int blockSize) {
		this.clientBlockSize = this.setBlockSize(blockSize);
		this.clientCache = new SimulatorCache(blockSize);
		this.clientNumber = clientNumber;
		this.mytickCount = new SimulatorTickCounts();
		this.setBlockSize(blockSize);
		this.fillInitialCache();
		SimulatorContentManager.lookUpTable
				.put(clientNumber, clientCache.cache);
	}

	/**
	 * Warm up the cache before calling replacement algorithms.
	 */
	private void fillInitialCache() {
		Random r = new Random();
		int i = 0;
		int p = 100000;
		while (i < clientBlockSize) {
			int reqBlock = r.nextInt(20000) + 1;
			clientCache.cache.add(reqBlock);
			clientCache.pattern_index.add(p);
			p = p + 1;
			Integer[] temp = new Integer[2];
			temp[0] = reqBlock;
			temp[1] = reqBlock;
			i++;
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

	@Override
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

		if (SimulatorConstants.ALGORITHM.equals("NChanceK")) {
			this.simNChanceK();
		}

	}

	/**
	 * Implements Least Frequency Used replacement strategy.
	 */
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
			reader = new BufferedReader(new FileReader(
					"/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/Patterns/"
							+ SimulatorConstants.FILEPREFIX
							+ SimulatorConstants.CLIENTS + "_"
							+ this.clientNumber + ".csv"));

			while ((line = reader.readLine()) != null) {

				updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String data = scanner.next();
					String[] parts = data.split(",");
					int reqBlock = Integer.parseInt(parts[0]);
					int pat_index = Integer.parseInt(parts[1]);

					logFile.writeToFile(this.clientNumber, "\n");
					logFile.writeToFile(this.clientNumber, "data request:  "
							+ reqBlock);

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
							logFile.writeToFile(this.clientNumber, "Filling...");
							clientCache.cache.add(reqBlock);
							freq.put(reqBlock, value);
							clientCache.pattern_index.add(pat_index);
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
											reqBlock, freq, pat_index);
									freq = (Map<Integer, Integer>) localCacheUpdated[1];
								}

								else {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheLFU(
											reqBlock, freq, pat_index);
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
											reqBlock, freq, pat_index);
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
											reqBlock, freq, pat_index);
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

	/**
	 * 
	 * @param reqBlock Data block being requested
	 * @param freq
	 * @param pattern_index
	 * @return
	 */
	private Object[] updateLocalCacheLFU(int reqBlock,
			Map<Integer, Integer> freq, int pattern_index) {

		ArrayList<Integer> minfreq = new ArrayList<Integer>();
		Object[] returnValues = new Object[2];
		if (freq.containsKey(reqBlock)) {
			int value = freq.get(reqBlock);
			freq.put(reqBlock, value + 1);
		}

		else {
			int value = 0;
			freq.put(reqBlock, value);
		}
		logFile.writeToFile(this.clientNumber, " --- " + clientCache.cache);
		logFile.writeToFile(this.clientNumber, "" + freq);
		for (int i = 0; i < clientCache.cache.size(); i++) {
			int cacheElement = clientCache.cache.get(i);
			minfreq.add(freq.get(cacheElement));
		}

		logFile.writeToFile(this.clientNumber, " index.."
				+ clientCache.pattern_index);
		int whatToReplace = minfreq.indexOf(Collections.min(minfreq));
		int val = clientCache.cache.get(whatToReplace);
		int pat_index = clientCache.pattern_index.get(whatToReplace);
		clientCache.cache.set(whatToReplace, reqBlock);
		clientCache.pattern_index.set(whatToReplace, pattern_index);
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
					logFile.writeToFile(this.clientNumber, "Now the size is: "
							+ clientCache.cache.size());
				}
			}

		}

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
					"/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/Patterns/"
							+ SimulatorConstants.FILEPREFIX
							+ SimulatorConstants.CLIENTS + "_"
							+ this.clientNumber + ".csv"));

			for (i = 0; i < clientBlockSize; i++) {
				indexArray.add(0);
			}

			while ((line = reader.readLine()) != null) {

				updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String data = scanner.next();
					String[] parts = data.split(",");
					int reqBlock = Integer.parseInt(parts[0]);
					int pat_index = Integer.parseInt(parts[1]);

					logFile.writeToFile(this.clientNumber, "\n");
					logFile.writeToFile(this.clientNumber, "Client "
							+ this.clientNumber + " is requesting the data "
							+ reqBlock);
					logFile.writeToFile(this.clientNumber, "Cache:  "
							+ clientCache.cache);
					logFile.writeToFile(this.clientNumber, "Index array:  "
							+ indexArray);

					int clientNum = this.clientNumber;
					List<Integer> client_data = this.getClientCache();
					SimulatorContentManager.lookUpTable.put(this.clientNumber,
							client_data);

					if (clientCache.cache.size() < clientBlockSize) {
						clientCache.cache.add(reqBlock);
						int index = clientCache.cache.indexOf(reqBlock);
						clientCache.pattern_index.add(pat_index);
						accessCounter = accessCounter + 1;
						indexArray.set(index, accessCounter);
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
											reqBlock, indexArray,
											accessCounter, pat_index);
									accessCounter = (int) localCacheUpdated[1];
									indexArray = (ArrayList<Integer>) localCacheUpdated[0];
								}

								else {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheLRU(
											reqBlock, indexArray,
											accessCounter, pat_index);
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
											reqBlock, indexArray,
											accessCounter, pat_index);
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
											reqBlock, indexArray,
											accessCounter, pat_index);
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

	/**
	 * 
	 * @param reqBlock Data block being requested
	 * @param indexArray
	 * @param accessCounter
	 * @param pattern_index
	 * @return
	 */
	private Object[] updateLocalCacheLRU(int reqBlock,
			ArrayList<Integer> indexArray, int accessCounter, int pattern_index) {

		Object[] returnValues = new Object[3];
		logFile.writeToFile(this.clientNumber, "Cache is:.."
				+ clientCache.cache);

		logFile.writeToFile(this.clientNumber, "Replacing..");

		int minIndex = -1;
		for (int i = 0; i < indexArray.size(); i++) {
			int min = Collections.min(indexArray);
			minIndex = indexArray.indexOf(min);
		}

		int whatToRemove = clientCache.cache.get(minIndex);
		int pat_index = clientCache.pattern_index.get(minIndex);
		logFile.writeToFile(this.clientNumber, "what to remove.."
				+ whatToRemove);
		logFile.writeToFile(this.clientNumber, "pat ind.." + pat_index);
		clientCache.cache.set(minIndex, reqBlock);
		clientCache.pattern_index.set(minIndex, pattern_index);
		accessCounter = accessCounter + 1;
		indexArray.set(minIndex, accessCounter);
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
					logFile.writeToFile(this.clientNumber, "Now the size is: "
							+ clientCache.cache.size());
				}
			}

		}

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
			reader = new BufferedReader(new FileReader(
					"/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/Patterns/"
							+ SimulatorConstants.FILEPREFIX
							+ SimulatorConstants.CLIENTS + "_"
							+ this.clientNumber + ".csv"));

			while ((line = reader.readLine()) != null) {

				updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String data = scanner.next();
					String[] parts = data.split(",");
					int reqBlock = Integer.parseInt(parts[0]);
					int pat_index = Integer.parseInt(parts[1]);

					int clientNum = this.clientNumber;
					List<Integer> client_data = this.getClientCache();
					SimulatorContentManager.lookUpTable.put(this.clientNumber,
							client_data);

					if (clientCache.cache.size() < clientBlockSize) {
						clientCache.cache.add(reqBlock);
						clientCache.pattern_index.add(pat_index);
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
										updateLocalCache(reqBlock, pat_index);
									}

									else {
										goToServer(clientNum, reqBlock);
										List<Integer> localCacheUpdated = updateLocalCache(
												reqBlock, pat_index);
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
										List<Integer> localCacheUpdated = updateLocalCache(
												reqBlock, pat_index);
										SimulatorContentManager
												.updateContentManager(
														clientNum, reqBlock,
														localCacheUpdated,
														logFile);
									} else {
										this.NeighborcacheHits++;
										updateLocalCache(reqBlock, pat_index);
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

	private List<Integer> updateLocalCache(int reqBlock, int pattern_index) {
		Random random = new Random();
		int indexOfwhatToReplace = random.nextInt(clientBlockSize - 1) + 1;
		int whatToRemove = clientCache.cache.get(indexOfwhatToReplace);
		int pat_index = clientCache.pattern_index.get(indexOfwhatToReplace);

		synchronized (clientCache.pattern_index) {
			Iterator<Integer> iter = clientCache.pattern_index.iterator();

			while (iter.hasNext()) {
				int p = iter.next();
				if (p == pat_index) {
					int partOfBlock = clientCache.pattern_index.indexOf(p);
					clientCache.cache.remove(partOfBlock);
					iter.remove();

				}

			}
			clientCache.cache.add(reqBlock);
			clientCache.pattern_index.add(pattern_index);
		}

		if (clientCache.cache.size() < clientBlockSize) {
			clientCache.cache.add(reqBlock);
		} else {
			clientCache.cache.set(indexOfwhatToReplace, reqBlock);
		}
		return clientCache.cache;
	}

	/**
	 * Search data in client's local cache
	 * */
	private boolean searchLocalCache(int req_data) {
		logFile.writeToFile(this.clientNumber, "Searching local cache ");
		for (int i = 0; i < clientCache.cache.size(); i++) {
			if (this.clientCache.cache.get(i) == req_data) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Implements FIFO replacement policy
	 */
	private void simFIFO() {

		BufferedReader reader;
		try {
			String line = null;
			Scanner scanner = null;

			reader = new BufferedReader(new FileReader(
					"/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/Patterns/"
							+ SimulatorConstants.FILEPREFIX
							+ SimulatorConstants.CLIENTS + "_"
							+ this.clientNumber + ".csv"));

			while ((line = reader.readLine()) != null) {

				updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String data = scanner.next();
					String[] parts = data.split(",");
					int reqBlock = Integer.parseInt(parts[0]);
					int pat_index = Integer.parseInt(parts[1]);

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

	/**
	 * 
	 * @return updated data request counter.
	 */
	private synchronized int updateDataReqCount() {
		synchronized (SimulatorConstants.DATAREQUESTS) {
			SimulatorConstants.DATAREQUESTS += 1;
		}
		return SimulatorConstants.DATAREQUESTS;
	}

	/**
	 * 
	 * @param reqBlock Data block being requested
	 * @param pattern_index
	 * @return
	 */
	private List<Integer> updateLocalCacheFIFO(int reqBlock, int pattern_index) {
		if (clientCache.cache.size() < clientBlockSize) {
			clientCache.cache.add(reqBlock);
			clientCache.pattern_index.add(pattern_index);
		}

		else {

			int whatToRemove = clientCache.cache.get(0);
			int pat_index = clientCache.pattern_index.get(0);

			synchronized (clientCache.pattern_index) {
				Iterator<Integer> iter = clientCache.pattern_index.iterator();

				while (iter.hasNext()) {
					int p = iter.next();
					if (p == pat_index) {
						int partOfBlock = clientCache.pattern_index.indexOf(p);

						clientCache.cache.remove(partOfBlock);
						iter.remove();

					}

				}
				clientCache.cache.add(reqBlock);
				clientCache.pattern_index.add(pattern_index);
			}
		}
		return clientCache.cache;
	}

	/**
	 * 
	 * @param reqBlock Data block being requested
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
	 * @param reqBlock Data block being requested
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
	 * @param reqBlock Data block being requested
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
		SimulatorConstants.NOTNEIGHBOR++;
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

	/**
	 * Implements NChance replacement strategy
	 */
	private synchronized void simNChance() {
		List<Integer> client_data = this.getClientCache();
		SimulatorContentManager.lookUpTable.put(this.clientNumber, client_data);
		BufferedReader reader;

		try {
			int c = 0;
			String line = null;
			Scanner scanner = null;

			reader = new BufferedReader(new FileReader(
					"/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/Patterns/"
							+ SimulatorConstants.FILEPREFIX
							+ SimulatorConstants.CLIENTS + "_"
							+ this.clientNumber + ".csv"));

			while ((line = reader.readLine()) != null) {
				int numReq = updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {

					String data = scanner.next();
					String[] parts = data.split(",");
					int reqBlock = Integer.parseInt(parts[0]);
					int pattern_num = Integer.parseInt(parts[1]);
					Integer[] counter = new Integer[2];
					counter[0] = reqBlock;
					counter[1] = pattern_num;

					logFile.writeToFile(this.clientNumber, "\n");
					logFile.writeToFile(this.clientNumber, "Iteration: " + c);
					logFile.writeToFile(this.clientNumber, "Client "
							+ this.clientNumber + " is requesting the data "
							+ reqBlock);

					logFile.writeToFile(this.clientNumber, "Requests " + numReq);

					if (clientCache.cache.size() < clientBlockSize) {
						clientCache.cache.add(reqBlock);
						clientCache.pattern_index.add(pattern_num);
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
									updateLocalCacheNChance(reqBlock,
											pattern_num);
								}

								else {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheNChance(
											reqBlock, pattern_num);
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
									Object[] localCacheUpdated = updateLocalCacheNChance(
											reqBlock, pattern_num);
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													(ArrayList<Integer>) localCacheUpdated[1],
													logFile);

								} else {
									this.NeighborcacheHits++;
									updateLocalCacheNChance(reqBlock,
											pattern_num);
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

	/**
	 * 
	 * @param reqBlock Data block being requested
	 * @param p
	 * @return
	 */
	private synchronized Object[] updateLocalCacheNChance(int reqBlock, int p) {
		ArrayList<Double> x = new ArrayList<Double>();
		Object[] returnValues = new Object[3];
		ArrayList<Integer> allClients = new ArrayList<Integer>();
		int numClients = SimulatorContentManager.allClients.length;

		for (int i = 0; i < numClients; i++) {
			allClients.add(SimulatorContentManager.allClients[i].clientNumber);
		}

		int indexClient = allClients.indexOf(this.clientNumber);
		allClients.remove(indexClient);

		synchronized (SimulatorContentManager.lock) {
			ListIterator<Integer> iter = clientCache.pattern_index
					.listIterator();

			int whatToRemove = clientCache.cache.get(0);
			int pat_index = clientCache.pattern_index.get(0);

			logFile.writeToFile(this.clientNumber, "What to Remove: "
					+ whatToRemove);
			logFile.writeToFile(this.clientNumber, "Pattern: " + pat_index);

			synchronized (clientCache.pattern_index) {
				synchronized (SimulatorContentManager.allClients) {
					logFile.writeToFile(this.clientNumber, ""
							+ this.clientCache.cache);
					ArrayList<Integer> exists = new ArrayList<Integer>();
					ArrayList<Integer> block = new ArrayList<Integer>();
					ArrayList<Integer> block_p = new ArrayList<Integer>();

					while (iter.hasNext()) {
						if (pat_index == iter.next()) {

							logFile.writeToFile(this.clientNumber, "Cache: "
									+ clientCache.cache);
							logFile.writeToFile(this.clientNumber,
									"Pattern Array: "
											+ clientCache.pattern_index);

							int p_in = iter.nextIndex() - 1;

							int e = clientCache.cache.get(p_in);

							logFile.writeToFile(this.clientNumber, "element: "
									+ e);
							logFile.writeToFile(this.clientNumber,
									"pattern: index " + p_in);

							int a = SimulatorContentManager
									.searchNeighborCache(this.clientNumber, e,
											logFile);
							exists.add(a);
							block.add(e);
							block_p.add(pat_index);
						}
					}

					ArrayList<Integer> newP = new ArrayList<Integer>();
					for (int i = 0; i < block.size(); i++) {
						int v = block.get(i);
						newP.add(v);
					}

					if (!SimulatorContentManager.frequencyCounter
							.containsKey(newP)) {
						SimulatorContentManager.frequencyCounter.put(newP, 1);
					} else {
						int value = SimulatorContentManager.frequencyCounter
								.get(newP);
						SimulatorContentManager.frequencyCounter.put(newP,
								value + 1);
					}

					int freq = SimulatorContentManager.frequencyCounter
							.get(newP);

					if (!SimulatorContentManager.blockArrayCounter
							.containsKey(newP)) {
						SimulatorContentManager.blockArrayCounter.put(newP, 3);
					}

					Integer[] temp = new Integer[2];

					if (exists.contains(-1)) {
						int r = new Random().nextInt(allClients.size());
						int whereTosendTo = allClients.get(r);
						logFile.writeToFile(this.clientNumber, "unique");
						logFile.writeToFile(this.clientNumber, "c " + exists);
						logFile.writeToFile(this.clientNumber, "b " + block);
						logFile.writeToFile(this.clientNumber, "ppp " + block_p);

						if (SimulatorContentManager.blockArrayCounter.get(newP) > 0) {
							for (int b : block) {
								int in = clientCache.cache.indexOf(b);
								int p1 = clientCache.pattern_index.get(in);

								temp[0] = b;
								temp[1] = p1;

								if (!SimulatorContentManager.blockArrayCounter
										.containsKey(newP)) {
									SimulatorContentManager.blockArrayCounter
											.put(newP, 3);
								}

								SimulatorContentManager.allClients[whereTosendTo]
										.updateLocalCacheFIFO(b, pat_index);

								logFile.writeToFile(this.clientNumber,
										"Adding " + b + " to " + whereTosendTo);

								clientCache.cache.remove(in);
								clientCache.pattern_index.remove(in);

							}

							int value = SimulatorContentManager.blockArrayCounter
									.get(newP);
							SimulatorContentManager.blockArrayCounter.put(newP,
									value - 1);
						}

						else {
							SimulatorContentManager.blockArrayCounter
									.remove(newP);
							for (int b : block) {
								int in = clientCache.cache.indexOf(b);
								int p1 = clientCache.pattern_index.get(in);

								logFile.writeToFile(this.clientNumber,
										"Survived enough times!");
								clientCache.cache.remove(in);
								clientCache.pattern_index.remove(in);
							}
						}
					}

					else {
						logFile.writeToFile(this.clientNumber, "not unique");
						logFile.writeToFile(this.clientNumber, "p " + exists);
						logFile.writeToFile(this.clientNumber, "b " + block);
						for (int b : block) {
							int in = clientCache.cache.indexOf(b);
							clientCache.cache.remove(in);
							clientCache.pattern_index.remove(in);
						}
					}
				}
			}

			if (clientCache.cache.size() < clientBlockSize) {
				clientCache.cache.add(reqBlock);
				clientCache.pattern_index.add(p);
				logFile.writeToFile(this.clientNumber, "XXXXXXXXXXXXXXX "
						+ clientCache.cache.indexOf(reqBlock));
				logFile.writeToFile(this.clientNumber, "YYYYYYYYYYYYYYY "
						+ clientCache.pattern_index.indexOf(p));
				logFile.writeToFile(this.clientNumber, "Cache: "
						+ clientCache.cache);
				logFile.writeToFile(this.clientNumber, "Pattern Array: "
						+ clientCache.pattern_index);

			}

			returnValues[0] = SimulatorContentManager.blockArrayCounter;
			returnValues[1] = clientCache.cache;
		}

		return returnValues;

	}

	/**
	 * Implements NChance-K replacement strategy
	 */
	private synchronized void simNChanceK() {
		List<Integer> client_data = this.getClientCache();
		SimulatorContentManager.lookUpTable.put(this.clientNumber, client_data);
		BufferedReader reader;

		try {
			int c = 0;
			String line = null;
			Scanner scanner = null;

			reader = new BufferedReader(new FileReader(
					"/Users/rajashreemaiya/Documents/workspace/Cache-Simulator-1/Patterns/"
							+ SimulatorConstants.FILEPREFIX
							+ SimulatorConstants.CLIENTS + "_"
							+ this.clientNumber + ".csv"));

			while ((line = reader.readLine()) != null) {
				int numReq = updateDataReqCount();
				scanner = new Scanner(line);
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {

					String data = scanner.next();
					String[] parts = data.split(",");
					int reqBlock = Integer.parseInt(parts[0]);
					int pattern_num = Integer.parseInt(parts[1]);
					Integer[] counter = new Integer[2];
					counter[0] = reqBlock;
					counter[1] = pattern_num;

					logFile.writeToFile(this.clientNumber, "\n");
					logFile.writeToFile(this.clientNumber, "Iteration: " + c);
					logFile.writeToFile(this.clientNumber, "Client "
							+ this.clientNumber + " is requesting the data "
							+ reqBlock);

					logFile.writeToFile(this.clientNumber, "Requests " + numReq);

					if (clientCache.cache.size() < clientBlockSize) {
						clientCache.cache.add(reqBlock);
						clientCache.pattern_index.add(pattern_num);
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
									updateLocalCacheNChanceK(reqBlock,
											pattern_num, numReq);
								}

								else {
									goToServer(this.clientNumber, reqBlock);
									Object[] localCacheUpdated = updateLocalCacheNChanceK(
											reqBlock, pattern_num, numReq);
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
									Object[] localCacheUpdated = updateLocalCacheNChanceK(
											reqBlock, pattern_num, numReq);
									SimulatorContentManager
											.updateContentManager(
													this.clientNumber,
													reqBlock,
													(ArrayList<Integer>) localCacheUpdated[1],
													logFile);

								} else {
									this.NeighborcacheHits++;
									updateLocalCacheNChanceK(reqBlock,
											pattern_num, numReq);
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

	/**
	 * 
	 * @param reqBlock Data block being requested
	 * @param p
	 * @param requests
	 * @return
	 */
	private synchronized Object[] updateLocalCacheNChanceK(int reqBlock, int p,
			int requests) {
		ArrayList<Double> x = new ArrayList<Double>();
		Object[] returnValues = new Object[3];
		ArrayList<Integer> allClients = new ArrayList<Integer>();
		int numClients = SimulatorContentManager.allClients.length;

		for (int i = 0; i < numClients; i++) {
			allClients.add(SimulatorContentManager.allClients[i].clientNumber);
		}

		int indexClient = allClients.indexOf(this.clientNumber);
		allClients.remove(indexClient);

		synchronized (SimulatorContentManager.lock) {
			ListIterator<Integer> iter = clientCache.pattern_index
					.listIterator();

			int whatToRemove = clientCache.cache.get(0);
			int pat_index = clientCache.pattern_index.get(0);

			logFile.writeToFile(this.clientNumber, "What to Remove: "
					+ whatToRemove);
			logFile.writeToFile(this.clientNumber, "Pattern: " + pat_index);

			synchronized (clientCache.pattern_index) {
				synchronized (SimulatorContentManager.allClients) {
					logFile.writeToFile(this.clientNumber, ""
							+ this.clientCache.cache);
					ArrayList<Integer> exists = new ArrayList<Integer>();
					ArrayList<Integer> block = new ArrayList<Integer>();
					ArrayList<Integer> block_p = new ArrayList<Integer>();

					while (iter.hasNext()) {
						if (pat_index == iter.next()) {

							logFile.writeToFile(this.clientNumber, "Cache: "
									+ clientCache.cache);
							logFile.writeToFile(this.clientNumber,
									"Pattern Array: "
											+ clientCache.pattern_index);

							int p_in = iter.nextIndex() - 1;

							int e = clientCache.cache.get(p_in);

							logFile.writeToFile(this.clientNumber, "element: "
									+ e);
							logFile.writeToFile(this.clientNumber,
									"pattern: index " + p_in);

							int a = SimulatorContentManager
									.searchNeighborCache(this.clientNumber, e,
											logFile);
							exists.add(a);
							block.add(e);
							block_p.add(pat_index);
						}
					}

					ArrayList<Integer> newP = new ArrayList<Integer>();
					for (int i = 0; i < block.size(); i++) {
						int v = block.get(i);
						newP.add(v);
					}

					if (!SimulatorContentManager.frequencyCounter
							.containsKey(newP)) {
						SimulatorContentManager.frequencyCounter.put(newP, 1);
					} else {
						int value = SimulatorContentManager.frequencyCounter
								.get(newP);
						SimulatorContentManager.frequencyCounter.put(newP,
								value + 1);
					}

					int freq = SimulatorContentManager.frequencyCounter
							.get(newP);
					logFile.writeToFile(this.clientNumber, freq + " "
							+ requests + " " + ((double) freq) / requests);

					double occ = freq / (double) requests;
					DecimalFormat df = new DecimalFormat("#");
					df.setMaximumFractionDigits(5);
					double formatted = Double.parseDouble(df.format(occ));

					if (!SimulatorContentManager.blockArrayCounter
							.containsKey(newP)) {
						if (formatted > 0.3) {
							SimulatorContentManager.blockArrayCounter.put(newP,
									5);
						}

						else if (formatted > 0.1 && formatted < 0.3) {
							SimulatorContentManager.blockArrayCounter.put(newP,
									4);
						} else {
							SimulatorContentManager.blockArrayCounter.put(newP,
									3);
						}
					}

					Integer[] temp = new Integer[2];

					if (exists.contains(-1)) {
						int r = new Random().nextInt(allClients.size());
						int whereTosendTo = allClients.get(r);
						logFile.writeToFile(this.clientNumber, "unique");
						logFile.writeToFile(this.clientNumber, "c " + exists);
						logFile.writeToFile(this.clientNumber, "b " + block);
						logFile.writeToFile(this.clientNumber, "ppp " + block_p);

						if (SimulatorContentManager.blockArrayCounter.get(newP) > 0) {
							for (int b : block) {
								int in = clientCache.cache.indexOf(b);
								int p1 = clientCache.pattern_index.get(in);

								temp[0] = b;
								temp[1] = p1;

								if (!SimulatorContentManager.blockArrayCounter
										.containsKey(newP)) {
									SimulatorContentManager.blockArrayCounter
											.put(newP, 3);
								}

								SimulatorContentManager.allClients[whereTosendTo]
										.updateLocalCacheFIFO(b, pat_index);

								logFile.writeToFile(this.clientNumber,
										"Adding " + b + " to " + whereTosendTo);

								clientCache.cache.remove(in);
								clientCache.pattern_index.remove(in);

							}

							int value = SimulatorContentManager.blockArrayCounter
									.get(newP);
							SimulatorContentManager.blockArrayCounter.put(newP,
									value - 1);
						}

						else {
							SimulatorContentManager.blockArrayCounter
									.remove(newP);
							for (int b : block) {
								int in = clientCache.cache.indexOf(b);
								int p1 = clientCache.pattern_index.get(in);

								logFile.writeToFile(this.clientNumber,
										"Survived enough times!");
								clientCache.cache.remove(in);
								clientCache.pattern_index.remove(in);
							}
						}
					}

					else {
						logFile.writeToFile(this.clientNumber, "not unique");
						logFile.writeToFile(this.clientNumber, "p " + exists);
						logFile.writeToFile(this.clientNumber, "b " + block);
						for (int b : block) {
							int in = clientCache.cache.indexOf(b);
							clientCache.cache.remove(in);
							clientCache.pattern_index.remove(in);
						}
					}
				}
			}

			if (clientCache.cache.size() < clientBlockSize) {
				clientCache.cache.add(reqBlock);
				clientCache.pattern_index.add(p);
				logFile.writeToFile(this.clientNumber, "XXXXXXXXXXXXXXX "
						+ clientCache.cache.indexOf(reqBlock));
				logFile.writeToFile(this.clientNumber, "YYYYYYYYYYYYYYY "
						+ clientCache.pattern_index.indexOf(p));
				logFile.writeToFile(this.clientNumber, "Cache: "
						+ clientCache.cache);
				logFile.writeToFile(this.clientNumber, "Pattern Array: "
						+ clientCache.pattern_index);

			}

			returnValues[0] = SimulatorContentManager.blockArrayCounter;
			returnValues[1] = clientCache.cache;
		}

		return returnValues;

	}
}
