package com.alfredvc;

import javassist.CannotCompileException;
import javassist.NotFoundException;

import org.junit.Test;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for FunctionParser.
 */
public class FunctionParserTest {
    @Test
    public void testSimpleMathematicalFunction() throws CannotCompileException, InstantiationException, NotFoundException, IllegalAccessException {
        double expectedResult = 10.0;
        ParsedFunction constraint = FunctionParser.fromString("double (Double x,y,z,f)->(x + y + z + f)");
        Object[] args = {Double.valueOf(1.0), Double.valueOf(2.0), Double.valueOf(3.0), Double.valueOf(4.0)};
        assertThat(constraint.evaluateToDouble(args), is(expectedResult));
    }

    @Test
    public void testWithComplexClassesAsInput() {
        double expectedResult = 4.0;
        ParsedFunction constraint = FunctionParser.fromString("double (java.awt.Point a,b)->Math.abs(a.x - b.x) + Math.abs(a.y - b.y)");
        Object[] args = {new Point(0, 0), new Point(2, 2)};
        assertThat(constraint.evaluateToDouble(args), is(expectedResult));
    }

    @Test
    public void testVariableReplaceRegex() {
        String source = "(x +xb.get() +x- x + Matx() + (x))";
        String expected = "(y +xb.get() +y- y + Matx() + (y))";
        assertThat(source.replaceAll(FunctionParser.BEHIND + "x" + FunctionParser.AHEAD, "y"), is(equalTo(expected)));
    }

    @Test
    public void testFunctionParsingWithSeveralInputTypes() {
        double expectedFirst = 2.0;
        double expectedSecond = 12.0;
        ParsedFunction constraintFromString = FunctionParser.fromString("double (Double x,y,z,f, Boolean a)->a ? x * y : z * f");
        Object[] args = {Double.valueOf(1.0), Double.valueOf(2.0), Double.valueOf(3.0), Double.valueOf(4.0), Boolean.TRUE};
        assertThat(constraintFromString.evaluateToDouble(args), is(expectedFirst));
        args[4] = Boolean.FALSE;
        assertThat(constraintFromString.evaluateToDouble(args), is(expectedSecond));
    }

    @Test
    public void testFunctionParsingWithSeveralInputTypesAndRandomSpaces() {
        double expectedFirst = 2.0;
        double expectedSecond = 12.0;
        ParsedFunction constraintFromString = FunctionParser.fromString("  double (     Double   x,   y ,z  ,f, Boolean    a     )   ->       a ? x * y : z * f");
        Object[] args = {Double.valueOf(1.0), Double.valueOf(2.0), Double.valueOf(3.0), Double.valueOf(4.0), Boolean.TRUE};
        assertThat(constraintFromString.evaluateToDouble(args), is(expectedFirst));
        args[4] = Boolean.FALSE;
        assertThat(constraintFromString.evaluateToDouble(args), is(expectedSecond));
    }

    /**
     * This is an example of how to create a method to iterate over lists.
     */
    @Test
    public void testFunctionParsingWithListInput() {
        double expectedResult = 24.0;
        ParsedFunction constraintFromString = FunctionParser.fromString("double (java.util.List l)->double tot = 1; for(java.util.Iterator iterator = ((java.util.List) l).iterator(); iterator.hasNext(); ){ Object o = iterator.next();tot*=((Double)o).doubleValue();} return tot;");
        List<Double> list = Arrays.asList(Double.valueOf(1.0), Double.valueOf(2.0), Double.valueOf(3.0), Double.valueOf(4.0));
        Object[] args = {list};
        assertThat(constraintFromString.evaluateToDouble(args), is(expectedResult));
    }

    /**
     * Simple test to evaluate the performance of generated methods vs compiled methods.
     */
    @Test
    public void performanceComparisonWithPrimitives() {
        ParsedFunction constraintFromString = FunctionParser.fromString("double(Double x,y,z,f)->x*y + y + z*z + x*f");
        Object[] args = {Double.valueOf(1.0), Double.valueOf(2.0), Double.valueOf(3.0), Double.valueOf(4.0)};

        long start1 = System.nanoTime();
        for (long i = 0; i < 1000000000l; i++) {
            constraintFromString.evaluateToDouble(args);
        }
        long end1 = System.nanoTime();
        long start2 = System.nanoTime();
        for (long i = 0; i < 1000000000l; i++) {
            func(1.0, 2.0, 3.0, 4.0);
        }
        long end2 = System.nanoTime();
        long fromStringTime = ((end1 - start1) / 1000000l);
        long functionTime = ((end2 - start2) / 1000000l);
        double percentageDifference = 1.0 - ((double) functionTime / (double) fromStringTime);

        System.out.printf("Method generated from function was %.3f%% slower than compiled method", percentageDifference);
    }


    @Test
    public void testGetVariableSet() {
        LinkedHashSet<String> expectedSet = new LinkedHashSet<>(Arrays.asList("x", "y", "z", "f"));
        ParsedFunction constraintFromString = FunctionParser.fromString("double (Double x,y,z,f)->x*y + y + z*z + x*f");
        assertThat(constraintFromString.getVariableSet(), is(equalTo(expectedSet)));
    }

    @Test
    public void testParametrizedFunction() {
        Point expectedResult = new Point(8, 7);
        Object[] args = {new Point(3, 5), new Point(5, 2)};
        ParsedFunction<Point> pointParsedFunction = FunctionParser.fromString("java.awt.Point(java.awt.Point a,b)->return new java.awt.Point(a.x + b.x,a.y + b.y);");
        assertThat(pointParsedFunction.evaluate(args), is(expectedResult));
    }

    private double func(double x, double y, double z, double f) {
        return (x * y + y + z * z + x * f);
    }

    @Test
    public void testBooleanFuction() {
        ParsedFunction booleanParsedFunction = FunctionParser.fromString("boolean (Integer x,y,z)-> x + y < z");
        Object[] args = {Integer.valueOf(3), Integer.valueOf(2), Integer.valueOf(15)};
        assertThat(booleanParsedFunction.evaluateToBoolean(args), is(true));
        Object[] args2 = {Integer.valueOf(4), Integer.valueOf(7), Integer.valueOf(10)};
        assertThat(booleanParsedFunction.evaluateToBoolean(args2), is(false));
    }

    @Test
    public void testToString() {
        String functionString = "double(Double x,y,z,f)->x*y + y + z*z + x*f";
        String expectedToString = "ParsedFunction[" + functionString + "]";
        ParsedFunction constraintFromString = FunctionParser.fromString(functionString);
        assertThat(constraintFromString.toString(), is(expectedToString));
    }

    @Test
    public void testGetFunctionString() {
        String functionString = "double(Double x,y,z,f)->x*y + y + z*z + x*f";
        ParsedFunction constraintFromString = FunctionParser.fromString(functionString);
        assertThat(constraintFromString.getFunctionString(), is(functionString));
    }
}
