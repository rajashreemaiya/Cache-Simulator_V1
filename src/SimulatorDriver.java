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