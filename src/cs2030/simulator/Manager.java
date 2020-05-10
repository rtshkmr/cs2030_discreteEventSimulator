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
    private final int numHumanServers;


    public Manager(int seed, int numServers, int numSelfServers, int qmax, int numArrivalEvents,
                   double lambda, double mu, double rho, double pRest) {
        this.mainQueue = new PriorityQueue<>();
        this.numHumanServers = numServers;
        this.logs = new LinkedList<>();
        this.pRest = pRest;
        this.randomGenerator = new RandomGenerator(seed, lambda, mu, rho);
        initServers(numServers, numSelfServers, qmax);
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
            }
            if (!isTerminalState(currentCustomer)) {
                Customer changed = changeCustomerState(currentCustomer);
                this.mainQueue.add(changed);
            } else { // customer has terminal state of leaves or done
                if (isDoneState(currentCustomer)) {
                    Server s = this.myServers[currentCustomer.serverID - 1];
                    double exitTime = currentCustomer.getPresentTime();
                    // todo: make this a void method:
                    serverHandlesDone(s, exitTime);
                }
            }
        }
    }

    // #todo handle the output such that the type of Server shouldn't change
    //   maybe via wildcards?
    private void serverHandlesDone(Server s, double exitTime) {
        if (!(s instanceof SelfServer)) {
            s = s.doneServing();
            if (this.serverNeedsRest()) {
                double restUntil = this.assignRestTime(exitTime);
                s = s.startResting(restUntil);
            }
            updateServerArray(s);
        } else {
            SelfServer selfS = (SelfServer) s;
            selfS = selfS.doneServing();
//            this.myServers[selfS.serverID - 1] = selfS;
            updateServerArray(selfS);
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
        Customer decided = null;
        if (isArrivesState(c)) {
            return handleArrivalState(c);
        } else {
            if (isServedState(c)) { // served --> done, server depends on what kind:
                double completionTime = this.getCompletionTime(c.getPresentTime());
                decided = c.fromServedToDone(completionTime);
                Server s = this.myServers[c.serverID - 1];
                if (!(s instanceof SelfServer)) {
                    Server newServer = s.actuallyServeCustomer(decided.getPresentTime());
                    this.myServers[s.serverID - 1] = newServer;
                } else {
                    SelfServer newServer = (SelfServer) s;
                    newServer = newServer.actuallyServeCustomer(decided.getPresentTime());
                    this.myServers[newServer.serverID - 1] = newServer;
                }
//                Server newServer = s.actuallyServeCustomer(decided.getPresentTime());
//                Server toInsert = serverNeedsRest()
//                        ? newServer.startResting(assignRestTime(completionTime))
//                        : newServer;
//                this.myServers[s.serverID - 1] = newServer;
            }
            if (isWaitsState(c)) {
                Server assignedServer = this.myServers[c.serverID - 1];
                // todo: when does a customer wait again?
                //       if the assigned server is resting
                //       or if he's not at the head of the queue.
                if (!(assignedServer instanceof SelfServer)) {
                    if (assignedServer.isResting
                            || !assignedServer.isIdle(c.getPresentTime())
                            || ((!c.equals(assignedServer.waitingQueue.peek()))
                    )) {
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
                } else { // settle for selfServers:
                    double now = c.getPresentTime();
                    SelfServer selfServer = (SelfServer) assignedServer;
                    SelfServer nextBestServer = querySelfServers(now);
                    // reassign first:
                    c = c.reassignServer(nextBestServer.serverID);
                    // from waits to served if is idle at c.getPresent time:
                    if(nextBestServer.isIdle(now)) {
                        decided = c.fromWaitsToServed(nextBestServer.nextAvailableTime);
                    } else {
                        decided = c.fromWaitsToWaits(nextBestServer.nextAvailableTime);
                    }

//                    if (nextBestServer != null /*&& freeSelfServer.isIdle(c.getPresentTime())*/) { // waits to served:
//                        c = c.reassignServer(nextBestServer.serverID);
//                        decided = c.fromWaitsToServed(nextBestServer.nextAvailableTime);
//                    } else { // from waits to waits:
//                        decided = c.fromWaitsToWaits(selfServer.nextAvailableTime);
//                    }
                }
            }
            return decided;
        }
    }

    private boolean someoneElseWaiting(int serverID, double time) {
        for(Customer c : this.mainQueue) {
            if(c.getCustomerStatus().equals("waits") && c.serverID == serverID){
                return true;
            }
        }
        return false;
    }

    /**
     * method is called when the customer is in selfservice queue and checks if
     * any of the selfServers are free at that time.
     * @param now
     * @return
     */
    private SelfServer querySelfServers(double now) {
        // I'm looking to see if anyone is free:
        for (int i = numHumanServers; i < this.myServers.length; i++) {
            SelfServer s = (SelfServer) this.myServers[i];
            if (s.isIdle(now)) {
                return s;
            }
        }
        // i'm looking for the nextEarliestAvailable selfServer  who's gonna be free the earliest:
        // none idle but find next best selfServer...
        SelfServer nextBestServer = null;
        double shortestTimeDiff = Double.MAX_VALUE;
        for (int i = numHumanServers; i < this.myServers.length; i++) {
            SelfServer s = (SelfServer) this.myServers[i];
            double timeDiff = s.nextAvailableTime - now;
            if (timeDiff < shortestTimeDiff) {
                nextBestServer = s;
                shortestTimeDiff = timeDiff;
            }
        }
        return nextBestServer;
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
        Server[] queriedServers = queryServers(c); // todo: Server[] supposed to be covariant. type shouldn't change
        Customer changedCustomer; // to be assigned based on query results
        if (queriedServers[0] != null) { // idleServer exists:
            Server s = queriedServers[0];
            ////Systemprintln("\t\t!!!customer immediately assigned to " + s);
            changedCustomer = c.fromArrivesToServed(s.serverID);
            //Systemprintln("Customer: from arrives to  Served");
            //Systemprintln("Server: serveUponArrival");
            if (!(s instanceof SelfServer)) { // normal server:
                updateServerArray(s.serveUponArrival());
            } else { // it's a self server:
                SelfServer selfServer = (SelfServer) s;
                updateServerArray(selfServer.serveUponArrival());
            }
        } else if (queriedServers[1] != null) { // queueableServer exists:
            Server s = queriedServers[1];
            ////Systemprintln("\t\t!!! to queue at this server: " + x);
            changedCustomer = c.fromArrivesToWaits(s.nextAvailableTime, s.serverID);
            // todo: the queable server will be non-idle so that remains,
            //       the server's nextavailable time won't change either
            //       server will add this waiting customer
            if (!(s instanceof SelfServer)) {
                Queue<Customer> newQueue = new LinkedList<>(s.waitingQueue);
                newQueue.add(changedCustomer);
                updateServerArray(s.addToWaitQueue(newQueue));
            } else {
                Queue<Customer> newQueue = new LinkedList<>(SelfServer.sharedQueue);
                newQueue.add(changedCustomer);
                SelfServer selfServer = (SelfServer) s;
                updateServerArray(selfServer.addToWaitQueue(newQueue));
            }
            //Systemprintln("Customer: from arrives to  waits");
            //Systemprintln("Server: addToWaitQueue");
        } else { // create terminal state of leaving, server needn't bother:
            ////Systemprintln("\t\t!!! customer leaves: ");
            changedCustomer = c.fromArrivesToLeaves();
            //Systemprintln("Customer: from arrives to  leaves");
            //Systemprintln("Server: nothing");
        }
        return changedCustomer;
    }


    private void terminateRests(double now) {
        for (int i = 0; i < this.numHumanServers; i++) {
            updateServerArray(this.myServers[i].stopResting(now));
        }
    }


    // todo: check this logic when fixing the servers...
    private Server[] queryServers(Customer c) {
        // gather relevant servers:
        Optional<? extends Server> idleServer = Optional.empty(),
            queueableServer = Optional.empty(),
            shortestServer = Optional.empty();
        boolean foundIdle = false, foundQueueable = false;
        for (Server s : this.myServers) {
            double now = c.getPresentTime();
            if (!(s instanceof SelfServer)) {
                s = s.stopResting(now);// todo: check if this is unnecessary
                if (!foundIdle && s.isIdle(now)) {
                    foundIdle = true;
                    idleServer = Optional.of(s);
                }
                if (!foundIdle && !foundQueueable && s.canQueue(now)) {
                    foundQueueable = true;
                    queueableServer = Optional.of(s);
                    shortestServer = Optional.of(s); // to init first
                }
                if (!foundIdle && foundQueueable && s.canQueue(now)) { // try looking for shortest
                    if (s.getQueueSize() < queueableServer.get().getQueueSize()) {
                        shortestServer = Optional.of(s);
                    }
                }
            } else { // settle selfServers:
                SelfServer selfServer = (SelfServer) s;
                if (!foundIdle && selfServer.isIdle(now)) {
                    foundIdle = true;
                    idleServer = Optional.of(selfServer);
                }
                if (!foundIdle && !foundQueueable && selfServer.canQueue(now)) {
                    foundQueueable = true;
                    queueableServer = Optional.of(s);
                }
                if (!foundIdle && foundQueueable && selfServer.canQueue(now)) {
                    if (selfServer.getQueueSize() < queueableServer.get().getQueueSize()) {
                        shortestServer = Optional.of(selfServer);
                    }
                }
            }
        }
        return new Server[]{idleServer.orElse(null),
            queueableServer.orElse(null),
            shortestServer.orElse(null)};
        // # todo: question: it said that generic array creation is not allowed why ah? **important**
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
    private void initServers(int numServers, int numSelfServers, int qmax) {
        // create array of servers and then assign to the servers field:
        Server[] servers = new Server[numServers + numSelfServers]; // the servers array is equal to the size of both
        for (int i = 0; i < numServers; i++) {
            Server s = new Server(i + 1, qmax);
            servers[i] = s;
        }
        // now allocate selfservers:
        for (int j = 0; j < numSelfServers; j++) {
            SelfServer s = new SelfServer(numServers + j + 1, qmax);
            servers[numServers + j] = s;
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
        String log = c.toString();
        if (!isArrivesState(c) && !isLeavesState(c)) {
            log += "" + this.myServers[c.serverID - 1];
        }
        this.logs.add(log);

    }

    /**
     * Takes in an updated server and updates the array accordingly.
     *
     * @param updatedServer the server to be put in.
     */
    private void updateServerArray(Server updatedServer) {
        if (!(updatedServer instanceof SelfServer)) {
            this.myServers[updatedServer.serverID - 1] = updatedServer;
        } else {
            this.myServers[updatedServer.serverID - 1] = (SelfServer) updatedServer;
        }
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