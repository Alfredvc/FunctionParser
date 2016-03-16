package com.alfredvc;

import java.util.LinkedHashSet;

/**
 * If the specified return type is a supported primitive, then use one of the evaluate functions
 * with primitive return types. If the specified return type is an Object, use evaluate() and
 * parametrize this interface with the return type;
 * <p>
 * For example: for a double return type ParsedFunction func = FunctionParser.fromString("double(Double
 * x,y,z,f)-&gt;(x + y + z + f)"); Object[] args = {Double.valueOf(1.0), Double.valueOf(2.0),
 * Double.valueOf(3.0), Double.valueOf(4.0)}; double result = func.evaluateToDouble(args);
 * <p>
 * For example: for a java.util.Point return type; ParsedFunction&lt;Point&gt; func =
 * FunctionParser.fromString("java.awt.Point(java.awt.Point a,b)-&gt;return new java.awt.Point(a.x +
 * b.x,a.y + b.y);"); Object[] args = {new Point(3, 5), new Point(5, 2)}; Point result =
 * func.evaluate(args);
 *
 * @param <T> the return type of the function. Only used if the return type is an Object and not a
 *            supported primitive.
 */
public interface ParsedFunction<T> {

    default double evaluateToDouble(Object[] args) {
        throw new UnsupportedOperationException();
    }

    default float evaluateToFloat(Object[] args) {
        throw new UnsupportedOperationException();
    }

    default boolean evaluateToBoolean(Object[] args) {
        throw new UnsupportedOperationException();
    }

    default int evaluateToInteger(Object[] args) {
        throw new UnsupportedOperationException();
    }

    default long evaluateToLong(Object[] args) {
        throw new UnsupportedOperationException();
    }

    default short evaluateToShort(Object[] args) {
        throw new UnsupportedOperationException();
    }

    default Object evaluateToObject(Object[] args) {
        throw new UnsupportedOperationException();
    }

    default T evaluate(Object[] args) {
        return (T) evaluateToObject(args);
    }

    /**
     * Returns the string used to parse the function.
     * @return the string used to parse the function
     */
    String getFunctionString();


    /**
     * Forced the use of LinkedHashSet because the order of variables in the variableSet must match
     * the order of the variables in the argument array in the evaluate functions;
     * @return the sorted variable set
     */
    LinkedHashSet<String> getVariableSet();
}
