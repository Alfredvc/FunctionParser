# FunctionParser
A Java library for dynamic parsing, creation and loading of functions from Strings.
[Javadoc](http://alfredvc.github.io/FunctionParser/)

# Simple Example
```java
ParsedFunction function = FunctionParser.fromString("boolean(Integer x,y)-> x > y");
System.out.println(function.evaluateToBoolean(new Integer[]{2, 3}));
```

# Parametrized example
```java
ParsedFunction<Point> func = FunctionParser.fromString("java.awt.Point(java.awt.Point a,b)->return new java.awt.Point(a.x + b.x,a.y + b.y);");
Object[] args = {new Point(3, 5), new Point(5, 2)};
Point result = func.evaluate(args);
System.out.println(result);
```

# Complex function example
```java
ParsedFunction function = FunctionParser.fromString("double(java.util.List l)->double tot = 0; for(java.util.Iterator iterator = ((java.util.List) l).iterator(); iterator.hasNext(); ){ Object o = iterator.next(); tot+=((Double)o).doubleValue();} return tot;");
Object[] args = new Object[]{Arrays.asList(1.0,2.0,3.0,4.0,5.0)};
System.out.println(function.evaluateToDouble(args));
```

# Maven dependency
```xml
<dependency>
  <groupId>com.github.alfredvc</groupId>
  <artifactId>FunctionParser</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```