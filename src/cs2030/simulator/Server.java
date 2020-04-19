package cs2030.simulator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A server is an employee of the Manager. Server shall tell the Manager if he/she's
 * idle, and if so, if he/she has a filled up waitingQueue. Also, a server
 * will change its state when various actions happen with respect to the actions
 * they do on the customers.
 */
public class Server {

    protected final int serverID;
    private final boolean isIdle;
    protected final double nextAvailableTime;
    protected final int qmax;
    protected final Queue<Customer> waitingQueue; // to restrict size to qmax
    // todo: might have to store queues inside manager to avoid cyclic dependency?

    /**
     * Usual constructor for a server, with empty queue.
     *
     * @param serverID the unique ID that a server shall have.
     */
    protected Server(int serverID, int qmax) {
        this.serverID = serverID;
        this.qmax = qmax;
        this.isIdle = true;
        this.nextAvailableTime = 0; // start availability will be at 0
        this.waitingQueue = new LinkedList<>();
    }

    /**
     * Constructs changed Server State.
     *
     * @param serverID          the unique ID for the server
     * @param isIdle            whether the server is idle
     * @param nextAvailableTime when the server is free next.
     */
    protected Server(int serverID, int qmax, boolean isIdle, double nextAvailableTime,
                     Queue<Customer> waitingQueue) {
        this.serverID = serverID;
        this.qmax = qmax;
        this.isIdle = isIdle;
        this.nextAvailableTime = nextAvailableTime;
        this.waitingQueue = waitingQueue;
    }


    /**
     * Server reports if he/she is idle, hence can serve a customer immediately.
     *
     * @return true if Server can serve the potentialCustomer immediately
     * @param c time compared to is a customer's time
     */
    // todo: this feels wrong, can't wrap my head around it.
    protected boolean isIdle(Customer c) {
        boolean ans =  this.isIdle && c.getPresentTime() >= this.nextAvailableTime;
        System.out.println("\t\tisIdle?" + ans);
        return ans;
    }

    /**
     * Server reports it's possible to queue if queue isn't full.
     *
     * @return true if Server can serve the potentialCustomer next if customer waits.
     * @param c timing provided by the customer's presense.
     */
    protected boolean canQueue(Customer c) {
        boolean ans = c.getPresentTime() < this.nextAvailableTime
                        && this.waitingQueue.size() < this.qmax;
        System.out.println("\t\tcanQueue?" + ans);
          return ans;
//        return !this.isIdle(c) && this.waitingQueue.size() < qmax;
////        return !this.isIdle && !this.hasCustomerWaiting;
//        boolean ans =  /*!this.isIdle*/!isIdle(potentialCustomer) && this.waitingQueue.size() < this.qmax;
//        // todo: a customer can wait for a server if the server's queue is not full
//        System.out.println(this);
//        System.out.println("canServeNext( " + potentialCustomer + ") ? " + ans);
//        return ans;
    }

    /**
     * Server serves customer upon arrival and state will change to not idle.
     * Only the idle attribute shall be modified (switched to false) by this method.
     *
     * @return a new instance of Server with an updated idle state.
     */
    public Server serveUponArrival() {
        assert this.waitingQueue.isEmpty();
        return new Server(this.serverID, this.qmax,false,
            this.nextAvailableTime,
            this.waitingQueue);
    }

    /**
     * Server adds the customer to his queue, queue is updated!
     * Only the queue attribute will be modified by this.
     *
     * @param waitingCustomer a customer that's waiting.
     * @return a new instance of Server with an updated queue.
     */
    // todo: note that nextAvailableTime shall only be updated once a customer is being served
    //       ie. in the finallyServe method.
    //       case1: when customer served immediately
    //       case2: when can finally serveCustomer that's waiting (?)
    public Server addToWaitQueue(Customer waitingCustomer) {
        assert !this.isIdle;
        assert this.nextAvailableTime == waitingCustomer.getNextTime();
        Queue<Customer> newQueue = new LinkedList<>(this.waitingQueue);
        newQueue.add(waitingCustomer);
        System.out.println("addToWaitQueue(), newQueueSize:" +newQueue.size());
        return new Server(this.serverID, this.qmax, this.isIdle,
            this.nextAvailableTime, newQueue);
    }


    /**
     * Actually serves a customer.
     * Changes the nextAvailableTime, which is the nextTime for Customer, as
     * determined by the Manager.
     * Might change queue:
     * 1. if doneCustomer was initially waiting in queue, then free up queue space
     * 2. else no change to the queue
     *
     * @param doneCustomer a customer that's waiting.
     * @return a new instance of Server with an updated state.
     */
    public Server actuallyServeCustomer(Customer doneCustomer) {
        assert !this.isIdle;
        if (this.waitingQueue.isEmpty()) { // means the doneCustomer wasn't waiting
            return new Server(this.serverID, this.qmax,true,
                doneCustomer.getPresentTime(), this.waitingQueue);
        } else { // doneCustomer was waiting, we modify the queue as well
            // todo: we compare idx first, if possible change to using the equals method.
            assert this.waitingQueue.peek().getID() == doneCustomer.getID();
            Queue<Customer> newQueue = new LinkedList<>(this.waitingQueue);
            newQueue.remove();
            System.out.println("actuallyServeCustomer(), newQueueSize:" +newQueue.size());
            return new Server(this.serverID,this.qmax, newQueue.isEmpty(),
                doneCustomer.getPresentTime(), newQueue);
        }
    }

    /**
     * Server is done serving customer.
     * Only idleness might change.
     *
     * @return a new instance of Server with an updated state.
     */
    public Server doneServingCustomer() {
        return new Server(this.serverID,this.qmax,
            !this.waitingQueue.isEmpty(), // server won't be idle if there are ppl in the queue
            this.nextAvailableTime,
            this.waitingQueue);
    }


    @Override
    public String toString() {
        return "Server [serverID " + this.serverID
                   + "| qmax: " + this.qmax
                   + "| isIdle? " + this.isIdle
                   + "| waitingQueueSize " + this.waitingQueue.size()
                   + "| nextAvailableTime " + this.nextAvailableTime + "]";
    }
}
