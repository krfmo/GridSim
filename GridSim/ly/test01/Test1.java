package test01;

import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCharacteristics;

import java.util.Calendar;
import java.util.LinkedList;

/**对照example1的练习
 * 创建一个含有三个machine的网格资源
 * 在创建任何GridSim实体前，记得调用GridSim.Init()*/
public class Test1 {
	/**主方法*/
	public static void main(String[] args) {
		System.out.println("创建一个网格资源");
		
		try {
			/*第一步：初始化GridSim包。应该在创建任何实体之前调用该方法，
			 * 我们不能在没有初始化GridSim之前使用网格资源。这样会导致run-time异常*/
			
			int num_user=0;//用户数需要被创建，本例中，因为不需要创建用户实体，所以将值设为0
			Calendar calendar=Calendar.getInstance();//内部计时器，用来记录模拟的开始和结束时间
			boolean trace_flag=true;//一个调试开关，值为真表示需要跟踪记录GridSim模拟的每一步
			
			//list of files or processing names to be excluded from any statistical measures
			//在统计过程中，不包含在内的文件名称和处理过程
			String[] exclude_from_file={""};
			String[] exclude_from_processing={""};
			
			String report_name=null;//报告名称，本例不需要写报告，可以参照其他用到ReportWriter类的例子
			
			//初始化GridSim包
			System.out.println("初始化GridSim包");
			GridSim.init(num_user, calendar, trace_flag, exclude_from_file,
					exclude_from_processing, report_name);
			
			/*GridSim3.0以后，可以使用另外一种初始化方式，不需要任何统计功能
			 * 代码如下：
			 * GridSim.init(num_user, calendar, trace_flag);*/
			
			/*第二步：创建一个网格资源*/
			GridResource gridResource=createGridResource();//
			System.out.println("事例1结束~");
			
			//NOTE:我们不需要调用GridSim.startGridSimulation()
			//因为没有用户实体向这个资源传递任务
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println("出错了！");
		}
	}
	
	/**
	 * 创建一个网格资源，一个网格资源包含一个或多个机器（Machine）。
	 * 类似的，一个机器包含一个或多个PE（处理单元或CPU）。
	 * 在本例中，我们模拟的网格资源含有三个机器，每个机器包含一个或多个PE。*/
	private static GridResource createGridResource(){
		System.out.println("开始创建带有3个machine的网格资源...");
		
		//以下是创建网格资源步骤：
		/*1.创建一个机器列表，用于存储一个或多个机器*/
		MachineList mList=new MachineList();
		System.out.println("创建一个机器列表");
		
		/*2.创建一个机器，参数分别是：它的id，PE个数，MIPS rating（处理器的计算能力）
		 * 本例中，所使用资源的信息为：hpc420.hpcc.jp, AIST, Tokyo, Japan
		 * NOTE：these data are taken the from GridSim paper, page 25.
		 * 本例中，所有PE都有相同的MIPS(Millions Instruction Per Second) Rating
		 * 即，每个PE处理能力相同
         * */
		int mipsRating=377;
		mList.add(new Machine(0, 4, mipsRating));//第一台机器
		System.out.println("创建第一台机器，处理器个数为4，已将其加入机器列表！");
		
		/*3.如果想要创建更多机器，重复第二步。
		 * 在本例中，日本的AIST有3个机器，每个机器的MIPS率相同，PE个数不同
		 * NOTE：如过你只想要为每一个网格资源创建一个机器，那么可以省略这步
		 * */
		mList.add(new Machine(1, 4, mipsRating));//第二台机器
		System.out.println("创建第二台机器，处理器个数为4，已将其加入机器列表！");
		
		mList.add(new Machine(2, 2, mipsRating));//第三台机器
		System.out.println("创建第三台机器，处理器个数为2，已将其加入机器列表！");
		
		/*4.创建一个资源特性对象，用来存储网格资源特性：
		 * 系统体系结构、操作系统、机器列表、分配策略：时间/空间共享、时区和代价（G$/PE time unit）
		 * */
		String arch="Sun Ultra";//系统体系结构
		String os="Solaris";//操作系统
		double time_zone=9.0;//资源所属时区
		double cost=3.0;//使用该资源的代价
		
		ResourceCharacteristics resConfig=new ResourceCharacteristics(arch, os, mList, ResourceCharacteristics.TIME_SHARED, time_zone, cost);
		System.out.println();
		System.out.println("创建了网格资源属性对象，并存储了机器列表");
		
		//5.最终，我们需要创建一个网格对象
		String name="Resource_0";//资源名称
		double baud_rate=100.0;//通信速度，传输能力
		long seed=11L*13*17*19*23+1;//???
		double peakLoad=0.0;//高峰时段资源负荷
		double offPeakLoad=0.0;//非高峰时段资源负荷
		double holidayLoad=0.0;//假期时段资源负荷
		
		//相当于无周末，网格资源一周工作7天
		LinkedList<Integer> Weekends=new LinkedList<>();
		Weekends.add(new Integer(Calendar.SATURDAY));
		Weekends.add(new Integer(Calendar.SUNDAY));
		
		//无假日。然而，本例中并没有设置假期
		LinkedList<Integer> Holidays=new LinkedList<>();
		
		GridResource gridRes=null;
		try {
			gridRes=new GridResource(name, baud_rate, seed,
					resConfig, peakLoad, offPeakLoad, holidayLoad, Weekends, Holidays);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		System.out.println("最后，创建一个网格资源，存储了该网格资源的属性");
		
		return gridRes;
	}
}





































