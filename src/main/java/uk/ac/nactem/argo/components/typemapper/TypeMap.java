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

import java.util.ArrayList;
import java.util.List;

public class TypeMap {
	private String sourceTypeName;
	private String targetTypeName;
	private Condition condition;
	private List<FeatureMap> featureMaps = new ArrayList<FeatureMap>();

	public TypeMap(String sourceTypeName, String targetTypeName) {
		this.sourceTypeName = sourceTypeName;
		this.targetTypeName = targetTypeName;
	}

	public String getSourceTypeName() {
		return sourceTypeName;
	}

	public void setSourceTypeName(String sourceTypeName) {
		this.sourceTypeName = sourceTypeName;
	}

	public String getTargetTypeName() {
		return targetTypeName;
	}

	public void setTargetTypeName(String targetTypeName) {
		this.targetTypeName = targetTypeName;
	}

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public List<FeatureMap> getFeatureMaps() {
		return featureMaps;
	}

	public void setFeatureMaps(List<FeatureMap> featureMaps) {
		this.featureMaps = featureMaps;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(sourceTypeName)
		.append(" (").append(getCondition()).append(") ")
		.append(" => ")
		.append(targetTypeName);

		for(int i=0; i<getFeatureMaps().size(); i++) {
			sb.append(",\n  ").append(getFeatureMaps().get(i).getSourceFeaturePath())
			.append(" => ")
			.append(getFeatureMaps().get(i).getTargetFeaturePath());
		}

		sb.append(';');
		return sb.toString();
	}
	
	public static class Condition {
		private FeaturePath featurePath;
		private ComparisonOperator comparisonOperator;
		private Object value;
		public FeaturePath getFeaturePath() {
			return featurePath;
		}
		public void setFeaturePath(FeaturePath featurePath) {
			this.featurePath = featurePath;
		}
		public ComparisonOperator getComparisonOperator() {
			return comparisonOperator;
		}
		public void setComparisonOperator(ComparisonOperator comparisonOperator) {
			this.comparisonOperator = comparisonOperator;
		}
		public Object getValue() {
			return value;
		}
		public void setValue(Object value) {
			this.value = value;
		}
		@Override
		public String toString() {
			return getFeaturePath() + " " + getComparisonOperator().toString() + " " + getValue();
		}
	}
	
	public enum ComparisonOperator {
		EQUAL("="), 
		NOT_EQUAL("!="), 
		LESS_THAN("<"), 
		GREATER_THAN(">"), 
		LESS_THEN_OR_EQUAL("<="), 
		GREATER_THAN_OR_EQUAL(">=");
		
		String operator;
		ComparisonOperator(String operator) {
			this.operator = operator;
		}
		
		public static ComparisonOperator recognise(String operatorString) {
			for(ComparisonOperator op : values()) {
				if (op.operator.equals(operatorString))
					return op;
			}
			return null;
		}
		
		@Override
		public String toString() {
			return operator;
		}
	}

	public static class FeatureMap {
		// sourceFeaturePath and assigneableValue are mutually exclusive
		private Object assigneableValue;
		private FeaturePath sourceFeaturePath;
		private FeaturePath targetFeaturePath;

		public FeatureMap(FeaturePath sourceFeaturePath, FeaturePath targetFeaturePath) {
			this(null, sourceFeaturePath, targetFeaturePath);
		}
		
		public FeatureMap(Object featureValue, FeaturePath targetFeaturePath) {
			this(featureValue,null,targetFeaturePath);
		}
		
		private FeatureMap(Object featureValue, FeaturePath sourceFeaturePath, 
				FeaturePath targetFeaturePath) {
			this.setAssigneableValue(featureValue);
			this.setSourceFeaturePath(sourceFeaturePath);
			this.setTargetFeaturePath(targetFeaturePath);
		}

		public Object getAssigneableValue() {
			return assigneableValue;
		}

		public void setAssigneableValue(Object featureValue) {
			this.assigneableValue = featureValue;
		}

		public FeaturePath getSourceFeaturePath() {
			return sourceFeaturePath;
		}

		public void setSourceFeaturePath(FeaturePath sourceFeaturePath) {
			this.sourceFeaturePath = sourceFeaturePath;
		}

		public FeaturePath getTargetFeaturePath() {
			return targetFeaturePath;
		}

		public void setTargetFeaturePath(FeaturePath targetFeaturePath) {
			this.targetFeaturePath = targetFeaturePath;
		}
		
		public boolean assignsValueDirectly() {
			return assigneableValue!=null;
		}

		@Override
		public String toString() {
			return (assignsValueDirectly()? assigneableValue : sourceFeaturePath) + 
					" => " + getTargetFeaturePath();
		}
	}

	// Object can be either String or Integer
	public static class FeaturePath extends ArrayList<Object> {
		private static final long serialVersionUID = 1371275060440719427L;
		
		@Override
		public String toString() {
			return toString(size());
		}
		
		public String toString(int segmentLimit) {
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<segmentLimit; ++i) {
				Object featureNameOrIndex = get(i);
				if (featureNameOrIndex instanceof Integer) {
					sb.append('[')
					.append((Integer)featureNameOrIndex)
					.append(']');
				} 
				else {
					if (i>0) sb.append('/');
					sb.append((String)featureNameOrIndex);
				}
			}
			return sb.toString();
		}
		
		public boolean isArrayIndex(int index) {
			return get(index) instanceof Integer;
		}
	}
}
