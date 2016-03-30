package test10;

/*
 * 描述：本程序展示了如何使用基础的、先进的预留功能。例如：创建、提交和状态
 */

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Random;

import gridsim.AdvanceReservation;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.GridletList;


/**
 * 一个提前预留资源的用户实体。
 * 在本例中，只探索几个功能，例如：
 * - 请求一个新的提前预留
 * - 请求一个新的即时预留。即时预留意味着使用当前时间作为开始时间
 * - 提交一个被接受的的预留
 * - 检查一个预留的状态
 */
public class ARTest extends AdvanceReservation{

	private GridletList list_;
	private GridletList receiveList_;
	private int failReservation_;
	
	private final int SEC=1;
	private final int MIN=60*SEC;
	private final int HOUR=60*MIN;
	private final int DAY=24*HOUR;
	
	/**
	 * 创建一个网格用户实体
	 * @param name	该对象实体名
	 * @param baudRate	通信速度
	 * @param timeZone	用户当地时区
	 * @param totalJob	将要被创建的网格任务总数
	 * @throws Exception	初始化前创建该实体或实体名为空则抛异常
	 */
	public ARTest(String name, double baudRate, double timeZone,
				int totalJob) throws Exception {
		super(name, baudRate, timeZone);
		this.receiveList_=new GridletList();
		this.failReservation_=0;
		
		//创建网格任务
		list_=createGridlet(totalJob, super.get_id());
		
		System.out.println("正在创建名为"+name+"，id="+super.get_id()+"的网格用户实体");
		System.out.println(name+":正在创建"+totalJob+"个网格任务");
	}
	
	public void body(){
		LinkedList resList;
		
		while(true){
			super.gridSimHold(2*SEC);
			
			resList=getGridResourceList();
			if(resList.size()>0){
				break;
			}else{
				System.out.println(super.get_name()+
						":等待获取资源列表...");
			}
		}
		
		//支持提前预约（AR）的资源ID集合
		ArrayList resARList=new ArrayList();
		
		//支持AR的资源名字集合
		ArrayList resNameList=new ArrayList();
		
		int totalPE=0;
		int i=0;
		Integer intObj=null;//资源ID
		String name;//资源名
		
		//循环以获取一组可以支持AR的资源
		for(i=0; i<resList.size(); i++){
			intObj=(Integer) resList.get(i);
			
			//double check一个资源是否支持AR。本例中，所有资源均支持AR。
			if(GridSim.resourceSupportAR(intObj)==true){
				//得到资源的名字
				name=GridSim.getEntityName(intObj.intValue());
				
				//得到资源拥有的PE的总数
				totalPE=super.getNumPE(intObj);
				
				//将资源的ID、名字和总PE数添加到他们各自的集合
				resARList.add(intObj);
				resNameList.add(name);
			}
		}
		
		//------------------------------------
		//发送一个或多个预约到一个网格资源实体
		sendReservation(resARList, resNameList);
		
		try {
			//然后重新获取结果或网格任务
			int size=list_.size()-failReservation_;
			for(i=0; i<size; i++){
				Gridlet gl=super.gridletReceive();
				this.receiveList_.add(gl);////////////忘记加这一句了，导致输出表格只有第一行没有数据...怎么会忘记这句呢？？？
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//各种关闭
		shutdownGridStatisticsEntity();
		terminateIOEntities();
		shutdownUserEntity();
		System.out.println(super.get_name()+":%%%%退出body()方法，失败的预约个数为"+failReservation_);
	}
	
	/**
	 * 创建一个新预约，并将它发送至资源。
	 * 一个预约只预留1个PE。此时，GridSim只能在一个PE上处理一个任务
	 * @param resARList	预留的资源ID集合
	 * @param resNameList	预留的资源名集合
	 */
	private void sendReservation(ArrayList resARList, ArrayList resNameList){
		//制定的预约总数，1次预约会预留一个PE
		int totalPE=1;
		int totalReservation=list_.size();//网格任务总数
		
		//获取仿真的初始时间
		Calendar cal=GridSim.getSimulationCalendar();
		
		//想要预约仿真初始时间后的一天
		int MILLI_SEC=1000;
		long time=cal.getTimeInMillis()+(1*DAY*MILLI_SEC);
		
		//每次预约需要大概10分钟的时间
		int duration=10*MIN;
		
		String result=null;
		Gridlet gl=null;
		int val=0;
		int resID=0;//一个资源ID
		int totalResource=resARList.size();//可获得的AR资源总数
		String resName=null;
		
		Random randObj=new Random(time);
		
		for(int i=0; i<totalReservation; i++){
			duration+=5*MIN;//一个预约的时常
			
			//获取资源ID和资源名
			val=randObj.nextInt(totalResource);
			resID=((Integer)resARList.get(val)).intValue();
			resName=(String) resNameList.get(val);
			
			//查看是否是即时预约，即起始时间为0，这意味着以当前时间作为起始时间
			if(val==i){
				time=0;//即使预约
			}else{
				time=cal.getTimeInMillis()+(1*DAY*MILLI_SEC)+(duration*MILLI_SEC);
			}
			
			//创建一个新的或即时预约
			result=super.createReservation(time, duration, totalPE, resID);
			System.out.println(super.get_name()+":从资源"+resName+"的预约结果是"+result+"，时间为"+GridSim.clock());
			
			//查询这些预约的状态
			val=super.queryReservation(result);
			System.out.println(super.get_name()+":查询结果="+AdvanceReservation.getQueryResult(val));
			
			//如果预约失败，则继续下一个网格任务
			if(val==GridSimTags.AR_STATUS_ERROR||
				val==GridSimTags.AR_STATUS_ERROR_INVALID_BOOKING_ID){
				failReservation_++;
				System.out.println("==================");
				continue;
			}
			
			//对于一个有偶数编号的预约，直接提交，而不需要发送任何网格任务
			if(i%2==0){
				val=super.commitReservation(result);
				System.out.println(super.get_name()+":只提交结果="+AdvanceReservation.getCommitResult(val));
			}
			
			//一个预约只需要预留一个PE
			gl=(Gridlet)list_.get(i);
			val=super.commitReservation(result, gl);
			System.out.println(super.get_name()+":提交结果="+AdvanceReservation.getCommitResult(val));
			
			//查询这个预约的状态
			val=super.queryReservation(result);
			System.out.println(super.get_name()+":查询结果="+AdvanceReservation.getQueryResult(val));
			System.out.println("========================");
		}
	}
	
	/**
	 * 获取一个网格任务集合
	 * @return	一个网格任务集合
	 */
	public GridletList getGridletList(){
		return this.receiveList_;
	}

	/**
	 * 一个网格用户有多个网格任务需要处理。
	 * 本方法将告诉你如何创建网格任务（使用或不使用GridSimRandom类）
	 * @param size	网格任务总数
	 * @param userID	网格任务所属的网格用户
	 * @return	一个网格任务列表对象
	 */
	private GridletList createGridlet(int size, int userID) {
		//创建一个容器来存储网格任务
		GridletList list=new GridletList();
		int length=5000;
		for(int i=0; i<size; i++){
			//创建一个新的网格任务对象
			Gridlet gridlet=new Gridlet(i, length, 1000, 5000);
			
			//将网格任务加入到任务列表
			list.add(gridlet);
			gridlet.setUserID(userID);
		}
		
		return list;
	}

}














