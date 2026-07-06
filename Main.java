
// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Main extends Utility {
   public Main() {
   }

   public static void main(String[] var0) {
      Scanner var1 = new Scanner(System.in);
      LocalTime var2 = LocalTime.now();
      pln("User time : " + var2.format(DateTimeFormatter.ofPattern("HH:mm")) + " hrs");
      pln(greetingFor(var2) + " Sir/Madam,");
      pln("What would you like to do today?");
      pln("1. Binomial Expression Solving");
      pln("2. Calculus");
      pln("3. Trignometry");
      switch (var1.nextLine().trim()) {
         case "1":
             pln("Under Development");
             break;
         case "2":
             Term.runCalculus(var1);
             break;
         case "3":
             pln("Under Development");
             break;
         default:
             pln("Invalid choice!");
      }

   }

   private static String greetingFor(LocalTime var0) {
      int var1 = var0.getHour();
      if (var1 < 12) {
         return "Good Morning";
      } else {
         return var1 < 17 ? "Good Afternoon" : "Good Evening";
      }
   }
}

