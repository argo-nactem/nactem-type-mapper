/**
 * Nactem Type Mapper - A UIMA component which is able to create new annotations from existing ones, using a mapping definition language
 * Copyright © 2016 The National Centre for Text Mining (NaCTeM), University of Manchester (jacob.carter@manchester.ac.uk)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.nactem.argo.components.typemapper;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

public class UimaUtils {
	public static CommonArrayFS createArray(CAS cas, Type type, int length) {
		String name = type.getName();
		if (CAS.TYPE_NAME_BOOLEAN_ARRAY.equals(name)) 
			return cas.createBooleanArrayFS(length);
		if (CAS.TYPE_NAME_BYTE_ARRAY.equals(name)) 
			return cas.createByteArrayFS(length);
		if (CAS.TYPE_NAME_DOUBLE_ARRAY.equals(name)) 
			return cas.createDoubleArrayFS(length);
		if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(name)) 
			return cas.createFloatArrayFS(length);
		if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(name)) 
			return cas.createIntArrayFS(length);
		if (CAS.TYPE_NAME_LONG_ARRAY.equals(name)) 
			return cas.createLongArrayFS(length);
		if (CAS.TYPE_NAME_SHORT_ARRAY.equals(name)) 
			return cas.createShortArrayFS(length);
		if (CAS.TYPE_NAME_STRING_ARRAY.equals(name)) 
			return cas.createStringArrayFS(length);
		return cas.createArrayFS(length);
	}

	public static void setArrayValue(CommonArrayFS array, int index, Object value) {
		String name = array.getType().getName();
		if (CAS.TYPE_NAME_BOOLEAN_ARRAY.equals(name)) 
			((BooleanArrayFS)array).set(index, (Boolean) value);
		else if (CAS.TYPE_NAME_BYTE_ARRAY.equals(name)) 
			((ByteArrayFS)array).set(index, (Byte) value);
		else if (CAS.TYPE_NAME_DOUBLE_ARRAY.equals(name)) 
			((DoubleArrayFS)array).set(index, (Double) value);
		else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(name)) 
			((FloatArrayFS)array).set(index, (Float) value);
		else if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(name)) 
			((IntArrayFS)array).set(index, (Integer) value);
		else if (CAS.TYPE_NAME_LONG_ARRAY.equals(name)) 
			((LongArrayFS)array).set(index, (Long) value);
		else if (CAS.TYPE_NAME_SHORT_ARRAY.equals(name)) 
			((ShortArrayFS)array).set(index, (Short) value);
		else if (CAS.TYPE_NAME_STRING_ARRAY.equals(name)) 
			((StringArrayFS)array).set(index, (String) value);
		else  
			((ArrayFS)array).set(index, (FeatureStructure) value);
	}

	public static Object getArrayValue(CommonArrayFS array, int index)  {
		// Taking a shortcut
		try {
			return array.getClass().getMethod("get", int.class)
					.invoke(array, index);
		} catch (Exception e) {
			//TODO handle it better
			throw new RuntimeException(e);
		}
	}
	
	public static Type getType(TypeSystem ts, Object value) {
		if (value instanceof String)
			return ts.getType(CAS.TYPE_NAME_STRING);
		if (value instanceof Integer)
			return ts.getType(CAS.TYPE_NAME_INTEGER);
		if (value instanceof Float)
			return ts.getType(CAS.TYPE_NAME_FLOAT);
		if (value instanceof Byte)
			return ts.getType(CAS.TYPE_NAME_BYTE);
		if (value instanceof Short)
			return ts.getType(CAS.TYPE_NAME_SHORT);
		if (value instanceof Long)
			return ts.getType(CAS.TYPE_NAME_LONG);
		if (value instanceof Double)
			return ts.getType(CAS.TYPE_NAME_DOUBLE);
		if (value instanceof Boolean)
			return ts.getType(CAS.TYPE_NAME_BOOLEAN);
		if (value instanceof FeatureStructure)
			return ((FeatureStructure)value).getType();
		return null;

	}
}
