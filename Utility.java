import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.function.DoubleUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

public class Utility{
    static {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException error) {
            // Keep the default console encoding if UTF-8 is unavailable.
        }
    }

    public static void pln(Object message){
        System.out.println(message);
    }
    public static void p(Object message){
        System.out.print(message);
    }
    public static double fact(int n){
        double fact=1;
        for(int i=1;i<=n;i++){
            fact*=i;
        }
        return fact;
    }

    public static DoubleUnaryOperator parseFunction(String equation) {
        return new Utility().new FunctionParser().parse(equation);
    }

    public static boolean containsTrig(String expression) {
        if (expression == null) {
            return false;
        }
        String lower = expression.toLowerCase();
        return containsFunctionName(lower, "sin") || containsFunctionName(lower, "cos")
                || containsFunctionName(lower, "tan") || containsFunctionName(lower, "cot")
                || containsFunctionName(lower, "sec") || containsFunctionName(lower, "cosec")
                || containsFunctionName(lower, "csc") || containsFunctionName(lower, "asin")
                || containsFunctionName(lower, "acos") || containsFunctionName(lower, "atan");
    }

    private static boolean containsFunctionName(String expression, String functionName) {
        return Pattern.compile("(^|[^a-zA-Z])" + Pattern.quote(functionName) + "([^a-zA-Z]|$)")
                .matcher(expression).find() || expression.startsWith(functionName);
    }

    public static String extractFinalExpression(String steps) {
        if (steps == null || steps.trim().isEmpty()) {
            return "";
        }
        String[] lines = steps.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("=>")) {
                return line.substring(2).trim();
            }
        }
        return lines[lines.length - 1].trim();
    }

    public static String removeTrailingConstant(String expression) {
        if (expression == null) {
            return "";
        }
        return expression.trim()
                .replaceAll("(?i)\\s*\\+\\s*C\\s*$", "")
                .replaceAll("(?i)\\s*-\\s*C\\s*$", "")
                .trim();
    }

    public static Double readDoubleGracefully(Scanner input, String prompt) {
        while (true) {
            pln(prompt);
            String value = input.nextLine().trim();
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException error) {
                pln("Invalid numeric input. Please enter an integer or decimal value.");
            }
        }
    }

    public static String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        double rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.000000001) {
            return String.valueOf((long) rounded);
        }
        String text = String.format(java.util.Locale.US, "%.6f", value);
        text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        return text.equals("-0") ? "0" : text;
    }

    public static String substituteVariable(String expression, double value, boolean trigExpression) {
        return substituteVariable(expression, value, trigExpression, 'x');
    }

    public static String substituteVariable(String expression, double value, boolean trigExpression, char variable) {
        String displayValue = formatNumber(value);
        String variablePattern = Pattern.quote(String.valueOf(variable));
        String substituted = expression;
        substituted = substituted.replaceAll("(?i)(sin|cos|tan|cot|sec|cosec|csc|asin|acos|atan)(\\^\\d+)?\\s*\\(?\\s*" + variablePattern + "\\s*\\)?",
                "$1$2(" + displayValue + (trigExpression ? "\u00b0" : "") + ")");
        substituted = substituted.replaceAll("(?<![a-zA-Z])" + variablePattern + "(?![a-zA-Z])", "(" + displayValue + ")");
        return substituted;
    }

    public static String substituteVariableForEvaluation(String expression, double value) {
        return substituteVariableForEvaluation(expression, value, 'x');
    }

    public static String substituteVariableForEvaluation(String expression, double value, char variable) {
        String displayValue = formatNumber(value);
        String variablePattern = Pattern.quote(String.valueOf(variable));
        String substituted = expression;
        substituted = substituted.replaceAll("(?i)(sin|cos|tan|cot|sec|cosec|csc|asin|acos|atan)(\\^\\d+)?\\s*\\(?\\s*" + variablePattern + "\\s*\\)?",
                "$1$2(" + formatAngleAsPi(value) + ")");
        substituted = substituted.replaceAll("(?<![a-zA-Z])" + variablePattern + "(?![a-zA-Z])", "(" + displayValue + ")");
        return substituted;
    }

    public static void displayEvaluationSteps(String expression, double userValue) {
        boolean trigExpression = containsTrig(expression);
        String expressionForEvaluation = trigExpression ? expressionWithTrigArgumentsInRadians(expression, userValue) : expression;
        String substitutionStep = substituteVariable(expression, userValue, trigExpression);
        String evaluationSubstitutionStep = trigExpression ? substituteVariableForEvaluation(expression, userValue) : substitutionStep;

        pln("=> " + substitutionStep);
        if (trigExpression && !evaluationSubstitutionStep.equals(substitutionStep)) {
            pln("=> " + evaluationSubstitutionStep);
        }

        String partialStep = evaluateTopLevelParts(expressionForEvaluation, userValue);
        if (!partialStep.isEmpty()) {
            pln("=> " + partialStep);
        }

        try {
            DoubleUnaryOperator function = parseFunction(expressionForEvaluation);
            pln("=> " + formatNumber(function.applyAsDouble(userValue)));
        } catch (RuntimeException error) {
            pln("Unable to evaluate expression: " + error.getMessage());
        }
    }

    public static void displayEvaluationSteps(String expression, double userValue, char variable) {
        boolean trigExpression = containsTrig(expression);
        String substitutionStep = substituteVariable(expression, userValue, trigExpression, variable);
        String evaluationSubstitutionStep = trigExpression ? substituteVariableForEvaluation(expression, userValue, variable) : substitutionStep;

        pln("=> " + substitutionStep);
        if (trigExpression && !evaluationSubstitutionStep.equals(substitutionStep)) {
            pln("=> " + evaluationSubstitutionStep);
        }

        String simplified = simplifySymbolicSubstitution(evaluationSubstitutionStep);
        if (!simplified.isEmpty() && !simplified.equals(evaluationSubstitutionStep)) {
            pln("=> " + simplified);
        }
    }

    public static String simplifySymbolicSubstitution(String expression) {
        try {
            return ChainRule.simplifyExpression(expression);
        } catch (RuntimeException error) {
            return "";
        }
    }

    public static String expressionWithTrigArgumentsInRadians(String expression, double userValue) {
        return expressionWithTrigArgumentsInRadians(expression, userValue, 'x');
    }

    public static String expressionWithTrigArgumentsInRadians(String expression, double userValue, char variable) {
        Pattern pattern = Pattern.compile("(?i)(sin|cos|tan|cot|sec|cosec|csc|asin|acos|atan)(\\^\\d+)?\\s*\\(?\\s*x\\s*\\)?");
        if (variable != 'x') {
            pattern = Pattern.compile("(?i)(sin|cos|tan|cot|sec|cosec|csc|asin|acos|atan)(\\^\\d+)?\\s*\\(?\\s*" + Pattern.quote(String.valueOf(variable)) + "\\s*\\)?");
        }
        Matcher matcher = pattern.matcher(expression);
        StringBuffer result = new StringBuffer();
        String radians = formatNumber(Math.toRadians(userValue));
        while (matcher.find()) {
            String function = matcher.group(1);
            String power = matcher.group(2) == null ? "" : matcher.group(2);
            String replacement = function + "(" + radians + ")" + power;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static List<Character> findVariables(String expression) {
        LinkedHashSet<Character> variables = new LinkedHashSet<>();
        if (expression == null) {
            return new ArrayList<>(variables);
        }

        String value = ChainRule.normalizeSuperscripts(expression).toLowerCase();
        for (int i = 0; i < value.length(); i++) {
            int functionLength = functionNameLengthAt(value, i);
            if (functionLength > 0) {
                i += functionLength - 1;
                continue;
            }

            char current = value.charAt(i);
            if (Character.isLetter(current)) {
                variables.add(current);
            }
        }
        return new ArrayList<>(variables);
    }

    public static boolean containsVariable(List<Character> variables, char variable) {
        for (char current : variables) {
            if (Character.toLowerCase(current) == Character.toLowerCase(variable)) {
                return true;
            }
        }
        return false;
    }

    public static String formatVariableList(List<Character> variables) {
        StringBuilder result = new StringBuilder("(");
        for (int i = 0; i < variables.size(); i++) {
            if (i > 0) {
                result.append(" , ");
            }
            result.append(variables.get(i));
        }
        result.append(")");
        return result.toString();
    }

    private static int functionNameLengthAt(String expression, int index) {
        String[] functions = {"cosec", "sqrt", "asin", "acos", "atan", "sin", "cos", "tan", "cot", "sec", "csc", "exp", "ln"};
        for (String function : functions) {
            if (expression.startsWith(function, index)) {
                return function.length();
            }
        }
        if (expression.startsWith("e^", index)) {
            return 2;
        }
        return 0;
    }

    public static String evaluateTopLevelParts(String expression, double value) {
        List<String> parts = splitTopLevelAdditive(expression);
        if (parts.size() <= 1) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            char sign = '+';
            if (trimmed.charAt(0) == '+' || trimmed.charAt(0) == '-') {
                sign = trimmed.charAt(0);
                trimmed = trimmed.substring(1).trim();
            }
            try {
                double evaluated = parseFunction(trimmed).applyAsDouble(value);
                if (result.length() == 0) {
                    if (sign == '-') {
                        result.append("-");
                    }
                    result.append(formatNumber(evaluated));
                } else {
                    result.append(sign == '-' ? " - " : " + ").append(formatNumber(evaluated));
                }
            } catch (RuntimeException error) {
                return "";
            }
        }
        return result.toString();
    }

    public static List<String> splitTopLevelAdditive(String expression) {
        ArrayList<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < expression.length(); i++) {
            char current = expression.charAt(i);
            if (current == '(' || current == '[') {
                depth++;
            } else if (current == ')' || current == ']') {
                depth = Math.max(0, depth - 1);
            } else if (depth == 0 && (current == '+' || current == '-') && isBinarySign(expression, i)) {
                if (i > start) {
                    parts.add(expression.substring(start, i).trim());
                }
                start = i;
            }
        }
        if (start < expression.length()) {
            parts.add(expression.substring(start).trim());
        }
        return parts;
    }

    private static boolean isBinarySign(String expression, int index) {
        int previous = index - 1;
        while (previous >= 0 && Character.isWhitespace(expression.charAt(previous))) {
            previous--;
        }
        if (previous < 0) {
            return false;
        }
        char previousChar = expression.charAt(previous);
        return previousChar != '+' && previousChar != '-' && previousChar != '*' && previousChar != '/'
                && previousChar != '^' && previousChar != '(';
    }

    public static String formatAngleAsPi(double degrees) {
        double normalized = degrees % 360.0;
        if (normalized < 0) {
            normalized += 360.0;
        }
        int rounded = (int) Math.round(normalized);
        if (Math.abs(normalized - rounded) > 0.000000001) {
            return formatNumber(Math.toRadians(degrees));
        }
        int gcd = gcdInt(Math.abs(rounded), 180);
        int numerator = rounded / gcd;
        int denominator = 180 / gcd;
        if (numerator == 0) {
            return "0";
        }
        if (denominator == 1) {
            return numerator == 1 ? "\u03c0" : numerator + "\u03c0";
        }
        return (numerator == 1 ? "\u03c0" : numerator + "\u03c0") + "/" + denominator;
    }

    private static int gcdInt(int first, int second) {
        while (second != 0) {
            int remainder = first % second;
            first = second;
            second = remainder;
        }
        return first == 0 ? 1 : first;
    }

    public final class FunctionParser {

        private FunctionParser() {
        }

        /**
         * Converts an equation such as:
         *
         * x^2 + 3*x
         * sin(x)
         * sin x
         * cos x + x^2
         * sqrt(x)
         *
         * into a DoubleUnaryOperator.
         */
        public DoubleUnaryOperator parse(String equation) {

            String expr = preprocess(equation);

            Expression expression = new ExpressionBuilder(expr)
                    .variables("x")
                    .functions(
                        new Function("sec", 1) {
                            public double apply(double... args) {
                                return 1.0 / Math.cos(args[0]);
                            }
                        },
                        new Function("csc", 1) {
                            public double apply(double... args) {
                                return 1.0 / Math.sin(args[0]);
                            }
                        },
                        new Function("cosec", 1) {
                            public double apply(double... args) {
                                return 1.0 / Math.sin(args[0]);
                            }
                        },
                        new Function("ln", 1) {
                            public double apply(double... args) {
                                return Math.log(args[0]);
                            }
                        }
                    )
                    .build();

            return x -> {
                expression.setVariable("x", x);
                return expression.evaluate();
            };
        }

        /**
         * Converts common mathematical notation into exp4j notation.
         */
        private String preprocess(String input) {

            String s = input;

            s = s.replaceAll("\\s+", "");
            s = s.replace("[", "(").replace("]", ")");
            s = s.replaceAll("(?i)ln\\|([^|]+)\\|", "ln($1)");
            s = s.replace("|", "");

            // Constants
            s = s.replace("\u00cf\u20ac", "pi");
            s = s.replace("\u03c0", "pi");
            s = s.replaceAll("(?i)cosec", "csc");
            s = wrapFunctionPower(s);

            // sin x -> sin(x)
            s = wrapFunction(s, "sin");
            s = wrapFunction(s, "cos");
            s = wrapFunction(s, "tan");

            s = wrapFunction(s, "asin");
            s = wrapFunction(s, "acos");
            s = wrapFunction(s, "atan");

            s = wrapFunction(s, "sqrt");
            s = wrapFunction(s, "log10");
            s = wrapFunction(s, "log");
            s = wrapFunction(s, "ln");
            s = wrapFunction(s, "abs");
            s = wrapFunction(s, "exp");
            s = wrapFunction(s, "cot");
            s = wrapFunction(s, "sec");
            s = wrapFunction(s, "csc");

            s = insertImplicitMultiplication(s);

            return s;
        }

        private String insertImplicitMultiplication(String expression) {
            String functions = "(sin|cos|tan|cot|sec|csc|asin|acos|atan|sqrt|log10|log|ln|abs|exp)";
            String s = expression;
            s = s.replaceAll("(?<=[0-9])(?=x)", "*");
            s = s.replaceAll("(?<=[0-9])(?=" + functions + "\\()", "*");
            s = s.replaceAll("(?<=x)(?=[0-9])", "*");
            s = s.replaceAll("(?<=x)(?=\\()", "*");
            s = s.replaceAll("(?<=\\))(?=x|[0-9])", "*");
            s = s.replaceAll("(?<=\\))(?=" + functions + "\\()", "*");
            return s;
        }

        private String wrapFunctionPower(String expression) {
            Pattern pattern = Pattern.compile("(?i)(sin|cos|tan|cot|sec|csc)\\^(\\d+)(x|\\([^()]*\\)|[0-9.]+)");
            Matcher matcher = pattern.matcher(expression);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String function = matcher.group(1);
                String power = matcher.group(2);
                String argument = matcher.group(3);
                if (argument.startsWith("(") && argument.endsWith(")")) {
                    argument = argument.substring(1, argument.length() - 1);
                }
                matcher.appendReplacement(result, function + "(" + argument + ")^" + power);
            }
            matcher.appendTail(result);
            return result.toString();
        }

        /**
         * Converts
         *
         * sinx      -> sin(x)
         * sin2*x    -> sin(2*x)
         * sin(x)    -> unchanged
         * sinx^2    -> sin(x^2)
         */
        private String wrapFunction(String expression, String function) {

            Pattern p = Pattern.compile(
                    function + "(?!\\()([a-zA-Z0-9_.]+(?:\\^[a-zA-Z0-9_.]+)?)");

            Matcher m = p.matcher(expression);

            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String arg = m.group(1);
                m.appendReplacement(sb, function + "(" + arg + ")");
            }

            m.appendTail(sb);

            return sb.toString();
        }
    }
}
