package cs2030.simulator;

import java.util.Optional;
import java.util.PriorityQueue;


/**
 * A Manager holds Priority Queues of Customers and Events.
 * When Customers enter the shop, the Manager adds them to a queue.
 * Next the Manager helps the Customer decide what to do based on the information
 * that the manager can get from his employees, i.e. the servers.
 * Manager then records this event down and sends the newly decided
 * Customer back to the main queue for further action
 * RandomGenerator Documentation at:
 * https://www.comp.nus.edu.sg/~cs2030/RandomGenerator/cs2030/simulator/RandomGenerator.html
 */
public class Manager {

    private final PriorityQueue<Customer> mainQueue;
    private final PriorityQueue<Event> queuedEvents;
    private Server[] myServers;
    private final RandomGenerator randomGenerator;


    public Manager(int seed, int numServers, int qmax, int numArrivalEvents,
                   double lambda, double mu) {

        // init check print statements:
        System.out.println("====== initcheck ============");
        System.out.println("seed : " + seed);
        System.out.println("numServers : " + numServers);
        System.out.println("qmax : " + qmax);
        System.out.println("numArrivalEvents: " + numArrivalEvents);
        System.out.println("lambda: " + lambda);
        System.out.println("mu: " + mu);
        System.out.println("====== initcheck ============");

        this.mainQueue = new PriorityQueue<>();
        this.queuedEvents = new PriorityQueue<>();
        double rho = 0.0;
        this.randomGenerator = new RandomGenerator(seed, lambda, mu, rho);
        initServers(numServers, qmax);
        initArrivals(numArrivalEvents);
    }

    /**
     * Manager pops customers from queue and helps them decide what to do.
     * If Customer has a terminal state, then generate relevant side-effects.
     * Customers are popped off the mainQueue definitely only if they have
     * a terminal state (i.e. done/leaves).
     * Else, help the customer decide and add decided customer back to queue.
     */
    public void operate() {
        while (!this.mainQueue.isEmpty()) {
            Customer currentCustomer = mainQueue.poll();
            if(currentCustomer.firstWaits)  registerEvent(currentCustomer, mainQueue);
            System.out.println(">>>>>>>>> MANAGER PICKS FROM MAIN QUEUE, CUSTOMER: " + currentCustomer);

            if (!isTerminalState(currentCustomer)) {
                Customer changed = changeCustomerState(currentCustomer);
                // todo: aim: shift the event registry somewhere else.
//                if(changed.getCustomerStatus() != currentCustomer.getCustomerStatus()) {
//                    System.out.println("## event registered..");
//                    registerEvent(currentCustomer,mainQueue);
//                }
                System.out.println("++++++++++  MANAGER ADDS TO  MAIN QUEUE, CUSTOMER: " + changed);
                this.mainQueue.add(changed);

//                changed.ifPresentOrElse(this.mainQueue::add, () -> this.mainQueue.add(currentCustomer));
                // todo control when to register event
            } /*else registerEvent(currentCustomer, mainQueue);*/ // terminal event

            //            if (isTerminalState(currentCustomer)) {
//                handleTerminalState(currentCustomer);
//            } else { // manager helps them decide and adds them back to queue:
//                this.mainQueue.add(changeCustomerState(currentCustomer));
//            }
        }
    }

    /**
     * Non-terminal Customer's state is changed based on what should be done next.
     * If customer has arrived, handleArrivalState.
     * Else customer either is Waiting or Served(currently being served).
     * <p>
     * Side Effects:
     * 1. if customer's state is "Served" then the Assigned Server will be serving
     * the Customer and that needs to be reflected by the Server's state.
     *
     * @param c Customer that's at the head of the priority queue.
     * @return A modified form of the Customer after the Manager has provided sufficient
     * assistance to help the customer make a decision and change its state.
     */
    private Customer changeCustomerState(Customer c) {
        if (isArrivesState(c)) {
            return handleArrivalState(c);
        } else {
            Customer decided = null;
            if (isServedState(c)) {
                double completionTime = this.getCompletionTime(c.getPresentTime());
                decided = c.fromServedToDone(completionTime);
                // todo: the server is actually working on this customer now,
                //       1. the server's nextavailable time definitely changes
                //          to the completion time.
                //       2. the server needs to check if the customer being
                //          served right now was waiting in the queue, if so,
                //          then frees up the queue space.
                Server s = this.myServers[c.serverID -1];
                this.myServers[s.serverID - 1] = s.actuallyServeCustomer(decided);
            }
            if (isWaitsState(c)) {
                Server assignedServer = this.myServers[c.serverID - 1];

                System.out.println("XXX keep waiting? " +
                                       "assigned server has queuesize: " + assignedServer.waitingQueue.size());
                System.out.println("    assigned server's head has customer " + assignedServer.waitingQueue.peek());
                System.out.println("    assigned server's next avail " + assignedServer.nextAvailableTime);


                if(!c.equals(assignedServer.waitingQueue.peek())) {
                    System.out.println("XXX yes keep waiting");
                    decided = c.fromWaitsToWaits(assignedServer);
                } else {
                    System.out.println("XXX no change to served");
                    decided = c.fromWaitsToServed(assignedServer);
                }
            }
            return decided;
        }
    }

