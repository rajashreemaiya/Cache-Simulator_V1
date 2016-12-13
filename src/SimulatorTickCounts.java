/**
 * Class to keep track of ticks to
 * measure memory access time.
 * 
 * @author Rajashree K Maiya
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
		this.tickCount += operationCost;
	}
}
