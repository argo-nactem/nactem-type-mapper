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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import uk.ac.nactem.argo.components.typemapper.FSReference.ArrayReference;
import uk.ac.nactem.argo.components.typemapper.FSReference.FSFeatureReference;
import uk.ac.nactem.argo.components.typemapper.TypeMap.ComparisonOperator;
import uk.ac.nactem.argo.components.typemapper.TypeMap.Condition;
import uk.ac.nactem.argo.components.typemapper.TypeMap.FeatureMap;
import uk.ac.nactem.argo.components.typemapper.TypeMap.FeaturePath;
import uk.ac.nactem.argo.components.typemapper.TypeMapBuilder.ParseException;

/**
 * 
 * @author NaCTeM - National Centre of Text Mining
 */
@ResourceMetaData(name="NaCTeM Type Mapper")
public class NactemTypeMapper extends JCasAnnotator_ImplBase {
	
	/**
	 * Definition of mappings from source types to target types.
	 */
	public static final String PARAM_NAME_MAP = "mappingDefinition";
	@ConfigurationParameter(name = PARAM_NAME_MAP,  mandatory = true)
	private String mappingDefinition;
	
	public static final String PARAM_NAME_IGNORE_MISSING_SOURCE = "ignoreMissingSourceType";
	@ConfigurationParameter(name = PARAM_NAME_IGNORE_MISSING_SOURCE, defaultValue = "true", mandatory = false)
	private boolean ignoreMissingSourceType;
	
	public static final String PARAM_NAME_IGNORE_MISSING_TARGET = "ignoreMissingTargetType";
	@ConfigurationParameter(name = PARAM_NAME_IGNORE_MISSING_TARGET, defaultValue = "false", mandatory = false)
	private boolean ignoreMissingTargetType;

	private List<TypeMap> typeMaps = null;
	private TypeSystem currentTs = null;
	private int currentMapIdx = 0;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		try {
			typeMaps = TypeMapBuilder.build(mappingDefinition);
		} catch (ParseException e) {
			throw new ResourceInitializationException(e);
		}

		if (typeMaps==null || typeMaps.size()==0) {
			throw new ResourceInitializationException(new Exception("No type mapping defined."));
		}
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		CAS cas = jcas.getCas();
		FSIndexRepository indexRepo = cas.getIndexRepository();
		if (indexRepo==null) return;

		currentTs = cas.getTypeSystem();
		
