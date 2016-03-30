package test09;

import eduni.simjava.Sim_event;
import gridsim.ARPolicy;
import gridsim.AllocPolicy;
import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.IO_data;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;

/**
 * 创建一个新的网格资源实体。该类通过先注册新标签到GIS实体的方式执行了一个简单的功能
 * 然后从发送者接收一个新标签，该类简单的打印一条消息，说标签已被接收。
 * 
 * 尽管GridSim有自己的网格资源实体，但是你可能会想添加新功能到实体。以下是不需要修改已存在的
 * 网格实体的步骤：
 * - 创建一个新类，继承自gridsim.GridResource类
 * - 重写registerOtherEntity()方法，实现将新标签注册到GIS实体功能
 * - 重写processOtherEvent()方法，实现处理从其他实体传来的新标签
 * 
 * 注意：确保标签值与已存在的GridSim标签值不同，因为该方法将会在最后调用。
 */
public class NewGridResource extends GridResource{

	/**
	 * 创建一个新的网格资源实体。有不同的方法调用父类构造方法。
	 * 在本例中，简单起见，只选择一个方法。
	 */
	public NewGridResource(String name, double baud_rate,
			ResourceCharacteristics resource, ResourceCalendar calendar,
			ARPolicy policy) throws Exception {
		super(name, baud_rate, resource, calendar, policy);
	}
	
	/**
	 * 重写该方法实现新标签或新功能
	 */
	@Override
	protected void processOtherEvent(Sim_event ev) {
		try {
			/*
			 * 得到发送者ID
			 * 注意：Sim_event.get_data()携带的是一个泛型的对象。它可能携带一个Gridlet，String，各种类型的兑现，
			 * 		这取决于发送者。因此，将object类型转换成具体类型时要小心。在本例中，发送者应该发送一个Integer对象。
			 */
			Integer obj=(Integer) ev.get_data();
			
			//得到发送者的name
			String name=GridSim.getEntityName(obj.intValue());
			switch(ev.get_tag()){
			case Test9.HELLO:
				System.out.println(super.get_name()+":从"+name+"收到HELLO标签，时间为"+GridSim.clock());
				break;
				
			case Test9.TEST:
				System.out.println(super.get_name()+":从"+name+"收到TEST标签，时间为"+GridSim.clock());
				break;
				
			default:
				break;
			}
		} catch (Exception e) {
			System.out.println(super.get_name()+".processOtherEvent():发生了异常！");
		}
	}

	/**
	 * 重写该方法来注册新标签到GIS实体。你需要创建一个新GIS实体以便处理你的新标签
	 */
	@Override
	protected void registerOtherEntity() {
		int SIZE=12;
		
		//获取GIS实体ID
		int gisID=GridSim.getGridInfoServiceEntityId();
		
		//获取GIS实体名
		String gisName=GridSim.getEntityName(gisID);
		
		//注册HELLO标签到GIS实体
		System.out.println(super.get_name()+".registerOtherEntity():注册HELLO标签到GIS对象"
				+gisName+"，时间为"+GridSim.clock());
		
		super.send(super.output, GridSimTags.SCHEDULE_NOW, Test9.HELLO, 
				new IO_data(new Integer(super.get_id()), SIZE, gisID));
		
		//注册HELLO标签到GIS实体
		System.out.println(super.get_name()+".registerOtherEntity():注册TEST标签到GIS对象"
				+gisName+"，时间为"+GridSim.clock());
		
		super.send(super.output, GridSimTags.SCHEDULE_NOW, Test9.TEST, 
				new IO_data(new Integer(super.get_id()), SIZE, gisID));
	}
}




















