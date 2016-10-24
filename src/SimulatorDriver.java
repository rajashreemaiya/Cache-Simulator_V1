import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Class that drives the simulation.
 * TODO: Settings have to be loaded from the configuration file
 */

/**
 * @author rmaiya
 *
 */
public class SimulatorDriver {

	public static void main(String[] args) {
		
		Properties prop = new Properties();
		if (args.length != 1) {
			System.out.println("Please attach input configuration file");
		}
		String propFileName = args[0];

		InputStream inputStream;
		try {

			inputStream = new FileInputStream(propFileName);
			prop.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int nClientInit = Integer.parseInt(prop.getProperty("nclientInit"));
		int nClientFinal = Integer.parseInt(prop.getProperty("nclientFinal"));
		int nClientdelta = Integer.parseInt(prop.getProperty("nclientDelta"));

		int nServerInit = Integer.parseInt(prop.getProperty("nserverInit"));
		int nServerFinal = Integer.parseInt(prop.getProperty("nserverFinal"));
		int nServerdelta = Integer.parseInt(prop.getProperty("nserverDelta"));

		int ncacheInit = Integer.parseInt(prop.getProperty("nCacheInit"));
		int nCacheFinal = Integer.parseInt(prop.getProperty("nCacheFinal"));
		int nCachedelta = Integer.parseInt(prop.getProperty("nCachetDelta"));

		for (int nclient = nClientInit; nclient < nClientFinal; nclient = nclient
				+ nClientdelta) {
			for (int ncachesize = ncacheInit; ncachesize < nCacheFinal; ncachesize = ncachesize
					* nCachedelta) {
				for (int serversize = nServerInit; serversize < nServerFinal; serversize = serversize
						* nServerdelta) {
					
					System.out.println("Main Memory size: "
							+ SimulatorDisk.memory.length);
					SimulatorDisk.setMemory();
					
					SimulatorContentManager.recircArray = Collections
							.synchronizedMap(new HashMap<Integer, Integer>());

					ArrayList<Integer> inLocalCache = new ArrayList<Integer>();

					ArrayList<Integer> inNeighborCache = new ArrayList<Integer>();

					ArrayList<Integer> cacheMiss = new ArrayList<Integer>();

					int numOfClients = nclient;
					int sizeOfClientCache = ncachesize;
					int serverMemory = serversize;

					SimulatorConstants.SEARCH = Integer.parseInt(prop
							.getProperty("search"));
					SimulatorConstants.TOCONTEXTMANAGER = Integer.parseInt(prop
							.getProperty("toContextManager"));
					SimulatorConstants.FROMCONTEXTMANAGER = Integer
							.parseInt(prop.getProperty("fromContextManager"));
					SimulatorConstants.TOSERVER = Integer.parseInt(prop
							.getProperty("toServer"));
					SimulatorConstants.FROMSERVER = Integer.parseInt(prop
							.getProperty("fromServer"));
					SimulatorConstants.TODISK = Integer.parseInt(prop
							.getProperty("toDisk"));
					SimulatorConstants.FROMDISK = Integer.parseInt(prop
							.getProperty("fromDisk"));
					SimulatorConstants.ALGORITHM = prop
							.getProperty("algorithm");
					SimulatorConstants.SERVER_ALGORITHM = prop
							.getProperty("serverAlgorithm");
					SimulatorConstants.FILEPREFIX = prop
							.getProperty("filePrefix"); 
					SimulatorConstants.CLIENTS = numOfClients;

					
					SimulatorServer.serverCacheSize = serverMemory;
					SimulatorServer.setServerMemory();
//					System.out.println("Main memory contents: ");
//					SimulatorDisk.printMainMemory();
//					System.out.println();
					SimulatorContentManager.allClients = new SimulatorClient[numOfClients];
					for (int i = 0; i < numOfClients; i++) {
						SimulatorClient client = new SimulatorClient(i,
								sizeOfClientCache);
						SimulatorContentManager.allClients[i] = client;
					}
					
					System.out.println("Configurations..");
					System.out.println("Clients: " +numOfClients + " Server Memory: " +  serversize + " Local Cache Size: "+ sizeOfClientCache );

					for (int i = 0; i < SimulatorContentManager.allClients.length; i++) {
						SimulatorContentManager.allClients[i].start();
					}

					ArrayList<Integer> totalTicks = new ArrayList<Integer>();

					for (int i = 0; i < SimulatorContentManager.allClients.length; i++) {
						try {
							Thread thread = SimulatorContentManager.allClients[i];
							thread.join();
							totalTicks
									.add(SimulatorContentManager.allClients[i].mytickCount
											.getTickCount());
							inLocalCache
									.add(SimulatorContentManager.allClients[i].LocalcacheHits);
							inNeighborCache
									.add(SimulatorContentManager.allClients[i].NeighborcacheHits);
							cacheMiss
									.add(SimulatorContentManager.allClients[i].cacheMiss);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					int ticks = 0;
					for (int i = 0; i < totalTicks.size(); i++) {
						ticks += totalTicks.get(i);
					}

//					System.out.println(cacheMiss);
//					System.out.println(inLocalCache);
//					System.out.println(inNeighborCache);
//					System.out.println(totalTicks);
//					System.out.println("For " + numOfClients
//							+ " clients, the total tick count is: " + ticks);

					int localHits = 0;
					for (int i = 0; i < inLocalCache.size(); i++) {
						localHits += inLocalCache.get(i);
					}
//					System.out.println("For " + numOfClients
//							+ " clients, the local cache hit is: " + localHits);

					int neighborHits = 0;
					for (int i = 0; i < inNeighborCache.size(); i++) {
						neighborHits += inNeighborCache.get(i);
					}
//					System.out.println("For " + numOfClients
//							+ " clients, the neighbor cache hit is: "
//							+ neighborHits);
//					


					int cacheMisses = 0;
					for (int i = 0; i < cacheMiss.size(); i++) {
						cacheMisses += cacheMiss.get(i);
					}
//					System.out.println("For " + numOfClients
//							+ " clients, the cache miss is: " + cacheMisses);

					double localHitRate = (localHits / (double) (neighborHits
							+ cacheMisses + localHits)) * 100;
					double neighborHitRate = (neighborHits / (double) (neighborHits
							+ cacheMisses + localHits)) * 100;
					double missRate = (cacheMisses / (double) (neighborHits
							+ cacheMisses + localHits)) * 100;

					String filename = "Output_traces/"+propFileName + "_output.csv";
					File file = new File(filename);
					file.getParentFile().mkdirs();
					try {
						file.createNewFile();
						FileWriter writer = new FileWriter(file, true);
						writer.append(numOfClients
								+ ","
								+ sizeOfClientCache
								+ ","
								+ serverMemory
								+ ","
								+ ticks
								+ ","
								+ new DecimalFormat("#.##")
										.format(localHitRate)
								+ ","
								+ new DecimalFormat("#.##")
										.format(neighborHitRate) + ","
								+ new DecimalFormat("#.##").format(missRate));
						writer.append("\n");
						writer.flush();
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}

	}

}
