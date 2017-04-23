package sampleclients;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import sampleclients.Heuristic.AStar;
import sampleclients.RandomWalkClient;
import sampleclients.Strategy.StrategyBestFirst;

//agent class

/*****************************************************************************/
public class Agent {
	Box currentBox; 
	Box currentBoxOff;
	Goal currentGoal;
	char id;
	String color;
	int[] location;
	int[] init_location;
	public HashMap<Point, Goal > myGoals = new HashMap<Point, Goal >();
	public HashMap<Point, Box > myBoxes = new HashMap<Point, Box >();
	public boolean[][] myWalls;
	public HashMap<Point, Box > restBoxes = new HashMap<Point, Box>();
	public HashMap<Integer,Vertex> myGraph;
	public LinkedList<Command> plan=new LinkedList<Command>();
	public LinkedList<Node> solution=new LinkedList<Node>();
	public HashMap<Integer, LinkedList<Node>> subplanboard=new HashMap<Integer, LinkedList<Node>>();
	public Strategy strategy;
	public Node initialState;
	public int esm_sum_dis;
	
	public Agent( char id, String color , int[] location) {
		System.err.println("Found " + color + " agent " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
		this.init_location= new int[]{location[0],location[1]};
		this.esm_sum_dis=0;
	}

	public String act() {
		
		
		//continued NoOp action to wait until other agents finish
		if (this.solution.size()==0)
			return "NoOp";
		//System.err.println(this.id+" action is !! "+this.solution.getLast().currentBox.toString());
		return this.solution.poll().action.toString();
	}

	public void findMyGoals(List< Goal > goals){
		Iterator<Point> ite_box = this.myBoxes.keySet().iterator();
		while (ite_box.hasNext()){
			Box mybox=this.myBoxes.get(ite_box.next());
			int dis=Integer.MAX_VALUE;
			Goal mygoal=null;
			for (int i = 0; i < goals.size(); i++){
				Goal a_goal=goals.get(i);
				int box2goal=RandomWalkClient.initial_level_grid.getBFSDistance(mybox.location,a_goal.location);
				if(Character.toUpperCase(a_goal.id)==mybox.id && box2goal<=dis && !a_goal.claimed){
					if(box2goal==dis){ //goal same dis to a box
						int agent2lastgoal=RandomWalkClient.initial_level_grid.getBFSDistance(this.location,mygoal.location);
						int agent2nowgoal=RandomWalkClient.initial_level_grid.getBFSDistance(this.location,a_goal.location);
						if(agent2lastgoal<=agent2nowgoal)
							continue;
					}
					dis=box2goal;
					mygoal=a_goal;
				}
			}
			myGoals.put(new Point(mygoal.location[0],mygoal.location[1]),mygoal);
			mygoal.claimed=true;
			esm_sum_dis=esm_sum_dis+dis;
		}
			
	}
	
	public void findMyBoxes(List< Box > boxes, HashMap<String,ArrayList<Agent>> color_agents){
		ArrayList<Agent> same_color_agents=color_agents.get(this.color);
		for (int i = 0; i < boxes.size(); i++){
			Box a_box=boxes.get(i);
			int agent2box=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,this.location);
			if(a_box.color.equals(this.color) && agent2box!=Integer.MAX_VALUE && !a_box.claimed){
				//TEST LOGIC, Load Balancer logic
				int closest_agent=Integer.MAX_VALUE;
				for (Agent same_color_agent: same_color_agents){
					int other_agent2box=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,same_color_agent.location);
					if(other_agent2box<closest_agent)
						closest_agent=other_agent2box;
				}
				if (closest_agent==agent2box){
					myBoxes.put(new Point(a_box.location[0],a_box.location[1]), a_box);
					a_box.claimed=true;
					esm_sum_dis=esm_sum_dis+closest_agent;
				}
			}
			
			if(!myBoxes.containsKey(new Point(a_box.location[0],a_box.location[1])))
			{
				System.err.println("This:"+a_box.toString()+" is a barrier");
				restBoxes.put(new Point(a_box.location[0],a_box.location[1]), a_box);
				this.myGraph.get(a_box.hashCode()).setAgentLock(this.id, true);
			}

		}
//		//TESST!!!!!!
//		for (Vertex currentvertex: this.myGraph.values()){
//			if (currentvertex.getLock()){
//				System.err.println("For agent"+this.id+"Vertex locked:"+currentvertex.toString());
//			}
//		}
	}
	
	public void setInitialWalls(boolean[][] all_walls){
		this.myWalls=new boolean[all_walls.length][all_walls[0].length];
		for(int i=0; i<all_walls.length; i++)
			  for(int j=0; j<all_walls[i].length; j++)
				 this.myWalls[i][j]=all_walls[i][j];
	}
	
	public void setInitialGraph(HashMap<Integer,Vertex> initial_graph){

		this.myGraph= initial_graph; //Reference to global graph
	}
	
	public int findMyGoalFreedom(Goal a_goal){
		int degree=0;
		int goal_x=a_goal.location[0];
		int goal_y=a_goal.location[1];
		if (RandomWalkClient.all_walls[goal_x-1][goal_y])
			degree++;
		if (RandomWalkClient.all_walls[goal_x+1][goal_y])
			degree++;
		if (RandomWalkClient.all_walls[goal_x][goal_y-1])
			degree++;
		if (RandomWalkClient.all_walls[goal_x][goal_y+1])
			degree++;
		return degree;
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
		System.err.println("My boxes "+myBoxes.values().toString());
		System.err.println("My goals "+myGoals.values().toString());
		
		while(!myBoxes.keySet().containsAll(myGoals.keySet()))
		{

			FindNextBoxGoal();
			
			System.err.println("Agent at: "+this.location[0]+","+this.location[1]+" The box is "+currentBox.location[0]+","+currentBox.location[1]+"; Goal is "+currentGoal.location[0]+","+currentGoal.location[1]);
			
			//true == other boxes are walls / false == other boxes are free spaces
			boolean aware_others=true;
			this.initialState = new Node(aware_others, null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,currentBox,currentGoal, this.location, this.id);
			
			
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
				
				//I have to validate the extracted plan here as well. make sure the next plan can continue
				boolean valid=validateCurrentPlan(currentGoal,new int[]{leafNode.agentRow,leafNode.agentCol});
				if(valid)
				{
					this.location=new int[]{leafNode.agentRow,leafNode.agentCol};
					this.myBoxes=leafNode.boxes;
					//update box locations for next subplan
					for (Point box_loc : this.myBoxes.keySet())
						this.myBoxes.get(box_loc).location=new int[]{box_loc.x,box_loc.y};
					
					return leafNode.extractPlan();
				}
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

	//Find next box/goal logic
	public void FindNextBoxGoal() {
			
		TreeMap<Integer,ArrayList<Goal>> unfinished_goals= new TreeMap<Integer,ArrayList<Goal>>();
		TreeMap<Integer,Box> unfinished_boxes= new TreeMap<Integer,Box>();  //there may have box in same distance(arraylist might be used later)
		for (Goal a_goal : myGoals.values()){
			Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
			if(myBoxes.containsKey(a_goal_loc) && Character.toUpperCase(a_goal.id)==myBoxes.get(a_goal_loc).id){
				continue;
			}
			int agent2goal=RandomWalkClient.initial_level_grid.getBFSDistance(a_goal.location,this.location);
			if(!unfinished_goals.containsKey(agent2goal))
				unfinished_goals.put(agent2goal, new ArrayList<Goal>());
			unfinished_goals.get(agent2goal).add(a_goal);
		}
		
		
		this.currentGoal=null;
		this.currentGoal=unfinished_goals.firstEntry().getValue().get(0); //if no next target goal is valid based on the hueristic below in "validateNextGoal"
		//System.err.println("Default is "+unfinished_goals.firstEntry().getValue().toString()+ " The distance:"+unfinished_goals.firstEntry().getKey());
		while(unfinished_goals.keySet().size()!=0){
			ArrayList<Goal> closest_goals=unfinished_goals.pollFirstEntry().getValue();
			boolean valid=true;
			for (Goal closest_goal : closest_goals){
				
				System.err.println("Inspect "+closest_goal.toString()+" validaity");
				valid=validateNextGoal(closest_goal);
				
				if(valid){
					this.currentGoal=closest_goal;
					System.err.println("Current Goal is "+ this.currentGoal.toString());
					break;
				}
				
			}
			if(valid){
				break;
			}
		}
		
		
		
		int currentDist=Integer.MAX_VALUE;
		for(Box a_box: myBoxes.values()){
			Point a_box_loc = new Point(a_box.location[0],a_box.location[1]);
			if(myGoals.containsKey(a_box_loc) && Character.toLowerCase(a_box.id)==myGoals.get(a_box_loc).id){
				//System.err.println("This box is in place:"+a_box_loc.toString());
				continue;
			}
			int box2goal=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,this.currentGoal.location);
			if(Character.toLowerCase(a_box.id)==this.currentGoal.id && box2goal<=currentDist ){
				currentDist=box2goal;
				this.currentBox=a_box;
			}
		}
		//this.currentBox=unfinished_boxes.pollFirstEntry().getValue();
		
	}
	
	
	public boolean validateNextGoal(Goal closest_goal){
		
		Set<Point> box_locs=new HashSet<Point>(this.myBoxes.keySet());
		this.myGraph.get(closest_goal.hashCode()).setAgentLock(this.id, true);
		boolean valid=true;

		for (Goal a_goal : myGoals.values()){
			//test goal count		
			Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
			if((myBoxes.containsKey(a_goal_loc) && Character.toUpperCase(a_goal.id)==myBoxes.get(a_goal_loc).id) || a_goal.equals(closest_goal) ){
				box_locs.remove(a_goal_loc);
				continue;
			}
			
			int currentDist=Integer.MAX_VALUE;
			Point clsbox2other=null;			
			Iterator<Point> ite_box = box_locs.iterator();
			while(ite_box.hasNext()){
				Point a_box_loc = ite_box.next();
				if(myGoals.containsKey(a_box_loc) && Character.toLowerCase(this.myBoxes.get(a_box_loc).id)==myGoals.get(a_box_loc).id)
					continue;	
				else if(Character.toLowerCase(this.myBoxes.get(a_box_loc).id)==a_goal.id){
					int sudobox2goal=Grid.getBFSPesudoDistance(this.myBoxes.get(a_box_loc).location,a_goal.location,this.myGraph, this.id);
					if (sudobox2goal<currentDist){
						currentDist=sudobox2goal;
						clsbox2other=a_box_loc;
					}
				}
				
			}
			
			//System.err.println("Agent at "+this.location[0]+","+this.location[1]+" locked goal: "+closest_goal.toString()+" inspect another goal "+a_goal.toString()+" and its closest box to itself is "+clsbox2other.toString()+" and the distance between this another goal and this box becomes "+currentDist+" The distance between agent to this box "+ Grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location,this.myGraph));
			//System.err.println("If I choose goal "+closest_goal.toString()+" and other goal:"+a_goal.toString()+ "'s closest box "+clsbox2other.toString()+"distance to its closest goal:"+currentDist);		
			if((Grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location,this.myGraph,this.id)+currentDist)>=Grid.LOCK_THRESHOLD){
				//System.err.println("Box "+clsbox2other.toString()+ "unreach "+RandomWalkClient.initial_level_grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location));
				valid=false;	
				break;
				
			}

			box_locs.remove(clsbox2other);
			
		}
		this.myGraph.get(closest_goal.hashCode()).setAgentLock(this.id, false);
		return valid;
	}
	 
	
	public boolean validateCurrentPlan(Goal closest_goal, int[] agent_location){
		this.myGraph.get(closest_goal.hashCode()).setLock(true);
		boolean valid=true;
		for (Goal a_goal : myGoals.values()){
			Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
			if((myBoxes.containsKey(a_goal_loc) && Character.toUpperCase(a_goal.id)==myBoxes.get(a_goal_loc).id) || a_goal.equals(closest_goal) )
				continue;
			//System.err.println("I inspect "+closest_goal.toString()+" and other goal:"+a_goal.toString()+ " The distance:"+RandomWalkClient.initial_level_grid.getBFSPesudoDistance(a_goal.location,this.location));
			if(RandomWalkClient.initial_level_grid.getBFSPesudoDistance(a_goal.location,agent_location)>=Grid.LOCK_THRESHOLD){
				valid=false;
				break;
			}
		}
		this.myGraph.get(closest_goal.hashCode()).setLock(false);
		return valid;
	}
	
}

/*****************************************************************************/

//goal class
class Goal{
	char id;
	String color; //kept for finding links between box and goal
	int[] location;
	boolean claimed;
	int freedom;
	public Goal( char id, String color , int[] location) {
		System.err.println("Found " + color + " Goal " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
		this.claimed=false;
		this.freedom=4;
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
	int freedom;
	
	public Box( char id, String color , int[] location) {
		//System.err.println("Found " + color + " Box " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
		this.claimed=false;
		this.freedom=4;
	}
	
    
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (this.getClass() != o.getClass())
			return false;
		Box c = (Box) o;
        return c.id == this.id && c.color == this.color && Arrays.equals(c.location, location);
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

