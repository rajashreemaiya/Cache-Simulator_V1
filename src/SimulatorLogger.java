import java.io.*;
import java.util.ArrayList;

/**
 * Logger class for each client,
 * creates a file and records all client activities.
 * Only for debugging.
 * 
 * @author Rajashree K Maiya
 *
 */
public class SimulatorLogger {

	ArrayList<Integer> allClients = new ArrayList<Integer>();

	SimulatorLogger(int clientNum) {
//		 File file = new File("Client_"+clientNum+".txt");
//		 try {
//		 if(file.delete())
//		 file.createNewFile();
//		 } catch (IOException e) {
//		 e.printStackTrace();
//		 }
	}

	public void writeToFile(int clientNum, String info) {
//		 try {
//		 FileWriter writer = new FileWriter("Client_"+clientNum+".txt",true);
//		 writer.append("\n"+ info);
//		 writer.flush();
//		 writer.close();
//		 } catch (IOException e) {
//		 e.printStackTrace();
//		 }
	}
}
