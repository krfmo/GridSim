package test08;

import java.util.Calendar;
import java.util.LinkedList;

import gridsim.AllocPolicy;
import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;

public class Test8 extends GridSim{

	private Integer ID_;
	private String name_;
	private GridletList list_;
	private GridletList receiveList_;
	private int totalResource_;
	
	/**
	 * 分配一个新的Test8对象
	 * @param name	该对象的实体名
	 * @param baudRate	通信速度
	 * @param total_resource	空闲的网格资源数量
	 * @param numGridlet
	 * @throws Exception	未初始化GridSim包或者实体名为空，则抛异常
	 */
	public Test8(String name, double baudRate, int total_resource, int numGridlet) throws Exception {
		super(name, baudRate);
		this.name_=name;
		this.totalResource_=total_resource;
		this.receiveList_=new GridletList();
		
		//为实体获取ID
		this.ID_=new Integer(getEntityId(name));
		System.out.println("正在创建一个名为"+name+"，id="+this.ID_+"的网格用户实体");
		
		//为网格用户创建一个网格任务集合
		this.list_=createGridlet(this.ID_.intValue(), numGridlet);
		System.out.println(name+":正在创建"+this.list_.size()+"个网格任务");
	}
	
	public void body(){
		int resourceID[]=new int[this.totalResource_];
		double resourceCost[]=new double[this.totalResource_];
		String resourceName[]=new String[this.totalResource_];
		
		LinkedList resList;
		ResourceCharacteristics resChar;
		
		while(true){
			super.gridSimHold(1.0);
			
			resList=getGridResourceList();
			if(resList.size()==this.totalResource_){
				break;
			}else{
				System.out.println(this.name_+":等待获取资源列表...");
			}
		}
		
		//循环以获取所有可用资源
		int i=0;
		for(i=0; i<this.totalResource_;i++){
			resourceID[i]=((Integer)resList.get(i)).intValue();
			
			send(resourceID[i], GridSimTags.SCHEDULE_NOW, GridSimTags.RESOURCE_CHARACTERISTICS, this.ID_);
			
			resChar=(ResourceCharacteristics)receiveEventObject();
			resourceName[i]=resChar.getResourceName();
			resourceCost[i]=resChar.getCostPerSec();
			
			System.out.println(this.name_+":从名为"+resourceName[i]+"，id="+resourceID[i]+"的资源收到资源属性");
		}
		
        /////////////////////////////////////////////////////
        // SUBMITS Gridlets
		
		Gridlet gridlet=null;
		String info;
		
		int id=0;
		boolean success=false;
		
		for(i=0; i<this.list_.size(); i++){
			gridlet=this.list_.get(i);
			info="任务_"+gridlet.getGridletID();
			
			System.out.println(this.name_+":正在发送"+info+"到名为"+resourceName[id]+"，id="+resourceID[id]+"的网格资源，时间为"+GridSim.clock());
			
			if(i%2==0){
				success=gridletSubmit(gridlet, resourceID[id], 0.0, true);
				System.out.println("Ack="+success);
				System.out.println();
			}else{
				success=gridletSubmit(gridlet, resourceID[id], 0.0, false);
			}
		}
		
        //////////////////////////////////////////////////
        // RECEIVES Gridlets
		
		super.gridSimHold(20);
		System.out.println("<<<<<<<<<<暂停20秒>>>>>>>>>>");
		
		for(i=0; i<this.list_.size(); i++){
			gridlet=(Gridlet) super.receiveEventObject();
			
			System.out.println(this.name_+":正在接收网格任务"+gridlet.getGridletID());
			
			this.receiveList_.add(gridlet);
		}
		
		shutdownUserEntity();
		terminateIOEntities();
		System.out.println(this.name_+":%%%%%%退出body()%%%%%%");
	}
	
	/**
	 * 获取网格任务列表
	 * @return	一个网格任务列表
	 */
	public GridletList getGridletList(){
		return this.receiveList_;
	}

