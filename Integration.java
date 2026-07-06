
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Integration extends Utility {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        pln("Enter the expression:");
        String equation = input.nextLine();
        performIntegration(equation, input);
    }

    public static void performIntegration(String equation, Scanner input) {
        pln("What type of Integration do you want to perform? Enter your choice:");
        pln("[D] - Definite Integration");
        pln("[I] - Indefinite Integration");

        String selectedChoice = input.nextLine().trim();
        char choice = selectedChoice.isEmpty() ? ' ' : selectedChoice.charAt(0);

        switch (choice) {
            case 'd':
            case 'D':
                pln("Under Development");
                break;
            case 'i':
            case 'I':
                pln("\nFinal Result: \n" + integrateIndefinitely(equation));
                break;
            default:
                pln("Invalid choice!");
        }
    }

    public static String integrateIndefinitely(String equation) {
        // Try reverse chain-rule first. Quotient cases such as 2x/(x^2+1)
        // do not always contain a function name, so they still need this pass.
        String chainRuleIntegral = ChainRule.integrateWithSteps(equation);
        if (chainRuleIntegral != null && !chainRuleIntegral.equals("Invalid expression.")) {
            return chainRuleIntegral;
        }

        if (Term.isTrigonometric(equation)) {
            return integrateTrigonometricExpression(equation);
        }

        ParsedExpression parsedExpression = parseExpression(equation);

        char variable = findVariable(parsedExpression.terms, equation);
        String originalExpression = equation.trim();
        ArrayList<Term> simplifiedTerms = simplify(parsedExpression.terms, parsedExpression.operators);
        String simplifiedExpression = formatIntegralExpression(simplifiedTerms);
        String integratedExpression = formatIntegralResult(simplifiedTerms, variable);

        StringBuilder result = new StringBuilder();
        result.append(formatIntegrationNotation(originalExpression, variable));
        if (!originalExpression.equals(simplifiedExpression)) {
            result.append("\n=> ").append(formatIntegrationNotation(simplifiedExpression, variable));
        }
        result.append("\n=> ").append(simplifyIntegratedResult(integratedExpression)).append(" + C");
        return result.toString();
    }

    private static String integrateTrigonometricExpression(String equation) {
        String originalExpression = equation.trim();
        String simplifiedTrig = simplifyTrigParts(equation);
        char variable = Term.FindVariable(simplifiedTrig);

        List<String> trigTerms = new ArrayList<>();
        List<Integer> trigSigns = new ArrayList<>();
        List<String> polyTokens = new ArrayList<>();
        List<String> polyOps = new ArrayList<>();
        Term.parseTrigAndPolyParts(simplifiedTrig, trigTerms, trigSigns, polyTokens, polyOps);

        String polyBodmasResult = Term.applyBODMAS(polyTokens, polyOps);
        String afterBodmas = Term.rebuildWithTrigTerms(trigTerms, trigSigns, polyBodmasResult);

        ArrayList<String> integratedParts = new ArrayList<>();
        addIntegratedPolynomialParts(polyBodmasResult, variable, integratedParts);
        addIntegratedTrigParts(trigTerms, trigSigns, integratedParts, variable);

        String integratedExpression = simplifyIntegratedResult(joinSignedParts(integratedParts));

        StringBuilder result = new StringBuilder();
        result.append(formatIntegrationNotation(originalExpression, variable));
        if (!originalExpression.equals(simplifiedTrig)) {
            result.append("\n=> ").append(formatIntegrationNotation(simplifiedTrig, variable));
        }
        if (!afterBodmas.equals(simplifiedTrig)) {
            result.append("\n=> ").append(formatIntegrationNotation(afterBodmas, variable));
        }
        result.append("\n=> ").append(integratedExpression).append(" + C");
        return result.toString();
    }

    private static String simplifyTrigParts(String equation) {
        try {
            return TrignometryForCalculus.simplifyEquationRaw(equation);
        } catch (RuntimeException wholeExpressionError) {
            List<Term.SignedExpression> parts = Term.splitTopLevelAdditiveTerms(equation);
            StringBuilder rebuilt = new StringBuilder();
            for (Term.SignedExpression part : parts) {
                String expression = Term.trimOuterParentheses(part.expression);
                String simplifiedPart = expression;
                if (Term.isTrigonometric(expression)) {
                    try {
                        simplifiedPart = TrignometryForCalculus.simplifyEquationRaw(expression);
                    } catch (RuntimeException trigPartError) {
                        simplifiedPart = expression;
                    }
                }

                if (rebuilt.length() == 0) {
                    if (part.sign < 0) {
                        rebuilt.append("-");
                    }
                    rebuilt.append(simplifiedPart);
                } else if (part.sign < 0) {
                    rebuilt.append(" - ").append(simplifiedPart);
                } else {
                    rebuilt.append(" + ").append(simplifiedPart);
                }
            }
            return rebuilt.length() == 0 ? equation.trim() : rebuilt.toString();
        }
    }

    private static ParsedExpression parseExpression(String equation) {
        ParsedExpression parsedExpression = new ParsedExpression();
        String[] tokens = ChainRule.normalizeSuperscripts(equation).split(" ");

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/")) {
                parsedExpression.operators.add(token.charAt(0));
            } else {
                Term nextTerm = new Term(normalizeReciprocalToken(token));
                nextTerm.TermAssignment();
                parsedExpression.terms.add(nextTerm);
            }
        }

        return parsedExpression;
    }

    private static String normalizeReciprocalToken(String token) {
        if (token.length() == 1 && Character.isLetter(token.charAt(0))) {
            return "1" + token + "^1";
        }
        if (token.startsWith("-") && token.length() == 2 && Character.isLetter(token.charAt(1))) {
            return "-1" + token.charAt(1) + "^1";
        }
        return token;
    }

    private static ArrayList<Term> simplify(ArrayList<Term> terms, ArrayList<Character> operators) {
        ArrayList<Term> collapsedTerms = new ArrayList<>();
        ArrayList<Character> collapsedOperators = new ArrayList<>();

        if (terms.isEmpty()) {
            collapsedTerms.add(new Term(0, 'x', 0));
            return collapsedTerms;
        }

        Term current = terms.get(0);
        for (int i = 0; i < operators.size() && i + 1 < terms.size(); i++) {
            char operator = operators.get(i);
            Term nextTerm = terms.get(i + 1);

            if (operator == '*') {
                current = Term.MultiplyTerms(current, nextTerm);
            } else if (operator == '/') {
                current = Term.DivideTerms(current, nextTerm);
            } else {
                collapsedTerms.add(current);
                collapsedOperators.add(operator);
                current = nextTerm;
            }
        }
        collapsedTerms.add(current);

        return Term.CombineLikeTerms(collapsedTerms, collapsedOperators);
    }

    private static String formatIntegrationNotation(String expression, char variable) {
        return "(" + expression + ") d" + variable;
    }

    private static String formatIntegralExpression(ArrayList<Term> terms) {
        StringBuilder expression = new StringBuilder();
        for (Term term : terms) {
            if (term.coefficient == 0) {
                continue;
            }

            String formattedTerm = formatTermForIntegralNotation(term);
            if (expression.length() == 0) {
                expression.append(formattedTerm);
            } else if (term.coefficient < 0) {
                expression.append(" - ").append(formatTermForIntegralNotation(new Term(-term.coefficient, term.variable, term.power)));
            } else {
                expression.append(" + ").append(formattedTerm);
            }
        }
        return expression.length() == 0 ? "0" : expression.toString();
    }

    private static String formatTermForIntegralNotation(Term term) {
        if (term.power == -1) {
            if (term.coefficient == 1) {
                return "1 / " + term.variable;
            }
            return term.coefficient + " / " + term.variable;
        }
        return Term.FormatTerm(term);
    }

    private static String formatIntegralResult(ArrayList<Term> terms, char variable) {
        ArrayList<String> integratedParts = new ArrayList<>();
        for (Term term : terms) {
            if (term.coefficient == 0) {
                continue;
            }

            String integratedTerm = integrateTerm(term, variable);
            if (integratedTerm.equals("0")) {
                continue;
            }

            integratedParts.add(integratedTerm);
        }
        return joinSignedParts(integratedParts);
    }

    private static String integrateTerm(Term term, char variable) {
        if (term.power == -1) {
            if (term.coefficient == 1) {
                return "ln " + variable;
            }
            if (term.coefficient == -1) {
                return "-ln " + variable;
            }
            return term.coefficient + " * ln " + variable;
        }

        if (term.power == 0) {
            return formatConstantIntegral(term.coefficient, variable);
        }

        int newPower = term.power + 1;
        if (term.coefficient % newPower == 0) {
            return formatPowerNumerator(term.coefficient / newPower, term.variable, newPower);
        }

        int gcd = gcd(Math.abs(term.coefficient), Math.abs(newPower));
        int reducedCoefficient = term.coefficient / gcd;
        int reducedDenominator = newPower / gcd;
        if (reducedDenominator < 0) {
            reducedCoefficient = -reducedCoefficient;
            reducedDenominator = -reducedDenominator;
        }
        if (reducedDenominator == 1) {
            return formatPowerNumerator(reducedCoefficient, term.variable, newPower);
        }

        String numerator = formatPowerNumerator(reducedCoefficient, term.variable, newPower);
        return numerator + " / " + reducedDenominator;
    }

    private static String formatConstantIntegral(int coefficient, char variable) {
        if (coefficient == 1) {
            return String.valueOf(variable);
        }
        if (coefficient == -1) {
            return "-" + variable;
        }
        return coefficient + String.valueOf(variable);
    }

    private static char findVariable(ArrayList<Term> terms, String equation) {
        for (Term term : terms) {
            if (term.power != 0) {
                return term.variable;
            }
        }
        return Term.FindVariable(equation);
    }

    private static class ParsedExpression {
        ArrayList<Term> terms = new ArrayList<>();
        ArrayList<Character> operators = new ArrayList<>();
    }

    private static void addIntegratedPolynomialParts(String polyExpression, char variable, ArrayList<String> integratedParts) {
        if (polyExpression == null || polyExpression.trim().isEmpty() || polyExpression.equals("0")) {
            return;
        }

        ParsedExpression parsedExpression = parseExpression(polyExpression);
        ArrayList<Term> simplifiedTerms = simplify(parsedExpression.terms, parsedExpression.operators);
        for (Term term : simplifiedTerms) {
            String integratedTerm = integrateTerm(term, variable);
            if (!integratedTerm.equals("0")) {
                integratedParts.add(integratedTerm);
            }
        }
    }

    private static void addIntegratedTrigParts(List<String> trigTerms, List<Integer> trigSigns, ArrayList<String> integratedParts, char variable) {
        for (int i = 0; i < trigTerms.size(); i++) {
            String integratedTerm = integrateTrigTerm(trigTerms.get(i));
            if (integratedTerm.equals("0")) {
                integratedTerm = "(" + trigTerms.get(i) + ") d" + variable;
            }
            if (trigSigns.get(i) < 0) {
                integratedTerm = negateIntegratedTerm(integratedTerm);
            }
            integratedParts.add(integratedTerm);
        }
    }

    private static String integrateTrigTerm(String trigTerm) {
        String term = trigTerm.trim();
        int coefficient = 1;

        int multiplyIndex = term.indexOf("*");
        if (multiplyIndex > 0) {
            String possibleCoefficient = term.substring(0, multiplyIndex).trim();
            if (isInteger(possibleCoefficient)) {
                coefficient = Integer.parseInt(possibleCoefficient);
                term = term.substring(multiplyIndex + 1).trim();
            }
        }

        String lower = term.toLowerCase();
        String function;
        String argument;
        if (lower.startsWith("cosec ")) {
            function = "cosec";
            argument = term.substring(6).trim();
        } else if (lower.startsWith("sin ") || lower.startsWith("cos ") || lower.startsWith("tan ")
                || lower.startsWith("sec ") || lower.startsWith("cot ")) {
            function = lower.substring(0, 3);
            argument = term.substring(4).trim();
        } else if (lower.startsWith("cosec(") && term.endsWith(")")) {
            function = "cosec";
            argument = term.substring(6, term.length() - 1).trim();
        } else if ((lower.startsWith("sin(") || lower.startsWith("cos(") || lower.startsWith("tan(")
                || lower.startsWith("sec(") || lower.startsWith("cot(")) && term.endsWith(")")) {
            function = lower.substring(0, 3);
            argument = term.substring(4, term.length() - 1).trim();
        } else {
            return "0";
        }

        String integrated;
        switch (function) {
            case "sin":
                integrated = "-cos " + argument;
                break;
            case "cos":
                integrated = "sin " + argument;
                break;
            case "tan":
                integrated = "ln |sec " + argument + "|";
                break;
            case "cosec":
                integrated = "-ln |cosec " + argument + " + cot " + argument + "|";
                break;
            case "sec":
                integrated = "ln |sec " + argument + " + tan " + argument + "|";
                break;
            case "cot":
                integrated = "ln |sin " + argument + "|";
                break;
            default:
                integrated = "0";
        }

        if (coefficient == 1 || integrated.equals("0")) {
            return integrated;
        }
        if (coefficient == -1) {
            return negateIntegratedTerm(integrated);
        }
        if (integrated.startsWith("-")) {
            return "-" + Math.abs(coefficient) + " * " + integrated.substring(1);
        }
        return coefficient + " * " + integrated;
    }

    private static String simplifyIntegratedResult(String expression) {
        if (expression == null || expression.trim().isEmpty() || expression.equals("0")) {
            return "0";
        }

        List<SignedPart> parts = splitSignedParts(expression);
        LinkedHashMap<String, Integer> coefficientByBody = new LinkedHashMap<>();
        for (SignedPart part : parts) {
            CoefficientBody coefficientBody = parseCoefficientBody(part.text);
            coefficientByBody.put(
                coefficientBody.body,
                coefficientByBody.getOrDefault(coefficientBody.body, 0) + part.sign * coefficientBody.coefficient
            );
        }

        ArrayList<String> simplifiedParts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : coefficientByBody.entrySet()) {
            int coefficient = entry.getValue();
            if (coefficient == 0) {
                continue;
            }
            simplifiedParts.add(formatCoefficientBody(coefficient, entry.getKey()));
        }

        return joinSignedParts(simplifiedParts);
    }

    private static List<SignedPart> splitSignedParts(String expression) {
        ArrayList<SignedPart> parts = new ArrayList<>();
        int sign = 1;
        int start = 0;
        int depth = 0;
        boolean insideAbsoluteValue = false;
        for (int i = 0; i < expression.length(); i++) {
            char current = expression.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
            } else if (current == '|') {
                insideAbsoluteValue = !insideAbsoluteValue;
            } else if (depth == 0 && !insideAbsoluteValue && (current == '+' || current == '-') && isTopLevelSign(expression, i)) {
                String text = expression.substring(start, i).trim();
                if (!text.isEmpty()) {
                    parts.add(new SignedPart(sign, text));
                }
                sign = current == '-' ? -1 : 1;
                start = i + 1;
            }
        }
        String text = expression.substring(start).trim();
        if (!text.isEmpty()) {
            parts.add(new SignedPart(sign, text));
        }
        return parts;
    }

    private static boolean isTopLevelSign(String expression, int index) {
        int previous = index - 1;
        while (previous >= 0 && Character.isWhitespace(expression.charAt(previous))) {
            previous--;
        }
        if (previous < 0) {
            return false;
        }
        char previousChar = expression.charAt(previous);
        return previousChar != '*' && previousChar != '/' && previousChar != '(' && previousChar != '|';
    }

    private static CoefficientBody parseCoefficientBody(String text) {
        String value = text.trim();
        if (value.startsWith("-")) {
            CoefficientBody inner = parseCoefficientBody(value.substring(1).trim());
            return new CoefficientBody(-inner.coefficient, inner.body);
        }

        int multiplyIndex = value.indexOf("*");
        if (multiplyIndex > 0) {
            String possibleCoefficient = value.substring(0, multiplyIndex).trim();
            if (isInteger(possibleCoefficient)) {
                return new CoefficientBody(Integer.parseInt(possibleCoefficient), value.substring(multiplyIndex + 1).trim());
            }
        }
        return new CoefficientBody(1, value);
    }

    private static String formatCoefficientBody(int coefficient, String body) {
        if (coefficient == 1) {
            return body;
        }
        if (coefficient == -1) {
            return "-" + body;
        }
        return coefficient + " * " + body;
    }

    private static String joinSignedParts(List<String> parts) {
        StringBuilder expression = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty() || part.equals("0")) {
                continue;
            }
            String value = part.trim();
            if (expression.length() == 0) {
                expression.append(value);
            } else if (value.startsWith("-")) {
                expression.append(" - ").append(value.substring(1));
            } else {
                expression.append(" + ").append(value);
            }
        }
        return expression.length() == 0 ? "0" : expression.toString();
    }

    private static String negateIntegratedTerm(String term) {
        return term.startsWith("-") ? term.substring(1) : "-" + term;
    }

    private static String formatPowerNumerator(int coefficient, char variable, int power) {
        String variablePart = power == 1 ? String.valueOf(variable) : variable + "^" + power;
        if (coefficient == 1) {
            return variablePart;
        }
        if (coefficient == -1) {
            return "-" + variablePart;
        }
        return coefficient + variablePart;
    }

    private static int gcd(int first, int second) {
        while (second != 0) {
            int remainder = first % second;
            first = second;
            second = remainder;
        }
        return first == 0 ? 1 : first;
    }

    private static boolean isInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static class SignedPart {
        final int sign;
        final String text;

        SignedPart(int sign, String text) {
            this.sign = sign < 0 ? -1 : 1;
            this.text = text;
        }
    }

    private static class CoefficientBody {
        final int coefficient;
        final String body;

        CoefficientBody(int coefficient, String body) {
            this.coefficient = coefficient;
            this.body = body;
        }
    }
}
