/**
 *
 */
import java.awt.List;
import java.io.*;
import java.security.KeyStore.Entry;
import java.text.DecimalFormat;
import java.util.*;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

/**
 * @author rmaiya
 *
 */
public class EditInputTraces {

	/**
	 * @param args
	 */
	public static void main(String args[]) throws IOException {

		BufferedReader inputStream = new BufferedReader(new FileReader(
				"trace.csv"));
		File decimalValuesFile = new File("decimalTraces.csv");
		// if File doesn't exist, then create it
		if (!decimalValuesFile.exists()) {
			decimalValuesFile.createNewFile();
		}
		FileWriter filewriter = new FileWriter(decimalValuesFile.getAbsoluteFile());
		BufferedWriter outputStream = new BufferedWriter(filewriter);
		String line;
		while ((line = inputStream.readLine()) != null) {
			try {
				line = line.toLowerCase();
				line = Integer.toString(Integer.parseInt(line, 16));
				outputStream.write(line + "\n");
			} catch (NumberFormatException e) {
				continue;

			}
		}
		outputStream.flush();
		outputStream.close();
		inputStream.close();

	}
}
