package cs2030.simulator;

/**
 * A Customer enters the shop, and if possible, gets served immediately else waits if
 * there isn't anyone else waiting else Customer leaves.
 */
public class Customer implements Comparable<Customer> {

    /*                             stats:                                   */
    static int CustomersEnteredCounter = 0; // class-level counter, used for setting IDs
    static int CustomersServed = 0;
    static int CustomersLeft = 0;
    static double TotalWaitingTime = 0;
    static int TotalWaitCounter = 0;
    protected static final int NO_SERVER = 0;


    // instance attributes:
    private final int myID;
    private final double presentTime;
    private final double nextTime;
    private final String customerStatus; // lowercase string
    protected int serverID; // NO_SERVER if unassigned
    protected boolean firstWaits = true;
    private final double entryTime;

    /**
     * Constructs a Customer when the Customer enters.
     *
     * @param myID        each customer has a unique customer ID
     * @param presentTime refers to the timing when the status "arrives" is assigned
     *                    to the customer
     */
    private Customer(int myID, double presentTime) {
        this.myID = myID;
        this.presentTime = presentTime;
        this.nextTime = presentTime; // initially set as the same upon arival
        this.customerStatus = "arrives";
        this.serverID = NO_SERVER;
        this.entryTime = presentTime;
    }

    /**
     * Constructs a customer whenever there's a state change.
     *
     * @param myID               each customer has a unique customer ID.
     * @param updatedPresentTime when the current state change has happened.
     * @param updatedNextTime    when the next state change will happen.
     * @param newStatus          the newly assigned status of the customer.
     * @param serverID           the Server assigned to this customer.
     */
    private Customer(int myID, double updatedPresentTime, double updatedNextTime,
                     String newStatus, int serverID, double entryTime) {
        this.myID = myID;
        this.presentTime = updatedPresentTime;
        this.nextTime = updatedNextTime;
        this.customerStatus = newStatus;
        this.serverID = serverID;
        this.entryTime = entryTime;
    }


    /**
     * Generates a customer when the someone enters.
     * Customer's status is "arrives".
     * Side effect:
     * 1. the CustomersEnteredCounter is incremented.
     *
     * @param arrivalTime when the customer entered the establishment
     * @return Customer the newly arrived customer
     */
    protected static Customer enter(double arrivalTime) {
        return new Customer(++CustomersEnteredCounter, arrivalTime);
    }

    /**
     * Gives the Customer Statistics when called.
     *
     * @return String representation of the class level statistics being tallied
     */
    protected static String customerStats() {
        double averageWaitingTime =
                (CustomersServed == 0 || TotalWaitingTime == 0)
                        ? 0 : Customer.TotalWaitingTime / Customer.CustomersServed;
        return "[" + prettyPrint(averageWaitingTime) + " "
                + CustomersServed + " " + (CustomersEnteredCounter - CustomersServed) + "]";
    }




    /*======================  STATE CHANGES: ==================================*/

    /*                        from arrival state                               */

    /**
     * Customer served upon arrival if there's an idle Server.
     * State change:  ARRIVES to SERVED
     * Since it's a change from arrival state, a server needs to be assigned.
     *
     * @param serverID the server that's assigned to the new Customer.
     * @return Customer Customer that gets served.
     */
    protected Customer fromArrivesToServed(int serverID) {
        // ARRIVES to SERVED (i.e served immediately)
        return new Customer(this.myID, this.presentTime, this.presentTime,
                "served", serverID, this.entryTime);
    }

    /**
     * Customer waits if there are no idle Servers and a Server exists whose
     * queue is not full.
     * State change: ARRIVES to WAITS
     * Since it's a change from arrival state, a server needs to be assigned.
     * Side effects:
     * 1. since waiting duration will be known (nextTime - presentTime)
     * then we can add this to the TotalWaitingTime statistic.
     * 2. we increment the TotalWaitCounter.
     *
     * @param nextAvailableTime the next time for a queueableServer.
     * @return Customer Customer that waits.
     */
    protected Customer fromArrivesToWaits(double nextAvailableTime, int serverID) {
        Customer.TotalWaitCounter++;
        return new Customer(this.myID, this.presentTime, nextAvailableTime,
                "waits", serverID, this.entryTime);
    }

