package test04;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Random;

import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.GridSimRandom;
import gridsim.GridSimStandardPE;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCharacteristics;

/*
 * 描述：本例展示了一个网格用户是如何向一个网格资源实体提交网格任务的。
 */

/**
 * Test4类创建网格任务并且将他们发送至一个网格资源实体。
 */
class Test4 extends GridSim{

	private Integer ID_;
	private String name_;
	private GridletList list_;
	private GridletList receiveList_;

	/**
	 * 分配一个Test4对象
	 * @param name	该对象的实体名
	 * @param baudRate	通信速度
	 * @throws Exception	初始化之前创建实体或实体名为空时，抛异常
	 */
	Test4(String name, double baudRate) throws Exception {
		super(name, baudRate);
		this.name_=name;
		this.receiveList_=new GridletList();

		//为实体指定ID
		this.ID_=new Integer(getEntityId(name));
		System.out.println("创建一个名为"+name+"的网格用户实体，id="+this.ID_);

		//为网格用户创建一个网格任务列表
		this.list_=createGridlet(this.ID_.intValue());
		System.out.println("正在创建"+this.list_.size()+"个网格任务");
	}

	/**
	 * 处理GridSim实体间通信的核心方法
	 */
	public void body() {
		int resourceID=0;
		String resourceName;
		LinkedList resList;
		ResourceCharacteristics resChar;

		/*
		 * 等待得到资源列表。
		 * 由于GridSim包使用多线程环境，你的请求可能会比一个或多个网格资源实体将其注册到网格信息服务（GIS）实体到达的早。
		 * 因此，首先最好处于等待状态
		 */
		while(true){
			//需要暂停一下，来等待网格资源完成向GIS的注册
			super.gridSimHold(1.0);//等待1秒
			resList=super.getGridResourceList();
			if(resList.size()>0){
				//在本例中，我们知道仅需要创建一个资源。资源列表保存的是资源Id的列表，而不是资源对象
				Integer num=(Integer) resList.get(0);
				resourceID=num.intValue();

				//向资源实体请求发送他的属性
				super.send(resourceID, GridSimTags.SCHEDULE_NOW,
						GridSimTags.RESOURCE_CHARACTERISTICS, this.ID_);

				//等待得到一个网格资源属性
				resChar=(ResourceCharacteristics) super.receiveEventObject();
				resourceName=resChar.getResourceName();

				System.out.println("收到资源名为"+resourceName+"的资源属性,id="+resourceID);

				//将事件记录到"stat.txt"文件
				super.recordStatistics("\"Received ResourceCharacteristics " +
                        "from " + resourceName + "\"", "");

				break;
			}
			else
				System.out.println("等待获取资源列表...");
		}

		Gridlet gridlet;
		String info;

		//一次获取一个网格任务的循环，并将其发送到一个网格资源实体。然后等待响应
		for(int i=0; i<this.list_.size(); i++){
			gridlet=(Gridlet)this.list_.get(i);
			info="网格任务_"+gridlet.getGridletID();

			System.out.println("发送"+info+"到资源"+resourceName+"，资源ID是"+resourceID);

			//将一个网格任务发送至具有指定资源ID的网格资源
			super.gridletSubmit(gridlet, resourceID);

			//另一种发送网格任务到网格实体的方法
			//super.send(resourceID, GridSimTags.SCHEDULE_NOW, GridSimTags.GRIDLET_SUBMIT, gridlet);

			//出于统计目的将事件记录到"stat.txt"
			super.recordStatistics("\"Submit " + info + " to " + resourceName +
                    "\"", "");

			//等待从资源实体返回的网格任务
			gridlet=super.gridletReceive();
			System.out.println("正在接收网格任务"+gridlet.getGridletID());

			//出于统计目的将事件记录到"GridSim_stat.txt"文件
			super.recordStatistics("\"Received " + info +  " from " +
                    resourceName + "\"", gridlet.getProcessingCost());

			//将收到的网格任务存储在新的网格任务列表对象中
			this.receiveList_.add(gridlet);
		}

		/*
		 * 关掉所有实体，包括GridStatistics实体，因为我们用它存储特定事件来着。。。
		 */
		super.shutdownGridStatisticsEntity();
		super.shutdownUserEntity();
		super.terminateIOEntities();
	}

	/**
	 * 得到网格任务列表
	 * @return	网格任务列表
	 */
	public GridletList getGridletList(){
		return this.receiveList_;
	}

