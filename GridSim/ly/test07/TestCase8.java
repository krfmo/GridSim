package test07;

import java.util.LinkedList;

import gridsim.GridSim;
import gridsim.Gridlet;
import gridsim.GridletList;

/**
 * 该测试案例是关于提交-暂停-恢复-移动-取消-完成
 */
class TestCase8 extends GridSim{
	private int myId_;
	private String name_;
	private GridletList list_;
	private GridletList receiveList_;
	private double delay_;
	
	/**
	 * 分配一个TestCase8对象
	 * @param name	该对象的实体名
	 * @param baudwidth	通信速度
	 * @param delay	模拟延时
	 * @param totalGridlet	应该创建的网格任务数量
	 * @param glLength	一个存储不同网格任务长度的数组
	 * @throws Exception	在初始化GridSim包之前创建该实体，或实体名为空时，抛异常
	 */
	TestCase8(String name, double baudwidth, double delay, int totalGridlet,
			int[] glLength) throws Exception {
		super(name, baudwidth);
		this.name_=name;
		this.delay_=delay;
		
		this.receiveList_=new GridletList();
		this.list_=new GridletList();
		
		//为该实体获取ID
		this.myId_=super.getEntityId(name);
		System.out.println("创建一个名为"+name+"的网格用户实体，id="+this.myId_);
		
		//为网格用户创建一个存放网格任务的列表
		System.out.println(name+":正在创建"+totalGridlet+"个网格任务");
		this.createGridlet(myId_, totalGridlet, glLength);
	}
	
	/**
	 * 处理GridSim实体间通信的核心方法
	 */
	public void body() {
		//给网格资源实体一些时间去向GIS实体注册自己的服务
		super.gridSimHold(3.0);
		LinkedList<Integer> resList=super.getGridResourceList();//比原来代码加了个泛型
		
		//初始化所有容器
		int totalResource=resList.size();
		int resourceID[]=new int[totalResource];
		String resourceName[]=new String[totalResource];
		
		//获取所有可用资源的循环
		int i=0;
		for(i=0; i<totalResource; i++){
			//资源列表保存的是资源ID的列表
			resourceID[i]=((Integer)resList.get(i)).intValue();
			
			//同时也获取他们的名字
			resourceName[i]=GridSim.getEntityName(resourceID[i]);
		}
		
        ////////////////////////////////////////////////
        // SUBMIT Gridlets
		
		//决定要发送到哪个网格资源
		int index=myId_%totalResource;
		if(index>=totalResource){
			index=0;
		}
		
		//发送所有的网格任务
		Gridlet gl=null;
		boolean success;
		for(i=0; i<list_.size(); i++){
			gl=(Gridlet)list_.get(i);
			
			//偶数的网格任务，发送并携带ACK
			if(i%2==0){
				success=super.gridletSubmit(gl, resourceID[index], 0.0, true);
				System.out.println(name_+":正在发送状态为"+success+"的网格任务#"+gl.getGridletID()+"到资源"+resourceName[index]);
			}else{
				//奇数的网格任务，发送不携带ACK
				success=super.gridletSubmit(gl, resourceID[index], 0.0, false);
				System.out.println(name_+":正在发送网格任务#"+gl.getGridletID()+"到资源"+resourceName[index]+"没有ACK，所以状态为"+success);
			}
		}
		
        ///////////////////////////////////////////////////////////
        // PAUSING Gridlets
		
		//暂停一段时间
		super.gridSimHold(15);
		System.out.println("<<<<<<<<<<暂停15个单位>>>>>>>>>>");
		
		//带确认的暂停一个网格任务
		for(i=0; i<list_.size(); i++){
			if(i%3==0){
				success=super.gridletPause(i, myId_, resourceID[index], 0.0, true);
				System.out.println(name_+":暂停任务#"+i+"，时间为"+GridSim.clock()+"，success="+success);
			}
		}
		
        ///////////////////////////////////////////////////////////
        // RESUMING Gridlets
		
		//暂停一段时间
		super.gridSimHold(15);
		System.out.println("<<<<<<<<<<暂停15个单位>>>>>>>>>>");
		
		//恢复一个网格任务
		for(i=0; i<list_.size(); i++){
			if(i%3==0){
				success=super.gridletResume(i, myId_, resourceID[index], 0.0, true);
				
				System.out.println(name_+":恢复任务#"+i+"，时间为"+GridSim.clock()+"，success="+success);
			}
		}
		
		//////////////////////////////////////////
		// MOVES Gridlets
		
		//暂停一段时间
		super.gridSimHold(45);
		System.out.println("<<<<<<<<<<暂停45个单位>>>>>>>>>>");
		
		//首先检查是否有足够的网格资源
		if(resourceID.length==1){
			System.out.println("不能移动网格任务，因为资源只有一个");
		}else{
			//感觉是将所有网格资源移位，2移成1，3移成2， ... ，1移成最后一个
			int move=0;
			if(index==0){
				move=resourceID.length-1;
			}else{
				move=index-1;
			}
			
			//只转移选定的网格任务
			for(i=0; i<list_.size(); i++){
				if(i%3==0){
					success=super.gridletMove(i, myId_, resourceID[index], resourceID[move], 0, true);
					System.out.println(name_+":移动网格任务#"+i+"时间="+GridSim.clock()+"success="+success);
				}
			}
		}
		
		//////////////////////////////////////////
		// CANCELING Gridlets
		
		super.gridSimHold(25);
		System.out.println("<<<<<<<<<<暂停25个单位>>>>>>>>>>");
		
		//必须取消之前暂停的哪个网格任务，否则程序会挂起（hang）
		for(i=0; i<list_.size(); i++){
			if((i%2==0)||(i%3==0)){
				gl=super.gridletCancel(i, myId_, resourceID[index], 0.0);//第二个参数写成和第三个参数一样啦！！！
				System.out.println(name_+":取消网格任务#"+i+"，时间为"+GridSim.clock());
				
				if(gl==null){
					System.out.println("result=NULL");
				}else{//如果取消成功，将该任务添加到列表
					System.out.println("result=NOT null");
					receiveList_.add(gl);
				}
			}
		}
		
		////////////////////////////////////////////////////////
		// RECEIVES Gridlets back
		
		//暂停的时间久一些，因为对于一个较小的带宽来说，网格任务的长度太长了...
		super.gridSimHold(1000);
		System.out.println("<<<<<<<<<<暂停1000个单位>>>>>>>>>>");
		
		//收回这些网格任务
		int size=list_.size()-receiveList_.size();
		for(i=0; i<size; i++){
			gl=(Gridlet)super.receiveEventObject();//得到该网格任务
			receiveList_.add(gl);//添加到received list
			
			System.out.println(name_+":收到网格任务#"+gl.getGridletID()+"接收时间为："+GridSim.clock());
		}
		
		System.out.println(this.name_+":%%%%退出body()，时间为"+GridSim.clock());
		
		//各种关闭
		shutdownUserEntity();
		terminateIOEntities();
		
		//打印模拟输出
		printGridletList(receiveList_, name_);
	}
	
