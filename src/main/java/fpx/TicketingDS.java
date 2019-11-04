package fpx;

public class TicketingDS implements TicketingSystem {

  public TicketingDS(int a, int b, int c, int d, int e){
    System.out.println("TODO");
  }
  
  
  public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
    return new Ticket();
  }

  public int inquiry(int route, int departure, int arrival) {
    // TODO
    return 0;
  }

  public boolean refundTicket(Ticket ticket) {
    // TODO
    return false;
  }
}
