package processor.communication.message;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;

import common.Settings;

/**
 * This class covert messages to strings and vice versa. When converting a
 * message to a string, the message's class name and all the field values are
 * encapsulated in a string. Delimiters are appended to the values of fields
 * based on the hierarchy of the fields in the message.
 *
 */
public class MessageUtil {

	static String[] delimiterMsgField = { "\u0000", "\u0001", "\u0002", "\u0003", "\u0004", "\u0005", "\u0006",
			"\u0007", "\u0008" };
	static String[] delimiterMsgListItem = { "\u0010", "\u0011", "\u0012", "\u0013", "\u0014", "\u0015", "\u0016",
			"\u0017", "\u0018" };

	static String[] getFieldStringsFromMessage(final String stringToSplit, final int level) {
		return stringToSplit.split(delimiterMsgField[level], -1);
	}

	static String[] getListItemStringsFromMessage(final String stringToSplit, final int level) {
		return stringToSplit.split(delimiterMsgListItem[level], -1);
	}

	public static Object read(final String message) {
		Object object = null;
		try {
			final int level = 0;
			final String[] parts = getFieldStringsFromMessage(message, level);
			final String className = parts[0];
			final String data = parts[1];
			object = Class.forName(className).newInstance();
			updateObjectWithMessageData(object, data, level + 1);
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		} catch (final InstantiationException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}
		return object;
	}

	static Object toObject(final Class clazz, final String value) {
		if (String.class == clazz) {
			return value;
		}
		if ((Boolean.class == clazz) || (Boolean.TYPE == clazz)) {
			return Boolean.parseBoolean(value);
		}
		if ((Byte.class == clazz) || (Byte.TYPE == clazz)) {
			return Byte.parseByte(value);
		}
		if ((Short.class == clazz) || (Short.TYPE == clazz)) {
			return Short.parseShort(value);
		}
		if ((Integer.class == clazz) || (Integer.TYPE == clazz)) {
			return Integer.parseInt(value);
		}
		if ((Long.class == clazz) || (Long.TYPE == clazz)) {
			return Long.parseLong(value);
		}
		if ((Float.class == clazz) || (Float.TYPE == clazz)) {
			return Float.parseFloat(value);
		}
		if ((Double.class == clazz) || (Double.TYPE == clazz)) {
			return Double.parseDouble(value);
		}
		return value;
	}

	static void updateObjectWithMessageData(final Object object, final String data, final int level) {
		try {
			final String[] fieldStrings = getFieldStringsFromMessage(data, level);
			final Field[] fields = object.getClass().getFields();
			for (int i = 0; i < fields.length; i++) {
				if (fieldStrings[i].length() == 0) {
					continue;
				}

				if (fields[i].getType().getName().equals("java.util.ArrayList")) {

					final ParameterizedType listType = (ParameterizedType) fields[i].getGenericType();
					final Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];

					final ArrayList list = new ArrayList(1000);
					final String[] listItemStrings = getListItemStringsFromMessage(fieldStrings[i], level + 1);
					for (final String listItemString : listItemStrings) {
						if (listItemString.length() == 0) {
							continue;
						}
						final Object itemObject = listClass.newInstance();
						updateObjectWithMessageData(itemObject, listItemString, level + 1);
						list.add(itemObject);
					}
					fields[i].set(object, list);
				} else {
					fields[i].set(object, toObject(fields[i].getType(), fieldStrings[i]));
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	StringBuilder sb = new StringBuilder();

	void appendFieldDelimiter(final int level) {
		sb.append(delimiterMsgField[level]);
	}

	void appendFieldValue(final Object object) {
		sb.append(String.valueOf(object));
	}

	void appendListDelimiter(final int level) {
		sb.append(delimiterMsgListItem[level]);
	}

	void appendObjectToMessage(final Object object, final int level) {
		try {
			final Field[] fields = object.getClass().getFields();
			for (final Field field : fields) {
				if (field.getType().getName().equals("java.util.ArrayList")) {
					final ArrayList list = (ArrayList) field.get(object);
					for (final Object item : list) {
						appendObjectToMessage(item, level + 1);
						appendListDelimiter(level + 1);
					}
				} else {
					appendFieldValue(field.get(object));
				}
				appendFieldDelimiter(level);
			}
		} catch (final IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String compose(final Object object) {

		sb.delete(0, sb.length());
		final int level = 0;
		appendFieldValue(object.getClass().getName());
		appendFieldDelimiter(level);
		appendObjectToMessage(object, level + 1);

		return sb.toString();
	}
}
