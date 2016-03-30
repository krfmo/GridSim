package test06;

import java.util.Calendar;
import java.util.LinkedList;

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
 * 描述：本例展示了一个网格用户如何提交它的任务到多个网格资源实体
 */

/**
 * Test6类
 */
public class Test6 extends GridSim{

	private Integer ID_;
	private String name_;
	private GridletList list_;
	private GridletList receiveList_;
	private int totalResource_;

	/**
	 * 分配一个新的Test6对象
	 * @param name	该对象的实体名字
	 * @param baudRate	通信速度
	 * @param totalResource	可获得的网格资源总数
	 * @throws Exception	若在初始化GridSim包之前创建实体或实体名为空则抛异常
	 */
	public Test6(String name, double baudRate, int totalResource) throws Exception {
		super(name, baudRate);
		this.name_=name;
		this.totalResource_=totalResource;
		this.receiveList_=new GridletList();

		//为实体获取一个ID
		this.ID_ =new Integer(getEntityId(name));
		System.out.println("创建一个名为"+name+"，id="+this.ID_ +"的网格用户实体");

		//为该网格用户创建一个网格任务列表
		this.list_=createGridlet(this.ID_ .intValue());
		System.out.println(name+":正在创建"+this.list_.size()+"个网格任务");
	}

	/**
	 * 处理GridSim实体间通信的核心方法
	 */
	public void body(){
		int resourceID[]=new int[this.totalResource_];
		double resourceCost[]=new double[this.totalResource_];
		String resourceName[]=new String[this.totalResource_];

		LinkedList resList;
		ResourceCharacteristics resChar;

		/*
		 * 等待获取资源列表。GridSim包采用多线程环境，所以你的请求可能比网格资源注册
		 * 到GIS实体到达的早。因此，必须先等待。
		 */
		while(true){
			//需要暂停一下等待网格资源结束向GIS的注册
			super.gridSimHold(1.0);//持续1s

			resList=super.getGridResourceList();//GridSim里的方法
			if(resList.size()==this.totalResource_){
				break;
			}else{
				System.out.println(this.name_+"：正在等待获取资源列表...");
			}
		}

		//一个循环，来得到所有可用资源
		int i=0;
		for(i=0; i<this.totalResource_; i++){
			//资源列表包含的是资源ID而不是资源对象
			resourceID[i]=((Integer)resList.get(i)).intValue();

			//向资源实体发送其属性的请求
			super.send(resourceID[i], GridSimTags.SCHEDULE_NOW,
					GridSimTags.RESOURCE_CHARACTERISTICS, this.ID_);

			//等待获取一个资源属性
			resChar=(ResourceCharacteristics) super.receiveEventObject();
			resourceName[i]=resChar.getResourceName();
			resourceCost[i]=resChar.getCostPerSec();

			System.out.println(this.name_+"：已从名为"+resourceName[i]+"，id="+resourceID[i]+"的资源接收到资源属性");

			//将事件记录在"stat.txt"文件
			super.recordStatistics("\"从"+resourceName[i]+"接收到资源属性\"", "");
		}

		Gridlet gridlet;
		String info;

		//一次获取一个网格任务，并将其发送至随机的网格资源实体，然后等待回复
		int id=0;
		for(i=0; i<this.list_.size(); i++){
			gridlet=(Gridlet)this.list_.get(i);
			info="网格任务_"+gridlet.getGridletID();

			//例5里面用的是Random对象来随机产生下标值
			id=GridSimRandom.intSample(this.totalResource_);
			System.out.println(this.name_+"：正在发送"+info+"到名为"+resourceName[id]+"，id="+resourceID[id]+"的资源");

			//发送一个网格任务到特定的"resourceID"网格资源
			super.gridletSubmit(gridlet, resourceID[id]);

			//出于统计目的，将事件记录在"stat.txt"文件
			super.recordStatistics("\"提交"+info+"到名为"+resourceName[id]+"的资源\"", "");

			//等待从资源实体收到一个网格任务
			gridlet=super.gridletReceive();
			System.out.println(this.name_+"：正在接收网格任务"+gridlet.getGridletID());

			//出于统计目的将事件记录在"stat.txt"文件
			super.recordStatistics("\"接收到从"+resourceName[id]+"发来的"+info+"\"", gridlet.getProcessingCost());

			//将收到的网格任务存储到一个新的网格任务列表对象
			this.receiveList_.add(gridlet);
		}

		//关闭实体
		super.shutdownGridStatisticsEntity();
		super.shutdownUserEntity();
		super.terminateIOEntities();
		System.out.println(this.name_+":%%%%退出body()");
	}


	/**
	 * 得到网格任务列表
	 * @return	一个网格任务列表
	 */
	public GridletList getGridletList(){
		return this.receiveList_;
	}

