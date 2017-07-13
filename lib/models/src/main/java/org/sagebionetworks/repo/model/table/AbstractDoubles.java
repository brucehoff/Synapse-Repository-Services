package org.sagebionetworks.repo.model.table;

/**
 * Representation of doubles that are either not-a-number (NaN) or non-finite quantities like +/- infinity.
 *
 */
public enum AbstractDoubles {
	
	POSITIVE_INFINITY (
			Double.POSITIVE_INFINITY,
			new String[]{"inf", "+inf", Double.toString(Double.POSITIVE_INFINITY).toLowerCase(), "+infinity", "\u221E", "+\u221E"},
			"1.7976931348623157E+308"
	),
	NEGATIVE_INFINITY (
			Double.NEGATIVE_INFINITY,
			new String[]{"-inf", Double.toString(Double.NEGATIVE_INFINITY).toLowerCase(), "-\u221E"},
			"-1.7976931348623157E+308"
	),
	NAN (
			Double.NaN,
			new String[]{Double.toString(Double.NaN).toLowerCase()},
			null
	);

	Double value;
	String[] possibleValues;
	String approximateValue;
	long longBits;
	
	/**
	 * 
	 * @param value The double value that represents this type.
	 * @param databaseName The database representation of this name.
	 * @param possibleValues The possible string values that represent this value.
	 * @param approximateValue An approximate string representation of this type.
	 */
	private AbstractDoubles(double value,
			String[] possibleValues, String approximateValue) {
		this.value = value;
		this.possibleValues = possibleValues;
		this.approximateValue = approximateValue;
		this.longBits = Double.doubleToLongBits(value);
	}
	
	/**
	 * Lookup the double type given a string value.
	 * 
	 * @param value
	 * @return
	 * @throws IllegalArgumentException if no match is found.
	 */
	public static AbstractDoubles lookupValue(String value){
		if(value == null){
			throw new NumberFormatException("For input string: \""+value+"\"");
		}
		for(AbstractDoubles meta: values()){
			for(String possible: meta.possibleValues){
				if(possible.equalsIgnoreCase(value)){
					return meta;
				}
			}
		}
		throw new NumberFormatException("For input string: \""+value+"\"");
	}
	
	/**
	 * Lookup the type given a double value.
	 * 
	 * @param value
	 * @return
	 */
	public static AbstractDoubles lookupType(Double value){
		if(value == null){
			throw new NumberFormatException("For input string: \""+value+"\"");
		}
		for(AbstractDoubles meta: values()){
			if(meta.longBits == Double.doubleToLongBits(value)){
				return meta;
			}
		}
		throw new NumberFormatException("For input string: \""+value+"\"");
	}
	
	/**
	 * Abstract values include NaN and +/- Infinity.
	 * Null is not considered abstract.
	 * @param value
	 * @return
	 */
	public static boolean isAbstractValue(Double value){
		if(value == null) return false;
		try{
			lookupType(value);
			return true;
		} catch(IllegalArgumentException e){
			return false;
		}
	}
	
	/**
	 * Get the double value representation for this type.
	 * @return
	 */
	public Double getDoubleValue(){
		return value;
	}
	
	/**
	 * The enumeration values are: NaN, Infinity, and -Infinity
	 * @return
	 */
	public String getEnumerationValue(){
		return Double.toString(value);
	}
	
	/**
	 * An approximate string representation for this type.
	 * @return
	 */
	public String getApproximateValue(){
		return approximateValue;
	}
	
}
