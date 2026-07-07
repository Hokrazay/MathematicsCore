
import java.util.*;



public class Term extends Utility {


        int coefficient;
        char variable;
        int power;
        String eqn;
        static ArrayList<Term> term = new ArrayList<>();
        static ArrayList<Character> Operators = new ArrayList<>();
        static StringBuilder finale = new StringBuilder();

        Term(int coefficient, char variable, int power) {
            this.coefficient = coefficient;
            this.variable = variable;
            this.power = power;
            this.eqn = coefficient + String.valueOf(variable) + "^" + power;
        }
        Term(String eqn) {
            this.eqn = eqn;
        }

    public  static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        runCalculus(input);
    }

    public static void runCalculus(Scanner input) {
        term.clear();
        Operators.clear();
        finale.setLength(0);

        pln("Enter the equation in the format given below : \n");
        pln("Example : 2 * sin x * cos x + x^3 + 3x^2 - 4x + 5");
        pln("Ensure that there is a space between the terms and the operators.\n");
        pln("Ensure there is only ONE VARIABLE and only ONE OF EACH OPERATOR in the equation.\n");
        pln("Note :\n-> 1 is represented as 1x^0 \n-> 0 is represented as 0x^0\n-> x is represented as 1x^1");
        String equation = input.nextLine();

        pln("What function do you want to perform on the equation? Enter your choice:");
        pln("[D] - Differentiation.");
        pln("[P] - Partial Differentiation");
        pln("[I] - Integration");

        String selectedChoice = input.nextLine().trim();
        char choice = selectedChoice.isEmpty() ? ' ' : selectedChoice.charAt(0);

        switch(choice) {
            case 'd':
            case 'D':
              if (ChainRule.isChainRuleCandidate(equation)) {
                String chainRuleResult = ChainRule.differentiateWithSteps(equation);
                finale.setLength(0);
                finale.append(chainRuleResult);
                pln("Final Results ->\n" + finale.toString());
              } else if (isTrigonometric(equation)) {
                processTrigonometricExpression(equation);
              } else {
                parsePolynomialEquation(equation);
                DifferentialCalculus();
                pln("\nFinal Results ->\n" + finale.toString());
              }
              evaluateDerivative(equation, input);
              break;
            case 'p':
            case 'P':
              pln("Under Development");
              break;
            case 'i':
            case 'I':
              Integration.performIntegration(equation, input);
              break;
            default:
              pln("Invalid choice!");
        }

    }

    public static void evaluateDerivative(String originalEquation, Scanner input) {
        String finalDerivative = extractFinalExpression(finale.toString());
        if (finalDerivative == null || finalDerivative.trim().isEmpty()
                || finalDerivative.equals("Invalid expression.")) {
            return;
        }

        pln("\nDo you want to evaluate this expression at any value?");
        pln("[Y] - Yes");
        pln("[N] - No, Thanks.");

        while (true) {
            String selectedChoice = input.nextLine().trim();
            char choice = selectedChoice.isEmpty() ? ' ' : selectedChoice.charAt(0);

            switch (choice) {
                case 'n':
                case 'N':
                    System.exit(0);
                    return;
                case 'y':
                case 'Y':
                    Double userValue = readDoubleGracefully(input, "\nEnter value of evaluation :-");
                    pln("\nEvaluating\n");
                    pln(finalDerivative + "\n");
                    pln("at\n");
                    pln(formatNumber(userValue) + "\n");
                    pln("-->");
                    pln(FormatDerivativeNotation(originalEquation.trim()) + " |x=" + formatNumber(userValue));
                    displayEvaluationSteps(finalDerivative, userValue);
                    return;
                default:
                    pln("Invalid choice! Please enter Y or N.");
            }
        }
    }

    public static void parsePolynomialEquation(String equation) {
        String[] tokens = ChainRule.normalizeSuperscripts(equation).split(" ");

        for(String token : tokens) {
            if(token.isEmpty()) {
                continue;
            }

            if(token.equals("+") || token.equals("-") ||
               token.equals("*") || token.equals("/")) {
                Operators.add(token.charAt(0));
            } else {
                Term terms = new Term(token);
                term.add(terms);
                term.get(term.size() - 1).TermAssignment();
            }
        }
    }

    // Detects if an expression contains trigonometric functions
    public static boolean isTrigonometric(String expression) {
        String lower = expression.toLowerCase();
        return lower.contains("sin") || lower.contains("cos") || lower.contains("tan") ||
               lower.contains("cot") || lower.contains("sec") || lower.contains("csc") ||
               lower.contains("cosec");
    }

    public static boolean isCompositeFunction(String expression) {
        return ChainRule.isCompositeFunction(expression);
    }

    public static String getOuterFunction(String expression) {
        return ChainRule.getOuterFunction(expression);
    }

    public static String getInnerExpression(String expression) {
        return ChainRule.getInnerExpression(expression);
    }

    public static String differentiateInnerExpression(String expression) {
        return ChainRule.differentiateInnerExpression(expression);
    }

    public static String integrateInnerExpression(String expression) {
        return ChainRule.integrateInnerExpression(expression);
    }

    // Processes an expression containing trigonometric functions
    public static void processTrigonometricExpression(String equation) {
        // If expression is a chain-rule candidate (nested functions, sqrt/ln/exp, trig powers with composite args, etc.),
        // route differentiation through ChainRule AST engine to ensure correct unlimited nesting & sign handling.
        if (ChainRule.isChainRuleCandidate(equation)) {
            String chainRuleResult = ChainRule.differentiateWithSteps(equation);
            if (chainRuleResult != null && !chainRuleResult.equals("Invalid expression.")) {
                finale.setLength(0);
                finale.append(chainRuleResult);
                pln("Final Results ->\n" + finale.toString());
                return;
            }
            // fallback to existing logic
        }

        // Step 1: Use TrignometryForCalculus to simplify the trig expression
        String simplifiedTrig = TrignometryForCalculus.simplifyEquationRaw(equation);


        // Step 2: Display the differentiation steps
        finale.setLength(0);
        finale.append(FormatDerivativeNotation(equation));

        // Show the simplified expression in derivative notation if it changed
        if (!simplifiedTrig.equals(equation)) {
            finale.append("\n=> ").append(FormatDerivativeNotation(simplifiedTrig));
        }

        // Step 3: Parse the simplified expression into trig terms and polynomial terms
        List<String> trigTerms = new ArrayList<>();
        List<Integer> trigSigns = new ArrayList<>();
        List<String> polyTokens = new ArrayList<>();
        List<String> polyOps = new ArrayList<>();

        parseTrigAndPolyParts(simplifiedTrig, trigTerms, trigSigns, polyTokens, polyOps);

        // Step 4: Apply BODMAS to polynomial part
        String polyBodmasResult = applyBODMAS(polyTokens, polyOps);

        // Step 5: Show BODMAS simplification step if polynomial part changed
        String afterBodmas = rebuildWithTrigTerms(trigTerms, trigSigns, polyBodmasResult);
        if (!afterBodmas.equals(simplifiedTrig)) {
            finale.append("\n=> ").append(FormatDerivativeNotation(afterBodmas));
        }

        // Step 6: Compute derivatives
        String trigDerivative = computeTrigDerivatives(trigTerms, trigSigns);
        String polyDerivative = computePolyDerivative(polyBodmasResult);

        // Step 7: Combine and display final result
        String finalDerivative = combineDerivativeResults(trigDerivative, polyDerivative);
        finale.append("\n=> ").append(finalDerivative);

        pln("Final Results ->\n" + finale.toString());
    }

    static class SignedExpression {
        int sign;
        String expression;

        SignedExpression(int sign, String expression) {
            this.sign = sign;
            this.expression = expression.trim();
        }
    }

    // Parses the simplified expression into trig terms and polynomial tokens
    public static void parseTrigAndPolyParts(String expression, List<String> trigTerms,
                                               List<Integer> trigSigns, List<String> polyTokens,
                                               List<String> polyOps) {
        List<SignedExpression> additiveParts = splitTopLevelAdditiveTerms(expression);

        for (SignedExpression part : additiveParts) {
            String currentExpression = trimOuterParentheses(part.expression);

            if (isTrigonometric(currentExpression)) {
                trigTerms.add(currentExpression);
                trigSigns.add(part.sign);
            } else {
                appendPolynomialPart(currentExpression, part.sign, polyTokens, polyOps);
            }
        }
    }

    public static List<SignedExpression> splitTopLevelAdditiveTerms(String expression) {
        List<SignedExpression> parts = new ArrayList<>();
        int depth = 0;
        int sign = 1;
        int start = 0;

        for (int i = 0; i < expression.length(); i++) {
            char current = expression.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
            } else if (depth == 0 && (current == '+' || current == '-') && isBinaryAdditiveOperator(expression, i)) {
                String termText = expression.substring(start, i).trim();
                if (!termText.isEmpty()) {
                    parts.add(new SignedExpression(sign, termText));
                }
                sign = current == '-' ? -1 : 1;
                start = i + 1;
            }
        }

        String termText = expression.substring(start).trim();
        if (!termText.isEmpty()) {
            parts.add(new SignedExpression(sign, termText));
        }
        return parts;
    }

    public static boolean isBinaryAdditiveOperator(String expression, int index) {
        int previous = index - 1;
        while (previous >= 0 && Character.isWhitespace(expression.charAt(previous))) {
            previous--;
        }
        if (previous < 0) {
            return false;
        }
        char previousChar = expression.charAt(previous);
        return previousChar != '+' && previousChar != '-' && previousChar != '*' && previousChar != '/' && previousChar != '^' && previousChar != '(';
    }

    public static void appendPolynomialPart(String expression, int sign, List<String> polyTokens, List<String> polyOps) {
        List<String> localTokens = new ArrayList<>();
        List<String> localOps = new ArrayList<>();
        splitTopLevelMultiplyDivide(expression, localTokens, localOps);

        if (localTokens.isEmpty()) {
            return;
        }

        if (polyTokens.isEmpty()) {
            String firstToken = localTokens.get(0);
            polyTokens.add(sign < 0 ? "-" + firstToken : firstToken);
        } else {
            polyOps.add(sign < 0 ? "-" : "+");
            polyTokens.add(localTokens.get(0));
        }

        for (int i = 0; i < localOps.size() && i + 1 < localTokens.size(); i++) {
            polyOps.add(localOps.get(i));
            polyTokens.add(localTokens.get(i + 1));
        }
    }

    public static void splitTopLevelMultiplyDivide(String expression, List<String> tokens, List<String> ops) {
        int depth = 0;
        int start = 0;

        for (int i = 0; i < expression.length(); i++) {
            char current = expression.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
            } else if (depth == 0 && (current == '*' || current == '/')) {
                String token = trimOuterParentheses(expression.substring(start, i).trim());
                if (!token.isEmpty()) {
                    tokens.add(token);
                    ops.add(String.valueOf(current));
                }
                start = i + 1;
            }
        }

        String token = trimOuterParentheses(expression.substring(start).trim());
        if (!token.isEmpty()) {
            tokens.add(token);
        }
    }

    public static String trimOuterParentheses(String expression) {
        String result = expression.trim();
        while (result.startsWith("(") && result.endsWith(")") && wrapsWholeExpression(result)) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    public static boolean wrapsWholeExpression(String expression) {
        int depth = 0;
        for (int i = 0; i < expression.length(); i++) {
            char current = expression.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0 && i < expression.length() - 1) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    // Checks if a token is a trig function name
    public static boolean isTrigFunction(String token) {
        String t = token.toLowerCase();
        return t.equals("sin") || t.equals("cos") || t.equals("tan") ||
               t.equals("cot") || t.equals("sec") || t.equals("csc") ||
               t.equals("cosec");
    }

    // Checks if a token looks like a trig function (including with power notation)
    public static boolean isTrigToken(String token) {
        String t = token.toLowerCase();
        // Check for sin^2, cos^2, etc.
        if (t.contains("^")) {
            String base = t.split("\\^")[0];
            return base.equals("sin") || base.equals("cos") || base.equals("tan") ||
                   base.equals("cot") || base.equals("sec") || base.equals("csc") ||
                   base.equals("cosec");
        }
        return isTrigFunction(t);
    }

    // Checks if a variable token follows a trig function (to avoid double-counting)
    public static boolean isSimpleVariableAfterTrig(String[] tokens, int index) {
        if (index == 0) return false;
        if (isTrigToken(tokens[index - 1])) {
            String current = tokens[index].toLowerCase();
            return current.equals("x") || current.equals("y") || current.length() == 1;
        }
        return false;
    }

    // Applies BODMAS to polynomial tokens (multiply/divide first, then add/subtract)
    public static String applyBODMAS(List<String> polyTokens, List<String> polyOps) {
        if (polyTokens.isEmpty()) {
            return "0";
        }

        // First pass: handle * and /
        ArrayList<Term> terms = new ArrayList<>();
        ArrayList<Character> ops = new ArrayList<>();

        // Parse all tokens into Term objects
        for (String token : polyTokens) {
            Term t = new Term(token);
            t.TermAssignment();
            terms.add(t);
        }

        // Build operators list from polyOps, but skip the first operator if it's a sign
        // polyOps contains operators between terms
        for (String op : polyOps) {
            if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
                ops.add(op.charAt(0));
            }
        }

        if (terms.isEmpty()) return "0";

        // Apply BODMAS: * and / first
        ArrayList<Term> collapsedTerms = new ArrayList<>();
        ArrayList<Character> collapsedOperators = new ArrayList<>();
        Term current = terms.get(0);

        for (int i = 0; i < ops.size() && i + 1 < terms.size(); i++) {
            char operator = ops.get(i);
            Term nextTerm = terms.get(i + 1);

            if (operator == '*') {
                current = MultiplyTerms(current, nextTerm);
            } else if (operator == '/') {
                current = DivideTerms(current, nextTerm);
            } else {
                collapsedTerms.add(current);
                collapsedOperators.add(operator);
                current = nextTerm;
            }
        }
        collapsedTerms.add(current);

        // Combine like terms
        ArrayList<Term> result = CombineLikeTerms(collapsedTerms, collapsedOperators);
        return FormatTermList(result);
    }

    // Computes derivative of polynomial expression
    public static String computePolyDerivative(String polyStr) {
        if (polyStr.equals("0") || polyStr.isEmpty()) {
            return "0";
        }

        // Parse the polynomial string into terms
        String[] tokens = polyStr.split(" ");
        ArrayList<Term> polyTerms = new ArrayList<>();
        ArrayList<Character> polyOps = new ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) continue;

            if (token.equals("+") || token.equals("-")) {
                polyOps.add(token.charAt(0));
            } else {
                Term t = new Term(token);
                t.TermAssignment();
                polyTerms.add(t);
            }
        }

        if (polyTerms.isEmpty()) return "0";

        // Differentiate each term
        ArrayList<Term> derivativeTerms = new ArrayList<>();
        for (Term t : polyTerms) {
            Term dt = TermFromString(t.DerivativeOfTerm());
            derivativeTerms.add(dt);
        }

        return FormatTermList(CombineLikeTerms(derivativeTerms, new ArrayList<Character>()));
    }

    // Rebuilds expression string combining trig terms and polynomial terms
    public static String rebuildWithTrigTerms(List<String> trigTerms, List<Integer> trigSigns, String polyStr) {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        // Add trig terms
        for (int i = 0; i < trigTerms.size(); i++) {
            if (first) {
                if (trigSigns.get(i) < 0) {
                    result.append("-");
                }
                result.append(trigTerms.get(i));
                first = false;
            } else {
                if (trigSigns.get(i) < 0) {
                    result.append(" - ").append(trigTerms.get(i));
                } else {
                    result.append(" + ").append(trigTerms.get(i));
                }
            }
        }

        // Add polynomial part
        if (!polyStr.isEmpty() && !polyStr.equals("0")) {
            if (!first) {
                if (polyStr.startsWith("-")) {
                    result.append(" - ").append(polyStr.substring(1).trim());
                } else {
                    result.append(" + ").append(polyStr);
                }
            } else {
                result.append(polyStr);
            }
        }

        return first ? "0" : result.toString();
    }

    // Computes derivatives of trigonometric terms
    public static String computeTrigDerivatives(List<String> trigTerms, List<Integer> trigSigns) {
        List<String> derivativeTerms = new ArrayList<>();

        for (int i = 0; i < trigTerms.size(); i++) {
            String trigTerm = trigTerms.get(i);
            int sign = trigSigns.get(i);
            String derivative = derivativeOfTrigTerm(trigTerm);

            if (derivative.equals("0")) continue;

            // Apply sign
            if (sign < 0) {
                if (derivative.startsWith("-")) {
                    derivative = derivative.substring(1); // -(-sin x) = sin x
                } else {
                    derivative = "-" + derivative;
                }
            }

            derivativeTerms.add(derivative);
        }

        if (derivativeTerms.isEmpty()) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < derivativeTerms.size(); i++) {
            String term = derivativeTerms.get(i);
            if (i == 0) {
                result.append(term);
            } else {
                if (term.startsWith("-")) {
                    result.append(" - ").append(term.substring(1));
                } else {
                    result.append(" + ").append(term);
                }
            }
        }

        return result.toString();
    }

    // Computes the derivative of a single trigonometric term
    public static String derivativeOfTrigTerm(String trigTerm) {
        // Parse the trig term: e.g., "sin x", "sin^2 x", "cos x", "cos^2 x"
        String function = "";
        int power = 1;
        String argument = "x";

        String lower = trigTerm.toLowerCase().trim();

        // Extract function name
        if (lower.startsWith("sin^")) {
            function = "sin";
            String rest = lower.substring(4); // skip "sin^"
            int powEnd = 0;
            while (powEnd < rest.length() && Character.isDigit(rest.charAt(powEnd))) {
                powEnd++;
            }
            power = Integer.parseInt(rest.substring(0, powEnd));
            argument = rest.substring(powEnd).trim();
        } else if (lower.startsWith("cos^")) {
            function = "cos";
            String rest = lower.substring(4);
            int powEnd = 0;
            while (powEnd < rest.length() && Character.isDigit(rest.charAt(powEnd))) {
                powEnd++;
            }
            power = Integer.parseInt(rest.substring(0, powEnd));
            argument = rest.substring(powEnd).trim();
        } else if (lower.startsWith("tan^")) {
            function = "tan";
            String rest = lower.substring(4);
            int powEnd = 0;
            while (powEnd < rest.length() && Character.isDigit(rest.charAt(powEnd))) {
                powEnd++;
            }
            power = Integer.parseInt(rest.substring(0, powEnd));
            argument = rest.substring(powEnd).trim();
        } else if (lower.startsWith("cot^")) {
            function = "cot";
            String rest = lower.substring(4);
            int powEnd = 0;
            while (powEnd < rest.length() && Character.isDigit(rest.charAt(powEnd))) {
                powEnd++;
            }
            power = Integer.parseInt(rest.substring(0, powEnd));
            argument = rest.substring(powEnd).trim();
        } else if (lower.startsWith("sec^")) {
            function = "sec";
            String rest = lower.substring(4);
            int powEnd = 0;
            while (powEnd < rest.length() && Character.isDigit(rest.charAt(powEnd))) {
                powEnd++;
            }
            power = Integer.parseInt(rest.substring(0, powEnd));
            argument = rest.substring(powEnd).trim();
        } else if (lower.startsWith("cosec^") || lower.startsWith("csc^")) {
            function = lower.startsWith("csc^") ? "csc" : "cosec";
            String base = lower.startsWith("csc^") ? "csc^" : "cosec^";
            String rest = lower.substring(base.length());
            int powEnd = 0;
            while (powEnd < rest.length() && Character.isDigit(rest.charAt(powEnd))) {
                powEnd++;
            }
            power = Integer.parseInt(rest.substring(0, powEnd));
            argument = rest.substring(powEnd).trim();
        } else if (lower.startsWith("sin ")) {
            function = "sin";
            argument = lower.substring(4).trim();
        } else if (lower.startsWith("cos ")) {
            function = "cos";
            argument = lower.substring(4).trim();
        } else if (lower.startsWith("tan ")) {
            function = "tan";
            argument = lower.substring(4).trim();
        } else if (lower.startsWith("cot ")) {
            function = "cot";
            argument = lower.substring(4).trim();
        } else if (lower.startsWith("sec ")) {
            function = "sec";
            argument = lower.substring(4).trim();
        } else if (lower.startsWith("cosec ")) {
            function = "cosec";
            argument = lower.substring(6).trim();
        } else if (lower.startsWith("csc ")) {
            function = "csc";
            argument = lower.substring(4).trim();
        } else {
            return "0";
        }

        // Compute derivative based on power
        if (power == 1) {
            // Basic trig derivatives
            switch (function) {
                case "sin":
                    return "cos " + argument;
                case "cos":
                    return "-sin " + argument;
                case "tan":
                    return "sec^2 " + argument;
                case "csc":
                    return "-csc " + argument + " * cot " + argument;
                case "cosec":
                    return "-csc " + argument + " * cot " + argument;
                case "sec":
                    return "sec " + argument + " * tan " + argument;
                case "cot":
                    return "-csc^2 " + argument;
                default:
                    return "0";
            }
        } else {
            // Chain rule for powers: d/dx(f(x)^n) = n * f(x)^(n-1) * f'(x)
            String innerDerivative = derivativeOfTrigTerm(function + " " + argument);
            // For power > 1, we need to handle: d/dx(sin^n x) = n * sin^(n-1) x * cos x
            // Return a simplified representation
            if (innerDerivative.startsWith("-")) {
                return "-" + power + " * " + function + "^" + (power - 1) + " " + argument +
                       " * " + innerDerivative.substring(1);
            } else {
                return power + " * " + function + "^" + (power - 1) + " " + argument +
                       " * " + innerDerivative;
            }
        }
    }

    // Combines trig derivative and polynomial derivative results
    public static String combineDerivativeResults(String trigDerivative, String polyDerivative) {
        if (trigDerivative.equals("0") || trigDerivative.isEmpty()) {
            return polyDerivative;
        }
        if (polyDerivative.equals("0") || polyDerivative.isEmpty()) {
            return trigDerivative;
        }

        // Combine them
        if (polyDerivative.startsWith("-")) {
            return trigDerivative + " - " + polyDerivative.substring(1);
        } else {
            return trigDerivative + " + " + polyDerivative;
        }
    }

    public   void TermAssignment() {
    int i = 0;
    // Read coefficient
    String coeff = "";
    boolean isNegative = false;
    if (eqn == null || eqn.isEmpty()) {
        this.coefficient = 0;
        this.variable = 'x';
        this.power = 0;
        return;
    }

    if (eqn.charAt(i) == '-') {
        isNegative = true;
        i++;
    }

        if (i < eqn.length() && eqn.charAt(i) == '+') {
            i++;
        }

        while (i < eqn.length() && Character.isDigit(eqn.charAt(i))) {
            coeff += eqn.charAt(i);
            i++;
        }

    int coefficient = coeff.isEmpty() ? 1 : Integer.parseInt(coeff);
    if (isNegative) {
        coefficient = -coefficient;
    }
        // Read variable
    char variable = 'x';
    boolean hasVariable = false;
    if (i < eqn.length() && Character.isLetter(eqn.charAt(i))) {
        variable = eqn.charAt(i);
        hasVariable = true;
        i++;
    }
    int power = 1;
    if (i == eqn.length()) {
        power = hasVariable ? 1 : 0;
    }
    // Read power
    if (i < eqn.length() && eqn.charAt(i) == '^') {
        i++;

        String pow = "";

        boolean negativePower = false;
        if (i < eqn.length() && eqn.charAt(i) == '-') {
            negativePower = true;
            i++;
        }

        while (i < eqn.length() && Character.isDigit(eqn.charAt(i))) {
            pow += eqn.charAt(i);
            i++;
        }

        power = pow.isEmpty() ? 1 : Integer.parseInt(pow);
        if (negativePower) {
            power = -power;
        }
    }
      this.coefficient = coefficient;
      this.variable = variable;
      this.power = power;
    }
    public String DerivativeOfTerm(){

        if (this.power == 0) {
            return "0";
        }
        int newCoefficient = this.coefficient * this.power;
        int newPower = this.power - 1;
        if (newPower == 0) {
            return String.valueOf(newCoefficient);
        } else if (newPower == 1) {
            return newCoefficient + String.valueOf(this.variable);
        } else {
            return newCoefficient + String.valueOf(this.variable) + "^" + newPower;
        }

    }
    public  static void DifferentialCalculus(){
        finale.setLength(0);
        if (term.isEmpty()) {
            return;
        }
        String originalExpression = FormatExpression(term, Operators);
        ArrayList<Term> simplifiedTerms = SimplifyOriginalExpression();
        String simplifiedExpression = FormatTermList(simplifiedTerms);
        String derivative = DerivativeOfTermList(simplifiedTerms);

        finale.append(FormatDerivativeNotation(originalExpression));
        if (!originalExpression.equals(simplifiedExpression)) {
            finale.append("\n=> ").append(FormatDerivativeNotation(simplifiedExpression));
        }
        finale.append("\n=> ").append(derivative);

        }
        public static String FormatDerivativeNotation(String expression) {
          return "d/d" + FindVariable(expression) + "(" + expression + ")";
        }
        public static char FindVariable(String expression) {
          String lowerExpression = expression.toLowerCase();
          for(int i = 0; i < expression.length(); i++) {
              if (isTrigFunctionNameAt(lowerExpression, i)) {
                  i += trigFunctionNameLength(lowerExpression, i) - 1;
                  continue;
              }
              char current = expression.charAt(i);
              if(Character.isLetter(current)) {
                  return current;
              }
          }
          return 'x';
        }
        public static boolean isTrigFunctionNameAt(String expression, int index) {
          return expression.startsWith("cosec", index) ||
                 expression.startsWith("sqrt", index) ||
                 expression.startsWith("csc", index) ||
                 expression.startsWith("sin", index) ||
                 expression.startsWith("cos", index) ||
                 expression.startsWith("tan", index) ||
                 expression.startsWith("cot", index) ||
                 expression.startsWith("sec", index) ||
                 expression.startsWith("exp", index) ||
                 expression.startsWith("ln", index) ||
                 expression.startsWith("e^", index);
        }
        public static int trigFunctionNameLength(String expression, int index) {
          if(expression.startsWith("cosec", index)) {
              return 5;
          }
          if(expression.startsWith("sqrt", index)) {
              return 4;
          }
          if(expression.startsWith("csc", index) ||
             expression.startsWith("sin", index) ||
             expression.startsWith("cos", index) ||
             expression.startsWith("tan", index) ||
             expression.startsWith("cot", index) ||
             expression.startsWith("sec", index) ||
             expression.startsWith("exp", index)) {
              return 3;
          }
          if(expression.startsWith("ln", index) ||
             expression.startsWith("e^", index)) {
              return 2;
          }
          return 1;
        }
        public static ArrayList<Term> SimplifyOriginalExpression() {
          ArrayList<Term> collapsedTerms = new ArrayList<>();
          ArrayList<Character> collapsedOperators = new ArrayList<>();
          Term current = term.get(0);

          for(int i = 0; i < Operators.size() && i + 1 < term.size(); i++) {
              char operator = Operators.get(i);
              Term nextTerm = term.get(i + 1);

              if(operator == '*') {
                  current = MultiplyTerms(current, nextTerm);
              } else if(operator == '/') {
                  current = DivideTerms(current, nextTerm);
              } else {
                  collapsedTerms.add(current);
                  collapsedOperators.add(operator);
                  current = nextTerm;
              }
          }
          collapsedTerms.add(current);

          return CombineLikeTerms(collapsedTerms, collapsedOperators);
        }
        public static ArrayList<Term> CombineLikeTerms(ArrayList<Term> termsToCombine, ArrayList<Character> operatorsToCombine) {
          ArrayList<Term> result = new ArrayList<>();

          for(int i = 0; i < termsToCombine.size(); i++) {
              Term signedTerm = termsToCombine.get(i);
              if(i > 0 && i - 1 < operatorsToCombine.size() && operatorsToCombine.get(i - 1) == '-') {
                  signedTerm = new Term(-signedTerm.coefficient, signedTerm.variable, signedTerm.power);
              }

              boolean combined = false;
              for(int j = 0; j < result.size(); j++) {
                  Term existing = result.get(j);
                  if(existing.variable == signedTerm.variable && existing.power == signedTerm.power) {
                      result.set(j, AddTerms(existing, signedTerm));
                      combined = true;
                      break;
                  }
              }

              if(!combined) {
                  result.add(signedTerm);
              }
          }

          for(int i = result.size() - 1; i >= 0; i--) {
              if(result.get(i).coefficient == 0) {
                  result.remove(i);
              }
          }
          if(result.isEmpty()) {
              result.add(new Term(0, 'x', 0));
          }
          return result;
        }
        public static String DerivativeOfTermList(ArrayList<Term> termsToDifferentiate) {
          ArrayList<Term> derivativeTerms = new ArrayList<>();
          for(Term currentTerm : termsToDifferentiate) {
              Term derivativeTerm = TermFromString(currentTerm.DerivativeOfTerm());
              derivativeTerms.add(derivativeTerm);
          }
          return FormatTermList(CombineLikeTerms(derivativeTerms, new ArrayList<Character>()));
        }
        public static String FormatExpression(ArrayList<Term> termsToFormat, ArrayList<Character> operatorsToFormat) {
          StringBuilder expression = new StringBuilder();
          if(termsToFormat.isEmpty()) {
              return "";
          }
          expression.append(FormatTerm(termsToFormat.get(0)));
          for(int i = 0; i < operatorsToFormat.size() && i + 1 < termsToFormat.size(); i++) {
              expression.append(" ").append(operatorsToFormat.get(i)).append(" ");
              expression.append(FormatTerm(termsToFormat.get(i + 1)));
          }
          return expression.toString();
        }
        public static String FormatTermList(ArrayList<Term> termsToFormat) {
          StringBuilder expression = new StringBuilder();
          for(Term currentTerm : termsToFormat) {
              if(currentTerm.coefficient == 0) {
                  continue;
              }
              if(expression.length() == 0) {
                  expression.append(FormatTerm(currentTerm));
              } else if(currentTerm.coefficient < 0) {
                  expression.append(" - ").append(FormatTerm(new Term(-currentTerm.coefficient, currentTerm.variable, currentTerm.power)));
              } else {
                  expression.append(" + ").append(FormatTerm(currentTerm));
              }
          }
          if(expression.length() == 0) {
              return "0";
          }
          return expression.toString();
        }
        public static String ProductRuleSteps(Term t1, Term t2) {
          Term dt1 = TermFromString(t1.DerivativeOfTerm());
          Term dt2 = TermFromString(t2.DerivativeOfTerm());
          Term firstProduct = MultiplyTerms(dt1, t2);
          Term secondProduct = MultiplyTerms(t1, dt2);
          Term finalTerm = AddTerms(firstProduct, secondProduct);

          return ProductRule(t1, t2) + "\n=> " +
                 FormatTerm(firstProduct) + " + " + FormatTerm(secondProduct) + "\n=> " +
                 FormatTerm(finalTerm);
        }
        public static String QuotientRuleSteps(Term t1, Term t2) {
          Term dt1 = TermFromString(t1.DerivativeOfTerm());
          Term dt2 = TermFromString(t2.DerivativeOfTerm());
          Term firstProduct = MultiplyTerms(dt1, t2);
          Term secondProduct = MultiplyTerms(t1, dt2);
          Term numerator = SubtractTerms(firstProduct, secondProduct);
          Term denominator = MultiplyTerms(t2, t2);

          return QuotientRule(t1, t2) + "\n=> (" +
                 FormatTerm(firstProduct) + " - " + FormatTerm(secondProduct) + ") / " +
                 FormatTerm(denominator) + "\n=> " +
                 FormatTerm(numerator) + " / " + FormatTerm(denominator) + "\n=> " +
                 DivideTermsToString(numerator, denominator);
        }
        public static String ProductRule(Term t1, Term t2) {
          return "((" + t1.DerivativeOfTerm() + ") * (" + t2.eqn + ")) + " +
                 "((" + t1.eqn + ") * (" + t2.DerivativeOfTerm() + "))";
        }
        public static String QuotientRule(Term t1, Term t2) {
          return "(((" + t1.DerivativeOfTerm() + ") * (" + t2.eqn + ")) - " +
                 "((" + t1.eqn + ") * (" + t2.DerivativeOfTerm() + "))) / " +
                 "((" + t2.eqn + ") * (" + t2.eqn + "))";
        }
        public static Term AddTerms(Term t1, Term t2) {
          if (t1.coefficient == 0) {
              return t2;
          }
          if (t2.coefficient == 0) {
              return t1;
          }
          if (t1.variable == t2.variable && t1.power == t2.power) {
              int newCoefficient = t1.coefficient + t2.coefficient;
              return new Term(newCoefficient, t1.variable, t1.power);
          }
          return new Term(0, t1.variable, 0);
        }
        public static Term SubtractTerms(Term t1, Term t2) {
          if (t2.coefficient == 0) {
              return t1;
          }
          if (t1.coefficient == 0) {
              return new Term(-t2.coefficient, t2.variable, t2.power);
          }
          if (t1.variable == t2.variable && t1.power == t2.power) {
              int newCoefficient = t1.coefficient - t2.coefficient;
              return new Term(newCoefficient, t1.variable, t1.power);
          }
          return new Term(0, t1.variable, 0);
        }
        public static Term MultiplyTerms(Term t1, Term t2) {
          int newCoefficient = t1.coefficient * t2.coefficient;
          if (newCoefficient == 0) {
              return new Term(0, t1.variable, 0);
          }
          char newVariable = t1.variable;
          int newPower = t1.power + t2.power;
          Term result = new Term(newCoefficient, newVariable, newPower);
          result.eqn = FormatTerm(result);
          return result;
        }
        public static Term DivideTerms(Term t1, Term t2) {
          if (t1.coefficient == 0) {
              return new Term(0, t1.variable, 0);
          }
          if (t2.coefficient == 0) {
              return new Term(0, t1.variable, 0);
          }
          int newCoefficient = t1.coefficient / t2.coefficient;
          int newPower = t1.power - t2.power;
          char newVariable = t1.power == 0 && t2.power != 0 ? t2.variable : t1.variable;
          Term result = new Term(newCoefficient, newVariable, newPower);
          result.eqn = FormatTerm(result);
          return result;
        }
        public static String DivideTermsToString(Term t1, Term t2) {
          if (t2.coefficient == 0) {
              return "Undefined";
          }
          int newCoefficient = t1.coefficient / t2.coefficient;
          int remainder = t1.coefficient % t2.coefficient;
          int newPower = t1.power - t2.power;
          if (remainder == 0) {
              return FormatTerm(new Term(newCoefficient, t1.variable, newPower));
          }
          return "(" + FormatTerm(t1) + ") / (" + FormatTerm(t2) + ")";
        }
        public static Term TermFromString(String value) {
          Term result = new Term(value);
          result.TermAssignment();
          result.eqn = FormatTerm(result);
          return result;
        }
        public static String FormatTerm(Term t) {
          if (t.coefficient == 0) {
              return "0";
          }
          if (t.power == 0) {
              return String.valueOf(t.coefficient);
          }
          if (t.power == 1) {
              return t.coefficient + String.valueOf(t.variable);
          }
          return t.coefficient + String.valueOf(t.variable) + "^" + t.power;
        }



}
