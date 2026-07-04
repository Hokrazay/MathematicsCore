public class Utility{
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
}
