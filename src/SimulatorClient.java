import java.util.ArrayList;
import java.util.Random;

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
	static int clientCacheCounter = 0;
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
		this.fillClientCache();
		SimulatorContentManager.lookUpTable
				.put(clientNumber, clientCache.cache);
	}

	public int setBlockSize(int blockSize) {
		return this.clientBlockSize = blockSize;
	}

	public int getBlockSize() {
		return clientBlockSize;
	}

	/**
	 * Fill client cache with random numbers
	 * */
	private ArrayList<Integer> fillClientCache() {
		Random random = new Random();
		for (int i = 0; i < clientBlockSize; i++) {
			int value = random.nextInt(50) + 1;
			clientCache.cache.add(value);
		}
		return clientCache.cache;

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
		
//		this.SimFifo();
//		this.SimNChance();
//		this.SimLRU();
		
		this.logFile = new SimulatorLogger(this.clientNumber);
		logFile.writeToFile(this.clientNumber, "System cache size: "
				+ SimulatorServer.serverCacheSize);
		logFile.writeToFile(this.clientNumber, "Client " + this.clientNumber);
		logFile.writeToFile(this.clientNumber, "Client cache size: "
				+ this.clientBlockSize);
		System.out.println();

		this.generateRequestData();

		logFile.writeToFile(this.clientNumber, "Local Cache Hits: "
				+ this.LocalcacheHits);
		logFile.writeToFile(this.clientNumber, "System Cache Hits: "
				+ this.NeighborcacheHits);
		logFile.writeToFile(this.clientNumber, "Cache Miss: " + this.cacheMiss);

		logFile.writeToFile(this.clientNumber, "Tick counts table: ");
		for(int k=0;k<clientTickCounts.length;k++)
			logFile.writeToFile(this.clientNumber, "For request "+ k + ": " + clientTickCounts[k]);
	}

	private void generateRequestData() {
		/**
		 * TODO: This is for the input from trace file
		 * */

		/*
		 * Scanner scanner; try { scanner = new Scanner(new
		 * File("psuedodata.txt")); ArrayList<Integer> inputData = new
		 * ArrayList<Integer>(); while(scanner.hasNext()){
		 * inputData.add(Integer.parseInt(scanner.next())); } scanner.close();
		 * for(int i=0;i<inputData.size();i++) { int reqBlock = inputData.get(i);
		 * boolean answer = this.request_data(this.clientNumber,reqBlock);
		 * logFile.writeToFile(this.clientNumber,"Counter: " + i);
		 * System.out.println(answer); this.printClientCache();
		 * System.out.println(); System.out.println("After Update " +
		 * SimulatorContentManager.lookUpTable);
		 * logFile.writeToFile(this.clientNumber,"After Update " +
		 * SimulatorContentManager.lookUpTable);
		 * logFile.writeToFile(this.clientNumber,"\n"); System.out.println(); }
		 * } catch (FileNotFoundException e) { e.printStackTrace(); }
		 */

		/*
		 * This is for testing with dummy data
		 */

		ArrayList<Integer> client_data = this.getClientCache();
		SimulatorContentManager.lookUpTable.put(this.clientNumber, client_data);
		System.out.println("lookup table: "
				+ SimulatorContentManager.lookUpTable);
		logFile.writeToFile(this.clientNumber, "lookup table: "
				+ SimulatorContentManager.lookUpTable);

		Random random = new Random();
		
		for (int i = 0; i < 15; i++) {
			mytickCount.tickCount = 0;
			int reqBlock = random.nextInt(9) + 1;
			logFile.writeToFile(this.clientNumber,"lookup table: "
					+ SimulatorContentManager.lookUpTable);
			boolean answer = this.request_data(this.clientNumber, reqBlock);
			logFile.writeToFile(this.clientNumber, "Counter: " + i);
			logFile.writeToFile(this.clientNumber,"lookup table: "
					+ SimulatorContentManager.lookUpTable);
			System.out.println(answer);
			this.printClientCache();
			System.out.println();
			clientTickCounts[i] = mytickCount.getTickCount();
			logFile.writeToFile(this.clientNumber, "ticks for " + i + " is  "
					+ mytickCount.getTickCount());
			logFile.writeToFile(this.clientNumber, "\n");
			System.out.println();
		}
	}

	private ArrayList<Integer> getClientCache() {
		return clientCache.cache;
	}

	/**
	 * Method that says where the data was fetched from, Always returns true
	 * since server always has the data
	 * */
	private boolean request_data(int clientNum, int reqBlock) {
		System.out.println("Client " + clientNum + " is requesting the data "
				+ reqBlock);
		logFile.writeToFile(clientNum, "Client " + clientNum
				+ " is requesting the data " + reqBlock);
		boolean found = false;

		if (searchLocalCache(reqBlock) == true) {
			this.LocalcacheHits++;
			mytickCount.setTickCount(SimulatorConstants.SEARCH);
			found = true;
			return found;
		}

		else {
			mytickCount.setTickCount(SimulatorConstants.CONTEXTMANAGERCOMM);
			int whoHasBlock = SimulatorContentManager.searchNeighborCache(
					clientNum, reqBlock, logFile);
			if (whoHasBlock != -1) {
				logFile.writeToFile(clientNum, "Found in neighbor: "
						+ whoHasBlock);
				int isBlockThere = checkForFalseHits(reqBlock, clientNum, whoHasBlock);
				if(isBlockThere != -1) {
					getDataFromNeighbor(whoHasBlock, clientNum,reqBlock);
					logFile.writeToFile(clientNum, "Got data from: "
							+ whoHasBlock);
					this.NeighborcacheHits++;
					updateLocalCache(reqBlock);
					found = true;
					return found;
				}
				
				else {
					goToServer(clientNum,reqBlock);
					found = true;
				}
			}

			else {
				
				goToServer(clientNum,reqBlock);
				found = true;
				return found;
			}
		}
		return found;
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
		mytickCount.setTickCount(SimulatorConstants.NEIGHBOR_COMM);
		logFile.writeToFile(whoAskedForBlock, "Getting from client: "
				+ whoHasBlock);
			mytickCount.setTickCount(SimulatorConstants.SEARCH);
			return reqBlock;

	}
	
	synchronized public int checkForFalseHits(int reqBlock, int whoAskedForBlock, int whoHasBlock)
		    {

					if(SimulatorContentManager.allClients[whoHasBlock].getClientCache().contains(reqBlock)) {
						System.out.println("OK Verified");
						logFile.writeToFile(whoAskedForBlock, "OK Verified ");
						return reqBlock;
					}
					
					else {
						System.out.println("Bad hit");
						logFile.writeToFile(whoAskedForBlock, "Bad hit");
						cacheMiss++;
						return -1;
					}
		    }
	
	private void goToServer(int clientNum,int reqBlock) {
		mytickCount.setTickCount(SimulatorConstants.SERVER_COMM);
		System.out.println("Not found in content manager: ");
		logFile.writeToFile(clientNum, "Not found in content manager: ");
		searchServerMemm(clientNum, reqBlock, logFile);
		// if(fromMemory == -1) System.exit(0);
		ArrayList<Integer> localCacheUpdated = updateLocalCache(reqBlock);
		SimulatorContentManager.updateContentManager(clientNum,
				reqBlock, localCacheUpdated, logFile);
		System.out.println("After Update "
				+ SimulatorContentManager.lookUpTable);
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
		return SimulatorServer.searchMemory(clientNum, reqBlock, logFile, mytickCount);
	}

	private ArrayList<Integer> updateLocalCache(int req_data) {
		Random random = new Random();
		int indexOfwhatToReplace = random.nextInt(clientBlockSize - 1) + 1;
		if (clientCache.cache.size() < clientBlockSize) {
			clientCache.cache.add(req_data);
		} else {
			// TODO: Random replacement algorithm
			clientCache.cache.set(indexOfwhatToReplace, req_data);
		}
		return clientCache.cache;
	}

	/**
	 * Search data in client's local cache
	 * */
	private boolean searchLocalCache(int req_data) {
		System.out.println("Searching local cache");
		logFile.writeToFile(this.clientNumber, "Searching local cache");
		for (int i = 0; i < clientCache.cache.size(); i++) {
			if (clientCache.cache.get(i) == req_data) {
				return true;
			}
		}
		return false;
	}
}
