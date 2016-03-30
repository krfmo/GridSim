package test05;

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

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Random;
/*
 * 描述：本例展示了一个网格用户如何提交网格任务到多个网格资源实体
 */

/**
 * Test5类生成网格任务，并将其发送至多个网格资源实体
 */
class Test5 extends GridSim{

	private Integer ID_;
	private String name_;
	private GridletList list_;
	private GridletList receiveList_;
	private int totalResource_;
	
	/**
	 * 分配一个新的Test5实体
	 * @param name	该对象的实体名
	 * @param baudRate	通信速度
	 * @param total_resource	可获得的网格资源数
	 * @throws Exception	在初始化GridSim包之前创建实体或实体名为空时抛异常
	 */
	public Test5(String name, double baudRate, int total_resource) throws Exception {
		super(name, baudRate);
		this.name_=name;
		this.totalResource_=total_resource;
		this.receiveList_=new GridletList();
		
		//给实体一个ID
		this.ID_=new Integer(getEntityId(name));
		System.out.println("正在创建一个名为"+name+"，id="+this.ID_+"的网格用户实体");
		
		//给该网格用户创建一个网格任务列表
		this.list_=createGridlet(this.ID_.intValue());
		System.out.println("正在创建"+this.list_.size()+"个网格任务");
	}
	
	/**
	 * 解决GridSim实体间通信的核心方法
	 */
	public void body() {
		int resourceID[]=new int[this.totalResource_];//总资源数就是该数组的长度
		double resourceCost[]=new double[this.totalResource_];
		String resourceName[]=new String[this.totalResource_];
		
		LinkedList resList;
		ResourceCharacteristics resChar;
		
		/*
		 * 等待获取资源列表。GridSim包使用多线程环境，你的请求到达时间可能比
		 * 一个或多个网格资源实体注册到网格信息服务（GIS）实体的时间早。因此，最好
		 * 先等待
		 */
		while(true){
			//需要等待网格资源结束向GIS的注册
			super.gridSimHold(1.0);//等待1秒
			
			resList=super.getGridResourceList();
			if(resList.size()==this.totalResource_) break;
			else System.out.println("正在等待获取资源列表...");
		}
		
		int i=0;
		
		//循环得到所有可获得的资源（空闲资源）
		for(i=0; i<this.totalResource_; i++){
			//资源列表包含的是资源ID，而不是网格资源对象
			resourceID[i]=((Integer)resList.get(i)).intValue();
			
			//向资源实体请求发送它的属性
			super.send(resourceID[i], GridSimTags.SCHEDULE_NOW, GridSimTags.RESOURCE_CHARACTERISTICS, this.ID_);
	
			//等待获取资源属性
			resChar=(ResourceCharacteristics)super.receiveEventObject();
			resourceName[i]=resChar.getResourceName();
			resourceCost[i]=resChar.getCostPerSec();
			
			System.out.println("已收到从"+resourceName[i]+"发来的资源属性，资源id="+resourceID[i]);
			
			//将这一事件记录在"stat.txt"文件中
			super.recordStatistics("\"Received ResourceCharacteristics " +
                    "from " + resourceName[i] + "\"", "");
		}
		
		Gridlet gridlet;
		String info;
		
		//一次获取一个网格任务的循环，并且将其随机发送到一个网格资源实体。然后等待响应
		Random random=new Random();//程序里只用这一个Random对象就够了
		int id=0;
		for(i=0; i<this.list_.size(); i++){
			gridlet=(Gridlet)this.list_.get(i);
			info="网格资源"+gridlet.getGridletID();
			
			id=random.nextInt(this.totalResource_);
			System.out.println("正在发送"+info+"到"+resourceName[id]+"，资源id="+resourceID[id]);
			
			//发送一个网格任务到特定的"resourceID"的网格资源
			super.gridletSubmit(gridlet, resourceID[id]);
			
			//出于统计目的，将这一事件存储到"stat.txt"文件
			super.recordStatistics("\"Submit " + info + " to " +
                    resourceName[id] + "\"", "");
			
			//等待从资源实体返回的网格任务
			gridlet=super.gridletReceive();
			System.out.println("正在接收网格任务"+gridlet.getGridletID());
			
			//出于统计目的，将这一事件记录在"stat.txt"文件中
			super.recordStatistics("\"Received " + info +  " from " +
                    resourceName[id] + "\"", gridlet.getProcessingCost());
			
			//将收到的网格任务存储在一个新的网格任务列表对象中
			this.receiveList_.add(gridlet);
		}
		
		//关掉所有实体，包括网格统计实体，因为我们用它记录事件了
		super.shutdownGridStatisticsEntity();
		super.shutdownUserEntity();
		super.terminateIOEntities();
	}
	
	/**
	 * 得到网格任务列表
	 * @return	一个网格任务列表
	 */
	public GridletList getGridletList(){
		return this.receiveList_;
	}
	
