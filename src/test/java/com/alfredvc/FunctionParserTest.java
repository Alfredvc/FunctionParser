package com.alfredvc;

import javassist.CannotCompileException;
import javassist.NotFoundException;

import org.junit.Ignore;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.NashornScriptEngine;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for FunctionParser.
 */
public class FunctionParserTest {
    @Test
    public void testSimpleMathematicalFunction() throws CannotCompileException, InstantiationException, NotFoundException, IllegalAccessException {
        double expectedResult = 10.0;
        ParsedFunction constraint = FunctionParser.fromString("double (Double x,y,z,f)->(x + y + z + f)");
        Object[] args = {1.0, 2.0, 3.0, 4.0};
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
        Object[] args = {1.0, 2.0, 3.0, 4.0, true};
        assertThat(constraintFromString.evaluateToDouble(args), is(expectedFirst));
        args[4] = Boolean.FALSE;
        assertThat(constraintFromString.evaluateToDouble(args), is(expectedSecond));
    }

    @Test
    public void testFunctionParsingWithSeveralInputTypesAndRandomSpaces() {
        double expectedFirst = 2.0;
        double expectedSecond = 12.0;
        ParsedFunction constraintFromString = FunctionParser.fromString("  double (     Double   x,   y ,z  ,f, Boolean    a     )   ->       a ? x * y : z * f");
        Object[] args = {1.0, 2.0, 3.0, 4.0, true};
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
        List<Double> list = Arrays.asList(1.0, 2.0, 3.0, 4.0);
        Object[] args = {list};
        assertThat(constraintFromString.evaluateToDouble(args), is(expectedResult));
    }

    /**
     * Simple test to evaluate the performance of generated methods vs compiled methods.
     */
    @Test
    @Ignore
    public void performanceComparisonWithPrimitives() {
        ParsedFunction constraintFromString = FunctionParser.fromString("double(Double x,y,z,f)->x*y + y + z*z + x*f");
        Object[] args = {1.0, 2.0, 3.0, 4.0};

        // Warmup
        for (int a = 0; a < 10; a++) {
            for (long i = 0; i < 1000000000L; i++) {
                constraintFromString.evaluateToDouble(args);
                func(1.0, 2.0, 3.0, 4.0);
            }
        }

        long start1 = System.nanoTime();
        for (long i = 0; i < 1000000000L; i++) {
            constraintFromString.evaluateToDouble(args);
        }
        long end1 = System.nanoTime();
        long start2 = System.nanoTime();
        for (long i = 0; i < 1000000000L; i++) {
            func(1.0, 2.0, 3.0, 4.0);
        }
        long end2 = System.nanoTime();
        long fromStringTime = ((end1 - start1) / 1000000L);
        long functionTime = ((end2 - start2) / 1000000L);
        double percentageDifference = ((double) functionTime / (double) fromStringTime);

        System.out.printf("Method generated from function was %.3fx slower than compiled method", percentageDifference);
    }

    /**
     * Simple test to evaluate the performance of generated primitive vs generic methods
     */
    @Test
    @Ignore
    public void performanceComparisonPrimitiveGeneric() {
        ParsedFunction constraintFromStringPrimitive = FunctionParser.fromString("double(Double x,y,z,f)->x*y + y + z*z + x*f");
        ParsedFunction<Double> constraintFromStringGeneric = FunctionParser.fromString("Double(Double x,y,z,f)->Double.valueOf(x*y + y + z*z + x*f)");
        Object[] args = {1.0, 2.0, 3.0, 4.0};

        // Warmup
        for (int a = 0; a < 100; a++) {
            for (long i = 0; i < 1000000000L; i++) {
                constraintFromStringPrimitive.evaluateToDouble(args);
                constraintFromStringGeneric.evaluate(args);
            }
        }

        long start1 = System.nanoTime();
        for (long i = 0; i < 1000000000L; i++) {
            constraintFromStringPrimitive.evaluateToDouble(args);
        }
        long end1 = System.nanoTime();
        long start2 = System.nanoTime();
        for (long i = 0; i < 1000000000L; i++) {
            constraintFromStringGeneric.evaluate(args);
        }
        long end2 = System.nanoTime();
        long fromStringTime = ((end1 - start1) / 1000000L);
        long functionTime = ((end2 - start2) / 1000000L);
        double percentageDifference = ((double) functionTime / (double) fromStringTime);

        System.out.printf("Method generic method was %.3fx slower than the primitive method.", percentageDifference);
    }

