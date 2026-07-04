public class Functions {
      public static void main(String[] args) {}
      public Term AddTerms(Term t1, Term t2) {
          String result = "";
          if (t1.variable == t2.variable && t1.power == t2.power) {
              int newCoefficient = t1.coefficient + t2.coefficient;
              result = newCoefficient + String.valueOf(t1.variable) + "^" + t1.power;
          } else {
              CaseNotHandled(t1, t2)
              ;
          }
          return new Term(result);
      }
      public Term MultiplyTerms(Term t1, Term t2) {
                   String coeff = String.valueOf(t1.coefficient * t2.coefficient);
            String result = coeff + String.valueOf(t1.variable) + "^" + (t1.power + t2.power);
            return new Term(result);
      }

      public Term DivideTermsC(Term t1, Term t2) {
            // placeholder implementation
            return new Term("");
      }

      public Term MultiplyTermsC(Term t1, Term t2) {
            // placeholder implementation
            return new Term("");
      }

      public Term DivideTerms(Term term1, Term term2) {
            // placeholder implementation
            return new Term("");
      }
      public String CaseNotHandled(Term t1, Term t2) {
          String message = "Case not handled for terms: " + t1.eqn + " and " + t2.eqn;
          System.out.println(message);
          return message;
      }
    }