	/**
	 * 如何创建一些网格任务的方法
	 * @param userID
	 * @param numGridlet
	 * @param data
	 */
	private void createGridlet(int userID, int numGridlet, int[] data){
		int k=0;
		for(int i=0; i<numGridlet; i++){
			if(k==data.length){
				k=0;
			}
			
			//创建一个网格任务
			Gridlet gl=new Gridlet(i, data[k], data[k], data[k]);
			gl.setUserID(userID);
			this.list_.add(gl);
			
			k++;
		}
	}
	
	/**
	 * 打印网格任务对象
	 * @param list
	 * @param name
	 */
	private void printGridletList(GridletList list, String name){
		int size=list.size();
		Gridlet gridlet=null;
		
		String indent="	";
		System.out.println();
		System.out.println("==========用户"+name+"的输出==========");
		System.out.println("任务ID"+indent+"状态"+indent+indent+"资源ID"+indent+"开销");
		
		//打印全部结果的循环
		int i=0;
		for(i=0; i<size; i++){
			gridlet=(Gridlet)list.get(i);
			System.out.print(gridlet.getGridletID()+indent+gridlet.getGridletStatusString());
			if(gridlet.getGridletStatusString().equals("Success")){
				System.out.print(indent);
			}
			System.out.println(indent+gridlet.getResourceID()+indent+gridlet.getProcessingCost());
		}
		
		//打印每一个网格任务历史的循环
		for(i=0; i<size; i++){
			gridlet=(Gridlet)list.get(i);
			System.out.println(gridlet.getGridletHistory());
			
			System.out.println("任务#"+gridlet.getGridletID()+"，长度="+gridlet.getGridletLength()
					+"，完成程度="+gridlet.getGridletFinishedSoFar());
			System.out.println("==============================\n");
		}
	}
	
}
