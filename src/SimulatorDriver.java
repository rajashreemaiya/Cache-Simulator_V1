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
		int numOfClients = 2;
		int sizeOfClientCache = 5;
		System.out.println("Main Memory size: " + SimulatorDisk.memory.length);
		SimulatorDisk.setMemory();
		SimulatorServer.setServerMemory();
		System.out.println("Main memory contents: ");
		SimulatorDisk.printMainMemory();
		System.out.println();
		for(int i=1;i<=numOfClients;i++) {
			SimulatorClient client = new SimulatorClient(i, sizeOfClientCache);
			client.start();
		}
	}

}