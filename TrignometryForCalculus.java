
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

public class TrignometryForCalculus {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the trigonometric equation:");
        String input = scanner.nextLine();
        System.out.println(simplifyEquation(input));
        scanner.close();
    }

    public static String simplifyEquation(String equation) {
        if (equation == null || equation.trim().isEmpty()) {
            return "";
        }

        Parser parser = new Parser(equation);
        Expr expression = parser.parse();
        List<String> steps = new ArrayList<>();
        steps.add(equation.trim());

        boolean changed;
        do {
            SimplifyResult result = simplifyOnce(expression);
            changed = result.changed;
            expression = result.expression;
            if (changed) {
                String formatted = expression.format();
                if (!steps.get(steps.size() - 1).equals(formatted)) {
                    steps.add(formatted);
                }
            }
        } while (changed);

        List<String> displaySteps = compressDisplaySteps(steps);
        StringBuilder output = new StringBuilder("Result : \n");
        output.append(displaySteps.get(0));
        for (int i = 1; i < displaySteps.size(); i++) {
            output.append("\n=> ").append(displaySteps.get(i));
        }
        return output.toString();
    }

    public static String simplifyEquationRaw(String equation) {
        if (equation == null || equation.trim().isEmpty()) {
            return "";
        }

        Parser parser = new Parser(equation);
        Expr expression = parser.parse();
        List<String> steps = new ArrayList<>();
        steps.add(equation.trim());

        boolean changed;
        do {
            SimplifyResult result = simplifyOnce(expression);
            changed = result.changed;
            expression = result.expression;
            if (changed) {
                String formatted = expression.format();
                if (!steps.get(steps.size() - 1).equals(formatted)) {
                    steps.add(formatted);
                }
            }
        } while (changed);

        // Return just the final simplified expression
        return steps.get(steps.size() - 1);
    }

    private static List<String> compressDisplaySteps(List<String> steps) {
        if (steps.size() <= 3) {
            return steps;
        }

        List<String> displaySteps = new ArrayList<>();
        displaySteps.add(steps.get(0));
        displaySteps.add(steps.get(steps.size() - 2));
        displaySteps.add(steps.get(steps.size() - 1));
        return displaySteps;
    }

    private static SimplifyResult simplifyOnce(Expr expression) {
        SimplifyResult childrenSimplified = expression.simplifyChildren();
        if (childrenSimplified.changed) {
            return childrenSimplified;
        }
        return applyIdentity(expression);
    }

    private static SimplifyResult applyIdentity(Expr expression) {
        if (expression instanceof Trig) {
            Trig trig = (Trig) expression;
            Expr arg = trig.argument;

            if (trig.power == 1 && isNegative(arg)) {
                Expr positiveArg = ((UnaryMinus) arg).inner;
                if (trig.function.equals("sin")) {
                    return changed(new UnaryMinus(new Trig("sin", positiveArg, 1)));
                }
                if (trig.function.equals("cos")) {
                    return changed(new Trig("cos", positiveArg, 1));
                }
                if (trig.function.equals("tan")) {
                    return changed(new Trig("tan", positiveArg, 1));
                }
            }

            if (trig.function.equals("tan") && trig.power == 1 && arg.equals(mul(num(2), sym("x")))) {
                Expr numerator = mul(num(2), new Trig("tan", sym("x"), 1));
                Expr denominator = sub(num(1), new Trig("tan", sym("x"), 2));
                return changed(div(numerator, denominator));
            }
        }

        if (expression instanceof Binary) {
            Binary binary = (Binary) expression;
            Expr left = binary.left;
            Expr right = binary.right;

            if (binary.operator.equals("+") || binary.operator.equals("-")) {
                SimplifyResult flatSumIdentity = simplifySignedTerms(binary);
                if (flatSumIdentity.changed) {
                    return flatSumIdentity;
                }
            }

            if (binary.operator.equals("+")) {
                SimplifyResult sumIdentity = simplifyAddition(left, right);
                if (sumIdentity.changed) {
                    return sumIdentity;
                }
            }

            if (binary.operator.equals("-")) {
                SimplifyResult differenceIdentity = simplifySubtraction(left, right);
                if (differenceIdentity.changed) {
                    return differenceIdentity;
                }
            }

            if (binary.operator.equals("*")) {
                SimplifyResult productIdentity = simplifyProduct(binary);
                if (productIdentity.changed) {
                    return productIdentity;
                }
            }

            if (binary.operator.equals("/")) {
                SimplifyResult quotientIdentity = simplifyQuotient(left, right);
                if (quotientIdentity.changed) {
                    return quotientIdentity;
                }
            }
        }

        if (expression instanceof Power) {
            Power power = (Power) expression;
            if (power.base instanceof Trig) {
                Trig trig = (Trig) power.base;
                return changed(new Trig(trig.function, trig.argument, trig.power * power.exponent));
            }
            SimplifyResult baseIdentity = simplifyAngleFormula(power.base);
            if (baseIdentity.changed && baseIdentity.expression instanceof Trig) {
                Trig compact = (Trig) baseIdentity.expression;
                return changed(new Trig(compact.function, compact.argument, power.exponent));
            }
        }

        SimplifyResult angleIdentity = simplifyAngleFormula(expression);
        if (angleIdentity.changed) {
            return angleIdentity;
        }

        return unchanged(expression);
    }

    private static SimplifyResult simplifySignedTerms(Expr expression) {
        List<SignedTerm> terms = new ArrayList<>();
        collectSignedTerms(expression, 1, terms);

        for (int i = 0; i < terms.size(); i++) {
            for (int j = i + 1; j < terms.size(); j++) {
                SignedTerm first = terms.get(i);
                SignedTerm second = terms.get(j);
                SignedTerm replacement = signedIdentity(first, second);
                if (replacement != null) {
                    terms.set(i, replacement);
                    terms.remove(j);
                    return changed(rebuildSignedTerms(terms));
                }
            }
        }

        SimplifyResult numericResult = combineNumericTerms(terms);
        if (numericResult.changed) {
            return numericResult;
        }

        return unchanged(expression);
    }

    private static void collectSignedTerms(Expr expression, int sign, List<SignedTerm> terms) {
        if (expression instanceof Binary && (((Binary) expression).operator.equals("+") || ((Binary) expression).operator.equals("-"))) {
            Binary binary = (Binary) expression;
            collectSignedTerms(binary.left, sign, terms);
            collectSignedTerms(binary.right, binary.operator.equals("+") ? sign : -sign, terms);
        } else {
            terms.add(new SignedTerm(sign, expression));
        }
    }

    private static SimplifyResult combineNumericTerms(List<SignedTerm> terms) {
        int total = 0;
        int numberCount = 0;
        List<SignedTerm> remaining = new ArrayList<>();

        for (SignedTerm term : terms) {
            if (term.expression instanceof NumberExpr) {
                total += term.sign * ((NumberExpr) term.expression).value;
                numberCount++;
            } else if (term.expression instanceof UnaryMinus && ((UnaryMinus) term.expression).inner instanceof NumberExpr) {
                total -= term.sign * ((NumberExpr) ((UnaryMinus) term.expression).inner).value;
                numberCount++;
            } else {
                remaining.add(term);
            }
        }

        if (numberCount > 1 || (numberCount == 1 && total == 0 && !remaining.isEmpty())) {
            if (total != 0 || remaining.isEmpty()) {
                remaining.add(new SignedTerm(total < 0 ? -1 : 1, num(Math.abs(total))));
            }
            return changed(rebuildSignedTerms(remaining));
        }

        return unchanged(rebuildSignedTerms(terms));
    }

    private static SignedTerm signedIdentity(SignedTerm first, SignedTerm second) {
        if (first.expression.equals(second.expression)) {
            if (first.sign != second.sign) {
                return new SignedTerm(1, num(0));
            }
            return new SignedTerm(first.sign, mul(num(2), first.expression));
        }

        if (sameTrigPower(first.expression, second.expression, "sin", "cos", 2)
            || sameTrigPower(first.expression, second.expression, "cos", "sin", 2)) {
            if (first.sign == second.sign) {
                return new SignedTerm(first.sign, num(1));
            }
        }

        if (sameTrigPower(first.expression, second.expression, "tan", "sec", 2)) {
            if (first.sign == 1 && second.sign == -1) {
                return new SignedTerm(-1, num(1));
            }
            if (first.sign == -1 && second.sign == 1) {
                return new SignedTerm(1, num(1));
            }
        }
        if (sameTrigPower(first.expression, second.expression, "sec", "tan", 2)) {
            if (first.sign == -1 && second.sign == 1) {
                return new SignedTerm(-1, num(1));
            }
            if (first.sign == 1 && second.sign == -1) {
                return new SignedTerm(1, num(1));
            }
        }

        if (sameTrigPower(first.expression, second.expression, "cot", "cosec", 2)) {
            if (first.sign == 1 && second.sign == -1) {
                return new SignedTerm(-1, num(1));
            }
            if (first.sign == -1 && second.sign == 1) {
                return new SignedTerm(1, num(1));
            }
        }
        if (sameTrigPower(first.expression, second.expression, "cosec", "cot", 2)) {
            if (first.sign == -1 && second.sign == 1) {
                return new SignedTerm(-1, num(1));
            }
            if (first.sign == 1 && second.sign == -1) {
                return new SignedTerm(1, num(1));
            }
        }

        if (sameTrigPower(first.expression, second.expression, "cos", "sin", 2)) {
            if (first.sign == 1 && second.sign == -1) {
                return new SignedTerm(1, new Trig("cos", mul(num(2), trigArgument(first.expression)), 1));
            }
            if (first.sign == -1 && second.sign == 1) {
                return new SignedTerm(-1, new Trig("cos", mul(num(2), trigArgument(first.expression)), 1));
            }
        }
        if (sameTrigPower(first.expression, second.expression, "sin", "cos", 2)) {
            if (first.sign == -1 && second.sign == 1) {
                return new SignedTerm(1, new Trig("cos", mul(num(2), trigArgument(first.expression)), 1));
            }
            if (first.sign == 1 && second.sign == -1) {
                return new SignedTerm(-1, new Trig("cos", mul(num(2), trigArgument(first.expression)), 1));
            }
        }

        SignedTerm reverse = reverseIdentity(first, second);
        if (reverse != null) {
            return reverse;
        }
        return reverseIdentity(second, first);
    }

    private static SignedTerm reverseIdentity(SignedTerm first, SignedTerm second) {
        if (first.sign == 1 && first.expression.equals(num(1)) && second.sign == -1) {
            if (isTrigPower(second.expression, "cos", 2)) {
                return new SignedTerm(1, new Trig("sin", trigArgument(second.expression), 2));
            }
            if (isTrigPower(second.expression, "sin", 2)) {
                return new SignedTerm(1, new Trig("cos", trigArgument(second.expression), 2));
            }
        }

        if (first.sign == 1 && isTrigPower(first.expression, "tan", 2)
            && second.sign == -1 && second.expression.equals(num(1))) {
            return new SignedTerm(1, new Trig("sec", trigArgument(first.expression), 2));
        }
        if (first.sign == 1 && first.expression.equals(num(1))
            && second.sign == 1 && isTrigPower(second.expression, "sec", 2)) {
            return new SignedTerm(1, new Trig("tan", trigArgument(second.expression), 2));
        }
        if (first.sign == 1 && isTrigPower(first.expression, "cot", 2)
            && second.sign == -1 && second.expression.equals(num(1))) {
            return new SignedTerm(1, new Trig("cosec", trigArgument(first.expression), 2));
        }
        if (first.sign == 1 && first.expression.equals(num(1))
            && second.sign == 1 && isTrigPower(second.expression, "cosec", 2)) {
            return new SignedTerm(1, new Trig("cot", trigArgument(second.expression), 2));
        }

        return null;
    }

    private static Expr rebuildSignedTerms(List<SignedTerm> terms) {
        if (terms.isEmpty()) {
            return num(0);
        }

        Expr result = applySign(terms.get(0).expression, terms.get(0).sign);
        for (int i = 1; i < terms.size(); i++) {
            SignedTerm term = terms.get(i);
            if (term.sign < 0) {
                result = sub(result, term.expression);
            } else {
                result = add(result, term.expression);
            }
        }
        return result;
    }

    private static Expr applySign(Expr expression, int sign) {
        if (sign >= 0) {
            return expression;
        }
        if (expression instanceof NumberExpr) {
            return num(-((NumberExpr) expression).value);
        }
        if (expression instanceof UnaryMinus) {
            return ((UnaryMinus) expression).inner;
        }
        return new UnaryMinus(expression);
    }

    private static SimplifyResult simplifyAddition(Expr left, Expr right) {
        List<Expr> terms = flatten("+", add(left, right));
        int numericTotal = 0;
        int numberCount = 0;
        List<Expr> nonNumericTerms = new ArrayList<>();

        for (Expr term : terms) {
            if (term instanceof NumberExpr) {
                numericTotal += ((NumberExpr) term).value;
                numberCount++;
            } else {
                nonNumericTerms.add(term);
            }
        }

        if (numberCount > 1 || (numberCount == 1 && numericTotal == 0 && !nonNumericTerms.isEmpty())) {
            if (numericTotal != 0 || nonNumericTerms.isEmpty()) {
                nonNumericTerms.add(num(numericTotal));
            }
            return changed(join("+", nonNumericTerms));
        }

        if (sameTrigPower(left, right, "sin", "cos", 2)) {
            return changed(num(1));
        }
        if (sameTrigPower(left, right, "cos", "sin", 2)) {
            return changed(num(1));
        }
        return unchanged(add(left, right));
    }

    private static SimplifyResult simplifySubtraction(Expr left, Expr right) {
        if (sameTrigPower(left, right, "tan", "sec", 2)) {
            return changed(num(1));
        }
        if (sameTrigPower(left, right, "cot", "cosec", 2)) {
            return changed(num(1));
        }
        if (sameTrigPower(left, right, "cos", "sin", 2)) {
            Expr arg = trigArgument(left);
            return changed(new Trig("cos", mul(num(2), arg), 1));
        }
        if (left.equals(num(1)) && isTrigPower(right, "cos", 2)) {
            return changed(new Trig("sin", trigArgument(right), 2));
        }
        if (left.equals(num(1)) && isTrigPower(right, "sin", 2)) {
            return changed(new Trig("cos", trigArgument(right), 2));
        }
        if (isTrigPower(left, "tan", 2) && right.equals(num(1))) {
            return changed(new Trig("sec", trigArgument(left), 2));
        }
        if (isTrigPower(left, "cot", 2) && right.equals(num(1))) {
            return changed(new Trig("cosec", trigArgument(left), 2));
        }

        return unchanged(sub(left, right));
    }

    private static SimplifyResult simplifyProduct(Binary expression) {
        List<Expr> factors = flatten("*", expression);
        if (factors.size() == 3
            && factors.contains(num(2))
            && containsTrig(factors, "sin", 1)
            && containsTrig(factors, "cos", 1)) {

            Expr sinArg = trigArgument(firstTrig(factors, "sin", 1));
            Expr cosArg = trigArgument(firstTrig(factors, "cos", 1));
            if (sinArg.equals(cosArg)) {
                return changed(new Trig("sin", mul(num(2), sinArg), 1));
            }
        }
        return unchanged(expression);
    }

    private static SimplifyResult simplifyQuotient(Expr left, Expr right) {
        if (!left.equals(num(1)) || !(right instanceof Trig)) {
            return unchanged(div(left, right));
        }

        Trig denominator = (Trig) right;
        if (denominator.power != 1) {
            return unchanged(div(left, right));
        }

        if (denominator.function.equals("sin")) {
            return changed(new Trig("cosec", denominator.argument, 1));
        }
        if (denominator.function.equals("cosec")) {
            return changed(new Trig("sin", denominator.argument, 1));
        }
        if (denominator.function.equals("cos")) {
            return changed(new Trig("sec", denominator.argument, 1));
        }
        if (denominator.function.equals("sec")) {
            return changed(new Trig("cos", denominator.argument, 1));
        }
        if (denominator.function.equals("tan")) {
            return changed(new Trig("cot", denominator.argument, 1));
        }
        if (denominator.function.equals("cot")) {
            return changed(new Trig("tan", denominator.argument, 1));
        }

        return unchanged(div(left, right));
    }

    private static SimplifyResult simplifyAngleFormula(Expr expression) {
        if (!(expression instanceof Binary)) {
            return unchanged(expression);
        }

        Binary binary = (Binary) expression;
        Expr left = binary.left;
        Expr right = binary.right;

        if (binary.operator.equals("+")) {
            AngleProduct first = angleProduct(left);
            AngleProduct second = angleProduct(right);
            if (first != null && second != null) {
                if (first.matches("sin", "cos") && second.matches("sin", "cos")) {
                    return changed(new Trig("sin", add(first.firstArg, first.secondArg), 1));
                }
                if (first.matches("cos", "cos") && second.matches("sin", "sin")) {
                    return changed(new Trig("cos", sub(first.firstArg, first.secondArg), 1));
                }
            }
        }

        if (binary.operator.equals("-")) {
            AngleProduct first = angleProduct(left);
            AngleProduct second = angleProduct(right);
            if (first != null && second != null) {
                if (first.matches("sin", "cos") && second.matches("sin", "cos")) {
                    return changed(new Trig("sin", sub(first.firstArg, first.secondArg), 1));
                }
                if (first.matches("cos", "cos") && second.matches("sin", "sin")) {
                    return changed(new Trig("cos", add(first.firstArg, first.secondArg), 1));
                }
            }
        }

        return unchanged(expression);
    }

    private static AngleProduct angleProduct(Expr expression) {
        if (!(expression instanceof Binary) || !((Binary) expression).operator.equals("*")) {
            return null;
        }
        List<Expr> factors = flatten("*", expression);
        if (factors.size() != 2 || !(factors.get(0) instanceof Trig) || !(factors.get(1) instanceof Trig)) {
            return null;
        }

        Trig first = (Trig) factors.get(0);
        Trig second = (Trig) factors.get(1);
        if (first.power != 1 || second.power != 1) {
            return null;
        }

        return new AngleProduct(first.function, first.argument, second.function, second.argument);
    }

    private static boolean sameTrigPower(Expr left, Expr right, String leftFunction, String rightFunction, int power) {
        return isTrigPower(left, leftFunction, power)
            && isTrigPower(right, rightFunction, power)
            && trigArgument(left).equals(trigArgument(right));
    }

    private static boolean isTrigPower(Expr expression, String function, int power) {
        return expression instanceof Trig
            && ((Trig) expression).function.equals(function)
            && ((Trig) expression).power == power;
    }

    private static Expr trigArgument(Expr expression) {
        return ((Trig) expression).argument;
    }

    private static boolean containsTrig(List<Expr> expressions, String function, int power) {
        return firstTrig(expressions, function, power) != null;
    }

    private static Expr firstTrig(List<Expr> expressions, String function, int power) {
        for (Expr expression : expressions) {
            if (isTrigPower(expression, function, power)) {
                return expression;
            }
        }
        return null;
    }

    private static List<Expr> flatten(String operator, Expr expression) {
        List<Expr> result = new ArrayList<>();
        if (expression instanceof Binary && ((Binary) expression).operator.equals(operator)) {
            Binary binary = (Binary) expression;
            result.addAll(flatten(operator, binary.left));
            result.addAll(flatten(operator, binary.right));
        } else {
            result.add(expression);
        }
        return result;
    }

    private static Expr join(String operator, List<Expr> expressions) {
        if (expressions.isEmpty()) {
            return num(0);
        }
        Expr result = expressions.get(0);
        for (int i = 1; i < expressions.size(); i++) {
            result = new Binary(operator, result, expressions.get(i));
        }
        return result;
    }

    private static boolean isNegative(Expr expression) {
        return expression instanceof UnaryMinus;
    }

    private static SimplifyResult changed(Expr expression) {
        return new SimplifyResult(expression, true);
    }

    private static SimplifyResult unchanged(Expr expression) {
        return new SimplifyResult(expression, false);
    }

    private static NumberExpr num(int value) {
        return new NumberExpr(value);
    }

    private static Symbol sym(String name) {
        return new Symbol(name);
    }

    private static Binary add(Expr left, Expr right) {
        return new Binary("+", left, right);
    }

    private static Binary sub(Expr left, Expr right) {
        return new Binary("-", left, right);
    }

    private static Binary mul(Expr left, Expr right) {
        return new Binary("*", left, right);
    }

    private static Binary div(Expr left, Expr right) {
        return new Binary("/", left, right);
    }

    private interface Expr {
        SimplifyResult simplifyChildren();
        String format();
        int precedence();
    }

    private static class SimplifyResult {
        final Expr expression;
        final boolean changed;

        SimplifyResult(Expr expression, boolean changed) {
            this.expression = expression;
            this.changed = changed;
        }
    }

    private static class NumberExpr implements Expr {
        final int value;

        NumberExpr(int value) {
            this.value = value;
        }

        public SimplifyResult simplifyChildren() {
            return unchanged(this);
        }

        public String format() {
            return String.valueOf(value);
        }

        public int precedence() {
            return 5;
        }

        public boolean equals(Object other) {
            return other instanceof NumberExpr && value == ((NumberExpr) other).value;
        }

        public int hashCode() {
            return Objects.hash(value);
        }
    }

    private static class Symbol implements Expr {
        final String name;

        Symbol(String name) {
            this.name = name;
        }

        public SimplifyResult simplifyChildren() {
            return unchanged(this);
        }

        public String format() {
            return name;
        }

        public int precedence() {
            return 5;
        }

        public boolean equals(Object other) {
            return other instanceof Symbol && name.equals(((Symbol) other).name);
        }

        public int hashCode() {
            return Objects.hash(name);
        }
    }

    private static class Monomial implements Expr {
        final int coefficient;
        final String variable;
        final int power;

        Monomial(int coefficient, String variable, int power) {
            this.coefficient = coefficient;
            this.variable = variable;
            this.power = power;
        }

        public SimplifyResult simplifyChildren() {
            return unchanged(this);
        }

        public String format() {
            if (power == 0) {
                return String.valueOf(coefficient);
            }
            if (power == 1) {
                return coefficient + variable;
            }
            return coefficient + variable + "^" + power;
        }

        public int precedence() {
            return 5;
        }

        public boolean equals(Object other) {
            if (!(other instanceof Monomial)) {
                return false;
            }
            Monomial monomial = (Monomial) other;
            return coefficient == monomial.coefficient
                && variable.equals(monomial.variable)
                && power == monomial.power;
        }

        public int hashCode() {
            return Objects.hash(coefficient, variable, power);
        }
    }

    private static class UnaryMinus implements Expr {
        final Expr inner;

        UnaryMinus(Expr inner) {
            this.inner = inner;
        }

        public SimplifyResult simplifyChildren() {
            SimplifyResult result = simplifyOnce(inner);
            if (result.changed) {
                return changed(new UnaryMinus(result.expression));
            }
            return unchanged(this);
        }

        public String format() {
            return "-" + wrap(inner, precedence());
        }

        public int precedence() {
            return 4;
        }

        public boolean equals(Object other) {
            return other instanceof UnaryMinus && inner.equals(((UnaryMinus) other).inner);
        }

        public int hashCode() {
            return Objects.hash(inner);
        }
    }

    private static class Trig implements Expr {
        final String function;
        final Expr argument;
        final int power;

        Trig(String function, Expr argument, int power) {
            this.function = function;
            this.argument = argument;
            this.power = power;
        }

        public SimplifyResult simplifyChildren() {
            SimplifyResult result = simplifyOnce(argument);
            if (result.changed) {
                return changed(new Trig(function, result.expression, power));
            }
            return unchanged(this);
        }

        public String format() {
            String functionText = power == 1 ? function : function + "^" + power;
            return functionText + " " + formatArgument(argument);
        }

        public int precedence() {
            return 5;
        }

        public boolean equals(Object other) {
            if (!(other instanceof Trig)) {
                return false;
            }
            Trig trig = (Trig) other;
            return function.equals(trig.function) && argument.equals(trig.argument) && power == trig.power;
        }

        public int hashCode() {
            return Objects.hash(function, argument, power);
        }
    }

    private static class Power implements Expr {
        final Expr base;
        final int exponent;

        Power(Expr base, int exponent) {
            this.base = base;
            this.exponent = exponent;
        }

        public SimplifyResult simplifyChildren() {
            SimplifyResult result = simplifyOnce(base);
            if (result.changed) {
                if (result.expression instanceof Trig) {
                    Trig trig = (Trig) result.expression;
                    return changed(new Trig(trig.function, trig.argument, trig.power * exponent));
                }
                return changed(new Power(result.expression, exponent));
            }
            return unchanged(this);
        }

        public String format() {
            return wrap(base, precedence()) + "^" + exponent;
        }

        public int precedence() {
            return 4;
        }

        public boolean equals(Object other) {
            return other instanceof Power
                && base.equals(((Power) other).base)
                && exponent == ((Power) other).exponent;
        }

        public int hashCode() {
            return Objects.hash(base, exponent);
        }
    }

    private static class Binary implements Expr {
        final String operator;
        final Expr left;
        final Expr right;

        Binary(String operator, Expr left, Expr right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        public SimplifyResult simplifyChildren() {
            SimplifyResult leftResult = simplifyOnce(left);
            if (leftResult.changed) {
                return changed(new Binary(operator, leftResult.expression, right));
            }

            SimplifyResult rightResult = simplifyOnce(right);
            if (rightResult.changed) {
                return changed(new Binary(operator, left, rightResult.expression));
            }

            return unchanged(this);
        }

        public String format() {
            if (operator.equals("*") && left instanceof NumberExpr && right instanceof Symbol) {
                return left.format() + right.format();
            }
            return wrap(left, precedence()) + " " + operator + " " + wrapRight(right, precedence(), operator);
        }

        public int precedence() {
            if (operator.equals("+") || operator.equals("-")) {
                return 1;
            }
            return 2;
        }

        public boolean equals(Object other) {
            if (!(other instanceof Binary)) {
                return false;
            }
            Binary binary = (Binary) other;
            return operator.equals(binary.operator) && left.equals(binary.left) && right.equals(binary.right);
        }

        public int hashCode() {
            return Objects.hash(operator, left, right);
        }
    }

    private static class AngleProduct {
        final String firstFunction;
        final Expr firstArg;
        final String secondFunction;
        final Expr secondArg;

        AngleProduct(String firstFunction, Expr firstArg, String secondFunction, Expr secondArg) {
            this.firstFunction = firstFunction;
            this.firstArg = firstArg;
            this.secondFunction = secondFunction;
            this.secondArg = secondArg;
        }

        boolean matches(String expectedFirst, String expectedSecond) {
            return firstFunction.equals(expectedFirst) && secondFunction.equals(expectedSecond);
        }
    }

    private static class SignedTerm {
        final int sign;
        final Expr expression;

        SignedTerm(int sign, Expr expression) {
            this.sign = sign < 0 ? -1 : 1;
            this.expression = expression;
        }
    }

    private static String wrap(Expr expression, int parentPrecedence) {
        if (expression.precedence() < parentPrecedence) {
            return "(" + expression.format() + ")";
        }
        return expression.format();
    }

    private static String wrapRight(Expr expression, int parentPrecedence, String operator) {
        if (expression.precedence() < parentPrecedence
            || ((operator.equals("-") || operator.equals("/")) && expression.precedence() == parentPrecedence)) {
            return "(" + expression.format() + ")";
        }
        return expression.format();
    }

    private static String formatArgument(Expr argument) {
        if (argument instanceof Symbol || argument instanceof NumberExpr || argument instanceof UnaryMinus) {
            return argument.format();
        }
        if (argument instanceof Binary && ((Binary) argument).operator.equals("*")) {
            return argument.format();
        }
        return "(" + argument.format() + ")";
    }

    private static class Parser {
        private final List<Token> tokens;
        private int position;

        Parser(String input) {
            tokens = tokenize(input);
        }

        Expr parse() {
            Expr expression = parseExpression();
            if (!peek().type.equals("EOF")) {
                throw new IllegalArgumentException("Unexpected token: " + peek().text);
            }
            return expression;
        }

        private Expr parseExpression() {
            Expr expression = parseTerm();
            while (match("+") || match("-")) {
                String operator = previous().text;
                Expr right = parseTerm();
                expression = new Binary(operator, expression, right);
            }
            return expression;
        }

        private Expr parseTerm() {
            Expr expression = parsePower();
            while (match("*") || match("/")) {
                String operator = previous().text;
                Expr right = parsePower();
                expression = new Binary(operator, expression, right);
            }
            return expression;
        }

        private Expr parsePower() {
            Expr expression = parsePrimary();
            if (match("^")) {
                Token exponent = consumeType("NUMBER", "Expected a number after ^");
                int value = Integer.parseInt(exponent.text);
                if (expression instanceof Trig) {
                    Trig trig = (Trig) expression;
                    return new Trig(trig.function, trig.argument, value);
                }
                return new Power(expression, value);
            }
            return expression;
        }

        private Expr parsePrimary() {
            if (match("-")) {
                return new UnaryMinus(parsePrimary());
            }
            if (match("(")) {
                Expr expression = parseExpression();
                consume(")", "Expected )");
                return expression;
            }
            if (checkType("NUMBER")) {
                Token number = advance();
                int coefficient = Integer.parseInt(number.text);
                if (checkType("NAME") && isVariable(peek().text)) {
                    String variable = advance().text;
                    int power = 1;
                    if (match("^")) {
                        power = Integer.parseInt(consumeType("NUMBER", "Expected variable power").text);
                    }
                    return new Monomial(coefficient, variable, power);
                }
                return new NumberExpr(coefficient);
            }
            if (checkType("NAME")) {
                Token name = advance();
                String value = normalizeName(name.text);
                if (isTrigFunction(value)) {
                    int power = 1;
                    if (match("^")) {
                        power = Integer.parseInt(consumeType("NUMBER", "Expected trig power").text);
                    }
                    return new Trig(value, parsePrimary(), power);
                }
                return new Symbol(value);
            }

            throw new IllegalArgumentException("Unexpected token: " + peek().text);
        }

        private boolean match(String text) {
            if (peek().text.equals(text)) {
                advance();
                return true;
            }
            return false;
        }

        private void consume(String text, String message) {
            if (!match(text)) {
                throw new IllegalArgumentException(message);
            }
        }

        private Token consumeType(String type, String message) {
            if (!checkType(type)) {
                throw new IllegalArgumentException(message);
            }
            return advance();
        }

        private boolean checkType(String type) {
            return peek().type.equals(type);
        }

        private Token advance() {
            if (position < tokens.size()) {
                position++;
            }
            return previous();
        }

        private Token peek() {
            return tokens.get(position);
        }

        private Token previous() {
            return tokens.get(position - 1);
        }
    }

    private static List<Token> tokenize(String input) {
        String value = input.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        List<Token> tokens = new ArrayList<>();

        for (int i = 0; i < value.length();) {
            char current = value.charAt(i);
            if ("+-*/^()".indexOf(current) >= 0) {
                tokens.add(new Token(String.valueOf(current), String.valueOf(current)));
                i++;
            } else if (Character.isDigit(current)) {
                int start = i;
                while (i < value.length() && Character.isDigit(value.charAt(i))) {
                    i++;
                }
                tokens.add(new Token("NUMBER", value.substring(start, i)));
            } else if (Character.isLetter(current)) {
                String trigName = trigNameAt(value, i);
                if (trigName != null) {
                    tokens.add(new Token("NAME", trigName));
                    i += trigName.length();
                } else {
                    tokens.add(new Token("NAME", String.valueOf(current)));
                    i++;
                }
            } else {
                throw new IllegalArgumentException("Invalid character: " + current);
            }
        }

        tokens.add(new Token("EOF", ""));
        return tokens;
    }

    private static String trigNameAt(String value, int start) {
        for (String function : new String[] {"cosec", "csc", "sin", "cos", "tan", "cot", "sec", "ctn"}) {
            if (value.startsWith(function, start)) {
                return function;
            }
        }
        return null;
    }

    private static String normalizeName(String name) {
        if (name.equals("csc")) {
            return "cosec";
        }
        if (name.equals("ctn")) {
            return "cot";
        }
        return name;
    }

    private static boolean isTrigFunction(String name) {
        return Arrays.asList("sin", "cos", "tan", "cot", "sec", "cosec").contains(name);
    }

    private static boolean isVariable(String name) {
        return !isTrigFunction(normalizeName(name));
    }

    private static class Token {
        final String type;
        final String text;

        Token(String type, String text) {
            this.type = type;
            this.text = text;
        }
    }
}
