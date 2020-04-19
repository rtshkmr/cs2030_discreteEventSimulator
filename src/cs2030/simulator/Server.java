package cs2030.simulator;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A server is an employee of the Manager. Server shall tell the Manager if he/she's
 * idle, and if so, if he/she has anyone being served next. Also, a server
 * will change its state when various actions happen with respect to the actions
 * they do on the customers.
 */
public class Server {
    // fields:
    static double defaultAvailabilityTime = 0;

    protected int serverID;
    private boolean isIdle;
    private boolean hasCustomerWaiting;
    protected double nextAvailableTime;
    protected int qmax;
    protected Queue<Customer> waitingQueue;
    // todo: consider encapsulating fixed queue logic into a class. Also might have
//           to store queues inside Manager to avoid cyclic dependency, but this
//           gonna be a quick fix if that problem arises.


    //public constructors for now:

    /**
     * Usual constructor for a server.
     *
     * @param serverID the unique ID that a server shall have.
     */
    // normal instantiation of server:
    public Server(int serverID, int qmax) {
        this.serverID = serverID;
        this.qmax = qmax;
        this.isIdle = true;
        this.hasCustomerWaiting = false;
        this.nextAvailableTime = defaultAvailabilityTime;
        this.waitingQueue = new LinkedList<>();
    }

    /**
     * Constructs a new instance of Server whenever there's a change in state for the
     * server.
     *
     * @param serverID           the unique ID for the server
     * @param isIdle             whether the server is idle
     * @param hasCustomerWaiting whether any customer is waiting to be served by the Server
     * @param nextAvailableTime  when the server is free next.
     */
    // change of state for Server: serves or makes customer wait
    public Server(int serverID,
                  boolean isIdle,
                  boolean hasCustomerWaiting,
                  double nextAvailableTime,
                  Queue<Customer> waitingQueue) {
        this.serverID = serverID;
        this.isIdle = isIdle;
        this.hasCustomerWaiting = hasCustomerWaiting;
        this.nextAvailableTime = nextAvailableTime;
        this.waitingQueue = waitingQueue;
    }


    /**
     * Server tells if a potential customer can be served immediately by him/her.
     *
     * @param potentialCustomer a customer that might potentially be served immediately.
     * @return true if Server can serve the potentialCustomer immediately
     */
    // check if server can serve a customer immediately or next:
    public boolean canServeImmediately(Customer potentialCustomer) {

//        return this.waitingQueue.isEmpty();
            boolean ans = this.isIdle
                              && this.waitingQueue.isEmpty()
                              && potentialCustomer.getPresentTime() >= this.nextAvailableTime;
            System.out.println(this);
        System.out.println("canServeImmediately( "+ potentialCustomer + ") ? " +  ans);
        return ans;

//        return this.isIdle && potentialCustomer.getTiming() >= this.nextAvailableTime;
    }

    /**
     * Server tells if a potential customer can be served next by him/her if the customer waits.
     *
     * @param potentialCustomer a customer that might potentially be served next if he/she waits.
     * @return true if Server can serve the potentialCustomer next if customer waits.
     */
    public boolean canServeNext(Customer potentialCustomer) {
//        return !this.isIdle && !this.hasCustomerWaiting;
        boolean ans =  /*!this.isIdle*/!canServeImmediately(potentialCustomer) && this.waitingQueue.size() < this.qmax;
        // todo: a customer can wait for a server if the server's queue is not full
        System.out.println(this);
        System.out.println("canServeNext( "+ potentialCustomer + ") ? " +  ans);
        return ans;
    }

    /**
     * Server serves the customer now when asked by the Manager.
     *
     * @param decidedCustomer the Customer currently being referred to.
     * @return a new instance of Server with an updated state.
     */
    public Server serveCustomerNow(Customer decidedCustomer) {

        Server s = new Server(this.serverID,
            false, false,
            this.nextAvailableTime,
            this.waitingQueue);
        System.out.println("\t\t### changing server[serveCustomerNow]: from " + this + "to " + s);
        return s;
    }
    // todo: when a customer is being served now, they WONT be added to the queue

