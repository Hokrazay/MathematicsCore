import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ChainRule extends Utility {
    private static final String[] FUNCTIONS = {"cosec", "sqrt", "sin", "cos", "tan", "cot", "sec", "csc", "ln", "exp"};

    public static boolean isChainRuleCandidate(String expression) {
        if (expression == null) {
            return false;
        }
        String normalized = normalize(expression);
        if (normalized.contains("sqrt") || normalized.contains("ln") || normalized.contains("exp") || normalized.contains("e^")) {
            return true;
        }
        if (normalized.matches(".*(sin|cos|tan|cot|sec|cosec|csc)\\^.*")) {
            return true;
        }
        for (String function : FUNCTIONS) {
            if (normalized.contains(function + "(")) {
                return true;
            }
        }
        return false;
    }

    public static String differentiateWithSteps(String equation) {
        try {
            String original = equation.trim();
            String converted = convertSqrtToPower(original);
            Expr expression = parse(converted);
            Expr derivative = simplify(expression.derivative());
            String derivativeText = simplifyText(derivative.format());

            StringBuilder result = new StringBuilder();
            result.append(Term.FormatDerivativeNotation(original));
            if (!normalize(original).equals(normalize(converted))) {
                result.append("\n=> ").append(Term.FormatDerivativeNotation(converted));
            }
            result.append("\n=> ").append(derivativeText);
            return result.toString();
        } catch (RuntimeException error) {
            return "Invalid expression.";
        }
    }

    public static String integrateWithSteps(String equation) {
        try {
            String original = equation.trim();
            String converted = convertSqrtToPower(original);
            Expr expression = parse(original);
            Expr integral = reverseChainIntegral(expression);
            if (integral == null) {
                return null;
            }

            char variable = Term.FindVariable(original);
            StringBuilder result = new StringBuilder();
            result.append("(").append(original).append(") d").append(variable);
            if (!normalize(original).equals(normalize(converted))) {
                result.append("\n=> (").append(converted).append(") d").append(variable);
            }
            result.append("\n=> ").append(simplifyText(integral.format())).append(" + C");
            return result.toString();
        } catch (RuntimeException error) {
            return "Invalid expression.";
        }
    }

    public static boolean isCompositeFunction(String expression) {
        try {
            Expr parsed = parse(expression);
            return containsFunction(parsed) || parsed instanceof Power;
        } catch (RuntimeException error) {
            return false;
        }
    }

    public static String getOuterFunction(String expression) {
        try {
            Expr parsed = parse(expression);
            if (parsed instanceof FunctionExpr) {
                return ((FunctionExpr) parsed).name;
            }
            if (parsed instanceof Power && ((Power) parsed).base instanceof FunctionExpr) {
                FunctionExpr function = (FunctionExpr) ((Power) parsed).base;
                return function.name + "^" + ((Power) parsed).exponent.format();
            }
            return "";
        } catch (RuntimeException error) {
            return "";
        }
    }

    public static String getInnerExpression(String expression) {
        try {
            Expr parsed = parse(expression);
            if (parsed instanceof FunctionExpr) {
                return ((FunctionExpr) parsed).argument.format();
            }
            if (parsed instanceof Power && ((Power) parsed).base instanceof FunctionExpr) {
                return ((FunctionExpr) ((Power) parsed).base).argument.format();
            }
            return "";
        } catch (RuntimeException error) {
            return "";
        }
    }

    public static String differentiateInnerExpression(String expression) {
        String inner = getInnerExpression(expression);
        if (inner.isEmpty()) {
            return "0";
        }
        return simplifyText(simplify(parse(inner).derivative()).format());
    }

    public static String integrateInnerExpression(String expression) {
        String inner = getInnerExpression(expression);
        if (inner.isEmpty()) {
            return "0";
        }
        Expr integral = reverseChainIntegral(parse(inner));
        return integral == null ? "0" : simplifyText(integral.format());
    }

    private static Expr reverseChainIntegral(Expr expression) {
        expression = simplify(expression);
        List<Expr> factors = flattenMul(expression);

        for (int i = 0; i < factors.size(); i++) {
            Expr factor = factors.get(i);
            Expr candidate = integralOfOuterDerivative(factor);
            if (candidate == null) {
                continue;
            }
            Expr inner = innerOfOuterDerivative(factor);
            Expr innerDerivative = simplify(inner.derivative());
            Expr remaining = multiplyAllExcept(factors, i);
            if (sameUpToConstant(remaining, innerDerivative)) {
                return candidate;
            }
            Integer scale = constantScale(remaining, innerDerivative);
            if (scale != null) {
                return simplify(new Binary("*", new Constant(scale), candidate));
            }
        }

        if (expression instanceof Binary && ((Binary) expression).operator.equals("/")) {
            Binary quotient = (Binary) expression;
            Expr denominator = quotient.right;
            Expr denominatorDerivative = simplify(denominator.derivative());
            if (sameUpToConstant(quotient.left, denominatorDerivative)) {
                return new FunctionExpr("ln", denominator);
            }
            Integer scale = constantScale(quotient.left, denominatorDerivative);
            if (scale != null) {
                return simplify(new Binary("*", new Constant(scale), new FunctionExpr("ln", denominator)));
            }
            if (denominator instanceof FunctionExpr && ((FunctionExpr) denominator).name.equals("sqrt")) {
                Expr inner = ((FunctionExpr) denominator).argument;
                Expr innerDerivative = simplify(inner.derivative());
                if (sameUpToConstant(quotient.left, innerDerivative)) {
                    return simplify(new Binary("*", new Constant(2), new FunctionExpr("sqrt", inner)));
                }
                scale = constantScale(quotient.left, innerDerivative);
                if (scale != null) {
                    return simplify(new Binary("*", new Constant(2 * scale), new FunctionExpr("sqrt", inner)));
                }
            }
        }

        return null;
    }

    private static Expr integralOfOuterDerivative(Expr expression) {
        if (expression instanceof FunctionExpr) {
            FunctionExpr function = (FunctionExpr) expression;
            if (function.name.equals("cos")) {
                return new FunctionExpr("sin", function.argument);
            }
            if (function.name.equals("sec")) {
                return null;
            }
            if (function.name.equals("exp")) {
                return new FunctionExpr("exp", function.argument);
            }
        }
        if (expression instanceof Power) {
            Power power = (Power) expression;
            if (power.base instanceof FunctionExpr) {
                FunctionExpr function = (FunctionExpr) power.base;
                if (function.name.equals("sec") && power.exponent.equals(new Constant(2))) {
                    return new FunctionExpr("tan", function.argument);
                }
            }
        }
        return null;
    }

    private static Expr innerOfOuterDerivative(Expr expression) {
        if (expression instanceof FunctionExpr) {
            return ((FunctionExpr) expression).argument;
        }
        if (expression instanceof Power && ((Power) expression).base instanceof FunctionExpr) {
            return ((FunctionExpr) ((Power) expression).base).argument;
        }
        return expression;
    }

    private static boolean sameUpToConstant(Expr first, Expr second) {
        return constantScale(first, second) != null;
    }

    private static Integer constantScale(Expr first, Expr second) {
        Expr a = simplify(first);
        Expr b = simplify(second);
        if (a.equals(b)) {
            return 1;
        }
        if (a instanceof Binary && ((Binary) a).operator.equals("*")) {
            Binary product = (Binary) a;
            if (product.left instanceof Constant && simplify(product.right).equals(b)) {
                return ((Constant) product.left).value;
            }
            if (product.right instanceof Constant && simplify(product.left).equals(b)) {
                return ((Constant) product.right).value;
            }
        }
        if (a instanceof Constant && b instanceof Constant && ((Constant) b).value != 0 && ((Constant) a).value % ((Constant) b).value == 0) {
            return ((Constant) a).value / ((Constant) b).value;
        }
        return null;
    }

    private static Expr multiplyAllExcept(List<Expr> factors, int excludedIndex) {
        Expr result = new Constant(1);
        for (int i = 0; i < factors.size(); i++) {
            if (i != excludedIndex) {
                result = simplify(new Binary("*", result, factors.get(i)));
            }
        }
        return result;
    }

    private static List<Expr> flattenMul(Expr expression) {
        ArrayList<Expr> factors = new ArrayList<>();
        collectMul(simplify(expression), factors);
        return factors;
    }

    private static void collectMul(Expr expression, List<Expr> factors) {
        if (expression instanceof Binary && ((Binary) expression).operator.equals("*")) {
            collectMul(((Binary) expression).left, factors);
            collectMul(((Binary) expression).right, factors);
        } else {
            factors.add(expression);
        }
    }

    private static Expr parse(String expression) {
        return new Parser(expression).parse();
    }

    private static String convertSqrtToPower(String expression) {
        try {
            Expr parsed = parse(expression);
            return parsed.formatSqrtAsPower();
        } catch (RuntimeException error) {
            return expression;
        }
    }

    private static String normalize(String expression) {
        return normalizeSuperscripts(expression).toLowerCase(Locale.ROOT)
            .replace("²", "^2")
            .replace("³", "^3")
            .replaceAll("\\s+", "");
    }

    public static String normalizeSuperscripts(String expression) {
        if (expression == null) {
            return "";
        }
        return expression
            .replace("⁰", "^0")
            .replace("¹", "^1")
            .replace("²", "^2")
            .replace("³", "^3")
            .replace("⁴", "^4")
            .replace("⁵", "^5")
            .replace("⁶", "^6")
            .replace("⁷", "^7")
            .replace("⁸", "^8")
            .replace("⁹", "^9");
    }

    private static String simplifyText(String expression) {
        String result = expression;
        result = result.replace("* 1", "").replace("1 * ", "");
        result = result.replace("1 / x^2 * 2x", "2 / x");
        result = result.replace("(1 / x^2) * 2x", "2 / x");
        result = result.replace("2x / x^2", "2 / x");
        result = result.replace("exp", "e^");
        return result;
    }

    private static boolean containsFunction(Expr expression) {
        if (expression instanceof FunctionExpr) {
            return true;
        }
        if (expression instanceof Binary) {
            Binary binary = (Binary) expression;
            return containsFunction(binary.left) || containsFunction(binary.right);
        }
        if (expression instanceof Power) {
            Power power = (Power) expression;
            return containsFunction(power.base) || containsFunction(power.exponent);
        }
        if (expression instanceof UnaryMinus) {
            return containsFunction(((UnaryMinus) expression).inner);
        }
        return false;
    }

    private static Expr simplify(Expr expression) {
        if (expression instanceof Binary) {
            Binary binary = (Binary) expression;
            Expr left = simplify(binary.left);
            Expr right = simplify(binary.right);

            if (binary.operator.equals("+")) {
                if (isZero(left)) return right;
                if (isZero(right)) return left;
                if (left instanceof Constant && right instanceof Constant) return new Constant(((Constant) left).value + ((Constant) right).value);
                return new Binary("+", left, right);
            }
            if (binary.operator.equals("-")) {
                if (isZero(right)) return left;
                if (left instanceof Constant && right instanceof Constant) return new Constant(((Constant) left).value - ((Constant) right).value);
                return new Binary("-", left, right);
            }
            if (binary.operator.equals("*")) {
                if (isZero(left) || isZero(right)) return new Constant(0);
                if (isOne(left)) return right;
                if (isOne(right)) return left;
                if (left instanceof UnaryMinus) {
                    return simplify(new UnaryMinus(new Binary("*", ((UnaryMinus) left).inner, right)));
                }
                if (right instanceof UnaryMinus) {
                    return simplify(new UnaryMinus(new Binary("*", left, ((UnaryMinus) right).inner)));
                }
                if (left instanceof Constant && right instanceof Fraction) {
                    return simplify(multiplyConstantAndFraction((Constant) left, (Fraction) right));
                }
                if (left instanceof Fraction && right instanceof Constant) {
                    return simplify(multiplyConstantAndFraction((Constant) right, (Fraction) left));
                }
                if (left instanceof Constant && right instanceof Constant) return new Constant(((Constant) left).value * ((Constant) right).value);
                if (left instanceof Constant && right instanceof Binary && ((Binary) right).operator.equals("*") && ((Binary) right).left instanceof Constant) {
                    Binary rp = (Binary) right;
                    return simplify(new Binary("*", new Constant(((Constant) left).value * ((Constant) rp.left).value), rp.right));
                }
                if (left instanceof Constant && right instanceof Binary && ((Binary) right).operator.equals("*") && ((Binary) right).left instanceof Fraction) {
                    Binary rp = (Binary) right;
                    return simplify(new Binary("*", multiplyConstantAndFraction((Constant) left, (Fraction) rp.left), rp.right));
                }
                if (right instanceof Constant) return simplify(new Binary("*", right, left));
                if (left instanceof Variable && right instanceof Variable && left.equals(right)) return new Power(left, new Constant(2));
                Expr normalizedProduct = normalizeProduct(left, right);
                if (normalizedProduct != null) return normalizedProduct;
                return new Binary("*", left, right);
            }
            if (binary.operator.equals("/")) {
                if (isZero(left)) return new Constant(0);
                if (isOne(right)) return left;
                if (left.equals(right)) return new Constant(1);
                if (left instanceof Constant && right instanceof Constant) {
                    int a = ((Constant) left).value;
                    int b = ((Constant) right).value;
                    if (b != 0 && a % b == 0) return new Constant(a / b);
                }
                if (left instanceof Binary && ((Binary) left).operator.equals("*")) {
                    Binary product = (Binary) left;
                    Expr cancelled = cancelOneFactor(product.left, product.right, right);
                    if (cancelled != null) return cancelled;
                }
                return new Binary("/", left, right);
            }
        }
        if (expression instanceof Power) {
            Power power = (Power) expression;
            Expr base = simplify(power.base);
            Expr exponent = simplify(power.exponent);
            if (isZero(exponent)) return new Constant(1);
            if (isOne(exponent)) return base;
            if (base instanceof Constant && exponent instanceof Constant) {
                int exp = ((Constant) exponent).value;
                if (exp >= 0) {
                    return new Constant((int) Math.pow(((Constant) base).value, exp));
                }
            }
            return new Power(base, exponent);
        }
        if (expression instanceof FunctionExpr) {
            FunctionExpr function = (FunctionExpr) expression;
            return new FunctionExpr(function.name, simplify(function.argument));
        }
        if (expression instanceof UnaryMinus) {
            Expr inner = simplify(((UnaryMinus) expression).inner);
            if (inner instanceof Constant) return new Constant(-((Constant) inner).value);
            if (inner instanceof UnaryMinus) return ((UnaryMinus) inner).inner;
            return new UnaryMinus(inner);
        }
        return expression;
    }

    private static Expr cancelOneFactor(Expr first, Expr second, Expr denominator) {
        if (first.equals(denominator)) return second;
        if (second.equals(denominator)) return first;
        if (denominator instanceof Power && ((Power) denominator).base.equals(second) && ((Power) denominator).exponent instanceof Constant) {
            int power = ((Constant) ((Power) denominator).exponent).value;
            if (power == 2) return new Binary("/", first, second);
        }
        return null;
    }

    private static Expr multiplyConstantAndFraction(Constant constant, Fraction fraction) {
        int numerator = constant.value * fraction.numerator;
        if (numerator % fraction.denominator == 0) {
            return new Constant(numerator / fraction.denominator);
        }
        return new Fraction(numerator, fraction.denominator);
    }

    private static Expr normalizeProduct(Expr left, Expr right) {
        List<Expr> factors = new ArrayList<>();
        collectMul(new Binary("*", left, right), factors);
        if (factors.size() <= 2) {
            return null;
        }

        int coefficient = 1;
        int constants = 0;
        boolean changed = false;
        ArrayList<Expr> nonNumericFactors = new ArrayList<>();

        for (Expr factor : factors) {
            Expr current = factor;
            if (current instanceof UnaryMinus) {
                coefficient *= -1;
                current = ((UnaryMinus) current).inner;
                changed = true;
            }
            if (current instanceof Constant) {
                coefficient *= ((Constant) current).value;
                constants++;
                if (constants > 1 || ((Constant) current).value < 0) {
                    changed = true;
                }
            } else {
                nonNumericFactors.add(current);
            }
        }

        if (!changed && constants <= 1) {
            return null;
        }
        if (coefficient == 0) {
            return new Constant(0);
        }

        Expr result = null;
        int absoluteCoefficient = Math.abs(coefficient);
        if (absoluteCoefficient != 1 || nonNumericFactors.isEmpty()) {
            result = new Constant(absoluteCoefficient);
        }
        for (Expr factor : nonNumericFactors) {
            result = result == null ? factor : new Binary("*", result, factor);
        }
        if (coefficient < 0) {
            return new UnaryMinus(result);
        }
        return result;
    }

    private static boolean isZero(Expr expression) {
        return expression instanceof Constant && ((Constant) expression).value == 0;
    }

    private static boolean isOne(Expr expression) {
        return expression instanceof Constant && ((Constant) expression).value == 1;
    }

    private interface Expr {
        Expr derivative();
        String format();
        String formatSqrtAsPower();
        int precedence();
    }

    private static class Constant implements Expr {
        final int value;

        Constant(int value) {
            this.value = value;
        }

        public Expr derivative() {
            return new Constant(0);
        }

        public String format() {
            return String.valueOf(value);
        }

        public String formatSqrtAsPower() {
            return format();
        }

        public int precedence() {
            return 5;
        }

        public boolean equals(Object other) {
            return other instanceof Constant && value == ((Constant) other).value;
        }

        public int hashCode() {
            return Objects.hash(value);
        }
    }

    private static class Variable implements Expr {
        final String name;

        Variable(String name) {
            this.name = name;
        }

        public Expr derivative() {
            return new Constant(name.equals("x") ? 1 : 0);
        }

        public String format() {
            return name;
        }

        public String formatSqrtAsPower() {
            return format();
        }

        public int precedence() {
            return 5;
        }

        public boolean equals(Object other) {
            return other instanceof Variable && name.equals(((Variable) other).name);
        }

        public int hashCode() {
            return Objects.hash(name);
        }
    }

    private static class UnaryMinus implements Expr {
        final Expr inner;

        UnaryMinus(Expr inner) {
            this.inner = inner;
        }

        public Expr derivative() {
            return simplify(new UnaryMinus(inner.derivative()));
        }

        public String format() {
            if (inner instanceof Binary && ((Binary) inner).operator.equals("*")) {
                return "-" + inner.format();
            }
            return "-" + wrap(inner, precedence());
        }

        public String formatSqrtAsPower() {
            return "-" + wrapSqrt(inner, precedence());
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

    private static class Binary implements Expr {
        final String operator;
        final Expr left;
        final Expr right;

        Binary(String operator, Expr left, Expr right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        public Expr derivative() {
            if (operator.equals("+")) return simplify(new Binary("+", left.derivative(), right.derivative()));
            if (operator.equals("-")) return simplify(new Binary("-", left.derivative(), right.derivative()));
            if (operator.equals("*")) {
                return simplify(new Binary("+", new Binary("*", left.derivative(), right), new Binary("*", left, right.derivative())));
            }
            if (operator.equals("/")) {
                Expr numerator = new Binary("-", new Binary("*", left.derivative(), right), new Binary("*", left, right.derivative()));
                Expr denominator = new Power(right, new Constant(2));
                return simplify(new Binary("/", numerator, denominator));
            }
            return new Constant(0);
        }

        public String format() {
            if (operator.equals("*")) {
                if (left instanceof Constant && isCompactRight(right)) return left.format() + right.format();
                if (right instanceof Constant && isCompactRight(left)) return right.format() + left.format();
            }
            return wrap(left, precedence()) + " " + operator + " " + wrapRight(right, precedence(), operator);
        }

        public String formatSqrtAsPower() {
            return wrapSqrt(left, precedence()) + " " + operator + " " + wrapRightSqrt(right, precedence(), operator);
        }

        public int precedence() {
            return operator.equals("+") || operator.equals("-") ? 1 : 2;
        }

        public boolean equals(Object other) {
            return other instanceof Binary
                && operator.equals(((Binary) other).operator)
                && left.equals(((Binary) other).left)
                && right.equals(((Binary) other).right);
        }

        public int hashCode() {
            return Objects.hash(operator, left, right);
        }
    }

    private static class Power implements Expr {
        final Expr base;
        final Expr exponent;

        Power(Expr base, Expr exponent) {
            this.base = base;
            this.exponent = exponent;
        }

        public Expr derivative() {
            if (exponent instanceof Constant) {
                int value = ((Constant) exponent).value;
                return simplify(new Binary("*",
                    new Binary("*", new Constant(value), new Power(base, new Constant(value - 1))),
                    base.derivative()));
            }
            if (exponent instanceof Fraction) {
                Fraction fraction = (Fraction) exponent;
                Expr loweredExponent = new Fraction(fraction.numerator - fraction.denominator, fraction.denominator);
                return simplify(new Binary("*",
                    new Binary("*", exponent, new Power(base, loweredExponent)),
                    base.derivative()));
            }
            return new Constant(0);
        }

        public String format() {
            if (base instanceof FunctionExpr && exponent instanceof Constant) {
                FunctionExpr function = (FunctionExpr) base;
                return function.name + "^" + exponent.format() + "(" + function.argument.format() + ")";
            }
            return wrap(base, precedence()) + "^" + wrapExponent(exponent);
        }

        public String formatSqrtAsPower() {
            if (base instanceof FunctionExpr && exponent instanceof Constant) {
                FunctionExpr function = (FunctionExpr) base;
                return function.name + "^" + exponent.formatSqrtAsPower() + "(" + function.argument.formatSqrtAsPower() + ")";
            }
            return wrapSqrt(base, precedence()) + "^" + wrapExponentSqrt(exponent);
        }

        public int precedence() {
            return 4;
        }

        public boolean equals(Object other) {
            return other instanceof Power && base.equals(((Power) other).base) && exponent.equals(((Power) other).exponent);
        }

        public int hashCode() {
            return Objects.hash(base, exponent);
        }
    }

    private static class Fraction implements Expr {
        final int numerator;
        final int denominator;

        Fraction(int numerator, int denominator) {
            if (denominator == 0) {
                throw new IllegalArgumentException("Zero denominator");
            }
            int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
            int sign = denominator < 0 ? -1 : 1;
            this.numerator = sign * numerator / gcd;
            this.denominator = sign * denominator / gcd;
        }

        public Expr derivative() {
            return new Constant(0);
        }

        public String format() {
            return numerator + " / " + denominator;
        }

        public String formatSqrtAsPower() {
            return format();
        }

        public int precedence() {
            return 0;
        }

        public boolean equals(Object other) {
            return other instanceof Fraction && numerator == ((Fraction) other).numerator && denominator == ((Fraction) other).denominator;
        }

        public int hashCode() {
            return Objects.hash(numerator, denominator);
        }
    }

    private static class FunctionExpr implements Expr {
        final String name;
        final Expr argument;

        FunctionExpr(String name, Expr argument) {
            this.name = name.equals("csc") ? "cosec" : name;
            this.argument = argument;
        }

        public Expr derivative() {
            Expr outer;
            if (name.equals("sin")) outer = new FunctionExpr("cos", argument);
            else if (name.equals("cos")) outer = new UnaryMinus(new FunctionExpr("sin", argument));
            else if (name.equals("tan")) outer = new Power(new FunctionExpr("sec", argument), new Constant(2));
            else if (name.equals("cot")) outer = new UnaryMinus(new Power(new FunctionExpr("cosec", argument), new Constant(2)));
            else if (name.equals("sec")) outer = new Binary("*", new FunctionExpr("sec", argument), new FunctionExpr("tan", argument));
            else if (name.equals("cosec")) outer = new UnaryMinus(new Binary("*", new FunctionExpr("cosec", argument), new FunctionExpr("cot", argument)));
            else if (name.equals("ln")) outer = new Binary("/", new Constant(1), argument);
            else if (name.equals("exp")) outer = new FunctionExpr("exp", argument);
            else if (name.equals("sqrt")) outer = new Binary("/", new Constant(1), new Binary("*", new Constant(2), new FunctionExpr("sqrt", argument)));
            else outer = new Constant(0);
            return simplify(new Binary("*", outer, argument.derivative()));
        }

        public String format() {
            if (name.equals("exp")) return "e^(" + argument.format() + ")";
            return name + "(" + argument.format() + ")";
        }

        public String formatSqrtAsPower() {
            if (name.equals("sqrt")) {
                return "(" + argument.formatSqrtAsPower() + ")^(1 / 2)";
            }
            if (name.equals("exp")) {
                return "e^(" + argument.formatSqrtAsPower() + ")";
            }
            return name + "(" + argument.formatSqrtAsPower() + ")";
        }

        public int precedence() {
            return 5;
        }

        public boolean equals(Object other) {
            return other instanceof FunctionExpr && name.equals(((FunctionExpr) other).name) && argument.equals(((FunctionExpr) other).argument);
        }

        public int hashCode() {
            return Objects.hash(name, argument);
        }
    }

    private static boolean isCompactRight(Expr expression) {
        return expression instanceof Variable || expression instanceof FunctionExpr || expression instanceof Power;
    }

    private static String wrap(Expr expression, int parentPrecedence) {
        if (expression.precedence() < parentPrecedence) {
            return "(" + expression.format() + ")";
        }
        return expression.format();
    }

    private static String wrapRight(Expr expression, int parentPrecedence, String operator) {
        if (expression.precedence() < parentPrecedence || ((operator.equals("-") || operator.equals("/")) && expression.precedence() == parentPrecedence)) {
            return "(" + expression.format() + ")";
        }
        return expression.format();
    }

    private static String wrapSqrt(Expr expression, int parentPrecedence) {
        if (expression.precedence() < parentPrecedence) {
            return "(" + expression.formatSqrtAsPower() + ")";
        }
        return expression.formatSqrtAsPower();
    }

    private static String wrapRightSqrt(Expr expression, int parentPrecedence, String operator) {
        if (expression.precedence() < parentPrecedence || ((operator.equals("-") || operator.equals("/")) && expression.precedence() == parentPrecedence)) {
            return "(" + expression.formatSqrtAsPower() + ")";
        }
        return expression.formatSqrtAsPower();
    }

    private static String wrapExponent(Expr expression) {
        if (expression instanceof Constant) return expression.format();
        return "(" + expression.format() + ")";
    }

    private static String wrapExponentSqrt(Expr expression) {
        if (expression instanceof Constant) return expression.formatSqrtAsPower();
        return "(" + expression.formatSqrtAsPower() + ")";
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int r = a % b;
            a = b;
            b = r;
        }
        return a == 0 ? 1 : a;
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
                expression = new Binary(operator, expression, parseTerm());
            }
            return expression;
        }

        private Expr parseTerm() {
            Expr expression = parsePower();
            while (peek().text.equals("*") || peek().text.equals("/") || beginsImplicitFactor()) {
                String operator = "*";
                if (peek().text.equals("*") || peek().text.equals("/")) {
                    operator = advance().text;
                }
                expression = new Binary(operator, expression, parsePower());
            }
            return expression;
        }

        private Expr parsePower() {
            Expr expression = parsePrimary();
            while (match("^")) {
                expression = new Power(expression, parseExponent());
            }
            return expression;
        }

        private Expr parseExponent() {
            if (match("(")) {
                Expr numerator = parseExpression();
                consume(")", "Expected )");
                if (numerator instanceof Binary && ((Binary) numerator).operator.equals("/")
                    && ((Binary) numerator).left instanceof Constant
                    && ((Binary) numerator).right instanceof Constant) {
                    return new Fraction(((Constant) ((Binary) numerator).left).value, ((Constant) ((Binary) numerator).right).value);
                }
                return numerator;
            }
            if (checkType("NUMBER")) {
                return new Constant(Integer.parseInt(advance().text));
            }
            throw new IllegalArgumentException("Expected exponent");
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
                return new Constant(Integer.parseInt(advance().text));
            }
            if (checkType("NAME")) {
                String name = advance().text;
                if (name.equals("e") && match("^")) {
                    return new FunctionExpr("exp", parsePrimary());
                }
                if (isFunction(name)) {
                    int power = 1;
                    if (match("^")) {
                        power = Integer.parseInt(consumeType("NUMBER", "Expected function power").text);
                    }
                    Expr argument = parsePrimary();
                    Expr function = new FunctionExpr(name, argument);
                    return power == 1 ? function : new Power(function, new Constant(power));
                }
                return new Variable(name);
            }
            throw new IllegalArgumentException("Unexpected token: " + peek().text);
        }

        private boolean beginsImplicitFactor() {
            return checkType("NUMBER") || checkType("NAME") || peek().text.equals("(");
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
        String value = normalize(input);
        ArrayList<Token> tokens = new ArrayList<>();

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
                String function = functionAt(value, i);
                if (function != null) {
                    tokens.add(new Token("NAME", function));
                    i += function.length();
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

    private static String functionAt(String value, int index) {
        for (String function : FUNCTIONS) {
            if (value.startsWith(function, index)) {
                return function;
            }
        }
        return null;
    }

    private static boolean isFunction(String name) {
        return Arrays.asList(FUNCTIONS).contains(name);
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
