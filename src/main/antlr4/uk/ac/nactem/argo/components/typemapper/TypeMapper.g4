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
grammar TypeMapper;

maps : map ( ';' map)* ';'?  ;

map : sourceTypeName condition? RIGHT_ARROW targetTypeName (',' featureMaps)? ;

sourceTypeName : QUALIFIED_ID ;

targetTypeName : QUALIFIED_ID ;

condition : 'where' featurePath COMPARISON_OPERATOR featureValue ;

featurePath : pathFeature ( '/' pathFeature)* ;

pathFeature : featureName (arrayIndex)? ;

featureName : ID ;

arrayIndex : '[' INT ']' ;

featureValue : INT | FLOAT	| STRING ;

function : coveredText ;

coveredText : 'coveredText(' ')' ;

featureMaps : featureMap (',' featureMap)* ;

featureMap : (featureValue | featurePath) RIGHT_ARROW featurePath ;

RIGHT_ARROW : '->' | '=>' ;

COMPARISON_OPERATOR : '=' | '!=' | '>' | '<' | '>=' | '<=' ;

ID  :	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
    ;

QUALIFIED_ID  :	(
	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')* '.'
	)*
	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')* 
    ;

INT :	'0'..'9'+
    ;

FLOAT
    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.' ('0'..'9')+ EXPONENT?
    |   ('0'..'9')+ EXPONENT
    ;

COMMENT
    :   '#' ~('\n'|'\r')* '\r'? '\n' -> skip
    ;

WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) -> skip
    ;

STRING
    :  '"' ( ESC_SEQ | ~('\\'|'"') )* '"'
    ;

CHAR:  '\'' ( ESC_SEQ | ~('\''|'\\') ) '\''
    ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;
