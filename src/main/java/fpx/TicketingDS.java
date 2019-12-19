package fpx;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 降低粒度 上锁的操作开销 总体操作开销
 *
 * 关键问题: 插入和删除
 *
 * 需要何种机制 ? 和设计模式
 *
 * - 使用内置的结构体 还是 自己定义，一共含有那些并发的发数据结构？
 *
 * 方案A: 使用hashmap, key 是数值范围，value 是剩余的数量，粒度太大了 !
 * 
 * 既然所有的线程数量可以确定，然后数量是均匀的，那么拆分出来多个资源池来 ? - 均匀的散列 直接捕获线程池 ? 划分为public 和 private
 * 部分 ? private 部分不上锁，只有自己访问 - routenum 必定单独设置
 *
 * 仅当发现目标组件 时，才锁定组件，然后确认该组件在被找到和被锁定期间没有发生任何变化。
 * 如果确定没有发生变化，则查找成功，返回正确的结果，否则此次查找失败， 只能重新查找。 -
 *
 * Java 编程接口 :
 * - volatile 如果实现全局消息通知，为什么需要使用synchronize配合使用 ?
 * - https://stackoverflow.com/questions/3519664/difference-between-volatile-and-synchronized-in-java
 *
 * Controlled Concurrent Initialization
 *
 * 问题 : 如果购票总是返回失败 ?　总是最优化的方式 ? holy shit
 * 乐观方法 ? 手写实现？ segment tree ? 复杂的结构 ? 范围逐渐减小 ?
 *
 * 上三角矩阵 :
 * - 命中，很方便。should be the best one !
 * - 线程池吗 ? 没有必要 ReadWriteLock 
 * - 向上查询的过程 ? slowly ! 利用一个求和的树 ?
 * - 添加一个统计行 ? 每一个数值表示恰好在
 *
 *
 * 加快查询操作 ?
 * 分配策略 ? segment tree 数组实现 ?
 *
 * tid 顺序并且唯一 ? wait free 的东西 ?
 *
 * 1. read 速度应该更快
 * 2. write 立刻返回，更新操作推迟进行，设置最高的推迟数量，当当前的数值在危险的数值的时候，进行保守的操作。可以利用latch操作，可以测试一下其中的效果。
 *
 *
 * query : 读不添加锁，连readLock 都不添加，使用reentrant lock 维持生活，read 直接统计访问 !
 * 不同的route 使用数组
 *
 * @全体成员 修改一下。 性能测试负载为 20个车次，每个车次15节车厢，每节车厢100个座位，途经10个车站。共有96个线程并发执行，各个线程执行100000次操作，其中查询、购票和退票操作的比例是80 : 15 : 5。
 *
 * @category fuck me
 * @version 0.1
 */
public class TicketingDS implements TicketingSystem {

  /**
   * 其中，routenum是车次总数（缺省为5个）， coachnum是列车 的车厢数目（缺省为8个）， seatnum是每节车厢的座位数（缺省为100个），
   * stationnum 是每个车次经停站的数量（缺省为10个，含始发站和终点站）， threadnum 是并发购票的线程数（缺省为16个）。
   *
   * 车票涉及的各项参数均从1开始计数，例如车厢从1到8编号，车站从1到10编号等
   */

	private ReentrantLock[] reentrantLock;
  private int tickets[][][];
  public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
    // 利用threadnum 如何处理?
    // possitive 的执行
    // 前面的一千次操作必定没有问题，导致可以将多次操作合并 !
    reentrantLock = new ReentrantLock[routenum];
    for (int i = 0; i < reentrantLock.length; i++) {
      reentrantLock[i] = new ReentrantLock();
    }
    tickets = new int[routenum + 1][stationnum + 1][stationnum + 1];
  }

  public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
    // 更新操作可以返回 ? 但是如果上面含有数值需要传递下来 ? 不能确定数目啊 ?
    // 只要数值读写即可 ? 除非上方无锁，才可以 ? 其实是向上和向下传递的，

    reentrantLock[route].lock();
    if(tickets[route][departure][arrival] == 0){
      reentrantLock[route].unlock();;
      return null;
    }
    
    reentrantLock[route].unlock();;
    return null;
  }

  /**
   * inquriy是查询余票方法，即查询route车次从departure站到arrival站的余票数
   *
   */
  public int inquiry(int route, int departure, int arrival) {
    return tickets[route][departure][arrival];
  }

  /**
   * refundTicket是退票方法，对有效的Ticket对象返回true，对错误或无效的Ticket对象返回false
   */
  public boolean refundTicket(Ticket ticket) {
    // TODO similar problem
    return false;
  }
}
