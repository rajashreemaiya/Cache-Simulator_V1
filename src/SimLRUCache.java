import java.util.LinkedHashMap;
import java.util.Map;

public class SimLRUCache <K, V> extends LinkedHashMap<K, V>
{
	/**
	 * @author rmaiya
	 *
	 */
	private int cacheSize;

	public SimLRUCache(int cacheSize) 
	{
		super(cacheSize, 0.75f, true);
		this.cacheSize = cacheSize;
	}

	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) 
	{
		return size() >= cacheSize;
	}
}