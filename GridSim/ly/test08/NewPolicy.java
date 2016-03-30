package test08;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;
import gridsim.AllocPolicy;
import gridsim.GridSimTags;
import gridsim.Gridlet;

/**
 * 该类必须继承AllocPolicy类，并实现5个抽象方法。
 * 在本例中，将展示如何与一个提交到网格资源的新网格任务取得联系
 */
public class NewPolicy extends AllocPolicy{

	/**
	 * 包含资源名和实体名的构造方法
	 * @param resName
	 * @param entityName
	 * @throws Exception
	 */
	protected NewPolicy(String resName, String entityName) throws Exception {
		//必须传递回父类
		super(resName, entityName);
		System.out.println("正在创建 "+entityName);
	}

	/**
	 * 如果来了一个网格任务，然后将其状态改为SUCCESS。
	 * 然后如果有需要则传递一个ack。然后将网格任务对象传回给发送者。
	 */
	@Override
	public void gridletSubmit(Gridlet gl, boolean ack) {
		System.out.println();
		System.out.println("NewPolicy.gridletSubmit():正在运行....");
		System.out.println("正在接收网格任务#"+gl.getGridletID());
		
		try {
			gl.setGridletStatus(Gridlet.SUCCESS);
		} catch (Exception e) {
			// ...忽略
		}
		
		//如果需要回复ack
		if(ack==true){
			System.out.println("NewPolicy.gridletSubmit():传回一个ack");
			
			//传回一个ack，说明该操作成功完成
			super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, true, gl.getGridletID(), gl.getUserID());
		}
		
		System.out.println("NewPolicy.gridletSubmit():传回任务#"+gl.getGridletID()+"到用户#"+gl.getUserID());
		
		//将网格任务对象传回给它的用户（拥有者）
		super.sendFinishGridlet(gl);
	}

	@Override
	public void gridletCancel(int gridletId, int userId) {
		//...自己的代码实现该功能
	}

	@Override
	public void gridletPause(int gridletId, int userId, boolean ack) {
		//...自己的代码实现该功能
	}

	@Override
	public void gridletResume(int gridletId, int userId, boolean ack) {
		//...自己的代码实现该功能
	}

	@Override
	public int gridletStatus(int gridletId, int userId) {
		//...自己的代码实现该功能
		return 1;
	}

	@Override
	public void gridletMove(int gridletId, int userId, int destId, boolean ack) {
		//...自己的代码实现该功能
	}

	/**
	 * 该方法的主要意图是处理内部事件，例如：
	 * 被发送到其自身同一个实体的事件。
	 * 它主要扮演的是一个时间保持者的角色，因为GridSim是一个离散事件的模拟器
	 */
	@Override
	public void body() {
		//只寻找内部事件的循环
		Sim_event ev=new Sim_event();
		while(Sim_system.running()){
			super.sim_get_next(ev);
			
			//如果仿真结束则跳出循环
			if(ev.get_tag()==GridSimTags.END_OF_SIMULATION||super.isEndSimulation()==true){
				break;
			}
		}
		
		//检查确认是否有待处理的内部事件
		while(super.sim_waiting()>0){
			//等待事件并忽略。
			//因为更新网格任务处理进程可能与内部事件调度有关
			super.sim_get_next(ev);
			System.out.println(super.resName_+".NewPolicy.body():忽略内部事件");
		}
	}
	
}

















