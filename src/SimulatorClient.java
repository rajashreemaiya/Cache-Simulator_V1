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
	int SystemcacheHits = 0;
	int cacheMiss = 0;
	static int clientCacheCounter = 0;
	Thread client;
	String clientName;
	int clientBlockSize;
	SimulatorCache clientCache;
	SimulatorTickCounts tickCount;
	SimulatorLogger logFile;

	public SimulatorClient(int clientNumber, int blockSize) {
		this.clientBlockSize = this.setBlockSize(blockSize);
		this.clientCache = new SimulatorCache(blockSize);
		this.clientNumber = clientNumber;
		this.setBlockSize(blockSize);
		this.fillClientCache();
		SimulatorContentManager.lookUpTable.put(clientNumber, clientCache.cache);
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
		for(int i=0;i<clientBlockSize;i++) {
			int value = random.nextInt(50)+1;
			clientCache.cache.add(value);
		}
		return clientCache.cache;

	}

	/**
	 * Helper method to for printing
	 * */
	public void printClientCache() {
		for(Integer j: clientCache.cache) {
			System.out.print(j + " "); 
		}

	}

	public synchronized void run() {
		this.logFile = new SimulatorLogger(this.clientNumber);
		logFile.writeToFile(this.clientNumber, "System cache size: " + SimulatorContentManager.sysCacheSize);
		System.out.println("System cache size: " + SimulatorContentManager.sysCacheSize);
		logFile.writeToFile(this.clientNumber, "Client " + this.clientNumber); 
		logFile.writeToFile(this.clientNumber, "Client cache size: " + this.clientBlockSize);
		System.out.println();

		this.generateRequestData();

		logFile.writeToFile(this.clientNumber,"Local Cache Hits: " + this.LocalcacheHits);
		logFile.writeToFile(this.clientNumber,"System Cache Hits: " + this.SystemcacheHits);
		logFile.writeToFile(this.clientNumber,"Cache Miss: " + this.cacheMiss);
	}

	private void generateRequestData() {
		/**
		 * TODO: This is for the input from trace file
		 * */
		
		/*		Scanner scanner;
		try {
			scanner = new Scanner(new File("psuedodata.txt"));
			ArrayList<Integer> inputData = new ArrayList<Integer>();
	        while(scanner.hasNext()){
	           inputData.add(Integer.parseInt(scanner.next()));
	        }
	        scanner.close();
	        for(int i=0;i<inputData.size();i++) {
	        	int reqData = inputData.get(i);
				boolean answer = this.request_data(this.clientNumber,reqData);
				logFile.writeToFile(this.clientNumber,"Counter: " + i);
				System.out.println(answer);
				this.printClientCache();
				System.out.println();
				System.out.println("After Update " + SimulatorContentManager.lookUpTable);
				logFile.writeToFile(this.clientNumber,"After Update " + SimulatorContentManager.lookUpTable);
				logFile.writeToFile(this.clientNumber,"\n");
				System.out.println();
	        }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} */

		/*
		 * This is for testing with dummy data
		 * */

		ArrayList<Integer> client_data = this.getClientCache();
		SimulatorContentManager.lookUpTable.put(this.clientNumber, client_data);
		System.out.println("lookup table: " + SimulatorContentManager.lookUpTable);
		logFile.writeToFile(this.clientNumber,"lookup table: " + SimulatorContentManager.lookUpTable);

		Random random = new Random();

		for(int i=0; i<25;i++) {
			int reqData = random.nextInt(90)+1;
			boolean answer = this.request_data(this.clientNumber,reqData);
			logFile.writeToFile(this.clientNumber,"Counter: " + i);
			System.out.println(answer);
			this.printClientCache();
			System.out.println();
			System.out.println("After Update " + SimulatorContentManager.lookUpTable);
			logFile.writeToFile(this.clientNumber,"After Update " + SimulatorContentManager.lookUpTable);
			logFile.writeToFile(this.clientNumber,"\n");
			System.out.println();
		}	
	}

	private ArrayList<Integer> getClientCache() {
		return clientCache.cache;
	}

	/**
	 * Method that says where the data was fetched from,
	 * Always returns true since server always has the data
	 * */
	private boolean request_data(int clientNum, int reqData) {
		System.out.println("Client " + clientNum + " is requesting the data " + reqData);
		logFile.writeToFile(clientNum,"Client " + clientNum + " is requesting the data " + reqData);
		boolean found = false;

		if(searchLocalCache(reqData) == true) { 
			this.LocalcacheHits++;
			System.out.println("System Cache: " + SimulatorContentManager.systemCache);
			logFile.writeToFile(clientNum,"System Cache: " + SimulatorContentManager.systemCache);
			found = true;
			return found;
		}

		else if(SimulatorContentManager.searchContentManager(clientNum,reqData,logFile) == true) {
			System.out.println("Searching content manager");

			this.SystemcacheHits++;
			updateLocalCache(reqData);
			System.out.println("System Cache: " + SimulatorContentManager.systemCache);
			logFile.writeToFile(clientNum,"System Cache: " + SimulatorContentManager.systemCache);
			found = true;
			return found;
		}

		else {
			this.cacheMiss++;
			System.out.println("Not found in content manager: ");
			logFile.writeToFile(clientNum,"Not found in content manager: ");
			System.out.println("Searching server memory");
			logFile.writeToFile(clientNum,"Searching server memory");
			found = fetchFromServer(clientNum,reqData);
			System.out.println("--------------- " + found);
			if(found == false) System.exit(0);
			ArrayList<Integer> localCacheUpdated = updateLocalCache(reqData);
			SimulatorContentManager.updateContentManager(clientNum,reqData,localCacheUpdated,logFile);
			System.out.println("System Cache: " + SimulatorContentManager.systemCache);
			logFile.writeToFile(clientNum,"System Cache: " + SimulatorContentManager.systemCache);
		}
		return found;	
	}

	private ArrayList<Integer> updateLocalCache(int req_data) {
		Random random = new Random();
		int indexOfwhatToReplace = random.nextInt(clientBlockSize-1)+1;
		if(clientCache.cache.size() < clientBlockSize) {
			clientCache.cache.add(req_data);
		}
		else {
			// TODO: Random replacement algorithm
			clientCache.cache.set(indexOfwhatToReplace, req_data);
		}
		return clientCache.cache;		
	}

	/**
	 * Get data from server (main memory)
	 * */
	private boolean fetchFromServer(int clientNum, int req_data) {
		return SimulatorServer.searchMemory(clientNum,req_data);	
	}

	/**
	 * Search data in client's local cache
	 * */
	private boolean searchLocalCache(int req_data) {
		System.out.println("Searching local cache");
		logFile.writeToFile(this.clientNumber,"Searching local cache");
		for(int i=0;i<clientCache.cache.size();i++) {
			if(clientCache.cache.get(i) == req_data) {
				return true;
			}
		}
		return false;
	}
}
