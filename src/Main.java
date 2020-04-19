
import cs2030.simulator.*;
import cs2030.simulator.Manager;

import java.util.Scanner;
import java.util.PriorityQueue;

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
        /*
         takes in unknown number of user inputs:
         todo: settle the new input types:
        */
        Scanner sc = new Scanner(System.in);
        int baseSeed = sc.nextInt();
        int numServers = sc.nextInt();
        int Qmax = sc.nextInt();
        int numArrivalEvents = sc.nextInt(); // aka number of customers expected to enter
        double lambda = sc.nextDouble();
        double mu = sc.nextDouble();
        assert (lambda > 0 && mu > 0);
        Manager myManager = new Manager(new PriorityQueue<>(),
                                        baseSeed,
                                        numServers,
                                        Qmax,
                                        numArrivalEvents,
                                        lambda,
                                        mu);
        sc.close();
        myManager.operate();
        System.out.println(myManager.showLogs());
    }
}
