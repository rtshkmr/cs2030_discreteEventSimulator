package cs2030.simulator;

import java.util.Optional;
import java.util.PriorityQueue;

// RNG javadocs: https://www.comp.nus.edu.sg/~cs2030/RandomGenerator/cs2030/simulator/RandomGenerator.html

/**
 * A Manager holds Priority Queues of Customers and Events.
 * When Customers enter the shop, the Manager adds them to a queue.
 * Next the Manager helps the Customer decide what to do based on the information
 * that the manager can get from his employees, i.e. the servers.
 * Manager then records this event down and sends the newly decided
 * Customer back to the main queue for further action
 */
public class Manager {
    // attributes:
    private PriorityQueue<Customer> mainQueue;
    private PriorityQueue<Event> queuedEvents;
    private Server[] myServers;
    private int numArrivalEvents;
    private RandomGenerator randomGenerator;

    /**
     * Constructs the single instance of manager.
     *
     * @param mainQueue a priority queue holding customers of various states,
     *                  prioritised based on time their state was assigned at
     *                  and their customer ID.
     */

    /*  Manager's constructor, single use only.
     * - assign the int variables
     * - create the randomGenerator to use for timing generation later
     * - create the array of servers
     * - generate the arrival timings
     * */
    public Manager(PriorityQueue<Customer> mainQueue, int seed, int numServers, int qmax, int numArrivalEvents,
                   double lambda, double mu) {
        this.mainQueue = mainQueue;
        this.queuedEvents = new PriorityQueue<>();
        this.numArrivalEvents = numArrivalEvents;
        double rho = 0.0;
        this.randomGenerator = new RandomGenerator(seed, lambda, mu, rho);
        initServers(numServers, qmax);
        initArrivals();
    }


    /**
     * Manager helps a customer decide what action to take, given the info
     * that the Manager can provide to the Customer.
     *
     * @param c Customer that's at the head of the priority queue.
     * @return A modified form of the Customer after the Manager has provided sufficient
     * assistance to help the customer make a decision and change its state.
     */
    private Customer helpCustomerDecide(Customer c) {
        if (arrivesState(c)) {
            return handleArrivalState(c);
        } else {
            Customer decided = null;
            if (servedState(c)) {
                double completiontime = this.getCompletionTime(c.getPresentTime());
                decided = c.fromServedToDone(completiontime);
            }
            if (waitsState(c)) {
                decided = c.fromWaitsToServed();
            }
            return decided;
        }
    }

    /**
     * Until the queue of customers is empty, the manager helps the customers
     * decide what they want to do and modify the servers accourdingly to the
     * Customers' decisions.
     */
    public void operate() {
        /*---------------------------------------------------------------
         * Customers are popped off the mainQueue definitely only if they have
         * a terminal state. Intermediate state customers will be modified and
         * pushed back into the mainQueue.
         *--------------------------------------------------------------- */
        while (!this.mainQueue.isEmpty()) {
            Customer currentCustomer = mainQueue.poll();
            registerEvent(currentCustomer, mainQueue);
            if (terminalStatus(currentCustomer)) {
                handleTerminalState(currentCustomer);
            } else { // manager helps them decide and adds them back to queue:
                this.mainQueue.add(helpCustomerDecide(currentCustomer));
            }
        }
    }

    private Customer handleArrivalState(Customer c) {
        Server[] queriedServers = queryServers();
        Customer decided;
        if (queriedServers[0] != null) { // idleServer exists:
            // todo: arrival to Served [customer + server changes]
            decided = c.fromArrivesToServed(queriedServers[0].serverID);
        } else if (queriedServers[1] != null) { // queueableServer exists:
            Server s = queriedServers[1];
            decided = c.fromArrivesToWaits(s.nextAvailableTime, s.serverID);
        } else { // create terminal state of leaving:
            decided = c.fromArrivesToLeaves();
        }
        return decided;
    }

    private Server[] queryServers() {
        // gather relevant servers:
        Optional<Server> idleServer = Optional.empty(),
            queueableServer = Optional.empty(),
            shortestServer = Optional.empty();
        boolean foundIdle = false, foundQueueable = false;
        // todo: boolean checks whether s.isIdle and s.canQueue
        for (Server s : this.myServers) {
            if (!foundIdle && s.isIdle) { // look for idle servers:
                foundIdle = true;
                idleServer = Optional.of(s);
            }
            if (!foundQueueable && s.canQueue) { // look for queueable servers:
                foundQueueable = true;
                queueableServer = Optional.of(s);
                shortestServer = Optional.of(s); // init
            }
            if (foundQueueable) { // if possible find the shortest queue-server:
                if (s.waitingQueue.size() < queueableServer.get().waitingQueue.size()) {
                    shortestServer = Optional.of(s);
                }
            }
        }
        return new Server[]{idleServer.orElse(null),
            queueableServer.orElse(null),
            shortestServer.orElse(null)};
    }

    private void handleTerminalState(Customer c) {
        if (doneState(c)) {
            // todo: [handle done state] maybe it affects the server?

        }
        // don't do anything if a customer leaves.
    }

    /**
     * Manager shows the Logs of the day's events, including the Customer Statistics.
     *
     * @return String Representation of all the events that have happened
     */
    public String showLogs() {
        StringBuilder res = new StringBuilder();
        for (Event e : queuedEvents) {
            res.append(e).append("\n");
        }
        res.append(Customer.customerStats());
        return res.toString();
    }


    //=================  HELPERS: =============================

    /*-----------------   INITIALIZER -------------------------------*/
    private void initServers(int numServers, int qmax) {
        // create array of servers and then assign to the servers field:
        Server[] servers = new Server[numServers];
        for (int i = 0; i < numServers; i++) {
            Server s = new Server(i + 1, qmax);
            servers[i] = s;
        }
        this.myServers = servers;
    }

    /*                   RANDOMISATION                       */
    private void initArrivals() {
        // generate arrival timings:
        double now = 0;
        double[] arrivalTimes = new double[this.numArrivalEvents];
        for (int i = 0; i < this.numArrivalEvents; i++) {
            arrivalTimes[i] = now;
            now = getNextArrival(now);
        }
        // generate arriving customers:
        for (double arrivalTime : arrivalTimes) {
            Customer myCustomer = Customer.enter(arrivalTime);
            this.mainQueue.add(myCustomer);
        }
    }

    private double getNextArrival(double now) {
        return now + this.randomGenerator.genInterArrivalTime();
    }

    private double getCompletionTime(double now) {
        return now + this.randomGenerator.genServiceTime();
    }
    /*----------------------------------------------------------*/

    private void registerEvent(Customer c, PriorityQueue<Customer> rest) {
        Event currentEvent = new Event(c, rest);
        this.queuedEvents.add(currentEvent);
    }


    /*-----------------   STATE CHECKS ----------------------*/
    private boolean terminalStatus(Customer x) {
        return doneState(x) || leavesState(x);
    }

    /*                  INTERMEDIATE STATES                        */
    private boolean arrivesState(Customer x) {
        return x.getCustomerStatus().equals("arrives");
    }

    private boolean waitsState(Customer x) {
        return x.getCustomerStatus().equals("waits");
    }

    private boolean servedState(Customer x) {
        return x.getCustomerStatus().equals("served");
    }

    /*                   TERMINAL STATES                          */
    private boolean doneState(Customer x) {
        return x.getCustomerStatus().equals("done");
    }

    private boolean leavesState(Customer x) {
        return x.getCustomerStatus().equals("leaves");
    }
    /*-------------------------------------------------------*/


    //=============================================================


}