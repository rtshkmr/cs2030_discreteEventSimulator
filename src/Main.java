import cs2030.simulator.Manager;
import java.util.Scanner;

/**
 * Takes in a previously unknown number of doubles, reads them as Arrival Timings
 * of customers, creates a Manager that handles all the Customers. The Manager registers
 * all the events that happen and finally shows a log of what happened and some Statistics.
 */
public class Main {

    /**
     * Reads in unknown number of lines, line by line and prints out the events
     * generated for the day.
     *
     * @param args Cli arguments
     */
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int seed = sc.nextInt();
        int numServers = sc.nextInt();
        int qmax = sc.nextInt();
        int numArrivalEvents = sc.nextInt(); // aka number of customers expected to enter
        double lambda = sc.nextDouble();
        double mu = sc.nextDouble();
        double rho = sc.nextDouble();
        double pResting = sc.nextDouble();
        assert (lambda > 0 && mu > 0 && rho > 0);

//        Number[] arr = new Number[]{1, 2, 2, 20, 1.0, 1.0, 0.1, 0.5};
//        int seed = (int) arr[0];
//        int numServers = (int) arr[1];
//        int qmax = (int) arr[2];
//        int numArrivalEvents = (int) arr[3]; // aka number of customers expected to enter
//        double lambda = (double) arr[4];
//        double mu = (double) arr[5];
//        double rho = (double) arr[6];
//        double pResting = (double) arr[7];
//        assert (lambda > 0 && mu > 0 && rho > 0);


        Manager myManager = new Manager(seed,
            numServers,
            qmax,
            numArrivalEvents,
            lambda,
            mu,
            rho,
            pResting);
        sc.close();
        myManager.operate();
        System.out.println(myManager.showLogs());
    }
}
