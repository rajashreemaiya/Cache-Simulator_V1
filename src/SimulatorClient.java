import java.util.*;

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
	static int clientExecCounter = 0;
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
		this.fillInitialCache();
		SimulatorContentManager.lookUpTable
				.put(clientNumber, clientCache.cache);
	}

	private void fillInitialCache() {
		Random r = new Random();
		for(int i=0;i<clientBlockSize;i++) {
			int value = r.nextInt(100)+1;
			clientCache.cache.add(value);
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
		/**
		 * TODO: Algorithms are started here This will have some log messages
		 * and beginning of algorithm execution.
		 */

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

		clientExecCounter++;
	}

	/**
	 * Implements LRU replacement policy
	 */
	private void SimLRU() {

		Random random = new Random();

		int accessCounter = 0;
		ArrayList<Integer> indexArray = new ArrayList<Integer>();
		ArrayList<Integer> client_data = this.getClientCache();
		logFile.writeToFile(this.clientNumber, "Cache is:.." + client_data);

		SimulatorContentManager.lookUpTable.put(this.clientNumber, client_data);
		int i = 0;
		
		for(i=0;i<clientBlockSize;i++) {
			indexArray.add(0);
		}

		for (int j = 0; j < 100; j++) {
			int reqBlock = random.nextInt(200) + 1;
			logFile.writeToFile(this.clientNumber, "\n");
			logFile.writeToFile(this.clientNumber, "Cache is:.."
					+ clientCache.cache);
			logFile.writeToFile(this.clientNumber, "Cache size:.."
					+ clientCache.cache.size());

			logFile.writeToFile(this.clientNumber, "Client "
					+ this.clientNumber + " is requesting the data " + reqBlock);
			
				logFile.writeToFile(this.clientNumber, "Access counter is "
						+ accessCounter);
				logFile.writeToFile(this.clientNumber, "-----" + indexArray);
//				System.out.println("Access counter is " + accessCounter);
				boolean found = false;

				if (searchLocalCache(reqBlock) == true) {
					int elmentIndex = clientCache.cache.indexOf(reqBlock);
					this.LocalcacheHits++;
					mytickCount.setTickCount(SimulatorConstants.SEARCH);
					accessCounter = accessCounter + 1;
					indexArray.set(elmentIndex, accessCounter);
					
					logFile.writeToFile(this.clientNumber, "Index array: "
							+ indexArray);
					found = true;
				}

				else {
					mytickCount
							.setTickCount(SimulatorConstants.TOCONTEXTMANAGER);
					mytickCount
							.setTickCount(SimulatorConstants.FROMCONTEXTMANAGER);
					int whoHasBlock = SimulatorContentManager
							.searchNeighborCache(this.clientNumber, reqBlock,
									logFile);
					if (whoHasBlock != -1) {
						int isBlockThere = checkForFalseHits(reqBlock,
								this.clientNumber, whoHasBlock);
						if (isBlockThere != -1) {
							getDataFromNeighbor(whoHasBlock, this.clientNumber,
									reqBlock);
							this.NeighborcacheHits++;
							Object[] localCacheUpdated = updateLocalCacheLRU(
									reqBlock, indexArray, accessCounter);
							accessCounter = (int) localCacheUpdated[1];
							indexArray = (ArrayList<Integer>) localCacheUpdated[0];
							found = true;
						}

						else {
							goToServer(this.clientNumber, reqBlock);
							Object[] localCacheUpdated = updateLocalCacheLRU(
									reqBlock, indexArray, accessCounter);
							accessCounter = (int) localCacheUpdated[1];
							indexArray = (ArrayList<Integer>) localCacheUpdated[0];
							SimulatorContentManager.updateContentManager(
									this.clientNumber, reqBlock,
									(ArrayList<Integer>) localCacheUpdated[2],
									logFile);
							found = true;
						}
					}

					else {

						int checkFalseMiss = checkForFalseMiss(reqBlock,
								this.clientNumber);
						if (checkFalseMiss == -1) {
							goToServer(this.clientNumber, reqBlock);
							Object[] localCacheUpdated = updateLocalCacheLRU(
									reqBlock, indexArray, accessCounter);
							accessCounter = (int) localCacheUpdated[1];
							indexArray = (ArrayList<Integer>) localCacheUpdated[0];
							SimulatorContentManager.updateContentManager(
									this.clientNumber, reqBlock,
									(ArrayList<Integer>) localCacheUpdated[2],
									logFile);
						} else {
							this.NeighborcacheHits++;
							Object[] localCacheUpdated = updateLocalCacheLRU(
									reqBlock, indexArray, accessCounter);
							accessCounter = (int) localCacheUpdated[1];
							indexArray = (ArrayList<Integer>) localCacheUpdated[0];
						}
						found = true;
					}
				}
			}
//		}
		logFile.writeToFile(this.clientNumber, "ticks for " + this.clientNumber
				+ " is  " + mytickCount.getTickCount());
		logFile.writeToFile(this.clientNumber, "\n");
	}

	private Object[] updateLocalCacheLRU(int reqBlock,
			ArrayList<Integer> indexArray, int accessCounter) {

		Object[] returnValues = new Object[3];
		logFile.writeToFile(this.clientNumber, "Cache is:.."
				+ clientCache.cache);

		logFile.writeToFile(this.clientNumber, "Replacing..");

		int minIndex = -1;
		for (int i = 0; i < indexArray.size(); i++) {
			minIndex = indexArray.indexOf(Collections.min(indexArray));
		}
		
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

	private ArrayList<Integer> getClientCache() {
		return clientCache.cache;
	}

	/**
	 * Implements Random replacement policy
	 * */
	private void SimCacheRandomReplacement() {

		int clientNum = this.clientNumber;
		ArrayList<Integer> client_data = this.getClientCache();
		SimulatorContentManager.lookUpTable.put(this.clientNumber, client_data);

		logFile.writeToFile(this.clientNumber, "lookup table: "
				+ SimulatorContentManager.lookUpTable);

		Random random = new Random();
		boolean found = false;
		for (int j = 0; j < 25; j++) {
			int reqBlock = random.nextInt(50) + 1;
			logFile.writeToFile(this.clientNumber, "\n");
			logFile.writeToFile(this.clientNumber, "Client "
					+ this.clientNumber + " is requesting the data " + reqBlock);

			if (clientCache.cache.size() < this.clientBlockSize
					&& (!clientCache.cache.contains(reqBlock))) {
				clientCache.cache.add(reqBlock);
			}

			else {

				if (searchLocalCache(reqBlock) == true) {
					this.LocalcacheHits++;
					mytickCount.setTickCount(SimulatorConstants.SEARCH);
					found = true;
				}

				else {
					mytickCount
							.setTickCount(SimulatorConstants.TOCONTEXTMANAGER);
					mytickCount
							.setTickCount(SimulatorConstants.FROMCONTEXTMANAGER);
					int whoHasBlock = SimulatorContentManager
							.searchNeighborCache(clientNum, reqBlock, logFile);
					if (whoHasBlock != -1) {
						logFile.writeToFile(clientNum, "Found in neighbor: "
								+ whoHasBlock);
						int isBlockThere = checkForFalseHits(reqBlock,
								clientNum, whoHasBlock);
						if (isBlockThere != -1) {
							getDataFromNeighbor(whoHasBlock, clientNum,
									reqBlock);
							logFile.writeToFile(clientNum, "Got data from: "
									+ whoHasBlock);
							this.NeighborcacheHits++;
							updateLocalCache(reqBlock);
							found = true;
						}

						else {
							goToServer(clientNum, reqBlock);
							ArrayList<Integer> localCacheUpdated = updateLocalCache(reqBlock);
							SimulatorContentManager.updateContentManager(
									clientNum, reqBlock, localCacheUpdated,
									logFile);
							found = true;
						}
					}

					else {

						int checkFalseMiss = checkForFalseMiss(reqBlock,
								clientNum);
						if (checkFalseMiss == -1) {
							goToServer(clientNum, reqBlock);
							ArrayList<Integer> localCacheUpdated = updateLocalCache(reqBlock);
							SimulatorContentManager.updateContentManager(
									clientNum, reqBlock, localCacheUpdated,
									logFile);
						} else {
							this.NeighborcacheHits++;
							updateLocalCache(reqBlock);
						}
						found = true;
					}
				}
			}
		}
	}

	private ArrayList<Integer> updateLocalCache(int req_data) {
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
		/**
		 * TODO: This is for the input from trace file
		 * */
		Random random = new Random();

		ArrayList<Integer> client_data = this.getClientCache();
		logFile.writeToFile(this.clientNumber, "Cache is:.." + client_data);

		SimulatorContentManager.lookUpTable.put(this.clientNumber, client_data);

		for (int j = 0; j < 100; j++) {
			int reqBlock = random.nextInt(200) + 1;
			logFile.writeToFile(this.clientNumber, "\n");

			logFile.writeToFile(this.clientNumber, "Client "
					+ this.clientNumber + " is requesting the data " + reqBlock);


			if (clientCache.cache.size() < this.clientBlockSize
					&& !(clientCache.cache.contains(reqBlock))) {
				LocalcacheHits++;
				logFile.writeToFile(this.clientNumber,
						"Filling initial cache...");
				clientCache.cache.add(reqBlock);
			}

			else {
				boolean found = false;
				if (searchLocalCache(reqBlock) == true) {
					this.LocalcacheHits++;
					mytickCount.setTickCount(SimulatorConstants.SEARCH);
					found = true;
				}

				else {
					mytickCount
							.setTickCount(SimulatorConstants.TOCONTEXTMANAGER);
					mytickCount
							.setTickCount(SimulatorConstants.FROMCONTEXTMANAGER);
					int whoHasBlock = SimulatorContentManager
							.searchNeighborCache(this.clientNumber, reqBlock,
									logFile);
					if (whoHasBlock != -1) {
						int isBlockThere = checkForFalseHits(reqBlock,
								this.clientNumber, whoHasBlock);
						if (isBlockThere != -1) {
							getDataFromNeighbor(whoHasBlock, this.clientNumber,
									reqBlock);
							this.NeighborcacheHits++;
							updateLocalCacheFIFO(reqBlock);
							found = true;
						}

						else {
							goToServer(this.clientNumber, reqBlock);
							ArrayList<Integer> localCacheUpdated = updateLocalCacheFIFO(reqBlock);
							SimulatorContentManager.updateContentManager(
									this.clientNumber, reqBlock,
									localCacheUpdated, logFile);
							found = true;
						}
					}

					else {

						int checkFalseMiss = checkForFalseMiss(reqBlock,
								this.clientNumber);
						if (checkFalseMiss == -1) {
							goToServer(this.clientNumber, reqBlock);
							ArrayList<Integer> localCacheUpdated = updateLocalCacheFIFO(reqBlock);
							SimulatorContentManager.updateContentManager(
									this.clientNumber, reqBlock,
									localCacheUpdated, logFile);
						} else {
							this.NeighborcacheHits++;
							updateLocalCacheFIFO(reqBlock);
						}
						found = true;
					}
				}
			}
		}
		logFile.writeToFile(this.clientNumber, "ticks for " + this.clientNumber
				+ " is  " + mytickCount.getTickCount());
		logFile.writeToFile(this.clientNumber, "\n");
	}

	private ArrayList<Integer> updateLocalCacheFIFO(int reqBlock) {
		if (clientCache.cache.size() < clientBlockSize) {
			clientCache.cache.add(reqBlock);
		}

		else {
			logFile.writeToFile(this.clientNumber, "Cache is:.."
					+ clientCache.cache);

			logFile.writeToFile(this.clientNumber, "Replacing..");

			logFile.writeToFile(this.clientNumber, "Removing..."
					+ clientCache.cache.get(0));

			clientCache.cache.remove(0);
			logFile.writeToFile(this.clientNumber, "Now the size is: "
					+ clientCache.cache.size());

			logFile.writeToFile(this.clientNumber, "Cache is: "
					+ clientCache.cache);

			clientCache.cache.add(reqBlock);
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
				mytickCount);
	}

	// TODO: // ------------- N chance
	//
	private void simNChance() {
		/**
		 * TODO: This is for the input from trace file
		 * */

		int c = 0;
		int n = 3;
		HashMap<Integer, Integer> recirculationArray = new HashMap<Integer, Integer>();
		Random random = new Random();

		ArrayList<Integer> client_data = this.getClientCache();
		logFile.writeToFile(this.clientNumber, "Cache is:.." + client_data);

		SimulatorContentManager.lookUpTable.put(this.clientNumber, client_data);

		for (int j = 0; j < 25; j++) {
			c++;
			int reqBlock = random.nextInt(9) + 1;
			System.out.println();
			logFile.writeToFile(this.clientNumber, "\n");
			logFile.writeToFile(this.clientNumber, "Iteration: " + c);
			System.out.println("Client " + this.clientNumber
					+ " is requesting the data " + reqBlock);
			logFile.writeToFile(this.clientNumber, "Client "
					+ this.clientNumber + " is requesting the data " + reqBlock);

			System.out.println("Client size " + this.clientCache.cache.size());
			recirculationArray.put(reqBlock, 3);
			if (clientCache.cache.size() < this.clientBlockSize
					&& (!clientCache.cache.contains(reqBlock))) {
				System.out.println("Filling initial cache...");
				logFile.writeToFile(this.clientNumber,
						"Filling initial cache...");
				clientCache.cache.add(reqBlock);
				System.out.println(clientCache.getCache());
				logFile.writeToFile(this.clientNumber,
						"" + clientCache.getCache());
			}

			else {
				boolean found = false;
				if (searchLocalCache(reqBlock) == true) {
					this.LocalcacheHits++;
					mytickCount.setTickCount(SimulatorConstants.SEARCH);
					found = true;
				}

				else {
					mytickCount
							.setTickCount(SimulatorConstants.TOCONTEXTMANAGER);
					mytickCount
							.setTickCount(SimulatorConstants.FROMCONTEXTMANAGER);
					int whoHasBlock = SimulatorContentManager
							.searchNeighborCache(this.clientNumber, reqBlock,
									logFile);
					if (whoHasBlock != -1) {
						int isBlockThere = checkForFalseHits(reqBlock,
								this.clientNumber, whoHasBlock);
						if (isBlockThere != -1) {
							getDataFromNeighbor(whoHasBlock, this.clientNumber,
									reqBlock);
							this.NeighborcacheHits++;
							updateLocalCacheNChance(reqBlock,
									recirculationArray, n);
							System.out.println("N Chance..... "
									+ clientCache.cache);
							logFile.writeToFile(this.clientNumber,
									"N Chance..... " + clientCache.cache);

							found = true;
						}

						else {
							goToServer(this.clientNumber, reqBlock);
							Object[] localCacheUpdated = updateLocalCacheNChance(
									reqBlock, recirculationArray, n);
							SimulatorContentManager.updateContentManager(
									this.clientNumber, reqBlock,
									(ArrayList<Integer>) localCacheUpdated[1],
									logFile);
							System.out.println("After Update "
									+ SimulatorContentManager.lookUpTable);
							found = true;
						}
					}

					else {

						int checkFalseMiss = checkForFalseMiss(reqBlock,
								this.clientNumber);
						if (checkFalseMiss == -1) {
							goToServer(this.clientNumber, reqBlock);
							Object[] localCacheUpdated = updateLocalCacheNChance(
									reqBlock, recirculationArray, n);
							SimulatorContentManager.updateContentManager(
									this.clientNumber, reqBlock,
									(ArrayList<Integer>) localCacheUpdated[1],
									logFile);
						} else {
							this.NeighborcacheHits++;
							updateLocalCacheNChance(reqBlock,
									recirculationArray, n);
						}
						found = true;
					}
				}
			}
		}
		logFile.writeToFile(this.clientNumber, "ticks for " + this.clientNumber
				+ " is  " + mytickCount.getTickCount());
		logFile.writeToFile(this.clientNumber, "\n");
	}

	synchronized private Object[] updateLocalCacheNChance(int reqBlock,
			HashMap<Integer, Integer> recircArray, int n) {

		logFile.writeToFile(this.clientNumber, "" + SimulatorContentManager.lookUpTable);
		boolean isUnique = false;
		Object[] returnValues = new Object[3];
		recircArray.put(reqBlock, n);
		logFile.writeToFile(this.clientNumber, "Replacing for N Chance");
		logFile.writeToFile(this.clientNumber, "arr " + recircArray);
		for (int i = 0; i < SimulatorContentManager.allClients.length; i++) {
			for (int j = 0; j < clientCache.cache.size(); j++) {
				if (this.clientNumber != i) {
					if (SimulatorContentManager.allClients[i].getClientCache()
							.contains(clientCache.cache.get(j))) {
						logFile.writeToFile(this.clientNumber, i
								+ " -client scan");
						logFile.writeToFile(
								this.clientNumber,
								"in "
										+ SimulatorContentManager.allClients[i].clientNumber
										+ " client");
						isUnique = false;
					} else {
						logFile.writeToFile(this.clientNumber, i
								+ " -client scan");
						logFile.writeToFile(
								this.clientNumber,
								"in "
										+ SimulatorContentManager.allClients[i].clientNumber
										+ " client");
						isUnique = true;
					}

					if (isUnique) {
						logFile.writeToFile(this.clientNumber,
								clientCache.cache.get(j) + " is a singlet");
						Random r = new Random();
						int whereTosendTo = r
								.nextInt(SimulatorContentManager.allClients.length - 1) + 1;
						logFile.writeToFile(this.clientNumber,
								"Fowarding to..  " + whereTosendTo);
						logFile.writeToFile(this.clientNumber,
								"My cache was:  " + clientCache.cache);
						updateLocalCacheFIFO(whereTosendTo, reqBlock);
						clientCache.cache.set(j, reqBlock);
						logFile.writeToFile(this.clientNumber, "My cache is:  "
								+ clientCache.cache);
						logFile.writeToFile(
								this.clientNumber,
								"Where it was sent to cache:  "
										+ SimulatorContentManager.allClients[whereTosendTo]
												.getClientCache());
						int value = recircArray.get(reqBlock);
						value = value - 1;
						recircArray.put(reqBlock, value);
						logFile.writeToFile(this.clientNumber, "arr "
								+ recircArray);
						System.out.println("-----------: " + recircArray);
						returnValues[0] = recircArray;
						returnValues[1] = clientCache.cache;

					}

					else {
						clientCache.cache.set(j, reqBlock);
						returnValues[0] = recircArray;
						returnValues[1] = clientCache.cache;
					}

					return returnValues;
				}
			}
			
			continue;
		}
		return returnValues;
	}

	synchronized private void updateLocalCacheFIFO(int forwardTo, int reqBlock) {
		logFile.writeToFile(
				this.clientNumber,
				forwardTo
						+ " cache is:  "
						+ SimulatorContentManager.allClients[forwardTo]
								.getClientCache());

		if (clientCache.cache.size() < clientBlockSize) {
			System.out.println("Adding...");
			SimulatorContentManager.allClients[forwardTo].clientCache.cache
					.add(reqBlock);
			int clientId = SimulatorContentManager.allClients[forwardTo].clientNumber;
			ArrayList<Integer> localCacheUpdated = SimulatorContentManager.allClients[forwardTo]
					.getClientCache();
			SimulatorContentManager.updateContentManager(clientId, reqBlock,
					localCacheUpdated, logFile);
		} else {

			SimulatorContentManager.allClients[forwardTo].getClientCache()
					.remove(0);
			SimulatorContentManager.allClients[forwardTo].clientCache.cache
					.add(reqBlock);
			int clientId = SimulatorContentManager.allClients[forwardTo].clientNumber;
			ArrayList<Integer> localCacheUpdated = SimulatorContentManager.allClients[forwardTo]
					.getClientCache();
			SimulatorContentManager.updateContentManager(clientId, reqBlock,
					localCacheUpdated, logFile);
		}
	}

}
