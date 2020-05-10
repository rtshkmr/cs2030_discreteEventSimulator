package cs2030.simulator;

import java.util.LinkedList;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;


/**
 * A Manager holds Priority Queues of Customers and a queue of Strings
 * representing the log of events for the day. When Customers enter the shop,
 * the Manager figures out whether they are greedy, adds them to the mainQueue.
 * Next the Manager helps the Customer decide what to do based on the information
 * that the he/she can get from his/her employees, i.e. the servers.
 * Manager then records this event down as a log and sends the newly decided
 * Customer back to the main queue for further action.
 * RandomGenerator Documentation at:
 * https://www.comp.nus.edu.sg/~cs2030/RandomGenerator/cs2030/simulator/RandomGenerator.html
 */
public class Manager {
    private final PriorityQueue<Customer> mainQueue;
    private final Queue<String> logs;
    private Server[] myServers;
    private final double pRest;
    private final double pGreedy;
    private final RandomGenerator randomGenerator;
    private final int numHumanServers;


    /**
     * Constructor for Manager, only used once!
     *
     * @param seed             the seed for the random generator.
     * @param numServers       the number of human servers.
     * @param numSelfServers   the number of self-checkout servers.
     * @param qmax             the max number of customers that can queue here.
     * @param numArrivalEvents the number of customers that will be arriving.
     * @param lambda           arrival rate of customers.
     * @param mu               service rate of Servers.
     * @param rho              resting rate of Human servers.
     * @param pRest            probability for resting.
     * @param pGreedy          probability for it being a greedy customer.
     */
    public Manager(int seed, int numServers, int numSelfServers, int qmax, int numArrivalEvents,
                   double lambda, double mu, double rho, double pRest, double pGreedy) {
        this.mainQueue = new PriorityQueue<>();
        this.numHumanServers = numServers;
        this.logs = new LinkedList<>();
        this.pRest = pRest;
        this.pGreedy = pGreedy;
        this.randomGenerator = new RandomGenerator(seed, lambda, mu, rho);
        initServers(numServers, numSelfServers, qmax);
        initArrivals(numArrivalEvents);
    }

