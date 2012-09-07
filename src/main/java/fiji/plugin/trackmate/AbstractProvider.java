package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;

import fiji.plugin.trackmate.detection.SpotDetectorFactory;

/**
 * A base class for providers, that contains utility methods and fields.
 * @author Jean-Yves Tinevez - 2012
 */
public abstract class AbstractProvider {


	protected static final String XML_MAP_KEY_ATTRIBUTE_NAME = "KEY";
	protected static final String XML_MAP_VALUE_ATTRIBUTE_NAME = "VALUE";
	/** The target keys. These names will be used as keys to access relevant classes. 
	 * The list order determines how target classes are presented in the GUI. */
	protected List<String> keys;
	/** The currently selected key. It must belong to the {@link #keys} list. */
	protected String currentKey;
	/** Storage for error messages. */
	protected String errorMessage;
	/** The target classes pretty names. With the same order that for {@link #keys}. */
	protected ArrayList<String> names;
	/** The target classes info texts. With the same order that for {@link #keys}. */
	protected ArrayList<String> infoTexts;

	

	/**
	 * @return the currently selected key.
	 */
	public String getCurrentKey() {
		return currentKey;
	}
	
	/**
	 * Configure this provider for the target {@link SpotDetectorFactory} identified by 
	 * the given key. If the key is not found in this provider's list, the 
	 * provider state is not changed.
	 * @return true if the given key was found and the target detector was changed.
	 */
	public boolean select(final String key) {
		if (keys.contains(key)) {
			currentKey = key;
			errorMessage = null;
			return true;
		} else {
			errorMessage = "Unknown key: "+key+".\n";
			return false;
		}
	}

	/**
	 * @return an error message for the last unsuccessful methods call.
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * Add a parameter attribute to the given element, taken from the given settings map. 
	 * Basic checks are made to ensure that the parameter value can be found and is of 
	 * the right class.
	 * @param settings  the map to take the parameter value from
	 * @param element  the JDom element to update
	 * @param parameterKey  the key to the parameter value in the map
	 * @param expectedClass  the expected class for the value
	 * @return  true if the parameter was found, of the right class, and was successfully added to the element.
	 */
	protected boolean writeAttribute(final Map<String, Object> settings, Element element, String parameterKey, Class<?> expectedClass) {
		Object obj = settings.get(parameterKey);

		if (null == obj) {
			errorMessage = "Could not find parameter "+parameterKey+" in settings map.\n";
			return false;
		}

		if (!expectedClass.isInstance(obj)) {
			errorMessage = "Exoected "+parameterKey+" parameter to be a "+expectedClass.getName()+" but was a "+obj.getClass().getName()+".\n";
			return false;
		}

		element.setAttribute(parameterKey, ""+obj);
		return true;
	}

	/** 
	 * Stores the given mapping in a given JDon element, using attributes in a KEY="VALUE" fashion.
	 */
	protected void marshallMap(final Map<String, Double> map, final Element element) {
		for (String key : map.keySet()) {
			element.setAttribute(key, map.get(key).toString());
		}
	}
	
	/** 
	 * Unmarshall the attributes of a JDom element in a map of doubles. 
	 * Mappings are added to the given map. If a value is found not to be a double, an 
	 * error is returned.
	 * @return  true if all values were found and mapped as doubles, false otherwise and 
	 * {@link #errorMessage} is updated. 
	 */
	protected boolean unmarshallMap(final Element element, final Map<String, Double> map) {
		boolean ok = true;
		@SuppressWarnings("unchecked")
		List<Attribute> attributes = element.getAttributes();
		for(Attribute att : attributes) {
			String key = att.getName();
			try {
				double val = att.getDoubleValue();
				map.put(key, val);
			} catch (DataConversionException e) {
				errorMessage = "Could not convert the "+key+" attribute to double. Got "+att.getValue()+".\n";
				ok = false;
			}
		}
		return ok;
	}
	

	protected boolean readDoubleAttribute(final Element element, Map<String, Object> settings, String parameterKey) {
		String str = element.getAttributeValue(parameterKey);
		if (null == str) {
			errorMessage = "Attribute "+parameterKey+" could not be found in XML element.\n";
			return false;
		}
		try {
			double val = Double.parseDouble(str);
			settings.put(parameterKey, val);
		} catch (NumberFormatException nfe) {
			errorMessage = "Could not read "+parameterKey+" attribute as a double value. Got "+str+".\n";
			return false;
		}
		return true;
	}

	protected boolean readIntegerAttribute(final Element element, Map<String, Object> settings, String parameterKey) {
		String str = element.getAttributeValue(parameterKey);
		if (null == str) {
			errorMessage = "Attribute "+parameterKey+" could not be found in XML element.\n";
			return false;
		}
		try {
			int val = Integer.parseInt(str);
			settings.put(parameterKey, val);
		} catch (NumberFormatException nfe) {
			errorMessage = "Could not read "+parameterKey+" attribute as an integer value. Got "+str+".\n";
			return false;
		}
		return true;
	}

