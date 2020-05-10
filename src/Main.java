import cs2030.simulator.Manager;

import java.util.Scanner;

/**
 * Main class drives the simulation: takes in inputs, generates Manager, makes the
 * manager do its operations and finally retrieves a log of the day's events and
 * statistics from the Manager.
 */
public class Main {

    /**
     * Reads in inputs from the cli.
     *
     * @param args Cli arguments
     */
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int seed = sc.nextInt();
        int numServers = sc.nextInt();
        int numSelfServers = sc.nextInt();
        int qmax = sc.nextInt();
        int numArrivalEvents = sc.nextInt(); // aka number of customers expected to enter
        double lambda = sc.nextDouble();
        double mu = sc.nextDouble();
        double rho = sc.nextDouble();
        double probResting = sc.nextDouble();
        double probGreedy = sc.nextDouble();
        assert (lambda > 0 && mu > 0 && rho > 0);

        Manager myManager = new Manager(seed,
            numServers,
            numSelfServers,
            qmax,
            numArrivalEvents,
            lambda,
            mu,
            rho,
            probResting,
            probGreedy);
        sc.close();
        myManager.operate();
        System.out.println(myManager.showLogs());
    }
}
