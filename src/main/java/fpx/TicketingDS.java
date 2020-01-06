package fpx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 其中，routenum是车次总数（缺省为5个）， coachnum是列车 的车厢数目（缺省为8个）， seatnum是每节车厢的座位数（缺省为100个），
 * stationnum 是每个车次经停站的数量（缺省为10个，含始发站和终点站）， threadnum 是并发购票的线程数（缺省为16个）。
 *
 * 车票涉及的各项参数均从1开始计数，例如车厢从1到8编号，车站从1到10编号等
 */
public class TicketingDS implements TicketingSystem {

  private ReentrantLock[] reentrantLock;
  private ArrayList<ArrayList<ArrayList<TreeSet<Integer>>>> tickets;

  // private int coachnum;
  private int routenum;
  private int seatnum;
  private int stationnum;
  // private int threadnum;
  // private int totalSeatNum;

  private AtomicLongArray tidArray;

  private ArrayList<HashMap<Long, Ticket>> bought;

  boolean equal(Ticket x, Ticket y){
    if(x.tid != y.tid) return false;
    if(x.passenger.compareTo(y.passenger) != 0) return false;
    if(x.route != y.route) return false;
    if(x.coach != y.coach) return false;
    if(x.seat != y.seat) return false;
    if(x.departure != y.departure) return false;
    if(x.arrival != y.arrival) return false;
    return true;
  }

  public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
    this.routenum = routenum;
    this.stationnum = stationnum;
    this.seatnum = seatnum;

    bought = new ArrayList<HashMap<Long, Ticket>>();
    for (int i = 0; i <= routenum; i++) {
      bought.add(new HashMap<Long, Ticket>());
    }

    tidArray = new AtomicLongArray(routenum + 1);
    for (int i = 1; i <= routenum; i++) {
      tidArray.getAndAdd(i, i - 1);
    }

    reentrantLock = new ReentrantLock[routenum + 1];
    for (int i = 0; i < reentrantLock.length; i++) {
      reentrantLock[i] = new ReentrantLock();
    }

    TreeSet<Integer> s = new TreeSet<Integer>();
    for (int i = 0; i < seatnum * coachnum; i++)
      s.add(i);

    tickets = new ArrayList<ArrayList<ArrayList<TreeSet<Integer>>>>();
    for (int i = 0; i <= routenum; i++) {
      tickets.add(new ArrayList<ArrayList<TreeSet<Integer>>>());
      for (int j = 0; j <= stationnum; j++) {
        tickets.get(i).add(new ArrayList<TreeSet<Integer>>());
        for (int k = 0; k <= stationnum; k++) {
          @SuppressWarnings("unchecked")
          TreeSet<Integer> h = (TreeSet<Integer>) s.clone();
          tickets.get(i).get(j).add(h);
        }
      }
    }
  }

  public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
    ArrayList<ArrayList<TreeSet<Integer>>> T = tickets.get(route);
    HashMap<Long, Ticket> map = bought.get(route);

    reentrantLock[route].lock();
    if (T.get(departure).get(arrival).isEmpty()) {
      reentrantLock[route].unlock();
      return null;
    }

    int loc = T.get(departure).get(arrival).pollFirst();

    // 含有交集的都需要处理
    for (int i = 1; i <= departure; i++) {
      for (int j = departure + 1; j <= stationnum; j++) {
       T.get(i).get(j).remove(loc);
      }
    }

    for (int i = departure + 1; i < arrival; i++) {
      for (int j = i + 1; j <= stationnum; j++) {
      T.get(i).get(j).remove(loc);
      }
    }

    Ticket t = new Ticket();
    t.tid = tidArray.getAndAdd(route, routenum);
    t.passenger = passenger; // 其实只是引用到相同的字符串常量中间，并不是深度拷贝了。
    t.route = route;
    t.coach = loc / seatnum + 1;
    t.seat = loc % seatnum + 1;
    t.departure = departure;
    t.arrival = arrival;

    map.put(t.tid, t);

    reentrantLock[route].unlock();
    return t;
  }

  /**
   * inquriy是查询余票方法，即查询route车次从departure站到arrival站的余票数
   */
  public int inquiry(int route, int departure, int arrival) {
    return tickets.get(route).get(departure).get(arrival).size();
  }

  /**
   * refundTicket是退票方法，对有效的Ticket对象返回true，对错误或无效的Ticket对象返回false
   */
  public boolean refundTicket(Ticket ticket) {
    int route = ticket.route;
    ArrayList<ArrayList<TreeSet<Integer>>> T = tickets.get(route);
    HashMap<Long, Ticket> map = bought.get(route);
    int loc = (ticket.coach - 1) * seatnum + ticket.seat - 1; // 注意，构造票的时候也需要减去一

    boolean[] s = new boolean[stationnum + 1]; // i : 从i 到 i + 1 含有该位置的票据
    Arrays.fill(s, Boolean.FALSE);

    for (int i = ticket.departure; i < ticket.arrival; i++)
      s[i] = true;

    reentrantLock[route].lock();
    Ticket t = map.remove(ticket.tid);
    if(t == null || !equal(t, ticket)){
      reentrantLock[route].unlock();
      return false;
    }
    for (int i = 1; i < stationnum; i++)
      if (T.get(i).get(i + 1).contains(loc))
        s[i] = true;

    int i = 1;
    int j = 1;
    while (true) {
      while (i < stationnum && !s[i])
        i++; // 移动到含有车票的连续区间的开始位置
      if(i == stationnum) break;
      j = i;
      while (s[j])
        j++; // 现在从i 到 j 都是持有该位置的车票的

      for (int m = i; m < j; m++) {
        for (int n = m + 1; n <= j; n++) {
          T.get(m).get(n).add(loc);
        }
      }

      i = j;
    }
    reentrantLock[route].unlock();
    return true;
  }
}
