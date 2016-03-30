package test07;

import java.util.Calendar;
import java.util.LinkedList;

import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCharacteristics;

/**
 * 这是本里的主程序，用来说明如何向不同的网格资源提交、取消、断点续传、暂停、移除任务
 * 你可以调整主方法中的少许参数来玩转该类，例如：totalUser、totalGridlet等
 */
public class MyTest {
	private static final int MIN=1;//测试事例最小值
	private static final int MAX=8;//测试事例最大值
	
	/**
	 * 那一大堆注释，等敲完所有case再加！
	 * 
	 * 这些测试事例提供的操作有：
	 * 事例1：提交网格任务-然后等待所有任务执行结束，手机测试结果
	 * 事例2：提交网格任务-取消其中一些网格任务-完成
	 * 事例3：提交网格任务-暂停其中一些网格任务-取消-完成
	 * 事例4：提交网格任务-暂停-恢复-取消-完成
	 * 事例5：提交网格任务-移动其中一些网格任务-完成
	 * 事例6：提交网格任务-暂停-移动-完成
	 * 事例7：提交网格任务-暂停-恢复-移动-完成
	 * 事例8：提交网格任务-暂停-恢复-移动-取消-完成
	 * 
	 * 提示：
	 * -事例1是最简单的，事例8是最复杂的
	 * -这些测试事例是非常灵活的，这意味着你可以通过增加或减少主方法中totalUser，totalPE等参数
	 *  来调整实验使之变得很庞大。而你不需要修改测试事例的任何一个类。
	 * -注意，不要将数字设置的太大（超过200），因为这可能会内存溢出。
	 * -如果是进行网格任务移植实验，你需要增加网格资源实体的数量，比如说超过6个资源实体
	 */
	public static void main(String[] args) {
		System.out.println("开始测试~");
		try {
			//解析命令行参数，第一个参数决定策略是时间共享还是空间共享
			int policy=0;
			if(args[0].equals("t")||args[0].equals("time")){
				policy=ResourceCharacteristics.TIME_SHARED;
			}else if(args[0].equals("s")||args[0].equals("space")){
				policy=ResourceCharacteristics.SPACE_SHARED;
			}else{
				System.out.println("错误：无效的分配策略");
				return;
			}
			
			//决定选择哪个测试事例
			int testNum=Integer.parseInt(args[1]);
			if(testNum<MIN||testNum>MAX){//这应该是，输入无效的数字都执行事例1吧
				testNum=MIN;
			}
			
            ////////////////////////////////////////
            // 第一步：初始化GridSim包。应该在创建任何实体之前调用该方法。
			// 没有初始化之前无法运行该案例。会得到run-time异常。
			Calendar calendar=Calendar.getInstance();
			boolean trace_flag=false;//true说明跟踪GridSim事件
			
			String[] exclude_from_file={""};
			String[] exclude_from_processing={""};
			
			String report_name=null;
			
			//初始化所有相关变量
			double baudRate[]={1000, 5000};//偶，奇不同的带宽
			int peRating[]={10, 50};//偶，奇不同的PE率
			double price[]={3.0, 5.0};//偶，奇不同的资源
			int gridletLength[]={1000, 2000, 3000, 4000, 5000};
			
			//初始化GridSim包
			int totalUser=2;//本次试验的总用户数
			GridSim.init(totalUser, calendar, trace_flag, exclude_from_file, exclude_from_processing, report_name);
			
            //////////////////////////////////////
            // 第二步：创建一个或多个网格资源对象
			int totalResource=3;//本次试验的网格资源总数
			int totalMachine=1;//每个资源的机器总数
			int totalPE=3;//每个机器的PE总数
			createResource(totalResource, totalMachine, totalPE, baudRate, peRating, price, policy);
			
            /////////////////////////////////////
            // 第三步：创建网格用户
			int totalGridlet=4;//每个用户的网格任务总数
			createUser(totalUser, totalGridlet, gridletLength, baudRate, testNum);
			
            ////////////////////////////////////
            // 第四步：开始模拟
			GridSim.startGridSimulation();
		} catch (Exception e) {
			System.out.println("出错啦！");
			System.out.println(e.getMessage());
			System.out.println("用法：java Test [time | space] [1-8]");
		}
		System.out.println("============测试类结束============");
	}
	
