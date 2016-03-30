package test09;

import eduni.simjava.Sim_event;
import gridsim.GridInformationService;
import gridsim.GridSim;
import gridsim.GridSimTags;

/**
 * 一个新的网格信息服务（GIS）实体
 * 尽管GridSim拥有自己的GIS实体，但是你可能会想
 * 向实体添加一些新功能。以下是在不改变已存在的GIS实体基础上
 * 的需要的步骤：
 * - 创建一个新类继承自gridsim.GridInformationService类
 * - 重写processOtherEvent（）方法来处理新标签
 *   注意：确保标签值与已存在的GridSim标签不同，因为该方法会被最后调用
 *   
 * 运行该实体：
 * - 在主方法中，使用初始化方法GridSim.init(...)，其中boolean值gis设置为false
 *   注意：gis参数设置为true表示使用默认的或已存在的GIS实体而不是你自己的实体
 * - 在主方法内，创建一个该实体对象，例如：NewGIS gisEntity=new NewGIS("NewGIS");
 * 
 * - 在主方法内，使用GridSim.setGIS(gisEntity)来存储你的GIS实体。
 *   注意：该方法应该在运行仿真之前调用，如在调用GridSim.startGridSimulation()方法前调用
 */
public class NewGIS extends GridInformationService{

	/**
	 * 创建一个新GIS实体
	 * @param name	本来有个两个参数的构造方法，第二个参数是带宽，看样子这里将带宽设为默认值了，所有不需要加带宽参数了
	 * @throws Exception
	 */
	public NewGIS(String name) throws Exception {
		super(name, GridSimTags.DEFAULT_BAUD_RATE);
	}
	
	/**
	 * 重写该方法来实现新标签或新功能
	 * 注意：GIS实体与其他实体之间的通信必须通过I/O端口。更多信息，参看gridsim.GridSimCore API
	 */
	@Override
	protected void processOtherEvent(Sim_event ev) {
		int resID=0;		//发送者ID
		String name=null;	//发送者name
		
		switch(ev.get_tag()){
		case Test9.HELLO:
			resID=((Integer)ev.get_data()).intValue();
			name=GridSim.getEntityName(resID);
			name=GridSim.getEntityName(resID);
			System.out.println(super.get_name()+":从资源"+name+"收到HELLO标签，时间为"+GridSim.clock());
			break;
			
		case Test9.TEST:
			resID=((Integer)ev.get_data()).intValue();
			name=GridSim.getEntityName(resID);
			System.out.println(super.get_name()+":从资源"+name+"收到TEST标签，时间为"+GridSim.clock());
			break;
			
		default:
			break;	
		}
	}

}
















