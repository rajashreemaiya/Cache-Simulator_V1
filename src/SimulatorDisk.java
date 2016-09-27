import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 
 */

/**
 * @author rmaiya
 *
 */
public class SimulatorDisk {

	static int[] memory = new int[110];

	/**
	 * Fill the main memory.
	 * TODO: This is dummy data, what will be here when we start?
	 */
	public static void setMemory() {	
		for(int i=0;i<110;i++) {
			memory[i] = i;
		}
	}
	
	/**
	 * Helper method to print the main memory
	 */
	public static void printMainMemory() {
		for(int j=0;j<memory.length;j++) {
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
