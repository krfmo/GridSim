package test10;

import gridsim.ARGridResource;
import gridsim.ARSimpleSpaceShared;
import gridsim.GridSim;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;

import java.util.Calendar;
import java.util.LinkedList;

import sun.font.CreatedFontTracker;

public class Test10 {
	
	/**
	 * 初始化GridSim，在运行仿真前创建用户和资源实体的主方法
	 */
	public static void main(String[] args) {
		System.out.println("开始Test10");
		try {
			int num_user=5;
			Calendar calendar=Calendar.getInstance();
			System.out.println("初始仿真时间="+calendar.getTime());
			
			boolean tace_flag=false;
			
			GridSim.init(num_user, calendar, tace_flag);
			
			//-----------------------
			
			//R0:vpac Compaq AlphaServer
			ARGridResource resource0=createGridResource("Resource_0", 4, 1, 10.0, 515);
			
			//R1:Manjra Linux PC
			ARGridResource resource1=createGridResource("Resource_1", 13, 1, 10.0, 684);
			
			//R2:Germany
			ARGridResource resource2=createGridResource("Resource_2", 16, 1, 1.0, 410);
			
			//R3:Czech
			ARGridResource resource3=createGridResource("Resource_3", 6, 1, 1.0, 410);
			
			//R4:Chichago
			ARGridResource resource4=createGridResource("Resource_4", 8, 1, -6.0, 377);
			
			//-----------------------
			
			ARTest[] userList=new ARTest[num_user];
			ARTest user=null;//一个用户实体
			double bandwidth=1000;//该用户的带宽
			double timeZone=0.0;//用户的时区
			int totalJob=0;//拥有的总任务数
			int i=0;
			
			//一个创建用户实体的循环
			for(i=0; i<num_user; i++){
				//偶数编号的用户时区为GMT+8
				if(i%2==0){
					timeZone=8.0;//GMT UTC
					totalJob=4;
				}else{
					timeZone=-3.0;
					totalJob=5;
				}
				
				//创建用户实体
				user=new ARTest("User_"+i, bandwidth, timeZone, totalJob);
				
				//将实体放入数组
				userList[i]=user;
			}
			
			//-------------------------------
			GridSim.startGridSimulation();
			
			//-------------------------------
			GridletList newList=null;
			for(i=0; i<num_user; i++){
				user=(ARTest)userList[i];
				newList=user.getGridletList();
				printGridletList(newList, user.get_name());
			}
			
			System.out.println("Test10结束");
		} catch (Exception e) {
			System.out.println("出错啦！");
			e.printStackTrace();
		}
	}

	/**
	 * 创建一个支持提前预约功能的网格资源实体。
	 * @param name
	 * @param totalPE
	 * @param totalMachine
	 * @param timeZone
	 * @param rating
	 * @return
	 */
	private static ARGridResource createGridResource(String name, int totalPE,
			int totalMachine, double timeZone, int rating) {
		MachineList mList=new MachineList();
		for(int i=0; i<totalMachine; i++){
			mList.add(new Machine(i, totalPE, rating));
		}
		
		String arch="Sun Ultra";
		String os="Solaris";
		double cost=3.0;
		
		ResourceCharacteristics resConfig=new ResourceCharacteristics(
				arch, os, mList, ResourceCharacteristics.ADVANCE_RESERVATION, 
				timeZone, cost);
		
		double baud_rate=100.0;
		long seed=11L*13*17*19*23+1;
		double peakLoad=0.0;
		double offPeakLoad=0.0;
		double holidayLoad=0.0;
		
		LinkedList Weekends=new LinkedList();
		Weekends.add(new Integer(Calendar.SATURDAY));
		Weekends.add(new Integer(Calendar.SUNDAY));
		
		LinkedList Holidays=new LinkedList();
		ARGridResource gridRes=null;
		
		ResourceCalendar cal=new ResourceCalendar(
				resConfig.getResourceTimeZone(), peakLoad, offPeakLoad, 
				holidayLoad, Weekends, Holidays, seed);
		
		try {
			String scheduler="scheduler";
			ARSimpleSpaceShared policy=new ARSimpleSpaceShared(name, scheduler);
			
			gridRes=new ARGridResource(name, baud_rate, resConfig, cal, policy);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("创建一个名为"+name+"的网格资源");
		return gridRes;
	}
	
	/**
	 * 打印网格任务对象
	 * @param list	网格任务集合
	 * @param name	网格用户名
	 */
	private static void printGridletList(GridletList list, String name) {
		String indent="	";
		System.out.println();
		System.out.println("============用户"+name+"的输出============");
		System.out.println("网格任务ID"+indent+"状态"+indent+indent+"资源ID"+indent+"开销");
		
		int size=list.size();
		int i=0;
		Gridlet gridlet=null;
		String status;
		
		for(i=0; i<size; i++){
			gridlet=(Gridlet)list.get(i);
			System.out.print(gridlet.getGridletID()+indent);
			
			status=gridlet.getGridletStatusString();
			System.out.print(status+indent);
			
			System.out.println(indent+gridlet.getResourceID()+indent+gridlet.getProcessingCost());
		}
	}
}
