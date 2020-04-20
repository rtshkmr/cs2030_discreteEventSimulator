package cs2030.simulator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * An Event is created whenever a state change occurs within the queue.
 * Events help keep track of time and have a log of the state of each Customer within
 * the queue at that particular time.
 */
public class Event implements Comparable<Event> {
    private boolean isArrivalLog = false;
    private final double eventTime;
    private Customer currentCustomer;
    private final Queue<String> eventLog; // might have to make this a priorityQueue
    private final PriorityQueue<Customer> remainingQueue;
    protected int serverID;

    /**
     * An Arrival Event is created with a queue of customers representing arrivals.
     * This constructor exists to handle the arrival logs of level 4.
     *
     * @param myQueue      the priority queue of arrived customers
     * @param isArrivalLog for level 4 of the project, whereby the arrivals have to be printed.
     */
    // to handle the Add Arrivals event:
    public Event(PriorityQueue<Customer> myQueue, boolean isArrivalLog) {
        assert myQueue.peek() != null;
        this.eventTime = myQueue.peek().getNextTime();
        this.remainingQueue = myQueue;
        this.eventLog = new LinkedList<>();
        this.isArrivalLog = isArrivalLog;
        generateEventLog();
    }

    /**
     * An event is created whenever there's a change of Customer's state.
     *
     * @param currentCustomer A customer's action/state change generates an
     *                        event and this is the customer currently looked at
     * @param remainingQueue  the rest of the priority queue of customers other than
     *                        the currently looked at customer
     */
    //usual constructor
    public Event(Customer currentCustomer, PriorityQueue<Customer> remainingQueue) {
        this.eventTime = currentCustomer.getPresentTime();
        this.currentCustomer = currentCustomer;
        this.remainingQueue = remainingQueue;
        this.eventLog = new LinkedList<>();
        this.serverID = currentCustomer.serverID;
        generateEventLog();
    }

    /**
     * A log of Customers is generated for a particular event.
     */
    // generate eventLog:
    public void generateEventLog() {
        if (this.isArrivalLog) {
            //this.eventLog.add("# Adding arrivals");
        } else {
            this.eventLog.add("" + currentCustomer);
        }
        for (Customer c : this.remainingQueue) {
            this.eventLog.add("" + c);
        }
    }

    /**
     * Determines the ordering of events within the PriorityQueue of Events.
     *
     * @param otherEvent another event that this event is compared to, to establish
     *                   an ordering based on their relative priorities
     * @return int the priority score of the comparison.
     */
    @Override // Events are ordered based on the specified event time
    public int compareTo(Event otherEvent) {
        if (this == otherEvent || this.eventTime == otherEvent.eventTime) {
            return Integer.compare(this.currentCustomer.getID(),
                otherEvent.currentCustomer.getID());
        } else if (this.eventTime < otherEvent.eventTime) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Displays the event "title" by peeking at the head of the event log.
     *
     * @return String representation of an event.
     */
    @Override
    public String toString() {
        //        StringBuilder res = new StringBuilder();
        //        for (String log : this.eventLog) {
        //            res.append(log).append("\n");
        //        }
        //        return res.toString();
        return this.eventLog.peek();
    }
}