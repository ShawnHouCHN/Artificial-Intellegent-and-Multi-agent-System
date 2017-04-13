package sampleclients;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import sampleclients.Heuristic.AStar;
import sampleclients.RandomWalkClient;
import sampleclients.Strategy.StrategyBestFirst;

//agent class

/*****************************************************************************/
public class Agent {
	Box currentBox;
	Goal currentGoal;
	char id;
	String color;
	int[] location;
	public HashMap<Point, Goal > myGoals = new HashMap<Point, Goal >();
	public HashMap<Point, Box > myBoxes = new HashMap<Point, Box >();
	public LinkedList<Command> plan=new LinkedList<Command>();
	public LinkedList<Node> solution=new LinkedList<Node>();
	public Strategy strategy;
	public Node initialState;
	
	public Agent( char id, String color , int[] location) {
		System.err.println("Found " + color + " agent " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
	}

	public String act() {
		
		
		//continued NoOp action to wait until other agents finish
		if (this.solution.size()==0)
			return "NoOp";
		//test end
		System.err.println(this.id+" action is !! "+this.solution.peek().action.toString());
		return this.solution.poll().action.toString();
	}

	public void findMyGoals(List< Goal > goals){
		for (int i = 0; i < goals.size(); i++){
			Goal a_goal=goals.get(i);
			int agent2goal=RandomWalkClient.level_grid.getBFSDistance(a_goal.location,this.location);
			if(a_goal.color.equals(this.color) && agent2goal!=Integer.MAX_VALUE && !a_goal.claimed){
				for(Box mybox:myBoxes.values()){
					if(Character.toLowerCase(mybox.id)==a_goal.id){
						myGoals.put(new Point(a_goal.location[0],a_goal.location[1]),a_goal);
						a_goal.claimed=true;
						
					}
				}
			}
		}
	}
	public void findMyBoxes(List< Box > boxes, HashMap<String,ArrayList<Agent>> color_agents){
		ArrayList<Agent> same_color_agents=color_agents.get(this.color);
		for (int i = 0; i < boxes.size(); i++){
			Box a_box=boxes.get(i);
			int agent2box=RandomWalkClient.level_grid.getBFSDistance(a_box.location,this.location);
			if(a_box.color.equals(this.color) && agent2box!=Integer.MAX_VALUE && !a_box.claimed){
				//TEST LOGIC, Load Balancer logic
				int closest_agent=Integer.MAX_VALUE;
				for (Agent same_color_agent: same_color_agents){
					int other_agent2box=RandomWalkClient.level_grid.getBFSDistance(a_box.location,same_color_agent.location);
					if(other_agent2box<closest_agent)
						closest_agent=other_agent2box;
				}
				if (closest_agent==agent2box){
					myBoxes.put(new Point(a_box.location[0],a_box.location[1]), a_box);
					a_box.claimed=true;
				}
			}
		}
	}
	public String printMyBoxes(){
		String s="";
		for(Box mybox : myBoxes.values()){
			s+=mybox.id+"\n";
		}
		System.err.println("my ID: "+id+" my boxes: "+s);
		return s;
	}
	public String printMyGoals(){
		String s="";
		for(Goal mygoal : myGoals.values()){
			s+=mygoal.id+"\n";
		}
		System.err.println("my ID: "+id+" my goals: "+s);
		return s;
	} 
	
	
	
	public int createPlan(){ 
		System.err.println("MY ID is: "+this.id);
		System.err.println("MY BOX KEYS:"+myBoxes.keySet().toString());
		System.err.println("MY GOAL KEYS:"+myGoals.keySet().toString());
		
		while(!myBoxes.keySet().containsAll(myGoals.keySet()))
		{
			//System.err.println("MY BOX VALUES:"+myBoxes.values().toString());
			int currentDist=Integer.MAX_VALUE;
			for (Goal a_goal : myGoals.values()){
				
				Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
				if(myBoxes.containsKey(a_goal_loc)){
					continue;
				}
				
				for(Box a_box : myBoxes.values()){
					
					Point a_box_loc = new Point(a_box.location[0],a_box.location[1]);
					if(myGoals.containsKey(a_box_loc)){
						continue;
					}					
					
					int dist1= RandomWalkClient.level_grid.getBFSDistance(a_box.location,a_goal.location);
					int dist2= RandomWalkClient.level_grid.getBFSDistance(a_box.location,this.location);
					if(dist1!=0 && dist1+dist2 < currentDist && Character.toUpperCase(a_goal.id)==a_box.id){
						currentDist = dist1+dist2;
						currentGoal = a_goal;
						currentBox = a_box;
					}
					
				}
			
			}
			//System.err.println("Agent at: "+this.location[0]+","+this.location[1]+" The box is"+currentBox.location[0]+","+currentBox.location[1]+"; Goal is "+currentGoal.location[0]+","+currentGoal.location[1]);
			this.initialState = new Node(null,RandomWalkClient.all_walls, myBoxes,myGoals,currentBox,currentGoal, this.location);
			this.strategy= new StrategyBestFirst(new AStar(initialState));
			LinkedList<Node> singlesolution=new LinkedList<Node>();
			
			try {			
				singlesolution=SearchSingle(strategy);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("Exception happened, solution cannot be found!!");
				e.printStackTrace();
			}
			this.solution.addAll(singlesolution);
		}
		return this.solution.size();
		
		
		
		
		
		//Old logic using A*//
//		this.initialState= new Node(null,RandomWalkClient.all_walls, myBoxes,myGoals,this.location);
//		strategy= new StrategyBestFirst(new AStar(initialState));
//		try {
//			this.solution=Search(strategy);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			System.err.println("Exception happened, solution cannot be found!!");
//			e.printStackTrace();
//		}
//		
//
//		return this.solution.size();
	}

	
	//Astar logic
	public LinkedList<Node> Search(Strategy strategy) throws IOException {
		System.err.format("Search starting with strategy %s.\n", strategy.toString());
		strategy.addToFrontier(this.initialState);
		
		int iterations = 0;
		while (true) {
            if (iterations == 1000) {
				System.err.println(strategy.searchStatus());
				iterations = 0;
			}

			if (strategy.frontierIsEmpty()) {
				return null;
			}

			Node leafNode = strategy.getAndRemoveLeaf();
			if (leafNode.isGoalState()) {
				return leafNode.extractPlan();
			}

			strategy.addToExplored(leafNode);
			for (Node n : leafNode.getExpandedNodes()) { // The list of expanded nodes is shuffled randomly; see Node.java.
				if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {					
					strategy.addToFrontier(n);
				}
			}
			iterations++;
		}
	}
	
	//Single Astar logic
	public LinkedList<Node> SearchSingle(Strategy strategy) throws IOException {
		System.err.format("Search starting with strategy Single %s.\n", strategy.toString());
		strategy.addToFrontier(this.initialState);
		
		int iterations = 0;
		while (true) {
            if (iterations == 1000) {
				System.err.println(strategy.searchStatus());
				iterations = 0;
			}

			if (strategy.frontierIsEmpty()) {
				System.err.println("Frontier is Empty Now. Weird?");
				return null;
			}

			Node leafNode = strategy.getAndRemoveLeaf();
			if (leafNode.isSingleGoalState()) {
				this.location=new int[]{leafNode.agentRow,leafNode.agentCol};
				this.myBoxes=leafNode.boxes;
				//update box locations for next subplan
				for (Point box_loc : this.myBoxes.keySet())
					this.myBoxes.get(box_loc).location=new int[]{box_loc.x,box_loc.y};
				
				return leafNode.extractPlan();
			}

			strategy.addToExplored(leafNode);
			for (Node n : leafNode.getExpandedNodes()) { // The list of expanded nodes is shuffled randomly; see Node.java.
				if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {					
					strategy.addToFrontier(n);
				}
			}
			iterations++;
		}
	}

}

/*****************************************************************************/

//goal class
class Goal{
	char id;
	String color; //kept for finding links between box and goal
	int[] location;
	boolean claimed;
	public Goal( char id, String color , int[] location) {
		System.err.println("Found " + color + " Goal " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
		this.claimed=false;
	}
	@Override
	public boolean equals(Object o) {
		Goal c = (Goal) o;
        return c.id == id && c.color == color && Arrays.equals(c.location, location);
    }
    
	@Override
    public int hashCode() {
    	return ((location[0] + location[1])*(location[0] + location[1] + 1))/2 + location[1];
    }
	@Override
    public String toString(){
    	return location[0]+","+location[1];
    }
}

/*****************************************************************************/	
//box class

class Box{
	char id;
	String color;
	int[] location;
	boolean claimed;
	public Box( char id, String color , int[] location) {
		//System.err.println("Found " + color + " Box " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
		this.claimed=false;
	}
	
    
	@Override
	public boolean equals(Object o) {
		Box c = (Box) o;
        return c.id == id && c.color == color && Arrays.equals(c.location, location);
    }
    
	@Override
    public int hashCode() {
    	return ((location[0] + location[1])*(location[0] + location[1] + 1))/2 + location[1];
    }
	 
	@Override
    public String toString(){
    	return location[0]+","+location[1];
    }
}
