/open Main.java
/open Customer.java
/open Server.java
/open Event.java
/open Manager.java
PriorityQueue<Customer> mainQueue = new PriorityQueue<>();
Customer myC1 = Customer.enter(0.500);
Customer myC2 = Customer.enter(0.600);
Customer myC3 = Customer.enter(0.700);
Customer myC4 = Customer.enter(1.500);
Customer myC5 = Customer.enter(1.600);
Customer myC6 = Customer.enter(1.700);
mainQueue.add(myC1);
mainQueue.add(myC2);
mainQueue.add(myC3);
mainQueue.add(myC4);
mainQueue.add(myC5);
mainQueue.add(myC6);
Manager myManager = new Manager(mainQueue, 1);
myManager.operate();
