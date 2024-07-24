package tools;
/**
 * This class is used to record the maximum memory usaged of an algorithm during
 * a given execution.
 *
 */  
public class MemoryLogger {
	
	// the only instance  of this class (this is the "singleton" design pattern)
	private static tools.MemoryLogger instance = new tools.MemoryLogger();

	// variable to store the maximum memory usage
	private double maxMemory = 0;
	
	/**
	 * Method to obtain the only instance of this class
	 * @return instance of MemoryLogger
	 */
	public static tools.MemoryLogger getInstance(){
		return instance;
	}
	
	/**
	 * To get the maximum amount of memory used until now
	 * @return a double value indicating memory as megabytes
	 */
	public double getMaxMemory() {
		return maxMemory;
	}

	/**
	 * Reset the maximum amount of memory recorded.
	 */
	public void reset(){
		maxMemory = 0;
	}
	
	/**
	 * Check the current memory usage and record it if it is higher
	 * than the amount of memory previously recorded.
	 * @return the memory usage in megabytes
	 */
	public double checkMemory() {
		double currentMemory = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())
				/ 1024d / 1024d;
		if (currentMemory > maxMemory) {
			maxMemory = currentMemory;
		}
		return currentMemory;
	}
}
