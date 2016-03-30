package test09;

/*
 * 描述：本例展示如何创建并定义自己的网格资源和网格信息服务实体。
 */
import java.util.Calendar;
import java.util.LinkedList;

import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.IO_data;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;

public class Test9 extends GridSim{

	public static final int HELLO=900;
	public static final int TEST=901;
	
	private Integer ID_;//该对象的实体ID
	private String name_;//该对象的实体名
	private int totalResource_;//可利用的网格资源总数
	public Test9(String name, double baudRate, int total_resource) throws Exception {
		super(name, baudRate);
		this.name_=name;
		this.totalResource_=total_resource;
		
		this.ID_=new Integer(getEntityId(name));
		System.out.println("正在创建名为"+name+"，id为"+this.ID_+"的网格用户实体");
	}
	
	public void body(){
		int resourceID[]=new int[this.totalResource_];
		String resourceName[]=new String[this.totalResource_];
		
		LinkedList resList;
		ResourceCharacteristics resChar;
		
		/*
		 * 等待获取一个资源列表。GridSim多线程，所有需要先等待
		 */
		while(true){
			super.gridSimHold(1.0);
			
			resList=getGridResourceList();
			if(resList.size()==this.totalResource_){
				break;
			}else{
				System.out.println(this.name_+":正在等待获取资源列表...");
			}
		}
		
		int SIZE=12;//Integer对象大约占12字节
		int i=0;
		
		/*
		 * 循环得到所有可用资源。
		 * 一旦资源被识别，向其发送HELLO和TEST标签
		 */
		for(i=0; i<this.totalResource_; i++){
			//资源列表存储的是资源ID而不是资源对象
			resourceID[i]=((Integer)resList.get(i)).intValue();
			
			//给资源实体的发送其属性的请求，注意，直接发送，不需要使用I/O端口
			super.send(resourceID[i], GridSimTags.SCHEDULE_NOW, GridSimTags.RESOURCE_CHARACTERISTICS, this.ID_);
			
			//等待获取一个资源属性
			resChar=(ResourceCharacteristics) receiveEventObject();
			resourceName[i]=resChar.getResourceName();
			
			//打印该实体收到了一个特定的资源属性
			System.out.println(this.name_+":收到名为"+resourceName[i]+"，id为"+resourceID[i]+"的资源发送的资源属性");
			
			//发送TEST标签到资源，使用I/O端口。
			//在互联网上传输，应考虑传输时间
			System.out.println(this.name_+":正在发送TEST标签至资源"+resourceName[i]+"，时间为"+GridSim.clock());
			super.send(super.output, GridSimTags.SCHEDULE_NOW, TEST,
					new IO_data(ID_, SIZE, resourceID[i]));
			
			//发送HELLO标签到资源，使用I/O端口。
			System.out.println(this.name_+":正在发送HELLO标签至资源"+resourceName[i]+"，时间为"+GridSim.clock());
			super.send(super.output, GridSimTags.SCHEDULE_NOW, HELLO,
					new IO_data(ID_, SIZE, resourceID[i]));
		}
		
		//需要等待10秒来让资源处理收到的事件
		super.sim_pause(10);
		
		//各种关闭
		shutdownGridStatisticsEntity();
		shutdownUserEntity();
		terminateIOEntities();
		System.out.println(this.name_+":%%%%%%退出body()%%%%%%");
	}

	////////////静态方法////////////
	
	public static void main(String[] args) {
		System.out.println("开始Test9");
		
		try {
			//第一步：初始化GridSim包。必须在创建任何实体之前调用初始化方法。
			int num_user=1;//网格用户总数
			Calendar calendar=Calendar.getInstance();
			boolean trace_flag=true;//true意味着追踪GridSim事件
			
			//初始化包
			System.out.println("初始化GridSim包");
			
			//在本例中，初始化GridSim而不创建一个默认的GIS实体
			GridSim.init(num_user, calendar, trace_flag, false);
			
			//创建一个新GIS实体
			NewGIS gis=new NewGIS("NewGIS");
			
			//需要在开始模拟前调用这个方法！
			GridSim.setGIS(gis);
			
			//第二步：创建一个或多个网格资源实体
			NewGridResource resource0=createGridResource("Resource_0");
			int total_resource=1;
			
			//第三步：创建一个或多个网格用户实体
			Test9 user0=new Test9("User_0", 560.00, total_resource);
			
			//第四步：开始模拟
			GridSim.startGridSimulation();
			System.out.println("Test9结束~");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("出错啦！");
		}
	}
	
	/**
	 * 创建一个网格资源。
	 * @param name	资源名
	 * @return	一个网格资源对象
	 */
	private static NewGridResource createGridResource(String name){
		MachineList mList=new MachineList();
		
		int mipsRating=377;
		mList.add(new Machine(0, 4, mipsRating));
		
		String arch="Sun Ultra";
		String os="Solaris";
		double time_zone=9.0;
		double cost=3.0;
		
		ResourceCharacteristics resConfig=new ResourceCharacteristics(
				arch, os, mList, ResourceCharacteristics.SPACE_SHARED,
				time_zone, cost);
		
		double baud_rate=100.0;
		long seed=11L*13*17*19*23+1;
		double peakLoad=0.0;
		double offPeakLoad=0.0;
		double holidayLoad=0.0;
		
		LinkedList Weekends=new LinkedList();
		Weekends.add(new Integer(Calendar.SATURDAY));
		Weekends.add(new Integer(Calendar.SUNDAY));
		
		LinkedList Holidays=new LinkedList();
		
		ResourceCalendar calendar=new ResourceCalendar(time_zone, peakLoad,
				offPeakLoad, holidayLoad, Weekends, Holidays, seed);
		
		NewGridResource gridRes=null;
		try {
			//下面的代码创建一个NewGridResource对象而不是它的父类对象
			gridRes=new NewGridResource(name, baud_rate, resConfig, calendar, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("创建一个名为"+name+"的网格资源");
		return gridRes;
	}
}



