	/**
	 * 创建许多网格资源
	 * @param totalRes	网格资源总数，本例中总数为3
	 * @param totalMachine	每个资源的机器总数，本例是1
	 * @param totalPE	每个机器的PE总数，本例是3
	 * @param baudRate	带宽，奇偶不同，偶1000，奇5000
	 * @param peRating	PE率，奇偶不同，偶10，奇50
	 * @param price	应该是与最终cost开销有关的参数，应该是说使用每个资源的代价，奇偶不同，偶3.0，奇5.0
	 * @param policy	时间共享或空间共享
	 */
	public static void createResource(int totalRes, int totalMachine,
							int totalPE, double[] baudRate, int[] peRating,
							double[] price, int policy){
		double bandwidth=0;
		double cost=0.0;
		
		//循环创建一个或多个网格资源
		for(int i=0; i<totalRes; i++){
			String name="GridResource_"+i;
			if(i%2==0){
				bandwidth=baudRate[0];
				cost=price[0];
			}else{
				bandwidth=baudRate[1];
				cost=price[1];
			}
			
			//创建一个网格资源
			createGridResource(name, totalMachine, totalPE, bandwidth, peRating, policy, cost);
		}
	}
	
	/**
	 * 创建许多网格用户
	 * @param totalUser	本次实验的用户总数，本例是2
	 * @param totalGridlet	每个用户的网格资源总数，本例是4
	 * @param glLength	网格任务长度，一个int数组，{1000, 2000, 3000, 4000, 5000}
	 * @param baudRate	带宽，奇偶不同，偶1000，奇5000
	 * @param testNum	命令行输入的测试事例序号（1-8）
	 */
	public static void createUser(int totalUser, int totalGridlet,
							int[] glLength, double[] baudRate, int testNum){
		try {
			double bandwidth=0;
			double delay=0.0;
			
			for(int i=0; i<totalUser; i++){
				String name="用户_"+i;
				if(i%2==0){
					bandwidth=baudRate[0];
					delay=5.0;
				}else{
					bandwidth=baudRate[1];
				}
				
				//创建一个网格用户（即new一个测试事例）
				createTestCase(name, bandwidth, delay, totalGridlet, glLength, testNum);
			}
		} catch (Exception e) {
			//忽略...
		}
	}
	
	/**
	 * 不同测试事例的选择器
	 * （里边的代码应该等写好所有测试事例类再写）
	 */
	private static void createTestCase(String name, double bandwidth,
							double delay, int totalGridlet, int[] glLength,
							int testNum) throws Exception{
		switch(testNum){
			case 1:
				new TestCase1(name, bandwidth, delay, totalGridlet, glLength);
				break;
				
			case 2:
				new TestCase2(name, bandwidth, delay, totalGridlet, glLength);
				break;
				
			case 3:
				new TestCase3(name, bandwidth, delay, totalGridlet, glLength);
				break;
				
			case 4:
				new TestCase4(name, bandwidth, delay, totalGridlet, glLength);
				break;
				
			case 5:
				new TestCase5(name, bandwidth, delay, totalGridlet, glLength);
				break;
				
			case 6:
				new TestCase6(name, bandwidth, delay, totalGridlet, glLength);
				break;
				
			case 7:
				new TestCase7(name, bandwidth, delay, totalGridlet, glLength);
				break;
				
			case 8:
				new TestCase8(name, bandwidth, delay, totalGridlet, glLength);
				break;
				
			default:
				System.out.println("不可识别的测试事例！");
				break;
		}
	}
	
	/**
	 * 创建一个网格资源
	 */
	private static void createGridResource(String name, int totalMachine,
							int totalPE, double bandwidth, int[] peRating,
							int policy, double cost){
		MachineList mList=new MachineList();
		
		int rating=0;
		for(int i=0; i<totalMachine; i++){
			//机器的PE Rating也根据奇偶有所不同
			if(i%2==0){
				rating=peRating[0];
			}else{
				rating=peRating[1];
			}
			
			mList.add(new Machine(i, totalPE, rating));
		}
		
		String arch="Sun Ultra";
		String os="Solaris";
		double time_zone=0.0;
		
		ResourceCharacteristics resConfig=new ResourceCharacteristics(arch, os, mList, policy, time_zone, cost);
		
		long seed=11L*13*17*19*23+1;
		double peakLoad=0.0;
		double offPeakLoad=0.0;
		double holidayLoad=0.0;
		
		LinkedList<Integer> Weekends=new LinkedList<>();
		Weekends.add(new Integer(Calendar.SATURDAY));
		Weekends.add(new Integer(Calendar.SUNDAY));
		
		LinkedList<Integer> Holidays=new LinkedList<>();
		try {
			GridResource gridRes=new GridResource(name, bandwidth, seed, resConfig, peakLoad, offPeakLoad, holidayLoad, Weekends, Holidays);
		} catch (Exception e) {
			System.out.println("创建网格资源出错");
			System.out.println(e.getMessage());
		}
		
		System.out.println("创建一个名为"+name+"的网格资源");
		return;//这句有必要吗？？？
	}
}
