    /**
     * Simple test to evaluate the performance of generated methods vs JavaScript methods via ScriptEngine
     */
    @Test
    @Ignore
    public void performanceComparisonScriptingEngine() throws ScriptException, NoSuchMethodException {
        ParsedFunction constraintFromStringPrimitive = FunctionParser.fromString("double(Double x,y,z,f)->x*y + y + z*z + x*f");
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval("var f1 = function(x,y,z,f){return x*y + y + z*z + x*f;}");
        Invocable invocable = (Invocable) engine;
        Object[] args = {1.0, 2.0, 3.0, 4.0};

        //Warmup
        for (int a = 0; a < 10; a++) {
            for (long i = 0; i < 10000000L; i++) {
                constraintFromStringPrimitive.evaluateToDouble(args);
                invocable.invokeFunction("f1",  args);
            }
        }

        long start1 = System.nanoTime();
        for (long i = 0; i < 10000000000L; i++) {
            constraintFromStringPrimitive.evaluateToDouble(args);
        }
        long end1 = System.nanoTime();
        long start2 = System.nanoTime();
        for (long i = 0; i < 100000000L; i++) {
            invocable.invokeFunction("f1",  args);
        }
        long end2 = System.nanoTime();
        long generatedTime = ((end1 - start1) / 1000000L);
        long scriptTime = ((end2 - start2) / 1000000L);
        System.out.println("Generated " + generatedTime);
        System.out.println("Script " + scriptTime);
        double percentageDifference = ((double) scriptTime / (double) generatedTime);

        System.out.printf("JavaScript method was %.3fx slower than the generated method.", percentageDifference);
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
        Object[] args = {3, 2, 15};
        assertThat(booleanParsedFunction.evaluateToBoolean(args), is(true));
        Object[] args2 = {4, 7, 10};
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


    @Test
    public void testNumberObjectParametrizedFunction_Double(){
        Object[] args = new Object[]{2.0};
        double expectedResult = 4;
        String functionString = "Double(Double x)->x+2.0";
        ParsedFunction<Double> f = FunctionParser.fromString(functionString);
        assertThat(f.evaluate(args) , is(expectedResult));
    }

    @Test
    public void testNumberObjectParametrizedFunction_Boolean(){
        Object[] args = new Object[]{2.0, 3.0};
        boolean expectedResult = false;
        String functionString = "Boolean(Double x,y)->x > y";
        ParsedFunction<Double> f = FunctionParser.fromString(functionString);
        assertThat(f.evaluate(args) , is(expectedResult));
    }

    @Test
    public void readmeSimpleExample(){
        boolean expectedResult = false;
        ParsedFunction function = FunctionParser.fromString("boolean(Integer x,y)-> x > y");
        assertThat(function.evaluateToBoolean(new Integer[]{2, 3}), is(expectedResult));
    }

    @Test
    public void readmeAdvancedExample(){
        Point expectedResult = new Point(8, 7);
        ParsedFunction<Point> func = FunctionParser.fromString("java.awt.Point(java.awt.Point a,b)->return new java.awt.Point(a.x + b.x,a.y + b.y);");
        Object[] args = {new Point(3, 5), new Point(5, 2)};
        Point result = func.evaluate(args);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void readmeComplexFunctionExample() {
        double expectedResult = 15;
        ParsedFunction function = FunctionParser.fromString("double(java.util.List l)->double tot = 0; for(java.util.Iterator iterator = ((java.util.List) l).iterator(); iterator.hasNext(); ){ Object o = iterator.next(); tot+=((Double)o).doubleValue();} return tot;");
        Object[] args = new Object[]{Arrays.asList(1.0,2.0,3.0,4.0,5.0)};
        assertThat(function.evaluateToDouble(args), is(expectedResult));
    }
}