	/**
	 * 生成8个网格任务
	 * @param userID	拥有这些任务的网格用户实体ID
	 * @return	一个网格任务列表对象
	 */
	private GridletList createGridlet(int userID){
		//生成一个存储网格任务的容器
		GridletList list=new GridletList();
		
		int id=0;
		double length=3500.0;
		long file_size=300;
		long output_size=300;
		Gridlet gridlet1=new Gridlet(id, length, file_size, output_size);
		id++;
		Gridlet gridlet2=new Gridlet(id, 5000, 500, 500);
		id++;
		Gridlet gridlet3=new Gridlet(id, 9000, 900, 900);
		
		//设置这些网格任务的拥有者（将任务与用户关联）
		gridlet1.setUserID(userID);
		gridlet2.setUserID(userID);
		gridlet3.setUserID(userID);
		
		//把任务存储到列表中
		list.add(gridlet1);
		list.add(gridlet2);
		list.add(gridlet3);
		
		long seed=11L*13*17*19*23+1;
		Random random=new Random(seed);
		
		GridSimStandardPE.setRating(100);
		
		int count=5;
		for(int i=1; i<count+1; i++){
			length=GridSimStandardPE.toMIs(random.nextDouble()*50);
			file_size=(long) GridSimRandom.real(100, 0.10, 0.40, random.nextDouble());
			output_size=(long) GridSimRandom.real(250, 0.10, 0.50, random.nextDouble());
			
			Gridlet gridlet=new Gridlet(id+i, length, file_size, output_size);
			gridlet.setUserID(userID);
			list.add(gridlet);
		}
		
		return list;
	}
	
	////////////////////////静态方法////////////////////////
	
	/**
	 * 创建主方法
	 */
	public static void main(String[] args) {
		System.out.println("开始Test5");
		
		try {
			//第一步：初始化GridSim包（必须在创建任何实体之前初始化，否则报错！）
			int num_user=1;//网格用户总数
			Calendar calendar=Calendar.getInstance();
			boolean trace_flag=false;//不追踪GridSim事件
			
			//从任何统计度量移除的文件或进程名列表
			String[] exclude_from_file={""};
			String[] exclude_from_processing={""};
			
			//被写下的报告文件名（这里不需要）
			String report_name=null;
			
			//初始化包
			System.out.println("初始化GridSim包");
			GridSim.init(num_user, calendar, trace_flag, exclude_from_file, exclude_from_processing, report_name);
			
			//第二步：创建一个或多个网格资源对象
			GridResource resource0=createGridResource("Resource_0");
			GridResource resource1=createGridResource("Resource_1");
			GridResource resource2=createGridResource("Resource_2");
			int total_resource=3;
			
			//第三步：创建Test5对象
			Test5 obj=new Test5("Test5", 560.00, total_resource);
			
			//第四步：开始仿真
			GridSim.startGridSimulation();
			
			//最后一步：仿真结束之后打印网格任务
			GridletList newList=obj.getGridletList();
			printGridletList(newList);
			
			System.out.println("Test5结束！");
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
		System.out.println();
		System.out.println("正在开始创建一个带有3个machine的网格资源");
		
		//创建一个网格资源的步骤如下
		//1.需要创建一个机器列表兑现来存储一个或多个机器
		MachineList mList=new MachineList();
		System.out.println("创建一个机器列表");
		
		//2.创建一个机器，参数是：id，PE数，MIPS率
		int mipsRating=377;
		mList.add(new Machine(0, 4, mipsRating));//第一台机器
		System.out.println("创建第一个机器，带有4个PE，并且将其添加至机器列表");
		
		//3.如果向创建更多机器，重复步骤2
		mList.add(new Machine(1, 4, mipsRating));//第二台机器
		System.out.println("创建第二个机器，带有4个PE，并且将其添加至机器列表");
		
		mList.add(new Machine(2, 2, mipsRating));//第三台机器
		System.out.println("创建第三个机器，带有2个PE，并且将其添加至机器列表");
		
		//4.创建一个资源属性兑现，用来寻出网格资源的属性：系统结构，操作系统，机器列表，分配策略：时间共享或空间共享，时区和花费
		String arch="Sun Ultra";
		String os="Solaris";
		double time_zone=9.0;
		double cost=3.0;
		
		ResourceCharacteristics resConfig=new ResourceCharacteristics(arch, os, mList, ResourceCharacteristics.TIME_SHARED, time_zone, cost);
		
		System.out.println("创建了网格资源属性，并存储了机器列表");
		
		//5.最后，需要创建一个网格资源对象
		double baud_rate=100.0;
		long seed=11L*13*17*19*23+1;
		double peakLoad=0.0;
		double offPeakLoad=0.0;
		double holidayLoad=0.0;
		
		LinkedList Weekends=new LinkedList();
		Weekends.add(new Integer(Calendar.SATURDAY));
		Weekends.add(new Integer(Calendar.SUNDAY));
		
		LinkedList Holidays=new LinkedList();
		GridResource gridRes=null;
		try {
			gridRes=new GridResource(name, baud_rate, seed, resConfig, peakLoad, offPeakLoad, holidayLoad, Weekends, Holidays);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("最终，创建一个网格资源，并存储网格资源的属性");
		System.out.println();
		
		return gridRes;
	}
	
	/**
	 * 打印网格任务对象
	 * @param list	网格任务对象
	 */
	private static void printGridletList(GridletList list){
		int size=list.size();
		Gridlet gridlet;
		
		String indent="	";
		System.out.println();
		System.out.println("==========输出==========");
		System.out.println("网格任务ID"+indent+indent+"状态"+indent+"资源ID"+indent+"开销");
		
		for(int i=0; i<size; i++){
			gridlet=(Gridlet)list.get(i);
			System.out.print(gridlet.getGridletID()+indent+indent);
			
			if(gridlet.getGridletStatus()==Gridlet.SUCCESS){
				System.out.print("成功");
			}
			
			System.out.println(indent+gridlet.getResourceID()+indent+gridlet.getProcessingCost());
		}
	}
	
}
