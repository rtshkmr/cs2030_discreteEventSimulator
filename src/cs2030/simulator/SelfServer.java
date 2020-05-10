package cs2030.simulator;

import java.util.LinkedList;
import java.util.Queue;

public class SelfServer extends Server {

    protected static Queue<Customer> sharedQueue;
    private static boolean doneInit = false; // flags the init for the queue

    /**
     * Usual constructor for a server.
     *
     * @param serverID int representation for the self-server.
     * @param qmax     the max number of customers that may queue in the shared queue.
     */
    public SelfServer(int serverID, int qmax) {
        super(serverID, qmax);
        if (!doneInit) {
            SelfServer.sharedQueue = new LinkedList<>();
            SelfServer.doneInit = true;
        }
    }

    /**
     * Constructs changed Server State.
     *
     * @param serverID          the unique ID for the server
     * @param isIdle            whether the server is idle
     * @param isResting         whether the server is resting
     * @param nextAvailableTime when the server is free next.
     */
    private SelfServer(int serverID, int qmax, boolean isIdle,
                       boolean isResting, double nextAvailableTime,
                       Queue<Customer> waitingQueue) {
        super(serverID, qmax, isIdle, isResting, nextAvailableTime, waitingQueue);
    }

    /**
     * A factory method to help update a SelfServer's state easily.
     *
     * @param isIdle            whether the selfServer is idle or not.
     * @param nextAvailableTime when the selfServer is free next.
     * @return updated self server.
     */
    protected SelfServer updateSelfServer(boolean isIdle, double nextAvailableTime) {
        return new SelfServer(this.serverID,
            this.qmax, isIdle, false, nextAvailableTime, this.waitingQueue);
    }

    /**
     * SelfServer reports if idle, hence can serve a customer immediately.
     *
     * @param arrivalTime the time the customer arrives
     * @return true if Server can serve the potentialCustomer immediately
     */
    @Override
    protected boolean isIdle(double arrivalTime) {
        return this.isIdle && arrivalTime >= this.nextAvailableTime;
    }

    /**
     * SelfServer reports it's possible to queue if the shared queue isn't full.
     *
     * @param arrivalTime the time the customer arrives
     * @return true if Server can serve the potentialCustomer next if customer waits.
     */
    @Override
    protected boolean canQueue(double arrivalTime) {
        return SelfServer.sharedQueue.size() < this.qmax;
    }

    /**
     * SelfServer serves customer upon arrival and state will change to not idle.
     * Only the idle attribute shall be modified (switched to false) by this method.
     * This happens only when the sharedqueue is empty
     *
     * @return a new instance of Server with an updated idle state.
     */
    @Override
    protected SelfServer serveUponArrival() {
        assert SelfServer.sharedQueue.isEmpty();
        return updateSelfServer(false, this.nextAvailableTime);
    }

    /**
     * SelfServer adds the customer to the sharedQueue, queue is updated!
     * Only the sharedQueue class-level attribute will be modified by this.
     *
     * @param newQueue new queue to replace the old sharedQueue.
     * @return a new instance of Server with an updated queue.
     */
    @Override
    protected SelfServer addToWaitQueue(Queue<Customer> newQueue) {
        SelfServer.sharedQueue = newQueue; // update sharedQueue to the newQueue
        return this;
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
    @Override
    protected SelfServer actuallyServeCustomer(double presentTime) {
        assert !this.isIdle;
        // if customer was waiting in the sharedqueue:
        if (!SelfServer.sharedQueue.isEmpty()) {
            SelfServer.sharedQueue.remove();
        }
        return updateSelfServer(false, presentTime);
    }

    @Override
    protected SelfServer doneServing() {
        return updateSelfServer(true, this.nextAvailableTime);
    }

    // resting features not available since it's a self-checkout server!

    @Override
    protected int getQueueSize() {
        return SelfServer.sharedQueue.size();
    }

    @Override
    public String toString() {
        return "self-check " + this.serverID;
    }
}