    /**
     * Server is done serving customer.
     *
     * @return a new instance of Server with an updated state.
     */
    public Server doneServingCustomer() {
        Server s =  new Server(this.serverID,
            !this.waitingQueue.isEmpty(), // server won't be idle if there are ppl in the queue
            this.waitingQueue.size() > 1,
            // todo: check if need to update the next available time.
            this.nextAvailableTime,
            this.waitingQueue);
        System.out.println("\t\t### changing server[doneServingCustomer]: from " + this + "to " + s);
        return s;
    }

    /**
     * If Customer is willing to wait, the Server lets the customer wait.
     *
     * @param waitingCustomer a customer that's waiting.
     * @return a new instance of Server with an updated state.
     */
    // todo: note that nextAvailableTime shall only be updated once a customer is being served
    //       case1: when customer served immediately
    //       case2: when can finally serveCustomer that's waiting (?)
    public Server letCustomerWait(Customer waitingCustomer) {
        assert !this.isIdle;
        assert this.nextAvailableTime == waitingCustomer.getNextTime();
        Queue<Customer> newQueue = new LinkedList<>(this.waitingQueue);
        newQueue.add(waitingCustomer);
       Server s =  new Server(this.serverID,
            false, true,
            waitingCustomer.getNextTime(), newQueue);
        System.out.println("\t\t### changing server [letCustomerWait()]: from " + this + "to " + s);
        return s;
    }
    // next available after the waiting customer has been served

    /**
     * Server serves a waiting customer finally, once the previous Customer has been served.
     *
     * @param doneCustomer a customer that's waiting.
     * @return a new instance of Server with an updated state.
     */
    public Server finallyServeCustomer(Customer doneCustomer) {
//        assert (this.hasCustomerWaiting);
        // see whether need to remove from the queue (if doneCustomer had been
        // waiting) or no need to remove.
        Customer headCustomer = this.waitingQueue.peek();
        // #error possible?
        if (doneCustomer.equals(headCustomer)) {
            System.out.println("this customer used to wait before");
        } else {
            System.out.println("this customer immediately got served");
        }


        if(doneCustomer.equals(headCustomer)) {
            Queue<Customer> newQueue = new LinkedList<>(this.waitingQueue);
            newQueue.remove();
            Server s =  new Server(this.serverID,
                               false,
                                !newQueue.isEmpty(),
                                doneCustomer.getPresentTime(),
                                newQueue);
            System.out.println("\t\t### changing server[finallyServeCustomer() [waitingguy]]: from " + this + "to " + s);
            return s;
        }
        Server s =  new Server(this.serverID,
                    true, !this.waitingQueue.isEmpty(),
                     doneCustomer.getNextTime(),
                      this.waitingQueue);
        System.out.println("\t\t### changing server[finallyServeCustomer() [straight guy]]: from " + this + "to " + s);
        return s;
        //        Customer headCustomer = newQueue.remove();
//        assert doneCustomer.getTiming() == headCustomer.getNextTiming();
//        return new Server(this.serverID, false,
//            this.nextAvailableTime >= doneWaitingCustomer.getTiming(),
//            this.nextAvailableTime);

//        return new Server(this.serverID,
//            false, newQueue.isEmpty(),
//            doneCustomer.getNextTiming(), // completion time is when server is next avail.
//            newQueue);
    }

    @Override
    public String toString() {
        return "Server [serverID " + this.serverID
                   + "| isIdle? " + this.isIdle
                   + "| hasCustomerWaiting? " + this.hasCustomerWaiting
                   + "| waitingQueueSize " + this.waitingQueue.size()
                   + "| nextAvailableTime " + this.nextAvailableTime + "]";
    }
}
