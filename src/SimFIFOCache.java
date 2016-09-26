import java.util.LinkedHashMap;
import java.util.Map;

public class SimFIFOCache <K, V> extends LinkedHashMap<K, V>
{
	/**
	 * @author rmaiya
	 *
	 */
	private int cacheSize;

	public SimFIFOCache(int cacheSize) 
	{
		super(cacheSize, 0.75f);
		this.cacheSize = cacheSize;
	}

	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) 
	{
		return size() >= cacheSize;
	}
}