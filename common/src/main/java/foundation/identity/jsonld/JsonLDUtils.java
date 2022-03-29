package foundation.identity.jsonld;

import com.google.api.client.util.DateTime;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class JsonLDUtils {

	/*
	 * convert
	 */

	public static final SimpleDateFormat DATE_FORMAT;

	static {

		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public static URI stringToUri(String string) {
		return string == null ? null : URI.create(string);
	}

	public static String uriToString(URI uri) {
		return uri == null ? null : uri.toString();
	}

	public static Date stringToDate(String string) {
		if (string == null) return null;
		DateTime dateTime = DateTime.parseRfc3339(string);
		return new Date(dateTime.getValue());
	}

	public static String dateToString(Date date) {
		return date == null ? null : DATE_FORMAT.format(date);
	}

	/*
	 * add
	 */

	public static void jsonLdAddAll(JsonLDObject jsonLdObject, Map<String, Object> jsonObject) {
		jsonLdObject.getJsonObject().putAll(jsonObject);
	}

	public static void jsonLdAdd(JsonLDObject jsonLdObject, String term, Object value) {

		// if (jsonLdObject.getJsonObject() == null || term == null || value == null) throw new NullPointerException();
        if (jsonLdObject.getJsonObject() == null || term == null || value == null) return;

		Object jsonValueExisting = jsonLdObject.getJsonObject().get(term);

		if (jsonValueExisting == null) {
			if (value instanceof List<?> && ((List<?>) value).size() == 0)
				;
			else if (value instanceof List<?> && ((List<?>) value).size() == 1)
				jsonLdObject.getJsonObject().put(term, jsonLdObjectOrId(((List<?>) value).get(0)));
			else if (value instanceof List<?>)
				jsonLdObject.getJsonObject().put(term, jsonLdObjectOrId(value));
			else
				jsonLdObject.getJsonObject().put(term, jsonLdObjectOrId(value));
		} else if (jsonValueExisting instanceof List<?>) {
			List<Object> jsonArray = new ArrayList<>((List<Object>) jsonValueExisting);
			jsonArray.add(value);
			jsonLdObject.getJsonObject().put(term, jsonLdObjectOrId(jsonArray));
		} else {
			List<Object> jsonArray = new ArrayList<>();
			jsonArray.add(jsonValueExisting);
			jsonArray.add(value);
			jsonLdObject.getJsonObject().put(term, jsonLdObjectOrId(jsonArray));
		}
	}

	public static void jsonLdAddAsJsonArray(JsonLDObject jsonLdObject, String term, List<? extends Object> values) {

		if (jsonLdObject.getJsonObject() == null || term == null || values == null) throw new NullPointerException();
		if (values.size() < 1) return;

		Object jsonValueExisting = jsonLdObject.getJsonObject().get(term);

		if (jsonValueExisting == null)  {
			List<Object> jsonArray = new ArrayList<>(values);
			jsonLdObject.getJsonObject().put(term, jsonLdObjectOrId(jsonArray));
		} else if (jsonValueExisting instanceof List<?>)  {
			List<Object> jsonArray = new ArrayList<>((List<Object>) jsonValueExisting);
			jsonArray.addAll(values);
			jsonLdObject.getJsonObject().put(term, jsonLdObjectOrId(jsonArray));
		} else {
			List<Object> jsonArray = new ArrayList<>();
			jsonArray.add(jsonValueExisting);
			jsonArray.addAll(values);
			jsonLdObject.getJsonObject().put(term, jsonLdObjectOrId(jsonArray));
		}
	}

	public static void jsonLdAddAsJsonArray(JsonLDObject jsonLdObject, String term, Object value) {

		jsonLdAddAsJsonArray(jsonLdObject, term, Collections.singletonList(value));
	}

	/*
	 * remove
	 */

	public static void jsonLdRemove(JsonLDObject jsonLdObject, String term) {

		jsonLdObject.getJsonObject().remove(term);
	}

	/*
	 * get
	 */

	public static Object jsonLdGetJsonValue(Map<String, Object> jsonObject, String term) {

		return jsonObject.get(term);
	}

	public static List<Object> jsonLdGetJsonArray(Map<String, Object> jsonObject, String term) {

		Object entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof List<?>) {
			return (List<Object>) entry;
		} else {
			throw new IllegalArgumentException("Cannot get json array '" + term + "' from " + jsonObject);
		}
	}

	public static Map<String, Object> jsonLdGetJsonObject(Map<String, Object> jsonObject, String term) {

		Object entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof Map<?, ?>) {
			return (Map<String, Object>) entry;
		} else if (entry instanceof List<?> && ((List<Object>) entry).size() == 1 && ((List<Object>) entry).get(0) instanceof Map<?, ?>) {
			return (Map<String, Object>) ((List<Object>) entry).get(0);
		} else {
			throw new IllegalArgumentException("Cannot get json object '" + term + "' from " + jsonObject);
		}
	}

	public static String jsonLdGetString(Map<String, Object> jsonObject, String term) {

		Object entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof String) {
			return (String) entry;
		} else if (entry instanceof List<?>) {
			if (((List<Object>) entry).size() == 1 && ((List<Object>) entry).get(0) instanceof String) {
				return (String) ((List<Object>) entry).get(0);
			} else {
				throw new IllegalArgumentException("Cannot get string '" + term + "' from " + jsonObject + " (list)");
			}
		} else {
			throw new IllegalArgumentException("Cannot get string '" + term + "' from " + jsonObject);
		}
	}

	public static String jsonLdGetStringOrObjectId(Map<String, Object> jsonObject, String term) {

		Object entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof String) {
			return (String) entry;
		} else if (entry instanceof List<?>) {
			if (((List<Object>) entry).size() == 1 && ((List<Object>) entry).get(0) instanceof String) {
				return (String) ((List<Object>) entry).get(0);
			} else {
				throw new IllegalArgumentException("Cannot get string '" + term + "' from " + jsonObject + " (list)");
			}
		} else if (entry instanceof Map<?, ?>) {
			String id = null;
			if (((Map<String, Object>) entry).get("@id") instanceof String) id = (String) ((Map<String, Object>) entry).get("@id");
			if (((Map<String, Object>) entry).get("id") instanceof String) id = (String) ((Map<String, Object>) entry).get("id");
			if (id == null) throw new IllegalArgumentException("Cannot get string '" + term + "' from " + jsonObject + " (map)");
			return id;
		} else {
			throw new IllegalArgumentException("Cannot get string '" + term + "' from " + jsonObject);
		}
	}

	public static List<String> jsonLdGetStringList(Map<String, Object> jsonObject, String term) {

		Object entry = jsonObject.get(term);
		if (entry == null) return null;

		if (entry instanceof String) {
			return Collections.singletonList((String) entry);
		} else if (entry instanceof List<?>) {
			return ((List<Object>) entry).stream().map(x -> x instanceof String ? (String) x : null).collect(Collectors.toList());
		} else {
			throw new IllegalArgumentException("Cannot get string list '" + term + "' from " + jsonObject);
		}
	}

	/*
	 * contains
	 */

	public static boolean jsonLdContainsString(Map<String, Object> jsonObject, String term, String value) {

		Object entry = jsonObject.get(term);
		if (entry == null) return false;

		if (entry instanceof String)
			return ((String) entry).equals(value);
		else if (entry instanceof List<?>)
			return ((List<Object>) entry).contains(value);
		else
			return false;
	}

	/*
	 * Helper methods
	 */

	private static Object jsonLdObjectOrId(Object value) {
		if (value instanceof Map<?, ?>) {
			Map<String, Object> map = (Map<String, Object>) value;
			String id = (String) map.get(JsonLDKeywords.JSONLD_TERM_ID);
			if (map.size() == 1 && id != null)
				return id;
			else
				return value;
		} else if (value instanceof List<?>) {
			List<Object> valueList = new ArrayList<>();
			for (Object valueObject : ((List<?>) value)) valueList.add(jsonLdObjectOrId(valueObject));
			return valueList;
		} else {
			return value;
		}
	}
}
