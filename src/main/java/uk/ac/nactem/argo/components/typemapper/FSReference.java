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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;


interface FSReference {
	Object getValue();
	void setValue(Object value);
	boolean isValueTypePrimitive();
	boolean isValueTypeArray();
	Type getFSType();
	Type getValueType();
	String featureSegmentToString();
	
	static class FSFeatureReference implements FSReference {
		private FeatureStructure fs;
		private Feature feature;

		FSFeatureReference(FeatureStructure fs, Feature feature) {
			this.fs = fs;
			this.feature = feature;
		}

		@Override
		public Object getValue() {
			if (!feature.getRange().isPrimitive()) {
				return fs.getFeatureValue(feature);
			}
			String rangeName = feature.getRange().getName();	
			if (CAS.TYPE_NAME_STRING.equals(rangeName)) {
				return fs.getStringValue(feature);
			} else if (CAS.TYPE_NAME_INTEGER.equals(rangeName)) {
				return fs.getIntValue(feature);
			} else if (CAS.TYPE_NAME_FLOAT.equals(rangeName)) {
				return fs.getFloatValue(feature);
			} else if (CAS.TYPE_NAME_BYTE.equals(rangeName)) {
				return fs.getByteValue(feature);
			} else if (CAS.TYPE_NAME_SHORT.equals(rangeName)) {
				return fs.getShortValue(feature);
			} else if (CAS.TYPE_NAME_LONG.equals(rangeName)) {
				return fs.getLongValue(feature);
			} else if (CAS.TYPE_NAME_DOUBLE.equals(rangeName)) {
				return fs.getDoubleValue(feature);
			} else if (CAS.TYPE_NAME_BOOLEAN.equals(rangeName)) {
				return fs.getBooleanValue(feature);
			}
			return null;
		}

		@Override
		public void setValue(Object value) {
			// TODO setting single-ref arrays

			if (!feature.getRange().isPrimitive()) {
				fs.setFeatureValue(feature, (FeatureStructure) value);
			}
			else {
				String rangeName = feature.getRange().getName();
				if (CAS.TYPE_NAME_STRING.equals(rangeName)) {
					fs.setStringValue(feature, (String) value);
				} else if (CAS.TYPE_NAME_INTEGER.equals(rangeName)) {
					fs.setIntValue(feature, (Integer) value);
				} else if (CAS.TYPE_NAME_FLOAT.equals(rangeName)) {
					fs.setFloatValue(feature, (Float) value);
				} else if (CAS.TYPE_NAME_BYTE.equals(rangeName)) {
					fs.setByteValue(feature, (Byte) value);
				} else if (CAS.TYPE_NAME_SHORT.equals(rangeName)) {
					fs.setShortValue(feature, (Short) value);
				} else if (CAS.TYPE_NAME_LONG.equals(rangeName)) {
					fs.setLongValue(feature, (Long) value);
				} else if (CAS.TYPE_NAME_DOUBLE.equals(rangeName)) {
					fs.setDoubleValue(feature, (Double) value);
				} else if (CAS.TYPE_NAME_BOOLEAN.equals(rangeName)) {
					fs.setBooleanValue(feature, (Boolean) value);
				}
			}
		}

		@Override
		public boolean isValueTypePrimitive() {
			return getValueType().isPrimitive();
		}

		@Override
		public boolean isValueTypeArray() {
			return getValueType().isArray();
		}

		@Override
		public String featureSegmentToString() {
			return feature.getShortName();
		}

		@Override
		public Type getFSType() {
			return fs.getType();
		}

		@Override
		public Type getValueType() {
			return feature.getRange();
		}

	}

	static class ArrayReference implements FSReference {
		CommonArrayFS array;
		int arrayIndex;
		FSFeatureReference parentFSRef;
		public ArrayReference(CommonArrayFS array, int arrayIndex, FSFeatureReference ref) {
			this.array = array;
			this.arrayIndex = arrayIndex;
			this.parentFSRef = ref;
		}

		@Override
		public Object getValue() {
			return UimaUtils.getArrayValue(array, arrayIndex);
		}

		@Override
		public void setValue(Object value) {
			UimaUtils.setArrayValue(array, arrayIndex, value);
		}

		@Override
		public boolean isValueTypePrimitive() {
			if (CAS.TYPE_NAME_FS_ARRAY.equals(array.getType().getName())) {
				return false;
			}
			return true;
		}

		@Override
		public boolean isValueTypeArray() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String featureSegmentToString() {
			return parentFSRef.featureSegmentToString()+"["+arrayIndex+"]";
		}

		@Override
		public Type getFSType() {
			return array.getType();
		}

		@Override
		public Type getValueType() {
			return parentFSRef.feature.getRange().getComponentType();
//			return UimaUtils.getType(array.getCAS().getTypeSystem(), getValue());
		}
	}
}