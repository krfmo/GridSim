package test03;

import java.util.Calendar;
import java.util.Random;

import gridsim.GridSim;
import gridsim.GridSimRandom;
import gridsim.GridSimStandardPE;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.GridletList;

/*
 * 描述：一个简单的程序来描述如何使用GridSim包。
 * 		本例展示了两个GridSim实体之间是如何互相联系的。
 */

/**
 * Test3类创建网格任务，并将其发送至另一个GridSim实体（Test类）
 */
class Test3 extends GridSim{
	private String entityName_;//这是另一个用户实体，就是网格任务被发送的目标实体
	private GridletList list_;
	private GridletList receiveList_;//从Test对象收到的网格任务列表
	
	/**
	 * 分配一个新的Test3对象
	 * @param name	实体名字
	 * @param baudRate	通信速度
	 * @param list	一个网格任务列表
	 * @throws Exception	当创建实体先于初始化GridSim包或者实体名字是空时，会抛异常
	 */
	public Test3(String name, double baudRate, GridletList list) throws Exception {
		super(name);
		this.list_=list;
		receiveList_=new GridletList();
		
		//创建一个Test实体，赋值给"entityName"
		entityName_="Test";
		new Test(entityName_, baudRate);//创建Test3对象时，就会创建Test对象，二者baudRate相同
	}
	
	/**
	 * 解决GridSim实体间通信最核心的方法
	 */
	@Override
	public void body() {
		int size=list_.size();
		Gridlet obj, gridlet;
		
		//循环，使每次处理一个网格任务，并将其发送到其他GridSim实体
		for(int i=0; i<size; i++){
			obj=(Gridlet)list_.get(i);
			System.out.println("Test3内部body()方法=>正在发送第"+obj.getGridletID()+"个任务");
			
			//无延迟的向"entityName"指定的GridSim实体发送一个网格任务（用到常量GridSimTags.SCHEDULE_NOW）
			super.send(entityName_, GridSimTags.SCHEDULE_NOW, GridSimTags.GRIDLET_SUBMIT, obj);
			
			//收到的发回来的网格任务
			gridlet=super.gridletReceive();
			
			System.out.println("Test3内部body()方法=>正在接收第"+obj.getGridletID()+"个任务");
			
			//将收到的网格任务存储到新的网格任务列表对象中
			receiveList_.add(gridlet);
		}
		
		//向"entityName"对应的GridSim实体发送模拟结束的信号
		super.send(entityName_, GridSimTags.SCHEDULE_NOW, GridSimTags.END_OF_SIMULATION);
	}
	
	/**
	 * 得到网格任务列表
	 * @return	一个网格任务的列表
	 */
	public GridletList getGridletList(){
		return receiveList_;
	}
	
	/**
	 * main方法
	 */
	public static void main(String[] args) {
		System.out.println("Test3开始");
		System.out.println();
		
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
			
			//第二步：创建一个网格任务列表
			GridletList list=createGridlet();
			System.out.println("正在创建"+list.size()+"个网格任务");
			
			//第三步：创建Test3对象
			Test3 obj=new Test3("Test3", 560.00, list);
			
			//第四步：开始模拟
			GridSim.startGridSimulation();
			
			//最后一步：仿真结束时打印网格任务
			GridletList newList=obj.getGridletList();
			printGridletList(newList);
			
			System.out.println("Test3运行结束！");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("出错啦！");
		}
	}
	
	/**
	 * 该方法将展示如何创建网格任务（应该是跟Test1一样的，稍有不同）
	 * @return	一个网格任务列表对象
	 */
	private static GridletList createGridlet(){
		//创建一个容器
		GridletList list=new GridletList();
		
		
		//我们不使用GridSimRandom，手动创建3个任务
		int id=0;
		double length=3500.0;
		long file_size=300;
		long output_size=300;
		Gridlet gridlet1=new Gridlet(id, length, file_size, output_size);
		id++;
		Gridlet gridlet2=new Gridlet(id, 5000, 500, 500);
		id++;
		Gridlet gridlet3=new Gridlet(id, 9000, 900, 900);
		
		//将任务存储至集合
		list.add(gridlet1);
		list.add(gridlet2);
		list.add(gridlet3);
		
		//我们使用GridSimRandom和GridSimStandardPE类创建5个任务
		long seed=11L*13*17*19*23+1;
		Random random=new Random(seed);
				
		//设置PE的MIPS Rating
		GridSimStandardPE.setRating(100);
		
		//生成5个网格任务
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
			
			//添加网格任务到集合
			list.add(gridlet);
		}
		
		return list;
	}
	
	private static void printGridletList(GridletList list){
		int size=list.size();
		Gridlet gridlet;
		
		String indent="	";
		System.out.println();
		System.out.println("==========输出结果==========");
		System.out.println("网格任务ID"+indent+indent+"状态");
		
		for(int i=0; i<size; i++){
			gridlet=(Gridlet)list.get(i);
			System.out.print(indent+gridlet.getGridletID()+indent);
			
			if(gridlet.getGridletStatus()==Gridlet.SUCCESS){
				System.out.println("成功");
			}
		}
	}
		
}




















