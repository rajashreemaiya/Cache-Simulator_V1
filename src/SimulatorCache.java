import java.util.*;

/**
 * 
 * Class to simulate the cache object
 * 
 * @author Rajashree K Maiya
 *
 */
public class SimulatorCache {

	/*
	 * This array list will act as the cache
	 */
	List<Integer> cache;
	List<Integer> pattern_index; //holds the pattern number that a block belongs to.

	public SimulatorCache(int blockSize) {
		cache = new ArrayList<Integer>(blockSize);
		pattern_index = Collections.synchronizedList(new ArrayList<Integer>());
	}

	public List<Integer> getPatternIndex() {
		return this.pattern_index;
	}
	
	public List<Integer> getCache() {
		return this.cache;
	}

	public void setCache(ArrayList<Integer> cache) {
		this.cache = cache;
	}
}
