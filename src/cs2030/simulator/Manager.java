package cs2030.simulator;

import java.util.LinkedList;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;


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
    private final Queue<String> logs;
    private Server[] myServers;
    private final double pRest;
    private final RandomGenerator randomGenerator;


    public Manager(int seed, int numServers, int qmax, int numArrivalEvents,
                   double lambda, double mu, double rho, double pRest) {
        this.mainQueue = new PriorityQueue<>();
        this.logs = new LinkedList<>();
        this.pRest = pRest;
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
            terminateRests(currentCustomer.getPresentTime());
            // if it's last person:
//            if(this.mainQueue.isEmpty() & !currentCustomer.firstWaits) {
//                Server assigned = this.myServers[ currentCustomer.serverID - 1];
//                assigned = assigned.stopResting(currentCustomer.getNextTime());
//                this.myServers[currentCustomer.serverID - 1] = assigned;
//            }

            //Systemprintln(">>>>>>>>> CUSTOMER: " + currentCustomer);
            if (currentCustomer.firstWaits) {
                registerEvent(currentCustomer);
                //Systemprintln("\t\t!!!event registered!");
            }
            //Systemprintln("\t\t\tcurrent servers' status \n \t\t\t _________________");
            for (Server s : this.myServers) {
                //Systemprintln("\t\t\t server: " + s);
            }
            if (!isTerminalState(currentCustomer)) {
                Customer changed = changeCustomerState(currentCustomer);
                //Systemprintln("++++++++++  CUSTOMER: " + changed + "\n\n");
                this.mainQueue.add(changed);
            } else {
                //Systemprintln("----------  CUSTOMER: " + currentCustomer + "\n\n");
                if(isDoneState(currentCustomer)) {
                    Server s = this.myServers[currentCustomer.serverID - 1];
                    //Systemprintln("{done state, check resting}");
                    //Systemprintln("Customer " + currentCustomer);
                    //Systemprintln("assigned server" + s);
                    double exitTime = currentCustomer.getPresentTime();
//                    boolean needsRest = this.serverNeedsRest();
//                    if(needsRest) {
//                        double restUntil = this.assignRestTime(exitTime);
//                        this.myServers[currentCustomer.serverID - 1] = s.startResting(restUntil);
//                    }
                    Server toInsert = serverHandlesDone(s, exitTime);
                    this.myServers[currentCustomer.serverID - 1] = toInsert;
                }
            }
        }
    }

    private Server serverHandlesDone(Server s, double exitTime) {
        s = s.doneServing();
        if(this.serverNeedsRest()) {
            double restUntil = this.assignRestTime(exitTime);
            s = s.startResting(restUntil);
        }
        return s;
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
                Server s = this.myServers[c.serverID - 1];
                //Systemprintln("Completion time: " + completionTime);
                //Systemprintln("Customer: from served to  done");
                Server newServer = s.actuallyServeCustomer(decided.getPresentTime());
//                Server toInsert = serverNeedsRest()
//                        ? newServer.startResting(assignRestTime(completionTime))
//                        : newServer;
                this.myServers[s.serverID - 1] = newServer;


//                if (serverNeedsRest()) {
//                    double endOfRestTime = assignRestTime(completionTime); // rest after completion!
//                    //Systemprintln("Server: serveThenRest");
//                    //Systemprintln("end of rest time: " + endOfRestTime);
//                    Server newServer = s.serveThenRest(endOfRestTime);
//                    this.myServers[s.serverID - 1] = newServer;
//                    //Systemprintln("after serving, server's state  {resting}: " + newServer);
//                } else {
//                    //Systemprintln("Server: actuallyServeCustomer");
//                    Server newServer = s.actuallyServeCustomer(decided.getPresentTime());
//                    this.myServers[s.serverID - 1] = newServer;
//                    //Systemprintln("after serving, server's state: " + newServer);
//
//                }


                // todo manager handles resting:

            }
            if (isWaitsState(c)) {
                Server assignedServer = this.myServers[c.serverID - 1];

                ////Systemprintln("XXX keep waiting? " +                                       "assigned server has queuesize: " + assignedServer.waitingQueue.size());
                ////Systemprintln("    assigned server's head has customer " + assignedServer.waitingQueue.peek());
                ////Systemprintln("    assigned server's next avail " + assignedServer.nextAvailableTime);


                // todo: when does a customer wait again?
                //       if the assigned server is resting
                //       or if he's not at the head of the queue.

                if( (assignedServer.isResting /*&& !this.mainQueue.isEmpty()*/)
                        || !assignedServer.isIdle(c.getPresentTime())
                        || ((!c.equals(assignedServer.waitingQueue.peek())/* && assignedServer.waitingQueue.size() > 1*/)
                )){



                    ////Systemprintln("XXX yes keep waiting");
                    decided = c.fromWaitsToWaits(assignedServer.nextAvailableTime);
                    //Systemprintln("Customer: from waits to  waits");
                    //Systemprintln("Server: nothing");
                } else {
                    ////Systemprintln("XXX no change to served");
                    decided = c.fromWaitsToServed(assignedServer.nextAvailableTime);
                    //Systemprintln("Customer: from waits to served");
                    //Systemprintln("Server: nothing");
                }
            }
            return decided;
        }
    }

    private boolean serverNeedsRest() {
        boolean res = this.randomGenerator.genRandomRest() < this.pRest;
        //Systemprintln("Server " + (res ? "needs" : "doesn't need") + " rest");
        return res;
    }

    private double assignRestTime(double completionTime) {
        double restDuration = this.randomGenerator.genRestPeriod();
        //Systemprintln("finish serving at " + completionTime);
        //Systemprintln("server rests until: " + (restDuration + completionTime) );
        return restDuration + completionTime;
    }

    /**
     * Manager queries the Servers, and modifies customers accordingly.
     *
     * @param c customer that has just arrived.
     * @return Customer that either gets served immediately, waits or leaves.
     */
    private Customer handleArrivalState(Customer c) {
        Server[] queriedServers = queryServers(c);
        ////Systemprintln("\t---- outcome of querying the servers: ");
//        for(Server s : queriedServers) {
//            ////Systemprintln("\t" + s);
//        }


        Customer changedCustomer; // to be assigned based on query results
        if (queriedServers[0] != null) { // idleServer exists:
            Server s = queriedServers[0];
            ////Systemprintln("\t\t!!!customer immediately assigned to " + s);
            changedCustomer = c.fromArrivesToServed(s.serverID);
            //Systemprintln("Customer: from arrives to  Served");
            //Systemprintln("Server: serveUponArrival");
            this.myServers[s.serverID - 1] = s.serveUponArrival();
        } else if (queriedServers[1] != null) { // queueableServer exists:
            Server s = queriedServers[1];
            ////Systemprintln("\t\t!!! to queue at this server: " + x);
            changedCustomer = c.fromArrivesToWaits(s.nextAvailableTime, s.serverID);
            // todo: the queable server will be non-idle so that remains,
            //       the server's nextavailable time won't change either
            //       server will add this waiting customer
            Queue<Customer> newQueue = new LinkedList<>(s.waitingQueue);
            newQueue.add(changedCustomer);
            //Systemprintln("Customer: from arrives to  waits");
            //Systemprintln("Server: addToWaitQueue");

            this.myServers[s.serverID - 1] = s.addToWaitQueue(newQueue);
        } else { // create terminal state of leaving, server needn't bother:
            ////Systemprintln("\t\t!!! customer leaves: ");
            changedCustomer = c.fromArrivesToLeaves();
            //Systemprintln("Customer: from arrives to  leaves");
            //Systemprintln("Server: nothing");
        }
        return changedCustomer;
    }

    private void terminateRests(double now) {
//        for (Server s : this.myServers) {
//            s = s.stopResting(now);
//        }
        for(int i = 0; i < this.myServers.length; i++) {
            this.myServers[i] = this.myServers[i].stopResting(now);
        }
    }


    // todo: check this logic when fixing the servers...
    private Server[] queryServers(Customer c) {
        // gather relevant servers:
        Optional<Server> idleServer = Optional.empty(),
            queueableServer = Optional.empty(),
            shortestServer = Optional.empty();
        boolean foundIdle = false, foundQueueable = false;
        // todo: boolean checks whether s.isIdle and s.canQueue
        ////Systemprintln("customer: " + c);
        for (Server s : this.myServers) {
//            boolean ans1 = s.isIdle(c);
//            boolean ans2 = s.canQueue(c);
//            ////Systemprintln("\t\tisIdle?" + ans1);
//            ////Systemprintln("\t\tcanQueue?" + ans2);
            ////Systemprintln("\tSERVER STATUS WHEN QUERYING: " + s);
            ////Systemprintln("\t\t****server qsize / qmax: " + s.waitingQueue.size() + "/"+ s.qmax);
            double now = c.getPresentTime();
            s = s.stopResting(now);

            if (!foundIdle && s.isIdle(now)) { // look for idle servers:
                foundIdle = true;
                idleServer = Optional.of(s);
            }
            if (!foundIdle && !foundQueueable && s.canQueue(now)) { // look for queueable servers:
                foundQueueable = true;
                queueableServer = Optional.of(s);
                shortestServer = Optional.of(s); // init first
            }
            if (!foundIdle && foundQueueable && s.canQueue(now)) { // if possible find the shortest queue-server:
                if (s.waitingQueue.size() < queueableServer.get().waitingQueue.size()) {
                    shortestServer = Optional.of(s);
                }
            }
        }
        return new Server[]{idleServer.orElse(null),
            queueableServer.orElse(null),
            shortestServer.orElse(null)};
    }

    /**
     * Manager shows the Logs of the day's events, including the Customer Statistics.
     *
     * @return String Representation of all the events that have happened
     */
    public String showLogs() {
        StringBuilder res = new StringBuilder();
        for (String s : logs) {
            res.append(s).append("\n");
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

    private void registerEvent(Customer c) {
        this.logs.add(c.toString());

    }


    /*-----------------   STATE CHECKS ----------------------*/
    private boolean isTerminalState(Customer c) {
        return isDoneState(c) || isLeavesState(c);
    }

    /*                  INTERMEDIATE STATES                        */
    private boolean isArrivesState(Customer c) {
        return c.getCustomerStatus().equals("arrives");
    }

    private boolean isWaitsState(Customer c) {
        return c.getCustomerStatus().equals("waits");
    }

    private boolean isServedState(Customer c) {
        return c.getCustomerStatus().equals("served");
    }

    /*                   TERMINAL STATES                          */
    private boolean isDoneState(Customer c) {
        return c.getCustomerStatus().equals("done");
    }

    private boolean isLeavesState(Customer c) {
        return c.getCustomerStatus().equals("leaves");
    }
    /*-------------------------------------------------------*/


    //=============================================================


}