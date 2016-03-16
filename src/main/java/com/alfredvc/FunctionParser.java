package com.alfredvc;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class used to parse strings into ParsedFunction objects.
 *
 * Since only Object types can be passed as arguments then functions with primitive parameter types
 * are not allowed. Return types can however be primitive types. For example double(double f)->(3*f)
 * is NOT valid use the Object type instead double(Double f)->(3*f)
 *
 *
 * Given strings have a similar syntax to Java functions: returnType(parameterType param1,param2)->
 * EXPRESSION returnType(parameterType param1,param2)-> return EXPRESSION; returnType(parameterType1
 * param1,param2, parameterType2 param3, param 4)-> EXPRESSION returnType(parameterType1 param1,
 * parameterType1 param2, parameterType2 param3, param 4)-> EXPRESSION
 *
 * For example:
 * double(Double x,y,z,f)->(x + y + z + f)
 *
 * double(java.util.List l)->double tot = 1;
 * for(java.util.Iterator iterator = ((java.util.List) l).iterator(); iterator.hasNext(); ){ Object
 * o = iterator.next();tot*=((Double)o).doubleValue();} return tot;
 */
public class FunctionParser {
    public static final String DEFAULT_RETURN_TYPE = "Object";
    public static final String BEHIND = "(?<=\\(|\\)|\\.|\\*|\\+|\\-|\\/|\\s|^|\\%|\\?|;|\\{|\\}|,)";
    public static final String AHEAD = "(?=\\(|\\)|\\.|\\*|\\+|\\-|\\/|\\s|$|\\%|;|\\?|\\{|\\}|,)";
    /**
     * Map of the supported primitive types
     */
    public static final Set<String> supportedPrimitives;
    private static final Map<String, String> primitiveToClass;
    private static final Map<String, String> classToPrimitive;

    static {
        Set<String> set = new HashSet<>();
        set.add("double");
        set.add("float");
        set.add("int");
        set.add("long");
        set.add("boolean");
        set.add("short");
        supportedPrimitives = Collections.unmodifiableSet(set);
    }

    static {
        Map<String, String> map = new HashMap<>();
        map.put("double", "Double");
        map.put("float", "Float");
        map.put("int", "Integer");
        map.put("long", "Long");
        map.put("boolean", "Boolean");
        map.put("short", "Short");
        primitiveToClass = Collections.unmodifiableMap(map);
    }

    static {
        Map<String, String> map = new HashMap<>();
        map.put("Double", "double");
        map.put("Float", "float");
        map.put("Integer", "int");
        map.put("Long", "long");
        map.put("Boolean", "boolean");
        map.put("Short", "short");
        classToPrimitive = Collections.unmodifiableMap(map);
    }

    private FunctionParser() {
        //Intentionally empty.
    }

