package test03;

import eduni.simjava.Sim_event;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;

/*
 * 描述：一个简单的程序来描述如何使用GridSim包。
 * 		本例展示了两个GridSim实体之间是如何互相联系的。
 */

/**
 * Test类建立了Input和Output实体。然后Test类监听事件模拟，
 * 等待接收从另一个GridSim实体（本例中是Test3类）传来的网格任务。
 * 然后，该类向Test3类传回网格任务。
 */
public class Test extends GridSim{

	/**
	 * 分配一个新的Test对象
	 * @param name	实体的名字
	 * @param baudRate	通信速度
	 * @throws Exception	当创建实体在初始化GridSim包之前，或者实体名字是空时，会抛出异常
	 * @see gridsim.GridSim#Init(int, Calendar, boolean, String[], String[],
     *          String)
	 */
	public Test(String name, double baudRate) throws Exception {
		/*
		 * 如果baud_rate是已知的，则不需要创建Input和Output实体。
		 * GridSim将在调用super()时创建。
		 */
		super(name, baudRate);
		System.out.println("...正在创建一个新的测试类实体");
	}
	
	/**
	 * 一次处理一个事件。从另一个GridSim实体接收一个网格任务对象。
	 * 更改网格任务的状态，然后将其回传给发送者。
	 */
	@Override
	public void body() {
		int entityID;
		Sim_event ev=new Sim_event();
		Gridlet gridlet;
		
		//一次处理一个事件
		for(sim_get_next(ev);ev.get_tag()!=GridSimTags.END_OF_SIMULATION;
				sim_get_next(ev)){
			//得到从Test3得到的网格任务对象
			gridlet=(Gridlet) ev.get_data();
			
			//更改网格任务的状态，意味着该网格任务已经被成功接收
			try {
				gridlet.setGridletStatus(Gridlet.SUCCESS);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			System.out.println("...Test类中的body()方法=>从Test3对象接收到ID为"+gridlet.getGridletID()+"的网格任务");
			
			//得到发送者ID，例如Test3类
			entityID=ev.get_src();
			
			//将更改后的网格任务发回给发送者
			super.send(entityID, GridSimTags.SCHEDULE_NOW, GridSimTags.GRIDLET_RETURN, gridlet);
		}
		
		//当仿真结束，结束输入输出实体
		super.terminateIOEntities();
	}
	
}