    /**
     * Manager queries the Servers, and modifies customers accordingly.
     *
     * @param c customer that has just arrived.
     * @return Customer that either gets served immediately, waits or leaves.
     */
    private Customer handleArrivalState(Customer c) {
        Server[] queriedServers = queryServers(c);
        System.out.println("\t---- outcome of querying the servers: ");
        for(Server s : queriedServers) {
            System.out.println("\t" + s);
        }


        Customer changedCustomer; // to be assigned based on query results
        if (queriedServers[0] != null) { // idleServer exists:
            Server s = queriedServers[0];
            System.out.println("\t\t!!!customer immediately assigned to " + s);
            changedCustomer = c.fromArrivesToServed(s.serverID);
            this.myServers[s.serverID - 1] = s.serveUponArrival();
        } else if (queriedServers[1] != null) { // queueableServer exists:
            Server x = queriedServers[1];
            System.out.println("\t\t!!! to queue at this server: " + x);
            changedCustomer = c.fromArrivesToWaits(x.nextAvailableTime, x.serverID);
            // todo: the queable server will be non-idle so that remains,
            //       the server's nextavailable time won't change either
            //       server will add this waiting customer
            this.myServers[x.serverID - 1] = x.addToWaitQueue(changedCustomer);
        } else { // create terminal state of leaving, server needn't bother:
            System.out.println("\t\t!!! customer leaves: ");
            changedCustomer = c.fromArrivesToLeaves();
        }
        return changedCustomer;
    }


    // todo: check this logic when fixing the servers...
    private Server[] queryServers(Customer c) {
        // gather relevant servers:
        Optional<Server> idleServer = Optional.empty(),
            queueableServer = Optional.empty(),
            shortestServer = Optional.empty();
        boolean foundIdle = false, foundQueueable = false;
        // todo: boolean checks whether s.isIdle and s.canQueue
        System.out.println("customer: " + c);
        for (Server s : this.myServers) {
//            boolean ans1 = s.isIdle(c);
//            boolean ans2 = s.canQueue(c);
//            System.out.println("\t\tisIdle?" + ans1);
//            System.out.println("\t\tcanQueue?" + ans2);
            System.out.println("\tSERVER STATUS WHEN QUERYING: " + s);
            System.out.println("\t\t****server qsize / qmax: " + s.waitingQueue.size() + "/"+ s.qmax);

            if (!foundIdle && s.isIdle(c)) { // look for idle servers:
                foundIdle = true;
                idleServer = Optional.of(s);
            }
            if (!foundIdle && !foundQueueable && s.canQueue(c)) { // look for queueable servers:
                foundQueueable = true;
                queueableServer = Optional.of(s);
                shortestServer = Optional.of(s); // init first
            }
            if (!foundIdle && foundQueueable && s.canQueue(c)) { // if possible find the shortest queue-server:
                if (s.waitingQueue.size() < queueableServer.get().waitingQueue.size()) {
                    shortestServer = Optional.of(s);
                }
            }
        }
        return new Server[]{idleServer.orElse(null),
            queueableServer.orElse(null),
            shortestServer.orElse(null)};
    }

    // todo: do nothing if terminal state? omg. YES DO NOTHING.
    private void handleTerminalState(Customer c) {
        if (isDoneState(c)) {
            // todo: [handle done state] maybe it affects the server?
            //       once done
            Server s = this.myServers[c.serverID - 1];
            this.myServers[s.serverID - 1] = s.doneServingCustomer();
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

    /*-----------------   INITIALIZERS -------------------------------*/
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
    private void initArrivals(int numArrivalEvents) {
        // generate arrival times:
        double now = 0;
        double[] arrivalTimes = new double[numArrivalEvents];
        for (int i = 0; i < numArrivalEvents; i++) {
            arrivalTimes[i] = now;
            now = getNextArrivalTime(now);
        }
        // generate arriving customers based on arrival times:
        for (double arrivalTime : arrivalTimes) {
            Customer myCustomer = Customer.enter(arrivalTime);
            this.mainQueue.add(myCustomer);
        }
    }

    private double getNextArrivalTime(double now) {
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
    private boolean isTerminalState(Customer x) {
        return isDoneState(x) || isLeavesState(x);
    }

    /*                  INTERMEDIATE STATES                        */
    private boolean isArrivesState(Customer x) {
        return x.getCustomerStatus().equals("arrives");
    }

    private boolean isWaitsState(Customer x) {
        return x.getCustomerStatus().equals("waits");
    }

    private boolean isServedState(Customer x) {
        return x.getCustomerStatus().equals("served");
    }

    /*                   TERMINAL STATES                          */
    private boolean isDoneState(Customer x) {
        return x.getCustomerStatus().equals("done");
    }

    private boolean isLeavesState(Customer x) {
        return x.getCustomerStatus().equals("leaves");
    }
    /*-------------------------------------------------------*/


    //=============================================================


}