    /**
     * Parses strings into functions, the functions that can be parsed are subject to the same
     * limitations as in the Javaassist library. For more information on these visit
     * http://jboss-javassist.github.io/.
     *
     * The returned class will override the relevant evaluate method of the ParsedFunction interface
     * with a method that is equivalent to the given functionString. All other evaluate methods
     * return an Unsupported operation exception.
     *
     * There is currently no validation on the given functionString, and it can generate dangerous
     * functions or an error.
     *
     * @param functionString the string to be parsed
     * @return a class implementing the ParsedFunction interface
     * @throws IllegalArgumentException are thrown with nested Javaassist exceptions, most of these
     *                                  exeptions are due to errors in the functionString.
     */
    public static ParsedFunction fromString(String functionString) {
        try {
            //TODO: validate functionString.
            String argsName = "o" + System.currentTimeMillis();
            LinkedHashSet<String> variables = new LinkedHashSet<>();
            String[] tempSplit = functionString.split("\\(");
            String returnType = tempSplit[0].equals("") ? DEFAULT_RETURN_TYPE : tempSplit[0].trim();
            String paramsString = tempSplit[1].split("\\)")[0].trim();

            List<String> types = new ArrayList<>();
            String methodBody = getMethodBody(functionString);
            String[] typesAndVariables = paramsString.split("\\,");
            int varNr = 0;
            String currentType = null;
            String currentVar;
            for (String typeAndVariables : typesAndVariables) {
                String trimmed = typeAndVariables.trim();
                String[] typeAndVariableSplit = trimmed.split("\\s+");
                if (typeAndVariableSplit.length == 2) {
                    currentType = typeAndVariableSplit[0];
                    types.add(currentType);
                    currentVar = typeAndVariableSplit[1];
                } else if (typeAndVariableSplit.length == 1) {
                    currentVar = typeAndVariableSplit[0];
                } else {
                    throw new IllegalArgumentException("Too many arguments near " + typeAndVariables);
                }
                if (currentType == null) {
                    throw new IllegalArgumentException("No argument type found in " + typeAndVariables);
                }
                variables.add(currentVar);
                methodBody = methodBody.replaceAll(BEHIND + currentVar + AHEAD, getReplaceForVariableAndType(currentVar, currentType, varNr, argsName));
                varNr++;
            }


            ClassPool pool = ClassPool.getDefault();
            CtClass evalClass = pool.makeClass("Eval" + System.currentTimeMillis());

            evalClass.addField(new CtField(pool.get("java.util.LinkedHashSet"), "variableSet", evalClass));
            evalClass.addField(new CtField(pool.get("java.lang.String"), "functionString", evalClass));

            evalClass.setInterfaces(
                    new CtClass[]{pool.makeClass("com.alfredvc.ParsedFunction")});


            String methodString = getMethodString(argsName, returnType, methodBody);


            evalClass.addMethod(
                    CtNewMethod.make(methodString, evalClass));

            addHelperMethods(evalClass);

            Class clazz = evalClass.toClass();
            ParsedFunction obj = (ParsedFunction) clazz.newInstance();
            clazz.getMethod("setVariableSet", java.util.LinkedHashSet.class).invoke(obj, variables);
            clazz.getMethod("setFunctionString", java.lang.String.class).invoke(obj, functionString);
            return obj;
        } catch (CannotCompileException | InvocationTargetException | NoSuchMethodException | NotFoundException | IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void addHelperMethods(CtClass evalClass) throws CannotCompileException {
        evalClass.addMethod(
                CtNewMethod.make("public java.util.LinkedHashSet getVariableSet(){return this.variableSet;}", evalClass)
        );

        evalClass.addMethod(
                CtNewMethod.make("public java.lang.String getFunctionString(){return this.functionString;}", evalClass)
        );

        evalClass.addMethod(
                CtNewMethod.make("public java.lang.String toString(){return \"ParsedFunction[\" + this.functionString + \"]\";}", evalClass)
        );

        evalClass.addMethod(
                CtNewMethod.make("public void setVariableSet(java.util.LinkedHashSet s){this.variableSet = s;}", evalClass)
        );

        evalClass.addMethod(
                CtNewMethod.make("public void setFunctionString(java.lang.String s){this.functionString = s;}", evalClass)
        );
    }

    private static String getMethodString(String argsName, String returnType, String methodBody) {
        String methodString;

        if (methodBody.split(BEHIND + "return" + AHEAD).length > 1) {
            methodString = "public " + getMethodNameAndReturnType(returnType) + "(Object[] " + argsName + "){" + methodBody + "}";
        } else {
            methodString = "public " + getMethodNameAndReturnType(returnType) + "(Object[] " + argsName + "){return ((" + returnType + ")(" + methodBody + "));}";
        }
        return methodString;
    }

    private static String getMethodBody(String functionString) {
        return functionString.split("->", 2)[1];
    }

    private static String getReplaceForVariableAndType(String var, String type, int varNr, String argsName) {
        String toReplace;
        String returnType;
        if (classToPrimitive.containsKey(type)) {
            returnType = classToPrimitive.get(type);
            toReplace = "(((" + type + ") " + argsName + "[" + varNr + "])." + returnType + "Value())";
        } else {
            toReplace = "((" + type + ") " + argsName + "[" + varNr + "])";
        }
        return toReplace;
    }


    private static String getMethodNameAndReturnType(String returnType) {
        if (supportedPrimitives.contains(returnType)) {
            return returnType + " " + "evaluateTo" + primitiveToClass.get(returnType);
        } else {
            return "Object evaluateToObject";
        }
    }
}
