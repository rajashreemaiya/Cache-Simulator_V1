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
		String propFileName = "config.properties";
		
		InputStream inputStream;
		try {
			 
			inputStream = new FileInputStream(propFileName);
			prop.load(inputStream);	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int numOfClients = Integer.parseInt(prop.getProperty("numOfClients"));
		int sizeOfClientCache = Integer.parseInt(prop.getProperty("clientBlockSize"));
		
		SimulatorConstants.SEARCH = Integer.parseInt(prop.getProperty("search"));
		SimulatorConstants.TOCONTEXTMANAGER = Integer.parseInt(prop.getProperty("toContextManager"));
		SimulatorConstants.FROMCONTEXTMANAGER = Integer.parseInt(prop.getProperty("fromContextManager"));
		SimulatorConstants.TOSERVER = Integer.parseInt(prop.getProperty("toServer"));
		SimulatorConstants.FROMSERVER = Integer.parseInt(prop.getProperty("fromServer"));
		SimulatorConstants.TODISK = Integer.parseInt(prop.getProperty("toDisk"));
		SimulatorConstants.FROMDISK = Integer.parseInt(prop.getProperty("fromDisk"));
		SimulatorConstants.ALGORITHM = prop.getProperty("algorithm");
		
		
		System.out.println("Main Memory size: " + SimulatorDisk.memory.length);
		SimulatorDisk.setMemory();
		SimulatorServer.serverCacheSize = Integer.parseInt(prop.getProperty("serverMemory"));
		SimulatorServer.setServerMemory();
		System.out.println("Main memory contents: ");
		SimulatorDisk.printMainMemory();
		System.out.println();
		SimulatorContentManager.allClients = new SimulatorClient[numOfClients];
		for(int i=0;i<numOfClients;i++) {
			SimulatorClient client = new SimulatorClient(i, sizeOfClientCache);
			SimulatorContentManager.allClients[i] = client;
		}
		
		for(int i=0;i<SimulatorContentManager.allClients.length;i++) {
			SimulatorContentManager.allClients[i].start();
		}
		
		ArrayList<Integer> totalTicks = new ArrayList<Integer>();
		
		
		for(int i=0;i<SimulatorContentManager.allClients.length;i++) {
			  try {
				Thread thread = SimulatorContentManager.allClients[i];
				thread.join();
//				System.out.println(SimulatorContentManager.allClients[i].clientNumber + " is done!");
				totalTicks.add(SimulatorContentManager.allClients[i].mytickCount.getTickCount());
//				System.out.println(SimulatorContentManager.allClients[i].clientNumber + "'s tick count: " + SimulatorContentManager.allClients[i].mytickCount.getTickCount());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		int sum = 0;
		for(int i=0;i<totalTicks.size();i++) {
			sum += totalTicks.get(i);
		}
		System.out.println("For " + numOfClients + " clients, the total tick count is: " + sum);
	}

}