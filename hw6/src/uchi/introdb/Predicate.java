package uchi.introdb;

/**
 * @author aelmore
 * Do not modify this class
 * @param <T>
 */
public class Predicate<T>{
	
	public T value;
	public Compare compare;
	public Field field;
	
	
	public Predicate(T value, Compare compare, Field field) {
		super();
		this.value = value;
		this.compare = compare;
		this.field = field;
	}

	public enum Compare {
		EQUALS, GREATERTHAN, LESSTHAN
	}
	
	public enum Field {
		NUM_POTHOLES, LATITUDE, LONGITUDE, ZIP,RECENT_ACTIONS
	}

	@Override
	public String toString() {
		return "Predicate [value=" + value + ", compare=" + compare + ", field=" + field + "]";
	}
	
}