    /**
     * Manager pops customers from queue and helps them decide what to do.
     * If Customer has a terminal state, then generate relevant side-effects.
     * Customers are popped off the mainQueue definitely only if they have
     * a terminal state (i.e. done/leaves).
     * Else, Manager helps the customer decide and adds decided customer back to queue.
     */
    public void operate() {
        while (!this.mainQueue.isEmpty()) {
            Customer currentCustomer = mainQueue.poll();
            terminateRests(currentCustomer.getPresentTime());
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
                    serverHandlesDone(s, exitTime);
                }
            }
        }
    }

    /**
     * Once a Customer has been done, then Manager asks Server if he/she needs a rest
     * and modifies the Server accordingly.
     *
     * @param s        the Server that just finished Serving a customer.
     * @param exitTime the completion time.
     */
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
            updateServerArray(selfS);
        }
    }

    /**
     * Non-terminal Customer's state is changed based on what should be done next.
     * If customer has arrived, handleArrivalState.
     * Else customer either is Waiting or Served(currently being served) and
     * has an assigned server.
     * If the Customer had chosen to do self-service, then the assigned server
     * will be one of the Self-Servers and the Server that finally serves this
     * customer may not necessarily be the same as the one initially assigned.
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
            }
            if (isWaitsState(c)) {
                Server assignedServer = this.myServers[c.serverID - 1];
                if (!(assignedServer instanceof SelfServer)) { // if human server:
                    if (assignedServer.isResting
                            || !assignedServer.isIdle(c.getPresentTime())
                            || ((!c.equals(assignedServer.waitingQueue.peek())))) {
                        decided = c.fromWaitsToWaits(assignedServer.nextAvailableTime);
                    } else {
                        decided = c.fromWaitsToServed(assignedServer.nextAvailableTime);
                    }
                } else { // for selfServers:
                    double now = c.getPresentTime();
                    SelfServer nextBestServer = bestSelfServerQuery(now);
                    // reassign to the nextBestServer first:
                    c = c.reassignServer(nextBestServer.serverID);
                    if (nextBestServer.isIdle(now)) {
                        decided = c.fromWaitsToServed(nextBestServer.nextAvailableTime);
                    } else {
                        decided = c.fromWaitsToWaits(nextBestServer.nextAvailableTime);
                    }
                }
            }
            return decided;
        }
    }

    /**
     * Method is called when the customer is waiting in selfservice queue
     * and checks if any of the selfServers are idle at that time.
     * If none of the selfServers are idle,
     * then find the selfServer that will be free next.
     *
     * @param now the time it is now.
     * @return the nextBestSelfServer.
     */
    private SelfServer bestSelfServerQuery(double now) {
        for (int i = numHumanServers; i < this.myServers.length; i++) {
            SelfServer s = (SelfServer) this.myServers[i];
            if (s.isIdle(now)) {
                return s;
            }
        }
        //  selfServer  who's gonna be free the earliest:
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

    /**
     * Manager queries the Servers, and assigns the most appropriate one to the Customer.
     * Manager handles the arrival of customers depending on whether the customer
     * is greedy or not.
     *
     * @param c customer that has just arrived.
     * @return Customer that either gets served immediately, waits or leaves.
     */
    private Customer handleArrivalState(Customer c) {
        Server[] queriedServers = queryServers(c);
        Customer changedCustomer; // to be assigned based on query results
        if (queriedServers[0] != null) { // idleServer exists:
            Server s = queriedServers[0];
            changedCustomer = c.fromArrivesToServed(s.serverID);
            if (!(s instanceof SelfServer)) { // normal server:
                updateServerArray(s.serveUponArrival());
            } else { // it's a self server:
                SelfServer selfServer = (SelfServer) s;
                updateServerArray(selfServer.serveUponArrival());
            }
        } else if (queriedServers[1] != null) { // queueableServer exists, need to queue:
            Server queueableServer = queriedServers[1];
            Server shortestServer = queriedServers[2];
            // if customer greedy, shall take the shortest server that exists:
            changedCustomer = (c.isGreedy && shortestServer != null)
                                  ? c.fromArrivesToWaits(shortestServer.nextAvailableTime,
                shortestServer.serverID)
                                  : c.fromArrivesToWaits(queueableServer.nextAvailableTime,
                queueableServer.serverID);
            if (c.isGreedy) {
                queueableServer = shortestServer;
            }
            // this is all about adding customers to their assigned Servers' queues:
            if (!(queueableServer instanceof SelfServer)) {
                Queue<Customer> newQueue = new LinkedList<>(queueableServer.waitingQueue);
                newQueue.add(changedCustomer);
                updateServerArray(queueableServer.addToWaitQueue(newQueue));
            } else {
                Queue<Customer> newQueue = new LinkedList<>(SelfServer.sharedQueue);
                newQueue.add(changedCustomer);
                SelfServer selfServer = (SelfServer) queueableServer;
                updateServerArray(selfServer.addToWaitQueue(newQueue));
            }
        } else { // create terminal state of leaving, server needn't bother:
            changedCustomer = c.fromArrivesToLeaves();
        }
        return changedCustomer;
    }


    /**
     * Manager queries the Servers to find out whether any are idle or can be queued.
     * Also finds the Server with the shortest queue to aid the greedy customer.
     *
     * @param c an arriving customer.
     * @return an array of Servers: idle, queable and shortestqueable.
     */
    private Server[] queryServers(Customer c) {
        // gather relevant servers:
        Optional<? extends Server> idleServer = Optional.empty(),
            queueableServer = Optional.empty(),
            shortestServer = Optional.empty();
        boolean foundIdle = false, foundQueueable = false;
        for (Server s : this.myServers) {
            double now = c.getPresentTime();
            if (!(s instanceof SelfServer)) { //it's a human server:
                s = s.stopResting(now);
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
                    if (s.getQueueSize() < shortestServer.get().getQueueSize()) {
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
                    shortestServer = Optional.of(s);
                }
                if (!foundIdle && foundQueueable && selfServer.canQueue(now)) {
                    if (selfServer.getQueueSize() < shortestServer.get().getQueueSize()) {
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

    //=================  HELPERS METHODS: =============================

    /*-----------------   INITIALIZERS -------------------------------*/

    /**
     * Creates Servers, both human and self-service ones.
     *
     * @param numServers     the number of human servers.
     * @param numSelfServers the number of self checkout servers.
     * @param qmax           the maximum number of Customers that may queue.
     */
    private void initServers(int numServers, int numSelfServers, int qmax) {
        // create array of servers and then assign to the servers field:
        Server[] servers = new Server[numServers + numSelfServers];
        // the servers array is equal to the size of both
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

    /**
     * Initialises arrival timings and then the Customers that arrive.
     *
     * @param numArrivalEvents how many customers arrive in the day.
     */
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
            Customer myCustomer = generateCustomer(arrivalTime);
            this.mainQueue.add(myCustomer);
        }
    }

    /**
     * Customer, whether greedy or normal, is generated.
     *
     * @param arrivalTime the time the customer had arrived.
     * @return either normal or greedy customer.
     */
    private Customer generateCustomer(double arrivalTime) {
        double prob = this.randomGenerator.genCustomerType();
        if (prob < this.pGreedy) { // generate greedy customer:
            return Customer.enterGreedily(arrivalTime);
        } else {
            return Customer.enter(arrivalTime);
        }
    }

    /**
     * Manager checks to see if the human server that's just done needs to rest.
     *
     * @return true if server should rest.
     */
    private boolean serverNeedsRest() {
        return this.randomGenerator.genRandomRest() < this.pRest;
    }

    /**
     * Manager assigns when the human server may rest until.
     *
     * @param completionTime when the human server is done completing the job.
     * @return time that the human server can rest until.
     */
    private double assignRestTime(double completionTime) {
        double restDuration = this.randomGenerator.genRestPeriod();
        return restDuration + completionTime;
    }


    private double getNextArrivalTime(double now) {
        return now + this.randomGenerator.genInterArrivalTime();
    }

    private double getCompletionTime(double now) {
        return now + this.randomGenerator.genServiceTime();
    }

    /*----------------------------------------------------------*/

    /**
     * Creates a log entry of a customer that's worth logging.
     *
     * @param c a customer worth logging.
     */
    private void registerEvent(Customer c) {
        String log = c.toString();
        if (!isArrivesState(c) && !isLeavesState(c)) {
            log += "" + this.myServers[c.serverID - 1];
        }
        this.logs.add(log);
    }

    /**
     * Takes in an updated server and updates the array of servers accordingly.
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

    /**
     * Manager looks tells all the Human servers to stop resting now, if
     * it's beyond their resting timing as allocated by the Manager.
     *
     * @param now the time now.
     */
    private void terminateRests(double now) {
        for (int i = 0; i < this.numHumanServers; i++) {
            updateServerArray(this.myServers[i].stopResting(now));
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