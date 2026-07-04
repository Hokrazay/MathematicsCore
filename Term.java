
//Project Started By Saket Shrivastava on 03/07/2026
/* 03/07/20206 - Made Structure of the Term.java file and added the TermAssignment() method to assign values to the coefficient, variable, and power of a term based on the input equation. 
 Also added the DerivativeOfTerm() method to calculate the derivative of a term. 
 Started to work on DifferentialCalculus() method to perform differential calculus operations on the equation. 
 Made outlines of MultiplyTerms() and DivideTerms() methods to perform multiplication and division of terms respectively.
*/
import java.util.*;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;



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
        
        
        pln("Enter the equation in the format given below : \n");
        pln("Example : x^3 + 3x^2 - 4x + 5");
        pln("Ensure that there is a space between the terms and the operators.\n");
        pln("Ensure there is only ONE VARIABLE and only ONE OF EACH OPERATOR in the equation.\n");
        pln("Note :\n-> 1 is represented as 1x^0 \n-> 0 is represented as 0x^0\n-> x is represented as 1x^1");
        String equation = input.nextLine();
        String[] tokens = equation.split(" ");

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

      pln("What function do you want to perform on the equation? Enter your choice:");
      pln("\n1. [D] Derivative\n3. [P] Partial Derivative\n4. [I] Definite Integration\n5. [N] Indefinite Integration\n6. [E] Exit");
      char choice = input.next().charAt(0);
      switch(choice) {
        case 'd':
        case 'D':
          DifferentialCalculus();
          break;
        case 'p':
        case 'P':
          // Perform partial derivative
          break;
        case 'i':
        case 'I':
          // Perform definite integration
          break;
        case 'n':
        case 'N':
          // Perform indefinite integration
          break;
        case 'e':
        case 'E':
          pln("Exiting the program.");
          System.exit(0);
          
        default:
          pln("Invalid choice!");
      }
      input.close();
      pln("Final Result: \n" + finale.toString());

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

        while (i < eqn.length() && Character.isDigit(eqn.charAt(i))) {
            pow += eqn.charAt(i);
            i++;
        }

        power = pow.isEmpty() ? 1 : Integer.parseInt(pow);
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

        finale.append(originalExpression);
        if (!originalExpression.equals(simplifiedExpression)) {
            finale.append("\n=> ").append(simplifiedExpression);
        }
        finale.append("\n=> ").append(derivative);
        
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
          Term result = new Term(newCoefficient, t1.variable, newPower);
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
        




    /* public static void DifferentialCalculus(){
        //==Division Checking==
     for(int i = 0; i < Operators.size(); i++){
     if (Operators.get(i) == '/') {
        
     }else{continue;}
    
     }
     for(int i = 0; i < Operators.size(); i++){
        if (Operators.get(i) == '/') {
            String get = "";
            Term term1 = term.get(i);
            Term term2 = term.get(i + 1);
            get = term1.DivideTermsC(term1, term2);
            Term newTerm = new Term(get);
            newTerm.TermAssignment();
            
            term.set(i,newTerm);
            term.remove(i+1);
        } else {
            continue;
        }}
     for(int i = 0; i < Operators.size(); i++){
        if (Operators.get(i) == '*') {
            String get = "";
            Term term1 = term.get(i);
            Term term2 = term.get(i + 1);            
            Term newTerm = MultiplyTermsC(term1, term2);
            newTerm.TermAssignment();
            
            term.set(i,newTerm);
            term.remove(i+1);
          
        } else {
            continue;
        }} for(Term t : term){
            pln("Derivative of the terms: " + t.eqn);
        }
    
       
    
    }
     public static Term DivideTermsC(Term t1, Term t2){
        String result = "";
        Term dt1 = new Term(t1.DerivativeOfTerm());
        dt1.TermAssignment();
        Term dt2 = new Term(t2.DerivativeOfTerm());
        dt2.TermAssignment();
        result = "("+MultiplyTerms(t2,dt1).eqn+" - "+MultiplyTerms(t1,dt2).eqn +")"+" / ("+MultiplyTerms(t2,t2).eqn+")";
        return new Term(result);
    }
        public static Term MultiplyTermsC(Term t1, Term t2){
        String result = "";
        Term dt1 = new Term(t1.DerivativeOfTerm());
        dt1.TermAssignment();
        Term dt2 = new Term(t2.DerivativeOfTerm());
        dt2.TermAssignment();
        Term returnedTerm = new Term(MultiplyTerms(t1,dt2).eqn); 
        Term nextreturnedTerm = new Term(MultiplyTerms(t2,dt1).eqn);       
        return returnedTerm;
    }
   public static Term MultiplyTerms(Term t1, Term t2) {
       String result = "";
        result = t1.coefficient * t2.coefficient + String.valueOf(t1.variable) + "^" + (t1.power + t2.power);
       return new Term(result);
    }
    public static Term DivideTerms(Term term1, Term term2) {
        int newCoefficient = term1.coefficient / term2.coefficient;
        char newVariable = term1.variable; // Assuming both terms have the same variable
        int newPower = term1.power - term2.power;
        Term z = new Term(newCoefficient + String.valueOf(newVariable) + "^" + newPower);
        z.TermAssignment();
        return z;
    }
        */
    
}

