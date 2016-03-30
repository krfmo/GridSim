package test02;

import java.util.Random;

import gridsim.GridSimRandom;
import gridsim.GridSimStandardPE;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.ResourceUserList;

/*对照example2的联系
 * 运行本例时，会打印一个任务列表和他们的属性，本例展示了如何创建任务（Gridlet），所以不需要模拟任何东西。
 * 本例不需要初始化GridSim和SimJava，因为创建任务与运行模拟是不相关的。
 * 
 * 描述：一个简单的程序来说明如何使用GridSim包。
 * 		本例展示了如何创建一个或多个网格用户。一个网格用户包括一个或多个任务。
 * 		因此，本例也会展示如何使用或不使用GridSimRandom类来创建任务。
 * 
 * 提示：本例中用到的值是从GridSim paper中提取的（http://www.gridbus.org/gridsim/）*/

/**这个类展示了如何创建一个或多个网格用户。另外，也讨论了网格任务Gridlet的创建*/
public class Test2 {
	/**
	 * 运行本例的主函数
	 * */
	public static void main(String[] args) {
		System.out.println("开始创建网格用户");
		System.out.println();
		
		try {
			//创建任务集合
			GridletList list=createGridlet();
			System.out.println("创建了"+list.size()+"个任务");
			
			ResourceUserList userList=createGridUser(list);
			System.out.println("创建了"+userList.size()+"个网格用户");
			
			//打印任务列表
			printGridletList(list);
			System.out.println("案例结束~");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println("出错啦！");
		}
	}
	
	/**
	 * 一个网格用户有多个要被处理的网格任务。本方法将展示如何创建网格任务使用或不使用GridSimRandom类
	 * */
	private static GridletList createGridlet(){
		//创建一个盛放网格任务的容器
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
		Random random=new Random();
		
		//设置PE的MIPS Rating
		GridSimStandardPE.setRating(100);
		
		//创建5个任务，随机生成的这5个任务，其长度等参数都是随机的，以后模拟时候用得上！
		int count=5;
		double min_range=0.10;
		double max_range=0.50;
		for(int i=1;i<count+1;i++){
			//任务长度由随机值和当前PE处理能力（MIPS Rating）决定
			length=GridSimStandardPE.toMIs(random.nextDouble()*output_size);
			
			//规定了任务文件的长度的变化范围是：100 + (10% to 50%)
			file_size=(long) GridSimRandom.real(100, min_range, max_range, random.nextDouble());
			
			//规定了任务输出长度的变化范围是：250 + (10% to 50%)
			output_size=(long) GridSimRandom.real(250, min_range, max_range, random.nextDouble());
			
			//创建一个新的网格任务对象
			Gridlet gridlet=new Gridlet(id+i, length, file_size, output_size);
			
			//添加网格任务到集合
			list.add(gridlet);
		}
		
		return list;
	}
	
	/**
	 * 创建网格用户。在本例中，创建3个用户，然后将他们分配给网格任务。
	 */
	private static ResourceUserList createGridUser(GridletList list){
		ResourceUserList userList=new ResourceUserList();//ResourceUserList类继承了LinkedList
														//类中只有三个方法，一个是add方法，一个是过时的myRemove，还有一个是super了LinkedList的remove方法
		
		userList.add(0);//用户id从0开始
		userList.add(1);//此处参数为int类型，然而方法内部会将其封装成Integer对象，然后添加到链表
		userList.add(2);//方法会先判断集合中是否已经存在该对象，若已存在则返回false，否则将对象添加到集合，并返回true
		
		int userSize=userList.size();
		int gridletSize=list.size();
		int id=0;
		
		//将用户ID分配给指定任务
		for(int i=0;i<gridletSize;i++){
			if(i!=0 && i%userSize==0){
				id++;
			}
				//这种分配方式也是挺特别...012分配给用户0，345分配给用户1，67分配给用户2，这应该是没用到什么调度算法吧...
			((Gridlet)list.get(i)).setUserID(id);//将第i个任务分配给第id个用户
		}
		
		return userList;
	}
	
	private static void printGridletList(GridletList list){
		int size=list.size();
		Gridlet gridlet;
		
		String indent="	";//缩进
		System.out.println();
		System.out.println("Gridlet ID"+ indent+indent +"User ID"+ indent+indent +"length"+ indent+indent 
				+"file size"+ indent +"output size");
		
		for(int i=0;i<size;i++){
			gridlet=(Gridlet)list.get(i);//为啥非要强转一下？？？
			System.out.println(indent+gridlet.getGridletID()+indent+
					indent+gridlet.getUserID()+indent+indent+
					(int)gridlet.getGridletLength()+indent+indent+
					(int)gridlet.getGridletFileSize()+indent+indent+
					(int)gridlet.getGridletOutputSize());
		}
	}
}




























