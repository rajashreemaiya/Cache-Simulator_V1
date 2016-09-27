/**
 * 
 */

/**
 * @author rmaiya
 *
 */
public class SimulatorTickCounts {

	int tickCount;
	
	public SimulatorTickCounts() {
		this.tickCount = 0;
	}
	
	public int getTickCount() {
		return tickCount;
	}
	
	public void setTickCount(int operationCost) {
		this.tickCount+=operationCost;
	}
}
