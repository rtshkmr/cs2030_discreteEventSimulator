package cs2030.simulator;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A server is an employee of the Manager. Server shall tell the Manager if he/she's
 * idle, and if so, if he/she has a filled up waitingQueue. Also, a server
 * will change its state when various actions happen with respect to the actions
 * they do on the customers. A normal server is a human server.
 */
public class Server {

    protected final int serverID;
    protected final boolean isIdle;
    protected final double nextAvailableTime;
    protected final int qmax;
    protected final Queue<Customer> waitingQueue;
    protected final boolean isResting;

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
        this.isResting = false;
    }

    /**
     * Constructs changed Server State.
     *
     * @param serverID          the unique ID for the server
     * @param isIdle            whether the server is idle
     * @param isResting         whether the server is resting
     * @param nextAvailableTime when the server is free next.
     */
    protected Server(int serverID, int qmax, boolean isIdle, boolean isResting, double nextAvailableTime,
                     Queue<Customer> waitingQueue) {
        this.serverID = serverID;
        this.qmax = qmax;
        this.isIdle = isIdle;
        this.isResting = isResting;
        this.nextAvailableTime = nextAvailableTime;
        this.waitingQueue = waitingQueue;
    }


    /**
     * Server reports if he/she is idle, hence can serve a customer immediately.
     *
     * @param arrivalTime the time the customer arrives
     * @return true if Server can serve the potentialCustomer immediately
     */
    protected boolean isIdle(double arrivalTime) {
        return !this.isResting && this.isIdle && arrivalTime >= this.nextAvailableTime;
    }

    /**
     * Server reports it's possible to queue if queue isn't full.
     *
     * @param arrivalTime the time the customer arrives
     * @return true if Server can serve the potentialCustomer next if customer waits.
     */
    protected boolean canQueue(double arrivalTime) {
        return arrivalTime < this.nextAvailableTime
                   && this.waitingQueue.size() < this.qmax;
    }

    /**
     * Server serves customer upon arrival and state will change to not idle.
     * Only the idle attribute shall be modified (switched to false) by this method.
     *
     * @return a new instance of Server with an updated idle state.
     */
    protected Server serveUponArrival() {
        assert this.waitingQueue.isEmpty();
        return new Server(this.serverID, this.qmax, false, false,
            this.nextAvailableTime,
            this.waitingQueue);
    }

    /**
     * Server adds the customer to his queue, queue is updated!
     * Only the queue attribute will be modified by this.
     *
     * @param newQueue new queue to replace the old one.
     * @return a new instance of Server with an updated queue.
     */
    protected Server addToWaitQueue(Queue<Customer> newQueue) {
        assert !this.isIdle;
        return new Server(this.serverID, this.qmax, this.isIdle, false,
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
     * @param presentTime time when the customer will be done.
     * @return a new instance of Server with an updated state.
     */
    protected Server actuallyServeCustomer(double presentTime) {
        assert !this.isIdle;
        if (this.waitingQueue.isEmpty()) { // means the doneCustomer wasn't waiting
            return new Server(this.serverID, this.qmax, false, false,
                presentTime, this.waitingQueue);
        } else { // doneCustomer was waiting, we modify the queue as well
            Queue<Customer> newQueue = new LinkedList<>(this.waitingQueue);
            newQueue.remove();
            return new Server(this.serverID, this.qmax,
                false, false, presentTime, newQueue);
        }
    }

    /**
     * Serve has done serving a customer is idle now.
     *
     * @return idle Server.
     */
    protected Server doneServing() {
        return new Server(this.serverID,
            this.qmax,
            true,
            false,
            this.nextAvailableTime,
            this.waitingQueue);
    }

    /**
     * Human server starts resting until a given timing.
     *
     * @param restUntil the timing to stop resting.
     * @return the resting human server.
     */
    protected Server startResting(double restUntil) {
        return new Server(this.serverID,
            this.qmax,
            true, true,
            restUntil,
            this.waitingQueue);
    }

    /**
     * Human server stops resting if now is beyond the allocated rest time.
     * @param now the time now.
     * @return Server that might potentially stop resting.
     */
    protected Server stopResting(double now) {
        if (now >= this.nextAvailableTime) {
            return new Server(this.serverID, this.qmax, this.isIdle, false,
                this.nextAvailableTime, this.waitingQueue);
        } else return this;
    }


    protected int getQueueSize() {
        return this.waitingQueue.size();
    }

    @Override
    public String toString() {
        return "server " + this.serverID;
    }
}
