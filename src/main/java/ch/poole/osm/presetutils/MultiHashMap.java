package ch.poole.osm.presetutils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This data structure is a combination of a Map and Set.
 * Each key can be assigned not one, but multiple values.
 * Sorted map/set implementations are used to guarantee (case-sensitive) alphabetical sorting of entries.
 * @author Jan
 *
 * @param <K> Key type
 * @param <V> Type of the values to be associated with the keys
 */
public class MultiHashMap<K, V> implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<K, Set<V>> map;
	private boolean sorted;
	
	/** 
	 * Creates a regular, unsorted MultiHashMap  
	 */
	public MultiHashMap() {
		this(false);
	}
	
	/**
	 * Creates a MultiHashMap.
	 * 
	 * @param sorted if true, Tree maps/sets will be used, if false, regular HashMap/HashSets will be used.
	 */
	public MultiHashMap(boolean sorted) {
		this.sorted = sorted;
		if (sorted) {
			map = new TreeMap<K, Set<V>>();
		} else {
			map = new HashMap<K, Set<V>>();
		}
	}
	
	/**
	 * Check for key in map
	 * 
	 * @param key  the key we are looking for
	 * @return true if key exists in map
	 */
	public boolean containsKey(K key) {
		return map.containsKey(key);
	}
		
	/**
	 * Adds item to the set of values associated with the key (null items are not added)
	 * 
	 * @param key  key to add
	 * @param item item to add
	 * @return true if the element was added, false if it was already in the set or null
	 */
	public boolean add(K key, V item) {
		Set<V> values = map.get(key);
		if (values == null) {
			values = (sorted ? new TreeSet<V>() : new HashSet<V>());
			map.put(key, values);
		}
		if (item == null) return false; 
		return values.add(item);
	}
	
	/**
	 * Adds all items to the set of values associated with the key
	 * 
	 * @param key the key
	 * @param items an array containing the items
	 */
	public void add(K key, V[] items) {
		Set<V> values = map.get(key);
		if (values == null) {
			values = (sorted ? new TreeSet<V>() : new HashSet<V>());
			map.put(key, values);
		}
		values.addAll(Arrays.asList(items));
	}
	
	/**
	 * Adds all items to the set of values associated with the key
	 * 
	 * @param key the key
	 * @param items a set containing the items
	 */
	public void add(K key, Set<V> items) {
		Set<V> values = map.get(key);
		if (values == null) {
			values = (sorted ? new TreeSet<V>() : new HashSet<V>());
			map.put(key, values);
		}
		values.addAll(items);
	}

	/**
	 * Removes the item from the set associated with the given key
	 * 
	 * @param key  the key of the item to remove
	 * @param item the item to remove
	 * @return true if the item was in the set
	 */
	public boolean removeItem(K key, V item) {
		Set<V> values = map.get(key);
		if (values != null) return values.remove(item);
		return false;
	}

	/**
	 * Completely removes all values associated with a key
	 * 
	 * @param key  key to remove all items for
	 */
	public void removeKey(K key) {
		map.remove(key);
	}
	
	/**
	 * Gets the list of items associated with a key.
	 * 
	 * @param key  key we want the values for
	 * @return a unmodifiable list of the items associated with the key, may be empty but never null
	 */
	public Set<V> get(K key) {
		Set<V> values = map.get(key);
		if (values == null) return Collections.emptySet();
		return Collections.unmodifiableSet(values);
	}
	
	/**
	 * Guess what.
	 */
	public void clear() {
		map.clear();
	}

	/**
	 * Get all keys as a Set
	 * 
	 * @return a Set containing the keys
	 */
	public Set<K> getKeys() {
		return map.keySet();
	}

	/** 
	 * Return all values
	 * 
	 * @return a Set of all values
	 */
	public Set<V> getValues() {
		Set<V> retval = new LinkedHashSet<V>();
		for (K key: getKeys()) {
			retval.addAll(get(key));
		}
		return retval;
	}

	/**
	 * Add all key/values from source to this Map
	 * 
	 * @param source add all entries from thiw MultiHashMap
	 */
	public void addAll(MultiHashMap<K, V> source) {
		for (K key:source.getKeys()) {
			add(key,source.get(key));
		}
		
	}
}