	/**
	 * 创建网格任务
	 * @param userID	拥有这些网格任务的用户实体ID
	 * @return	一个网格任务列表对象
	 */
	private GridletList createGridlet(int userID){
		//创建一个容器盛放网格任务
		GridletList list=new GridletList();

		//手动创建3个网格任务
		int id=0;
		double length=3500.0;
		long file_size=300;
		long output_size=300;
		Gridlet gridlet1=new Gridlet(id, length, file_size, output_size);
		id++;
		Gridlet gridlet2=new Gridlet(id, 5000, 500, 500);
		id++;
		Gridlet gridlet3=new Gridlet(id, 9000, 900, 900);

		//设置这些任务的拥有者
		gridlet1.setUserID(userID);
		gridlet2.setUserID(userID);
		gridlet3.setUserID(userID);

		//存储到列表
		list.add(gridlet1);
		list.add(gridlet2);
		list.add(gridlet3);

		//用方法创建5个网格任务
		long seed=11L*13*17*19*23+1;
		Random random=new Random(seed);

		//设置处理器的MIPS Rating
		GridSimStandardPE.setRating(100);

		//创建5个网格任务
		int count=5;
		for(int i=1; i<count+1; i++){
			//任务长度由随机值和当前PE处理能力（MIPS Rating）决定
			length=GridSimStandardPE.toMIs(random.nextDouble()*50);

			//规定了任务文件的长度的变化范围是：100 + (10% to 40%)
			file_size=(long) GridSimRandom.real(100, 0.10, 0.40, random.nextDouble());

			//规定了任务输出长度的变化范围是：250 + (10% to 50%)
			output_size=(long) GridSimRandom.real(250, 0.10, 0.50, random.nextDouble());

			//创建一个新的网格任务对象
			Gridlet gridlet=new Gridlet(id+i, length, file_size, output_size);

			gridlet.setUserID(userID);

			//添加网格任务到集合
			list.add(gridlet);
		}

		return list;
	}

	//////////////////静态方法//////////////////

	/**
	 * 创建主方法
	 */
	public static void main(String[] args) {
		System.out.println("开始Test4");

		try {
			/*第一步：初始化GridSim包。应该在创建任何实体之前调用该方法，
			 * 我们不能在没有初始化GridSim之前使用网格资源。这样会导致run-time异常*/

			/*
			 * 妈蛋啊！！！找了好几天的错！！！竟然是用户数错了！！！
			 * 果然不能全靠复制粘贴！！！
			 * 细心点会死吗？？？
			 * 不细心真的会死的！！！
			 * 因为个0和1，我人生观都快变了啊喂！！！
			 * 祈祷再也不会出这样的错！！！
			 */
			int num_user=1;//用户数需要被创建，本例中，因为不需要创建用户实体，所以将值设为0
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

			//第二步：创建一个网格资源对象
			String name="Resource_0";
			GridResource resource=createGridResource(name);

			//第三步：创建一个Test4对象
			Test4 obj=new Test4("Test4", 560.00);

			//第四步：开始模拟
			GridSim.startGridSimulation();

			//最后一步：模拟结束时打印网格任务
			GridletList newList=obj.getGridletList();
			printGridletList(newList);

			System.out.println("Test4结束");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("出错啦！");
		}
	}

	/**
	 * 创建一个网格资源，一个网格资源包含一个或多个机器（Machine）。
	 * 类似的，一个机器包含一个或多个PE（处理单元或CPU）。
	 * 在本例中，我们模拟的网格资源含有三个机器，每个机器包含一个或多个PE。
	 * @param name	一个网格资源名称
	 * @return	一个网格资源对象
	 */
	private static GridResource createGridResource(String name){
		System.out.println();
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

		ResourceCharacteristics resConfig=new ResourceCharacteristics(
				arch, os, mList, ResourceCharacteristics.TIME_SHARED, time_zone, cost);
		System.out.println("创建了网格资源属性对象，并存储了机器列表");

		//5.最终，我们需要创建一个网格对象
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
		System.out.println();

		return gridRes;
	}

	/**
	 * 打印网格任务对象
	 * @param list	网格任务列表
	 */
	private static void printGridletList(GridletList list){
		int size=list.size();
		Gridlet gridlet;

		String indent="	";
		System.out.println();
		System.out.println("==========输出==========");
		System.out.println("任务ID"+indent+"状态"+indent+"资源ID"+indent+"开销");

		for(int i=0; i<size; i++){
			gridlet=(Gridlet)list.get(i);
			System.out.print(gridlet.getGridletID()+indent);

			if(gridlet.getGridletStatus()==Gridlet.SUCCESS){
				System.out.print("成功");
			}

			System.out.println(indent+gridlet.getResourceID()+indent+gridlet.getProcessingCost());
		}
	}
}


















