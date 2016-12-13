/**
 * Initializes disk memory for the simulation
 * 
 * @author Rajashree K Maiya
 *
 */
public class SimulatorDisk {

	static int[] memory = new int[20000];

	public static void setMemory() {
		for (int i = 0; i < memory.length; i++) {
			memory[i] = i;
		}
	}

	/**
	 * Helper method to print the main memory
	 */
	public static void printMainMemory() {
		for (int j = 0; j < memory.length; j++) {
			System.out.print(memory[j] + " ");
		}
	}

	/**
	 * Helper method to get requested data from main memory
	 */
	public static int getDataFromMemory(int reqData) {
		return reqData;
	}
}
