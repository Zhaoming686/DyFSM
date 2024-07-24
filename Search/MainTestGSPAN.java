package Search;

import utilities.DataGlobal;
import utilities.Settings;

import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * @author Zhaoming Chen
 */
public class MainTestGSPAN {

	public static void main(String[] arg) throws Exception {

		// Apply the algorithm
		AlgoGSPAN algo = new AlgoGSPAN();

		// Time0
		int endgID = 3000;
		algo.runAlgorithm(endgID);
		algo.printStats();

		//Time1
		endgID = endgID + Settings.IncSize;
		algo.runAlgorithm(endgID);
		algo.printStats();

		//Time2
		endgID = endgID + Settings.IncSize;
		algo.runAlgorithm(endgID);
		algo.printStats();

		//Time3
		endgID = endgID + Settings.IncSize;
		algo.runAlgorithm(endgID);
		algo.printStats();

		//Time4
		endgID = endgID + Settings.IncSize;
		algo.runAlgorithm(endgID);
		algo.printStats();

		//Time5
		endgID = endgID + Settings.IncSize;
		algo.runAlgorithm( endgID);
		algo.printStats();

//		// output
//		algo.writeResultToFile(Settings.outPath);

	}

//	public static String fileToPath(String filename) throws UnsupportedEncodingException {
//		URL url = MainTestGSPAN.class.getResource(filename);
//		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
//	}
}
