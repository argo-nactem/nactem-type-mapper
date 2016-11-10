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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;

import uk.ac.nactem.argo.components.typemapper.TypeMapperBaseVisitor;
import uk.ac.nactem.argo.components.typemapper.TypeMapperLexer;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser;
import uk.ac.nactem.argo.components.typemapper.TypeMap.ComparisonOperator;
import uk.ac.nactem.argo.components.typemapper.TypeMap.Condition;
import uk.ac.nactem.argo.components.typemapper.TypeMap.FeatureMap;
import uk.ac.nactem.argo.components.typemapper.TypeMap.FeaturePath;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser.ArrayIndexContext;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser.ConditionContext;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser.FeatureMapContext;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser.FeatureMapsContext;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser.FeaturePathContext;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser.FeatureValueContext;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser.MapContext;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser.MapsContext;
import uk.ac.nactem.argo.components.typemapper.TypeMapperParser.PathFeatureContext;

public class TypeMapBuilder  {

	public static void main(String[] args) throws Exception {
		String inputFile = null;
		inputFile = args[0];
		InputStream is = new FileInputStream(inputFile);

		List<TypeMap> result = build(is);
		for(int i=0; i<result.size(); ++i) {
			System.out.println(result.get(i));
		}
	}

	public static List<TypeMap> build(InputStream mappingStream) throws IOException, ParseException {
		return build(new ANTLRInputStream(mappingStream));
	}

	public static List<TypeMap> build(String mappingString) throws ParseException {
		return build(new ANTLRInputStream(mappingString));
	}

	private static List<TypeMap> build(ANTLRInputStream input) throws ParseException {
		TypeMapperLexer lexer = new TypeMapperLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		TypeMapperParser parser = new TypeMapperParser(tokens);
		ParseException parseException = new ParseException();
		parser.removeErrorListeners();
		parser.addErrorListener(new ErrorListener(parseException));
		ParseTree tree = parser.maps(); // parse

		if (parseException.getSyntaxErrors().size()>0) 
			throw parseException;

		Visitor visitor = new Visitor();
		return visitor.visit(tree);
	}

	public static class ParseException extends Exception {
		private static final long serialVersionUID = 3890597938489487785L;
		private List<String> errorsSink = new ArrayList<String>();
		public ParseException() {
			super("Syntax error(s) found in the mapping");
		}

		void addSyntaxError(int line, int charPositionInLine, String msg)  {
			errorsSink.add("Line "+line+", char "+charPositionInLine+": "+msg);
		}

		public List<String> getSyntaxErrors() {
			return errorsSink;
		}

		@Override
		public String getMessage() {
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<errorsSink.size(); i++) {
				if (i>0) sb.append('\n');
				sb.append(errorsSink.get(i));
			}
			return sb.toString();
		}
	}

	private static class ErrorListener extends BaseErrorListener {
		private ParseException exception;
		ErrorListener(ParseException exception) {
			this.exception = exception;
		}

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer,
				Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
			exception.addSyntaxError(line, charPositionInLine, msg);
		}
	}

	private static class Visitor extends TypeMapperBaseVisitor<Object> {
		@SuppressWarnings("unchecked")
		@Override
		public List<TypeMap> visit(ParseTree tree) {
			return (List<TypeMap>) super.visit(tree);
		}

		@Override
		public List<TypeMap> visitMaps(MapsContext ctx) {
			int i=0;
			List<TypeMap> typeMaps = new ArrayList<TypeMap>();
			while(true) {
				MapContext mapCtx = ctx.map(i++);
				if (mapCtx==null) break;
				TypeMap map = visitMap(mapCtx);
				typeMaps.add(map);
			}
			return typeMaps;
		}

		@Override
		public TypeMap visitMap(MapContext ctx) {
			TypeMap map = new TypeMap(ctx.sourceTypeName().getText(), ctx.targetTypeName().getText());
			if (ctx.condition()!=null)
				map.setCondition(visitCondition(ctx.condition()));
			if (ctx.featureMaps()!=null) 
				map.setFeatureMaps(visitFeatureMaps(ctx.featureMaps()));
			return map;
		}

		@Override
		public Condition visitCondition(ConditionContext ctx) {
			Condition condition = new Condition();
			FeaturePath featurePath = visitFeaturePath(ctx.featurePath());
			condition.setFeaturePath(featurePath);
			condition.setComparisonOperator(ComparisonOperator.recognise(ctx.COMPARISON_OPERATOR().getText()));
			condition.setValue(visitFeatureValue(ctx.featureValue()));
			return condition;
		}

		@Override
		public FeaturePath visitFeaturePath(FeaturePathContext ctx) {
			FeaturePath featurePath = new FeaturePath();
			int i=0;
			while(true) {
				PathFeatureContext pathFeatureCtx = ctx.pathFeature(i++);
				if (pathFeatureCtx==null) break;
				featurePath.add(pathFeatureCtx.featureName().getText());
				if (pathFeatureCtx.arrayIndex()!=null) {
					featurePath.add(visitArrayIndex(pathFeatureCtx.arrayIndex()));
				}
			}
			return featurePath;
		}

		@Override
		public Integer visitArrayIndex(ArrayIndexContext ctx) {
			return Integer.parseInt(ctx.INT().getText());
		}

		@Override
		public Object visitFeatureValue(FeatureValueContext ctx) {
			if (ctx.STRING()!=null) {
				String quotedString = ctx.STRING().getText();
				return quotedString.substring(1, quotedString.length()-1);
			}
			if (ctx.INT()!=null) {
				return Integer.parseInt(ctx.INT().getText());
			}
			if (ctx.FLOAT()!=null) {
				return Float.parseFloat(ctx.FLOAT().getText());
			}
			return null;
		}

		@Override
		public List<FeatureMap> visitFeatureMaps(FeatureMapsContext ctx) {
			List<FeatureMap> featureMaps = new ArrayList<FeatureMap>();
			int i=0;
			while(true) {
				FeatureMapContext featureMapCtx = ctx.featureMap(i++);
				if (featureMapCtx==null) break;
				featureMaps.add(visitFeatureMap(featureMapCtx));
			}
			return featureMaps;
		}

		@Override
		public FeatureMap visitFeatureMap(FeatureMapContext ctx) {
			if (ctx.featureValue()!=null) {
				return new FeatureMap(visitFeatureValue(ctx.featureValue()),
						visitFeaturePath(ctx.featurePath(0)));
			}
			else {
				return new FeatureMap(
						visitFeaturePath(ctx.featurePath(0)),
						visitFeaturePath(ctx.featurePath(1))
						);
			}
		}

	}
}