	protected boolean readBooleanAttribute(final Element element, Map<String, Object> settings, String parameterKey) {
		String str = element.getAttributeValue(parameterKey);
		if (null == str) {
			errorMessage = "Attribute "+parameterKey+" could not be found in XML element.\n";
			return false;
		}
		try {
			boolean val = Boolean.parseBoolean(str);
			settings.put(parameterKey, val);
		} catch (NumberFormatException nfe) {
			errorMessage = "Could not read "+parameterKey+" attribute as an boolean value. Got "+str+".";
			return false;
		}
		return true;
	}


	/**
	 * @return a list of the detector keys available through this provider.
	 */
	public List<String> getKeys() {
		return keys;
	}

	/**
	 * @return a list of the detector names available through this provider.
	 */
	public List<String> getNames() {
		return names;
	}

	/**
	 * @return a list of the detector informative texts available through this provider.
	 */
	public List<String> getInfoTexts() {
		return infoTexts;
	}
	
	
	/**
	 * Check that the given map has all some keys. Two String sollection allows specifying 
	 * that some keys are mandatory, other are optional.
	 * @param map  the map to inspect.
	 * @param mandatoryKeys the collection of keys that are expected to be in the map. Can be <code>null</code>.
	 * @param optionalKeys the collection of keys that can be - or not - in the map. Can be <code>null</code>.
	 * @param errorHolder will be appended with an error message.
	 * @return if all mandatory keys are found in the map, and possibly some optional ones, but no others.
	 */
	public static final <T> boolean checkMapKeys(final Map<T, ?> map, Collection<T> mandatoryKeys, Collection<T> optionalKeys, final StringBuilder errorHolder) {
		if (null == optionalKeys) {
			optionalKeys = new ArrayList<T>();
		}
		if (null == mandatoryKeys) {
			mandatoryKeys = new ArrayList<T>();
		}
		boolean ok = true;
		Set<T> keySet = map.keySet();
		for(T key : keySet) {
			if (! (mandatoryKeys.contains(key) || optionalKeys.contains(key)) ) {
				ok = false;
				errorHolder.append("Map contains unexpected key: "+key+".\n");
			}
		}
		
		for(T key : mandatoryKeys) {
			if (!keySet.contains(key)) {
				ok = false;
				errorHolder.append("Mandatory key "+key+" was not found in the map.\n");
			}
		}
		return ok;
		
	}
	
	/**
	 * Check the presence and the validity of a key in a map, and test it is of the desired class.
	 * @param map the map to inspect.
	 * @param key  the key to find.
	 * @param expectedClass  the expected class of the target value .
	 * @param errorHolder will be appended with an error message.
	 * @return  true if the key is found in the map, and map a value of the desired class.
	 */
	public static final boolean checkParameter(final Map<String, Object> map, String key, final Class<?> expectedClass, final StringBuilder errorHolder) {
		Object obj = map.get(key);
		if (null == obj) {
			errorHolder.append("Parameter "+key+" could not be found in settings map.\n");
			return false;
		}
		if (!expectedClass.isInstance(obj)) {
			errorHolder.append("Value for parameter "+key+" is not of the right class. Expected "+expectedClass.getName()+", got "+obj.getClass().getName()+".\n");
			return false;
		}
		return true;
	}
	

	/**
	 * Check the validity of a feature penalty map in a settings map. 
	 * <p>
	 * A feature penalty setting is valid if it is either <code>null</code> (not here, that is)
	 * or an actual Map<String, Double>. Then, all its keys must be Strings and all its values 
	 * as well.
	 * 
	 * @param map the map to inspect.
	 * @param key  the key that should map to a feature penalty map.
	 * @param errorHolder will be appended with an error message.
	 * @return  true if the feature penalty map is valid.
	 */
	@SuppressWarnings("rawtypes")
	public static final boolean checkFeatureMap(final Map<String, Object> map, final String featurePenaltiesKey, final StringBuilder errorHolder) {
		Object obj = map.get(featurePenaltiesKey);
		if (null == obj) {
			return true; // NOt here is acceptable
		}
		if (!(obj instanceof Map)) {
			errorHolder.append("Feature penalty map is not of the right class. Expected a Map, got a "+obj.getClass().getName()+".\n");
			return false;
		}
		boolean ok = true;
		Map fpMap = (Map) obj;
		Set fpKeys = fpMap.keySet();
		for(Object fpKey : fpKeys) {
			if (!(fpKey instanceof String)) {
				ok = false;
				errorHolder.append("One key ("+fpKey.toString()+") in the map is not of the right class.\n" +
						"Expected String, got "+fpKey.getClass().getName()+".\n"); 
			}
			Object fpVal = fpMap.get(fpKey);
			if (!(fpVal instanceof String)) {
				ok = false;
				errorHolder.append("The value for key "+fpVal.toString()+" in the map is not of the right class.\n" +
						"Expected String, got "+fpVal.getClass().getName()+".\n"); 
			}
		}
		return ok;
	}


}