	/**
	 * 创建网格任务
	 * @param userID	拥有这些任务的网格用户实体ID
	 * @param numGridlet	
	 * @return	一个网格任务列表对象
	 */
	private GridletList createGridlet(int userID, int numGridlet) {
		GridletList list=new GridletList();
		
		int data[]={900, 600, 200, 300, 400, 500, 600};
		int size=0;
		if(numGridlet>=data.length){
			size=6;
		}else{
			size=numGridlet;
		}
		
		for(int i=0; i<size; i++){
			Gridlet gl=new Gridlet(i, data[i], data[i], data[i]);
			gl.setUserID(userID);
			list.add(gl);
		}
		
		return list;
	}
	
	////////////////////////静态方法////////////////////////
	
	public static void main(String[] args) {
		System.out.println("开始Test8");
		
		try {
			//First step
			int num_user=1;
			Calendar calendar=Calendar.getInstance();
			boolean trace_flag=true;
			
			String[] exclude_from_file={""};
			String[] exclude_from_processing={""};
			
			String report_name=null;
			
			GridSim.init(num_user, calendar, trace_flag, exclude_from_file, 
					exclude_from_processing, report_name);
			
			//Second step
			NewPolicy test=new NewPolicy("GridResource_0", "NewPolicy");
			GridResource resTest=createGridResource("GridResource_0", test);
			
			//Third step
			int total_resource=1;
			int numGridlet=4;
			double bandwidth=1000.00;
			Test8 user0=new Test8("User_0", bandwidth, total_resource, numGridlet);
			
			//Fourth step
			GridSim.startGridSimulation();
			
			//Final step
			GridletList newList=null;
			newList=user0.getGridletList();
			printGridletList(newList, "User_0");
			System.out.println("Test8结束！");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("出错啦！");
		}
	}
	
	private static GridResource createGridResource(String name, AllocPolicy obj){
		//1.
		MachineList mList=new MachineList();
		
		//2.
		int mipsRating=377;
		mList.add(new Machine(0, 4, mipsRating));
		
		//3.
		mList.add(new Machine(1, 4, mipsRating));
		mList.add(new Machine(2, 2, mipsRating));
		
		//4.
		String arch="Sun Ultra";
		String os="Solaris";
		double time_zone=9.0;
		double cost=3.0;
		
		ResourceCharacteristics resConfig=new ResourceCharacteristics(
				arch, os, mList, ResourceCharacteristics.SPACE_SHARED, time_zone, cost);
		
		//5.
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
			ResourceCalendar resCalendar=new ResourceCalendar(time_zone, peakLoad, offPeakLoad, holidayLoad, Weekends, Holidays, seed);
			gridRes=new GridResource(name, baud_rate, resConfig, resCalendar, obj);
		} catch (Exception e) {
			System.out.println("msg="+e.getMessage());
		}
		
		System.out.println("创建一个名为"+name+"的网格资源");
		return gridRes;
	}
	
	private static void printGridletList(GridletList list, String name){
		int size=list.size();
		Gridlet gridlet=null;
		
		String indent="	";
		System.out.println();
		System.out.println("==========用户"+name+"的输出==========");
		System.out.println("网格任务ID"+indent+indent+"状态"+indent+indent+"资源ID"+indent+"开销");
		
		int i=0;
		for(i=0; i<size; i++){
			gridlet=(Gridlet)list.get(i);
			System.out.println(gridlet.getGridletID()+indent+indent+
						gridlet.getGridletStatusString()+indent+indent+
						gridlet.getResourceID()+indent+
						gridlet.getProcessingCost());
		}
		
		for(i=0; i<size; i++){
			gridlet=(Gridlet)list.get(i);
			System.out.println(gridlet.getGridletHistory());
			
			System.out.print("网格任务#"+gridlet.getGridletID());
			System.out.println("，长度="+gridlet.getGridletLength()+
						"，完成程度"+gridlet.getGridletFinishedSoFar());
			System.out.println("==============================");
		}
	}

}





















