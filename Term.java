
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

        Term(int coefficient, char variable, int power) {
            this.coefficient = coefficient;
            this.variable = variable;
            this.power = power; 
        }
        Term(String eqn) {
            this.eqn = eqn;
        }
    
    public  static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        
        
        pln("Enter the equation in the format given below : \n");
        pln("Example : x^3 + 3x^2 - 4x + 5");
        pln("Ensure that there is a space between the terms and the operators.\n");
        pln("Note :\n-> 1 is represented as x^0 \n-> 0 is represented as 0x^0\n-> x is represented as 1x^1");
        String equation = input.nextLine();
        String[] tokens = equation.split(" ");

        for(String token : tokens) {

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

    }
    public   void TermAssignment() {
    int i = 0;
    // Read coefficient
    String coeff = "";
    while (i < eqn.length() && Character.isDigit(eqn.charAt(i))) {
        coeff += eqn.charAt(i);
        i++;
    }

    int coefficient = coeff.isEmpty() ? 1 : Integer.parseInt(coeff);
        // Read variable
    char variable = eqn.charAt(i);
    i++;
    int power = 1;
    // Read power
    if (i < eqn.length() && eqn.charAt(i) == '^') {
        i++;

        String pow = "";

        while (i < eqn.length() && Character.isDigit(eqn.charAt(i))) {
            pow += eqn.charAt(i);
            i++;
        }

        power = Integer.parseInt(pow);
    }
      this.coefficient = coefficient;
      this.variable = variable;
      this.power = power;
    }
    public String DerivativeOfTerm(){
       
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
    public static void DifferentialCalculus(){
        //==Division Checking==
     for(int i = 0; i < Operators.size(); i++){
     if (Operators.get(i) == '/') {
        
     }else{continue;}
    
     }
     for(int i = 0; i < Operators.size(); i++){
        if (Operators.get(i) == '*') {
            String result = "";
            Term term1 = term.get(i);
            Term term2 = term.get(i + 1);
            result = term1.MultiplyTerms(term1, term2);
            pln("The result of multiplication is: " + result);
        } else {
            continue;
        }}
    
       
    
    }
    public String DivideTerms(Term term1, Term term2){
        String result = "";
        String diffrentiatedTerm1 = term1.DerivativeOfTerm();
        String diffrentiatedTerm2 = term2.DerivativeOfTerm();
        // Perform division logic here
        return result;
    }
    public String MultiplyTerms(Term term1, Term term2){
        String result = "";
        String diffrentiatedTerm1 = term1.DerivativeOfTerm();
        String diffrentiatedTerm2 = term2.DerivativeOfTerm();
        // Perform multiplication logic here
        return result;
    }
}

