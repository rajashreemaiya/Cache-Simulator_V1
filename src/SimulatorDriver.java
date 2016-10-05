import java.io.*;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int initValue = Integer.parseInt(prop.getProperty("initValue"));
		int finalValue = Integer.parseInt(prop.getProperty("finalValue"));
		int delta = Integer.parseInt(prop.getProperty("delta"));

		for (int in = initValue; in <= finalValue; in = in * delta) {
			ArrayList<Integer> inLocalCache = new ArrayList<Integer>();

			ArrayList<Integer> inNeighborCache = new ArrayList<Integer>();

			ArrayList<Integer> cacheMiss = new ArrayList<Integer>();
			System.out.println();
			System.out.println("----" + in);
			System.out.println();

			int numOfClients = Integer.parseInt(prop
					.getProperty("numOfClients"));
			int sizeOfClientCache = in;
			// Integer.parseInt(prop.getProperty("clientBlockSize"));
			int serverMemory = Integer.parseInt(prop
					.getProperty("serverMemory"));

			SimulatorConstants.SEARCH = Integer.parseInt(prop
					.getProperty("search"));
			SimulatorConstants.TOCONTEXTMANAGER = Integer.parseInt(prop
					.getProperty("toContextManager"));
			SimulatorConstants.FROMCONTEXTMANAGER = Integer.parseInt(prop
					.getProperty("fromContextManager"));
			SimulatorConstants.TOSERVER = Integer.parseInt(prop
					.getProperty("toServer"));
			SimulatorConstants.FROMSERVER = Integer.parseInt(prop
					.getProperty("fromServer"));
			SimulatorConstants.TODISK = Integer.parseInt(prop
					.getProperty("toDisk"));
			SimulatorConstants.FROMDISK = Integer.parseInt(prop
					.getProperty("fromDisk"));
			SimulatorConstants.ALGORITHM = prop.getProperty("algorithm");

			System.out.println("Main Memory size: "
					+ SimulatorDisk.memory.length);
			SimulatorDisk.setMemory();
			SimulatorServer.serverCacheSize = Integer.parseInt(prop
					.getProperty("serverMemory"));
			SimulatorServer.setServerMemory();
			System.out.println("Main memory contents: ");
			SimulatorDisk.printMainMemory();
			System.out.println();
			SimulatorContentManager.allClients = new SimulatorClient[numOfClients];
			for (int i = 0; i < numOfClients; i++) {
				SimulatorClient client = new SimulatorClient(i,
						sizeOfClientCache);
				SimulatorContentManager.allClients[i] = client;
			}

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

			int sum = 0;
			for (int i = 0; i < totalTicks.size(); i++) {
				sum += totalTicks.get(i);
			}

			System.out.println(cacheMiss);
			System.out.println(inLocalCache);
			System.out.println(inNeighborCache);
			System.out.println(totalTicks);
			System.out.println("For " + numOfClients
					+ " clients, the total tick count is: " + sum);

			int sum0 = 0;
			for (int i = 0; i < inLocalCache.size(); i++) {
				sum0 += inLocalCache.get(i);
			}
			System.out.println("For " + numOfClients
					+ " clients, the local cache hit is: " + sum0);

			int sum1 = 0;
			for (int i = 0; i < inNeighborCache.size(); i++) {
				sum1 += inNeighborCache.get(i);
			}
			System.out.println("For " + numOfClients
					+ " clients, the neighbor cache hit is: " + sum1);

			int sum2 = 0;
			for (int i = 0; i < cacheMiss.size(); i++) {
				sum2 += cacheMiss.get(i);
			}
			System.out.println("For " + numOfClients
					+ " clients, the cache miss is: " + sum2);

			File file = new File("Output.txt");
			try {
				file.createNewFile();
				FileWriter writer = new FileWriter("Output.txt", true);
				writer.append("\n" + numOfClients + ", " + sizeOfClientCache
						+ ", " + sum + ", " + sum0 + ", " + sum1 + ", " + sum2);
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}