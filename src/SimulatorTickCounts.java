/**
 * 
 */

/**
 * @author rmaiya
 *
 */
public class SimulatorTickCounts {
	
	 static final int READ = 1;

	int tickCount;
	
	public SimulatorTickCounts() {
		this.tickCount = 0;
	}
	
	public int getTickCount() {
		return tickCount;
	}
	
	public int setTickCount(int operationCost) {
		return this.tickCount + operationCost;
	}
}