		for(currentMapIdx=0; currentMapIdx<typeMaps.size(); ++currentMapIdx) {
			TypeMap typeMap = typeMaps.get(currentMapIdx);
			String sourceTypeName = typeMap.getSourceTypeName();
			Type sourceType = currentTs.getType(sourceTypeName);
			String targetTypeName = typeMap.getTargetTypeName();
			Type targetType = currentTs.getType(targetTypeName);

			if (sourceType!=null && targetType!=null) {
				Set<Feature> commonFeatures = commonFeatures(sourceType, targetType);
				FSIterator<FeatureStructure> fsIter = indexRepo.getAllIndexedFS(sourceType);
				while(fsIter.hasNext()) {
					FeatureStructure sourceFs = fsIter.next();

					Condition condition = typeMap.getCondition();

					try {
						if (conditionSatisfied(sourceFs, condition)) {

							FeatureStructure targetFs = cas.createFS(targetType);

							// copy common features
							copyFeatures(sourceFs, targetFs, commonFeatures);

							List<FeatureMap> featureMaps = typeMap.getFeatureMaps();
							for(FeatureMap featMap : featureMaps) {
								Object value = null;
								if (featMap.assignsValueDirectly()) {
									value = featMap.getAssigneableValue();
								} 
								else {
									FSReference fsRef = extractFSReference(sourceFs, featMap.getSourceFeaturePath());
									value = fsRef.getValue();
								}
								FSReference targetFsRef = extractFSReference(targetFs, featMap.getTargetFeaturePath(), cas);
								targetFsRef.setValue(value);
							}

							cas.addFsToIndexes(targetFs);
						}
					} catch (IncompatibleTypeComparison e) {
						throw new AnalysisEngineProcessException(e);
					} catch (InvalidFeaturePath e) {
						throw new AnalysisEngineProcessException(e);
					}
				}
			}
			else if (targetType==null && !ignoreMissingTargetType) {
				throw new AnalysisEngineProcessException(new Exception(
						"Target type '"+targetTypeName+"' not defined in the current type system.")); 
			}
			else if (sourceType==null && !ignoreMissingSourceType) {
				throw new AnalysisEngineProcessException(new Exception(
						"Source type '"+sourceTypeName+"' not defined in the current type system.")); 
			}
		}
	}

	private boolean conditionSatisfied(FeatureStructure fs, Condition condition)
			throws IncompatibleTypeComparison, InvalidFeaturePath {
		if (condition==null) return true;

		FSReference fsRef = extractFSReference(fs, condition.getFeaturePath());
		Object lhsValue = fsRef.getValue();

		Object rhsValue = condition.getValue();

		if (!lhsValue.getClass().equals(rhsValue.getClass())) {
			throw new IncompatibleTypeComparison(condition);
		}

		@SuppressWarnings("rawtypes")
		Comparable lhs = (Comparable) lhsValue;
		@SuppressWarnings("rawtypes")
		Comparable rhs = (Comparable) rhsValue;
		ComparisonOperator operator = condition.getComparisonOperator();
		@SuppressWarnings("unchecked")
		int score = lhs.compareTo(rhs);
		switch(operator) {
		case EQUAL:
			return score==0;
		case GREATER_THAN:
			return score>0;
		case GREATER_THAN_OR_EQUAL: 
			return score>=0;
		case LESS_THAN:
			return score<0;
		case LESS_THEN_OR_EQUAL:
			return score<=0;
		case NOT_EQUAL:
			return score!=0;
		}
		return false;
	}

	private void copyFeatures(FeatureStructure sourceFs, FeatureStructure targetFs, Set<Feature> features) {
		for(Feature feat : features) {
			if (feat.getRange().isPrimitive()) {
				copyPrimiteFeature(sourceFs, targetFs, feat);
			}
			else {
				// TODO consider arrays
				targetFs.setFeatureValue(feat, sourceFs.getFeatureValue(feat));
			}
		}
	}

	private void copyPrimiteFeature(FeatureStructure sourceFs,
			FeatureStructure targetFs, Feature feat) {
		String rangeName = feat.getRange().getName();
		if (CAS.TYPE_NAME_STRING.equals(rangeName)) {
			targetFs.setStringValue(feat, sourceFs.getStringValue(feat));
		} else if (CAS.TYPE_NAME_INTEGER.equals(rangeName)) {
			targetFs.setIntValue(feat, sourceFs.getIntValue(feat));
		} else if (CAS.TYPE_NAME_FLOAT.equals(rangeName)) {
			targetFs.setFloatValue(feat, sourceFs.getFloatValue(feat));
		} else if (CAS.TYPE_NAME_BYTE.equals(rangeName)) {
			targetFs.setByteValue(feat, sourceFs.getByteValue(feat));
		} else if (CAS.TYPE_NAME_SHORT.equals(rangeName)) {
			targetFs.setShortValue(feat, sourceFs.getShortValue(feat));
		} else if (CAS.TYPE_NAME_LONG.equals(rangeName)) {
			targetFs.setLongValue(feat, sourceFs.getLongValue(feat));
		} else if (CAS.TYPE_NAME_DOUBLE.equals(rangeName)) {
			targetFs.setDoubleValue(feat, sourceFs.getDoubleValue(feat));
		} else if (CAS.TYPE_NAME_BOOLEAN.equals(rangeName)) {
			targetFs.setBooleanValue(feat, sourceFs.getBooleanValue(feat));
		}
	}

	private Set<Feature> commonFeatures(Type type1, Type type2) {
		Set<Feature> type1Features = new HashSet<Feature>(type1.getFeatures());
		Set<Feature> type2Features = new HashSet<Feature>(type2.getFeatures());
		Set<Feature> commonFeatures = new HashSet<Feature>();
		for(Feature feat : type1Features) {
			if (type2Features.contains(feat)) {
				commonFeatures.add(feat);
			}
		}
		return commonFeatures;
	}

	private FSReference extractFSReference(FeatureStructure fs, FeaturePath path) throws InvalidFeaturePath {
		return extractFSReference(fs, path, null);
	}

	private FSReference extractFSReference(FeatureStructure fs, FeaturePath path, CAS cas) throws InvalidFeaturePath {
		FeatureStructure ultimateFs = fs;
		FSReference ref = null;
		for(int pathIndex=0; pathIndex<path.size(); ++pathIndex) {
			if (pathIndex>0) {
				if (ref.isValueTypePrimitive()) {
					throw new InvalidFeaturePath(path, "Unexpected segment after '"+path.toString(pathIndex)+"' whose range is primitive");
				}
				ultimateFs = (FeatureStructure) ref.getValue();

				if (ultimateFs==null) {
					// Proceed only if a CAS is supplied. 
					if (cas==null) {
						throw new InvalidFeaturePath(path, "Intermediate type "+ref.getFSType()+" returned null for feature "+ref.featureSegmentToString());
					}

					Type valueType = ref.getValueType();
					if (valueType.isArray()) {
						if (!path.isArrayIndex(pathIndex)) {
							throw new InvalidFeaturePath(path, "Expected array reference; found '"+path.toString(pathIndex+1)+"'");
						}
						// Create an array of the size of the reference (+1).
						ultimateFs = UimaUtils.createArray(cas, valueType, (Integer)path.get(pathIndex)+1);
					}
					else {
						ultimateFs = cas.createFS(valueType);
					}
					ref.setValue(ultimateFs);
				}
			}

			Type fsType = ultimateFs.getType();
			if (path.isArrayIndex(pathIndex)) { // ref would have been set here
				if (!fsType.isArray()) {
					throw new InvalidFeaturePath(path, "Unexpected array reference '"+path.toString(pathIndex+1)+"' - corresponding type is "+fsType);
				}
				Integer arrayIndex = (Integer) path.get(pathIndex);
				if (arrayIndex>=((CommonArrayFS) ultimateFs).size()) { 
					if (cas==null) {
						throw new InvalidFeaturePath(path, "Index out of bounds in "+path.toString(pathIndex+1));
					}

					CommonArrayFS sourceArray = (CommonArrayFS)ultimateFs;
					CommonArrayFS expandedArray = UimaUtils.createArray(cas, sourceArray.getType(), (Integer)path.get(pathIndex)+1);

					// copy arrays
					for(int i=0; i<sourceArray.size(); ++i) {
						UimaUtils.setArrayValue(expandedArray, i, 
								UimaUtils.getArrayValue(sourceArray,i));
					}
					ultimateFs = expandedArray;
					ref.setValue(ultimateFs);
				}
				ref = new ArrayReference((CommonArrayFS) ultimateFs, (Integer) path.get(pathIndex), (FSFeatureReference) ref);
			}
			else {
				String featName = (String) path.get(pathIndex);
				Feature feature = fsType.getFeatureByBaseName(featName);
				if (feature==null) {
					throw new InvalidFeaturePath(path, "Feature '"+featName+"' is not defined for type "+fsType);
				}
				ref = new FSFeatureReference(ultimateFs, feature);
			}


		}
		return ref;
	}

	
	public class TypeMapperException extends Exception {
		private static final long serialVersionUID = 8851034959805175877L;
		private int mapNumber;

		public TypeMapperException() {
			mapNumber = currentMapIdx+1;
		}

		public TypeMapperException(String message) {
			super(message);
			mapNumber = currentMapIdx+1;
		}


		public int getMapNumber() {
			return mapNumber;
		}
	}

	public class InvalidFeaturePath extends TypeMapperException {
		private static final long serialVersionUID = -6889267525318574245L;
		private FeaturePath path;
		private String extraMessage;

		public InvalidFeaturePath(FeaturePath path) {
			this(path, null);
		}

		public InvalidFeaturePath(FeaturePath path, String extraMessage) {
			this.path = path;
			this.extraMessage = extraMessage;
		}

		public FeaturePath getFeaturePath() {
			return path;
		}

		@Override
		public String getMessage() {
			return "Feature path is not valid in mapping #"+getMapNumber()+": "+
					path+(extraMessage!=null? ". "+extraMessage : "");
		}
	}

	public class IncompatibleTypeComparison extends TypeMapperException {
		private static final long serialVersionUID = -6581276455052640380L;
		private Condition condition;

		public IncompatibleTypeComparison(Condition condition) {
			this.condition = condition;
		}

		public Condition getCondition() {
			return condition;
		}

		@Override
		public String getMessage() {
			return "Types are incompatible in mapping #"+getMapNumber()+" in condition: "+condition;
		}
	}
	
	public static void main(String[] args) {
		
	}

	
}
