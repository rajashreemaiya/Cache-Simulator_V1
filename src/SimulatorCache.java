import java.util.*;

/**
 * The cache object
 */

/**
 * @author rmaiya
 *
 */
public class SimulatorCache {

	/*
	 * This array list will act as the cache
	 */
	List<Integer> cache;

	public SimulatorCache(int blockSize) {
		cache = new ArrayList<Integer>(blockSize);
	}

	public List<Integer> getCache() {
		return this.cache;
	}

	public void setCache(ArrayList<Integer> cache) {
		this.cache = cache;
	}
}
