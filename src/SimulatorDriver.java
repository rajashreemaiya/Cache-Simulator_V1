import java.io.*;
import java.util.Properties;
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

//		int numOfClients = prop.getProperty("numOfClients");
		int numOfClients = 5;
		int sizeOfClientCache = 5;
		System.out.println("Main Memory size: " + SimulatorDisk.memory.length);
		SimulatorDisk.setMemory();
		SimulatorServer.setServerMemory();
		System.out.println("Main memory contents: ");
		SimulatorDisk.printMainMemory();
		System.out.println();
		SimulatorContentManager.allClients = new SimulatorClient[numOfClients];
		for(int i=0;i<numOfClients;i++) {
			SimulatorClient client = new SimulatorClient(i, sizeOfClientCache);
			SimulatorContentManager.allClients[i] = client;
			client.start();
		}
		
		System.out.println("The all clients array:--------------------");
		for(int j=0;j<numOfClients;j++)
			System.out.println(SimulatorContentManager.allClients[j]);
	}

}