	/**
	 * 创建网格任务
	 * @param userID	该任务所属的用户实体ID
	 * @return	一个网格任务列表对象
	 */
	private GridletList createGridlet(int userID){
		//创建一个容器来存储网格任务
		GridletList list=new GridletList();

		//手动创建3个任务
		int id=0;
		double length=3500.0;
		long file_size=300;
		long output_size=300;
		Gridlet gridlet1=new Gridlet(id, length, file_size, output_size);
		id++;
		Gridlet gridlet2=new Gridlet(id, 5000, 500, 500);
		id++;
		Gridlet gridlet3=new Gridlet(id, 9000, 900, 900);

		//设置网格任务的主人
		gridlet1.setUserID(userID);
		gridlet2.setUserID(userID);
		gridlet3.setUserID(userID);

		//存储任务到集合
		list.add(gridlet1);
		list.add(gridlet2);
		list.add(gridlet3);

		//借助GridSimRandom和GridSimStandardPE类创建5个任务

		//设置PE的MIPS率
		GridSimStandardPE.setRating(100);

		//创建随机个数的网格任务，最多5个
		int max=5;
		int count=GridSimRandom.intSample(max);//这句是随机生成任务的个数
		for(int i=1; i<count+1; i++){
			//网格任务长度取决于随机值和PE当前MIPS率
			length=GridSimStandardPE.toMIs(GridSimRandom.doubleSample()*50);

			//网格任务文件大小在范围内变化	100+（10% 到40%）
			file_size=(long)GridSimRandom.real(100, 0.10, 0.40, 
					GridSimRandom.doubleSample());

			//网格任务输出大小在范围内变化	100+（10%到50%）
			output_size=(long)GridSimRandom.real(250, 0.10, 0.50, 
					GridSimRandom.doubleSample());

			//创建一个网格任务对象
			Gridlet gridlet=new Gridlet(id+i, length, file_size, output_size);

			gridlet.setUserID(userID);

			//添加任务到集合
			list.add(gridlet);
		}

		return list;
	}

	////////////////////////静态方法///////////////////////

	/**
	 * 创建主方法
	 */
	public static void main(String[] args) {
		System.out.println("开始Test6");

		try {
			//第一步：初始化GridSim包。在创建任何实体之前调用。
			//没初始化之前本例不能运行，会抛run-time异常
			int num_user=3;//网格用户数
			Calendar calendar=Calendar.getInstance();
			boolean trace_flag=false;

			String[] exclude_from_file={""};
			String[] exclude_from_processing={""};

			String report_name=null;

			//初始化GridSim包。
			GridSim.init(num_user, calendar, trace_flag, exclude_from_file, 
					exclude_from_processing, report_name);

			//第二步：创建一个或多个网格资源对象
			GridResource resource0=createGridResource("Resource_0");
			GridResource resource1=createGridResource("Resource_1");
			GridResource resource2=createGridResource("Resource_2");

			int total_resource=3;

			//第三步：创建网格用户
			Test6 user0=new Test6("User_0", 560.00, total_resource);
			Test6 user1=new Test6("User_1", 250.00, total_resource);
			Test6 user2=new Test6("User_2", 150.00, total_resource);

			//第四步：开始模拟
			GridSim.startGridSimulation();

			//最后一步：仿真结束打印网格任务
			GridletList newList=null;
			newList=user0.getGridletList();
			printGridletList(newList, "User_0");

			newList=user1.getGridletList();
			printGridletList(newList, "User_1");

			newList=user2.getGridletList();
			printGridletList(newList, "User_2");

			System.out.println("结束Test6");

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("出错啦！");
		}
	}

	/**
	 * 创建一个网格资源。
	 * @param name	一个网格资源名
	 * @return	一个网格资源对象
	 */
	private static GridResource createGridResource(String name){
		//1.创建一个机器列表对象来存储一台或多台机器
		MachineList mList=new MachineList();

		//2.创建机器
		int mipsRating=377;
		mList.add(new Machine(0, 4, mipsRating));//第一台机器

		//3.若需创建更多机器则重复步骤2
		mList.add(new Machine(1, 4, mipsRating));//第二台机器
		mList.add(new Machine(2, 2, mipsRating));//第三台机器

		//4.创建一个资源属性对象，来存储网格资源属性
		String arch="Sun Ultra";
		String os="Solaris";
		double time_zone=9.0;
		double cost=3.0;

		ResourceCharacteristics resConfig=new ResourceCharacteristics(
				arch, os, mList, ResourceCharacteristics.TIME_SHARED,
				time_zone, cost);

		//5.最后，创建网格资源对象
		double baud_rate=100.0;
		long seed=11L*13*17*19*23+1;
		double peakLoad=0.0;
		double offPeakLoad=0.0;
		double holidayLoad=0.0;

		LinkedList<Integer> Weekends=new LinkedList<>();
		Weekends.add(new Integer(Calendar.SATURDAY));
		Weekends.add(new Integer(Calendar.SUNDAY));

		LinkedList<Integer> Holidays=new LinkedList<>();
		GridResource gridRes=null;
		try {
			gridRes=new GridResource(name, baud_rate, seed,
					resConfig, peakLoad, offPeakLoad, holidayLoad, Weekends,
					Holidays);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("创建一个名为"+name+"的网格资源");
		return gridRes;
	}

	/**
	 * 打印网格任务对象
	 * @param list	网格任务列表
	 * @param name	用户名
	 */
	private static void printGridletList(GridletList list, String name){
		int size=list.size();
		Gridlet gridlet;

		String indent="	";
		System.out.println();
		System.out.println("=========="+name+"的输出==========");
		System.out.println("网格任务ID"+indent+"状态"+indent+"资源ID"+indent+"开销");

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











