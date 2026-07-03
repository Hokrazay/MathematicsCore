

import java.util.*;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;



public class Term extends Utility {

    
        int coefficient;
        char variable;
        int power;
        String eqn;
        static ArrayList<Term> term = new ArrayList<>();

        Term(int coefficient, char variable, int power) {
            this.coefficient = coefficient;
            this.variable = variable;
            this.power = power; 
        }
        Term(String eqn) {
            this.eqn = eqn;
        }
    
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        
        List<Character> Operators = new ArrayList<>();
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
        input.close();
        

    }
    public  void TermAssignment() {
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
    }
