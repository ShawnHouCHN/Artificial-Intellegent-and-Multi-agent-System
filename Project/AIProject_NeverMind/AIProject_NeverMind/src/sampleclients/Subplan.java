package sampleclients;

import java.awt.Point;
import java.util.LinkedList;

public class Subplan {
	
	
	public Box target_box;
	public Goal target_goal;
	public LinkedList<Node> sub_solution;
	public LinkedList<Point> sub_agent_plan;
	public LinkedList<Command> sub_plan;
	public int[] agent_start_loc;
	public int[] agent_end_loc;
	public Subplan(Box target_box, Goal target_goal, LinkedList<Node> sub_solution, LinkedList<Point> sub_agent_plan, LinkedList<Command> sub_plan ){
		this.target_box=target_box;
		this.target_goal=target_goal;
		this.sub_solution=sub_solution;
		this.sub_agent_plan=sub_agent_plan;
		this.sub_plan=sub_plan;
		//this.agent_start_loc=agent_start_loc;
		//this.agent_end_loc=agent_end_loc;
		
	}
	
	public String toString(){
		return this.sub_plan.toString();
	}
	
}