    /**
     *  Customer needs to wait if there's someone else still in the queue
     * @param nextAvailableTime need to try waiting until next time
     * @return Customer with modified present and next times
     */
    public Customer fromWaitsToWaits(double nextAvailableTime) {
        assert (this.customerStatus.equals("waits"));
        Customer res = new Customer(this.myID, nextAvailableTime,
                nextAvailableTime, "waits", this.serverID, this.entryTime);
//        if (firstWaits) {
        res.firstWaits = false;
//        }
        return res;
    }

    /**
     * Customer is done waiting and is ready to be served.
     * State change: WAITS to SERVED.
     * The presentTime shall be when the Serving happens i.e. when done waiting.
     * The nextTime shall be sort of a stub, since it's not gonna be used
     *
     * @return Customer Customer that is done waiting and will be served next.
     */
    protected Customer fromWaitsToServed(double nextAvailableTime) {
        assert (this.customerStatus.equals("waits"));
        Customer.TotalWaitingTime += (nextAvailableTime - this.entryTime);
        // will def be served if there's no one else waiting:
        return new Customer(this.myID,
                nextAvailableTime,
                nextAvailableTime,
                "served",
                this.serverID, this.entryTime);
    }

    /*                        to terminal  state                               */

    /**
     * Customer is actually being served and has a known completion time and will be done.
     * State change: SERVED to DONE
     * A done customer's present time is when he's done.
     * A done customer, being a terminal state will have the same next time as present.
     * Side effect:
     * 1. The CustomersServed counter is incremented.
     *
     * @param completionTime when the customer will be done, as informed by the Manager.
     * @return Customer Customer that is done.
     */
    protected Customer fromServedToDone(double completionTime) {
        ++CustomersServed;
        return new Customer(this.myID, completionTime, completionTime, "done", this.serverID, this.entryTime);
    }

    /**
     * Customer decides to leave if no Server is idle and not possible to queue.
     * State change: ARRIVES to LEAVES
     * Side Effect:
     * 1. The CustomersLeft counter is incremented.
     *
     * @return Customer Customer that leaves.
     */
    protected Customer fromArrivesToLeaves() {
        Customer.CustomersLeft++;
        return new Customer(this.myID, this.presentTime, this.presentTime,
                "leaves", NO_SERVER, this.entryTime);
    }

    //==========================================================================


    @Override
    public String toString() {
        String status = this.customerStatus;
        String res = prettyPrint(this.presentTime) + " " + this.myID + " " + status;
        switch (status) {
            case "served":
                res += " by server " + this.serverID;
                break;
            case "done":
                res += " serving by server " + this.serverID;
                break;
            case "waits":
                res += " to be served by server " + this.serverID;
                break;
            default:
                res += "";
        }
        return res;

    }

    /**
     * Determines the ordering of the Customers within the Priority Queue.
     * Ordered by their respective timings, and if tie-broken by their IDs.
     *
     * @param otherCustomer another Customer to be compared to be ordered
     *                      within the priority queue of customers.
     * @return int priority score.
     */
    @Override
    public int compareTo(Customer otherCustomer) {
        if (this == otherCustomer || this.presentTime == otherCustomer.getPresentTime()) {
            // tiebreaker: compare via id number:
            return Integer.compare(this.myID, otherCustomer.getID());
        } else return Double.compare(this.presentTime, otherCustomer.presentTime);
//        else if (this.presentTime < otherCustomer.getPresentTime()) {
//            return -1;
//        } else {
//            return 1;
//        }
    }

    /**
     * Equality comparison between two Customers, equal if IDs are the same.
     *
     * @param obj another customer to check equality with.
     * @return true if the other object is equal to this object.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Customer) {
            Customer c = (Customer) obj;
            return this.myID == c.myID; // equality comparison by IDs
        }
        return false;
    }

    //================= HELPERS AND UTILS: =============================
    /*                          GETTERS:                                 */
    protected int getID() {
        return this.myID;
    }

    public double getPresentTime() {
        return this.presentTime;
    }

    public double getNextTime() {
        return this.nextTime;
    }

    public String getCustomerStatus() {
        return this.customerStatus;
    }

    /**
     * Pretty prints any double value to 3 decimal places.
     *
     * @param x a double to be made pretty
     * @return String representation of the double, to 3 decimal places.
     */
    public static String prettyPrint(double x) {
        return String.format("%.3f", x);
    }
    //=============================================================
}
