package sampleclients;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	Box secondaryBox;
	Goal currentGoal;
	Goal secondaryGoal;
	char id;
	String color;
	int[] location;
	public int[] init_location;
	public HashMap<Point, Goal > myGoals = new HashMap<Point, Goal >();
	public HashMap<Point, Box > myBoxes = new HashMap<Point, Box >();
	public HashMap<Point, Box > myInitBoxes = new HashMap<Point, Box >();
	public boolean[][] myWalls;
	public boolean[][] realfrees;
	public HashMap<Point, Box > restBoxes = new HashMap<Point, Box>();
	public HashMap<Point, Goal > restGoals = new HashMap<Point, Goal>();
	public HashMap<Integer,Vertex> myGraph;
	public LinkedList<Command> plan=new LinkedList<Command>();
	public LinkedList<Point> agent_plan=new LinkedList<Point>();
	public LinkedList<int[]> agent_start_plan=new LinkedList<int[]>();
	public LinkedList<Boolean> agent_rescue_plan=new LinkedList<Boolean>();
	public LinkedList<Node> solution=new LinkedList<Node>();
	public HashMap<Integer, Subplan> subplanboard=new HashMap<Integer, Subplan>();
	public Strategy strategy;
	public Node initialState;
	public int esm_sum_dis;
	public int plan_mode; //sa planning mode ()
	public static final int SAFE_MODE = 0;
	public static final int SAFE_SECONDAEY_MODE = 1;
	public static final int CLOSEST_MODE = 2;
	public static final int MA_SAFE_MODE = 3;
	public static final int MA_SECONDAEY_RISK_MODE = 4;
	public static final int MA_RISK_MODE = 5;
	public static final int EMPTY_ACTION_THRESHOLD = 100;
	
	public Agent( char id, String color , int[] location) {
		//System.err.println("Found " + color + " agent " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
		this.init_location= new int[]{location[0],location[1]};
		this.esm_sum_dis=0;
		this.plan_mode=0;
	}

	public String act() {
		
		
		//continued NoOp action to wait until other agents finish
		if (this.plan.size()==0)
			return "NoOp";
		////System.err.println(this.id+" action is !! "+this.solution.getLast().currentBox.toString());
		return this.plan.poll().toString();
		//return this.solution.poll().action.toString();
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
				if(Character.toUpperCase(a_goal.id)==mybox.id && box2goal<dis && !a_goal.claimed){
					dis=box2goal;
					mygoal=a_goal;
				}
			}
			if(mygoal==null)
				continue;
			myGoals.put(new Point(mygoal.location[0],mygoal.location[1]),mygoal);
			mygoal.claimed=true;
			esm_sum_dis=esm_sum_dis+dis;
		} 
		
		for(Goal other_goal: goals){
			if(!myGoals.containsKey(new Point(other_goal.location[0],other_goal.location[1]))){
				restGoals.put(new Point(other_goal.location[0],other_goal.location[1]), other_goal);
			}
		}
			
	}
	
	public void findAllBoxesGoals(List< Box > boxes,List< Goal > goals){
		HashMap<int[], Integer> dis_map=new HashMap<int[], Integer>();
		
		for(Goal a_goal: goals){
			Box closed_box=null;
			int goal2box=Integer.MAX_VALUE;
			for (Box a_box: boxes){		
				if(!a_goal.claimed && Character.toUpperCase(a_goal.id)==a_box.id && !myBoxes.keySet().contains(new Point(a_box.location[0],a_box.location[1]))){
					int dis=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,a_goal.location);
					if(dis<goal2box){
						closed_box=a_box;
						goal2box=dis;
					}
				}
			}
			closed_box.zombie=false;
			closed_box.claimed=true;
			a_goal.claimed=true;
			myGoals.put(new Point(a_goal.location[0],a_goal.location[1]),a_goal);
			myBoxes.put(new Point(closed_box.location[0],closed_box.location[1]), closed_box);	
			//System.err.println("To Goal:"+a_goal.id+a_goal+" box : "+closed_box);
		}
		
		for (Box a_box: boxes){
			if(!myBoxes.keySet().contains(new Point(a_box.location[0],a_box.location[1]))){
				a_box.zombie=true;
				a_box.claimed=true;
				myBoxes.put(new Point(a_box.location[0],a_box.location[1]), a_box);	
			}
		}
		
		
//		for (Box a_box: boxes){
//			boolean zombie=true;
//			for(Goal a_goal: goals){
//				myGoals.put(new Point(a_goal.location[0],a_goal.location[1]),a_goal);			
//				if(a_goal.claimed==false && Character.toUpperCase(a_goal.id)==a_box.id){
//					a_goal.claimed=true;
//					zombie=false;
//					break;
//				}
//				
//			}
//			a_box.zombie=zombie;
//			myBoxes.put(new Point(a_box.location[0],a_box.location[1]), a_box);	
//		}
	}
	
	
	public void findMyBoxes(List< Box > boxes, HashMap<String,ArrayList<Agent>> color_agents){
		ArrayList<Agent> same_color_agents=color_agents.get(this.color);
		for (int i = 0; i < boxes.size(); i++){
			Box a_box=boxes.get(i);
			Box copy_box=new Box(a_box.id,a_box.color,a_box.location);
			int agent2box=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,this.location);
			////System.err.println("Agent to box:"+a_box.id+a_box.color+" dis: "+agent2box);
			if(a_box.color!=null && a_box.color.equals(this.color) && agent2box!=Integer.MAX_VALUE && !a_box.claimed){
				//TEST LOGIC, Load Balancer logic
				int closest_agent=Integer.MAX_VALUE;
				for (Agent same_color_agent: same_color_agents){
					int other_agent2box=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,same_color_agent.location);
					if(other_agent2box<closest_agent)
						closest_agent=other_agent2box;
				}
				if (closest_agent==agent2box){
					myBoxes.put(new Point(a_box.location[0],a_box.location[1]), a_box);
					myInitBoxes.put(new Point(copy_box.location[0],copy_box.location[1]), copy_box);
					a_box.claimed=true;
					copy_box.claimed=true;
					esm_sum_dis=esm_sum_dis+closest_agent;
					//System.err.println("For agent "+this.id+","+a_box.id+a_box.color+" dis: "+agent2box);
				}
			}
			
			if(!myBoxes.containsKey(new Point(a_box.location[0],a_box.location[1])))
			{
				////System.err.println("For agent "+this.id+" This:"+a_box.toString()+" is a barrier");
				restBoxes.put(new Point(a_box.location[0],a_box.location[1]), a_box);
				this.myGraph.get(a_box.hashCode()).setAgentLock(this.id, true);
			}

		}

	}
	
	public void setInitialWalls(boolean[][] all_walls){
		this.myWalls=new boolean[all_walls.length][all_walls[0].length];
		for(int i=0; i<all_walls.length; i++)
			  for(int j=0; j<all_walls[i].length; j++)
				 this.myWalls[i][j]=all_walls[i][j];
	}
	
	public void setInitialFrees(boolean[][] real_frees){
		this.realfrees=real_frees;
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
		//System.err.println("my ID: "+id+" my boxes: "+s);
		return s;
	}
	public String printMyGoals(){
		String s="";
		for(Goal mygoal : myGoals.values()){
			s+=mygoal.id+"\n";
		}
		//System.err.println("my ID: "+id+" my goals: "+s);
		return s;
	} 
	
//	public void printZombieBoxes(){
//		for(Point box_loc: myBoxes.keySet()){
//			if(myBoxes.get(box_loc).zombie)
//				//System.err.println("Zombie at: "+box_loc);
//		}
//	}

	public int createInitSAPlan(){ 
		//System.err.println("MY ID is: "+this.id);
		//System.err.println("My boxes "+myBoxes.values().toString());
		//System.err.println("My goals "+myGoals.values().toString());
		boolean sa_mode=true;
		while(!myBoxes.keySet().containsAll(myGoals.keySet()))
		{
			
			
			findNextSafeBoxGoal(sa_mode);
			
			System.err.println("Plan A: find next safe target. Agent at: "+this.location[0]+","+this.location[1]+" The box is "+currentBox.id+" "+currentBox.location[0]+","+currentBox.location[1]+"; Goal is "+currentGoal.location[0]+","+currentGoal.location[1]);
			//true == other boxes are walls / false == other boxes are free spaces
			boolean aware_others=false;
			boolean aware_sa=true;
			this.plan_mode=SAFE_MODE;
			
			this.initialState = new Node(aware_others, aware_sa, null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,currentBox,currentGoal, this.location, this.id);		
			this.strategy= new StrategyBestFirst(new AStar(initialState));
			//Node singlesolution=new LinkedList<Node>();
			
			try {			
				Node singlesolutionA=SearchSingle(strategy);
				if (singlesolutionA==null){
					
					this.plan_mode=SAFE_SECONDAEY_MODE;
						
					////System.err.println("Plan B: find next secondary safe target. Agent at: "+this.location[0]+","+this.location[1]+" The box is "+secondaryBox.id+" "+secondaryBox.location[0]+","+secondaryBox.location[1]+"; Goal is "+secondaryGoal.location[0]+","+secondaryGoal.location[1]);
					this.initialState = new Node(aware_others, aware_sa, null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,secondaryBox,secondaryGoal, this.location, this.id);
					this.strategy= new StrategyBestFirst(new AStar(initialState));
					Node singlesolutionB=SearchSingle(strategy);	
					
					if(singlesolutionB==null){
						this.plan_mode= CLOSEST_MODE;
						findNextClosestBoxGoal();
						//System.err.println("Plan C: find next closest target. Agent at: "+this.location[0]+","+this.location[1]+" The box is  "+currentBox.id+" "+currentBox.location[0]+","+currentBox.location[1]+"; Goal is "+currentGoal.location[0]+","+currentGoal.location[1]);
						this.initialState = new Node(aware_others, aware_sa, null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,currentBox,currentGoal, this.location, this.id);
						this.strategy= new StrategyBestFirst(new AStar(initialState));
						Node singlesolutionC=SearchSingle(strategy);
					}
					
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//System.err.println("Exception happened, solution cannot be found!!");
				e.printStackTrace();
			}
			//this.solution.addAll(singlesolution);
		}
		return this.solution.size();
		
	}
	
	//IF THE AGENT HAS NO GOAL OR NO GOAL
	public int createEmptyPlan(int length, int index){
		if(index==0){
			length=EMPTY_ACTION_THRESHOLD;
			for(int i=0;i<length;i++){
				this.plan.addFirst(new Command());							
				this.agent_plan.addFirst(new Point(this.location[0],this.location[1]));
				boolean aware_others=true;
				boolean aware_sa=false;
				Node a_node = new Node(aware_others, aware_sa, null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,currentBox,currentGoal, this.location, this.id);
				this.solution.addFirst(a_node);	
				this.agent_start_plan.addFirst(this.location);	
				this.agent_rescue_plan.addFirst(false);										
			}
			return this.solution.size();
		}else{
			for(int i=0;i<length;i++){
				this.plan.add(index, new Command());							
				this.agent_plan.add(index,this.agent_plan.get(index-1));
				this.solution.add(index,this.solution.get(index-1));	
				this.agent_start_plan.add(index,this.agent_start_plan.get(index-1));	
				this.agent_rescue_plan.add(index,this.agent_rescue_plan.get(index-1));									
			}
			return this.solution.size();			
		}
	}
	
	public int createInitPlan(){ 
		//System.err.println("MY ID is: "+this.id);
		//System.err.println("My boxes "+myBoxes.values().toString());
		//System.err.println("My goals "+myGoals.values().toString());
		boolean sa_mode=false;
		int subplan_key=0;
		while(!myBoxes.keySet().containsAll(myGoals.keySet()))
		{

			findNextSafeBoxGoal(sa_mode);
			
			//System.err.println("Agent at: "+this.location[0]+","+this.location[1]+" The box is "+currentBox.location[0]+","+currentBox.location[1]+"; Goal is "+currentGoal.location[0]+","+currentGoal.location[1]);
			Box init_box=new Box(currentBox.id,currentBox.color,currentBox.location);
			//true == other boxes are walls / false == other boxes are free spaces
			boolean aware_others=true;
			boolean aware_sa=true; //TEST!!!!!
			this.initialState = new Node(aware_others, aware_sa, null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,currentBox,currentGoal, this.location, this.id);
			
			
			this.strategy= new StrategyBestFirst(new AStar(initialState));
			//Node singlesolution=new LinkedList<Node>();
			
			try {	
				this.plan_mode=MA_SAFE_MODE;
				Node singlesolution=SearchSingle(strategy);
				if(singlesolution==null)
				{
					//System.err.println("Current goal or box is unreachable if seeing other boxes as walls PLAN B");
					this.plan_mode=MA_SECONDAEY_RISK_MODE;
					aware_others=true;
					aware_sa=true;
					this.initialState = new Node(aware_others, aware_sa, null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,currentBox,currentGoal, this.location, this.id);				
					this.strategy= new StrategyBestFirst(new AStar(initialState));
					singlesolution=SearchSingle(strategy);
					if (singlesolution==null){
						//System.err.println("Current goal or box is unreachable if seeing other boxes as walls PLAN C(Seeing others as passable walls)");
						aware_others=false;
						aware_sa=true;
						this.plan_mode=MA_RISK_MODE;
						this.initialState = new Node(aware_others, aware_sa, null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,currentBox,currentGoal, this.location, this.id);				
						this.strategy= new StrategyBestFirst(new AStar(initialState));
						singlesolution=SearchSingle(strategy);
					}
				}
				//this.subplanboard.put(subplan_key, new Subplan(init_box,currentGoal,singlesolution.solution_plan,singlesolution.agent_plan,singlesolution.action_plan));
				//subplan_key++;
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//System.err.println("Exception happened, solution cannot be found!!");
				e.printStackTrace();
			}
			//this.solution.addAll(singlesolution);
		}
		return this.solution.size();
		
	}
	
	
	
	public LinkedList<Node> createDetourAltPlan(){
		//System.err.println("Detour rest MY ID is: "+this.id);
		//System.err.println("Detour rest My boxes "+myBoxes.values().toString());
		//System.err.println("Detour rest My goals "+myGoals.values().toString());
		boolean sa_mode=false;
		while(!myBoxes.keySet().containsAll(myGoals.keySet()))
		{

			findNextSafeBoxGoal(sa_mode);
			
			//System.err.println("Agent at: "+this.location[0]+","+this.location[1]+" The box is "+currentBox.location[0]+","+currentBox.location[1]+"; Goal is "+currentGoal.location[0]+","+currentGoal.location[1]);
			
			//true == other boxes are walls / false == other boxes are free spaces
			boolean aware_others=true;
			boolean aware_sa=false;
			this.initialState = new Node(aware_others, aware_sa,null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,currentBox,currentGoal, this.location, this.id);
			
			
			this.strategy= new StrategyBestFirst(new AStar(initialState));
			LinkedList<Node> singlesolution=new LinkedList<Node>();
			
			try {			
				singlesolution=SearchDetourRestSingle(strategy);
				if(singlesolution==null){
					aware_others=false;
					this.initialState = new Node(aware_others, aware_sa,null,RandomWalkClient.all_walls, myBoxes,myGoals,myGraph,currentBox,currentGoal, this.location, this.id);
								
					this.strategy= new StrategyBestFirst(new AStar(initialState));	
					singlesolution=SearchDetourRestSingle(strategy);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//System.err.println("Exception happened, solution cannot be found!!");
				e.printStackTrace();
			}
			//this.solution.addAll(singlesolution);
		}
		return this.solution;
	}
	
	public LinkedList<Node> createDetourPlan(Agent refer_agent, int index){
		

		HashMap<Point, Box > refersInitBoxes = refer_agent.myInitBoxes;
		HashMap<Point, Box > refersCurrentBoxes;
		Point curr_agent;
		if (index>=refer_agent.solution.size()){
			refersCurrentBoxes = refer_agent.solution.getLast().boxes;
			curr_agent=refer_agent.agent_plan.getLast();
		}else{
			refersCurrentBoxes = refer_agent.solution.get(index).boxes;
			curr_agent=refer_agent.agent_plan.get(index);
		}
		Set<Point> initbox_loc=refersInitBoxes.keySet();
		Set<Point> currbox_loc=refersCurrentBoxes.keySet();
		Set<Point> intersection = new HashSet<Point>(initbox_loc); // use the copy constructor
		intersection.retainAll(currbox_loc);
		for (Point init_box: initbox_loc){
			if (!intersection.contains(init_box)){
				int key=((init_box.x+ init_box.y)*(init_box.x+ init_box.y + 1))/2 + init_box.y;
				this.myGraph.get(key).setAgentLock(this.id, false);
				////System.err.println("Unset lock at :"+init_box.toString());
			}
		}

		for (Point curr_box: currbox_loc){
			if (!intersection.contains(curr_box)){
				int key=((curr_box.x+ curr_box.y)*(curr_box.x+ curr_box.y + 1))/2 + curr_box.y;
				this.myGraph.get(key).setAgentLock(this.id, true);
				////System.err.println("Set lock at :"+curr_box.toString());
			}
		}
		
		
		//also has to add the refer's agent location a lock as it is not passable.
		
		int agent_key=((curr_agent.x+ curr_agent.y)*(curr_agent.x+ curr_agent.y + 1))/2 + curr_agent.y;
		this.myGraph.get(agent_key).setAgentLock(this.id, true);
		
		
		boolean aware_others=true;
		boolean aware_sa=false;
		Node detourState=null;
		if (index!=0){
			int index_ahead=index-1; //one step back
			detourState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
				this.solution.get(index_ahead).boxes,myGoals,myGraph,
				this.solution.get(index_ahead).currentBox,
				this.solution.get(index_ahead).currentGoal, new int[]{this.agent_plan.get(index_ahead).x,this.agent_plan.get(index_ahead).y}, this.id);
			//System.err.println("Detour computation from index:"+index_ahead+ " to replace rest plan from "+index);
		}else{
			detourState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
					this.myInitBoxes,myGoals,myGraph,
					this.solution.get(index).currentBox,
					this.solution.get(index).currentGoal, this.init_location, this.id);
			//System.err.println("Detour computation at inital state");
		}
		this.strategy= new StrategyBestFirst(new AStar(detourState));
		LinkedList<Node> singlesolution=new LinkedList<Node>();
		
		try {			
			singlesolution=SearchDetourSingle(strategy,detourState,index);
			if(singlesolution==null){
				aware_others=true;
				aware_sa=true;
				if (index!=0){
					int index_ahead=index-1; //one step back
					detourState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
						this.solution.get(index_ahead).boxes,myGoals,myGraph,
						this.solution.get(index_ahead).currentBox,
						this.solution.get(index_ahead).currentGoal, new int[]{this.agent_plan.get(index_ahead).x,this.agent_plan.get(index_ahead).y}, this.id);
						//System.err.println("Detour computation from index:"+index_ahead+ " to replace rest plan from "+index);
					}else{
					detourState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
							this.myInitBoxes,myGoals,myGraph,
							this.solution.get(index).currentBox,
							this.solution.get(index).currentGoal, this.init_location, this.id);
					//System.err.println("Detour computation at inital state");
					}
					this.strategy= new StrategyBestFirst(new AStar(detourState));	
					singlesolution=SearchDetourSingle(strategy,detourState,index);
			}
			////System.err.println("Found a detour with length:"+singlesolution.size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//System.err.println("Exception happened, Detour solution logic has errors!!");
			e.printStackTrace();
		}
		return singlesolution;
		
	}
	
	
//	public LinkedList<Node> createSelfMoveEmergencyPlan(Agent refer_agent, int index){
//		HashMap<Point, Box > refersInitBoxes = refer_agent.myInitBoxes;
//		HashMap<Point, Box > refersCurrentBoxes;
//		Point curr_agent, current_block;
//		Box block_box;
//		this.currentBox=null;
//		if (index>=refer_agent.solution.size()){
//			refersCurrentBoxes = refer_agent.solution.getLast().boxes;
//			curr_agent=refer_agent.agent_plan.getLast();
//			//find the blokcing box
//			current_block=refer_agent.agent_plan.getLast();
//			block_box=this.solution.get(index).boxes.get(current_block);
//			if(block_box!=null){
//				block_box.location=new int[]{current_block.x,current_block.y};		
//			}else{
//				block_box=refer_agent.solution.getLast().engagedBox;
//			}
//			
//		}else{
//			refersCurrentBoxes = refer_agent.solution.get(index).boxes;
//			curr_agent=refer_agent.agent_plan.get(index);
//			//find the blokcing box
//			current_block=refer_agent.agent_plan.get(index);
//			block_box=this.solution.get(index).boxes.get(current_block);
//			if(block_box!=null){
//				block_box.location=new int[]{current_block.x,current_block.y};				
//			}else{
//				block_box=refer_agent.solution.get(index).engagedBox;
//			}
//			
//		}
//		////System.err.println("Find a blk box for agent "+refer_agent.agent_plan.get(index));
//		////System.err.println("Find a blk box for agent "+this.solution.get(index).boxes.keySet());
//		Set<Point> initbox_loc=refersInitBoxes.keySet();
//		Set<Point> currbox_loc=refersCurrentBoxes.keySet();
//		Set<Point> intersection = new HashSet<Point>(initbox_loc); // use the copy constructor
//		intersection.retainAll(currbox_loc);
//		for (Point init_box: initbox_loc){
//			if (!intersection.contains(init_box)){
//				int key=((init_box.x+ init_box.y)*(init_box.x+ init_box.y + 1))/2 + init_box.y;
//				this.myGraph.get(key).setAgentLock(this.id, false);
//				////System.err.println("Unset lock at :"+init_box.toString());
//			}
//		}
//
//		for (Point curr_box: currbox_loc){
//			if (!intersection.contains(curr_box)){
//				int key=((curr_box.x+ curr_box.y)*(curr_box.x+ curr_box.y + 1))/2 + curr_box.y;
//				this.myGraph.get(key).setAgentLock(this.id, true);
//				////System.err.println("Set lock at :"+curr_box.toString());
//			}
//		}
//		
//		int agent_key=((curr_agent.x+ curr_agent.y)*(curr_agent.x+ curr_agent.y + 1))/2 + curr_agent.y;
//		this.myGraph.get(agent_key).setAgentLock(this.id, true);
//		
//
//		
//		////System.err.println("Set Current block box at :"+block_box.toString());
//		this.currentGoal=null;
//		//this.location=new int[]{curr_agent.x,curr_agent.y};
//		///find a free destination
//		int closest=Grid.LOCK_THRESHOLD;
//	    for(int i=0; i<this.realfrees.length; i++) {
//	        for(int j=0; j<realfrees[i].length; j++) {
//	            Point free_loc=new Point(i,j);
//	            Point free_n=new Point(i-1,j);
//	            Point free_s=new Point(i+1,j);
//	            Point free_e=new Point(i,j+1);
//	            Point free_w=new Point(i,j-1);
//	            if(realfrees[i][j]  && !refer_agent.agent_plan.contains(free_loc) && !refersCurrentBoxes.keySet().contains(free_loc)){
//	            	if((realfrees[i-1][j] && !refer_agent.agent_plan.contains(free_n) && !refersCurrentBoxes.keySet().contains(free_n)) ||
//	            	   (realfrees[i+1][j] && !refer_agent.agent_plan.contains(free_s) && !refersCurrentBoxes.keySet().contains(free_s)) ||
//	            	   (realfrees[i][j+1] && !refer_agent.agent_plan.contains(free_e) && !refersCurrentBoxes.keySet().contains(free_e)) ||
//	            	   (realfrees[i][j-1] && !refer_agent.agent_plan.contains(free_w) && !refersCurrentBoxes.keySet().contains(free_w))){
//	            		////System.err.println("INspect "+i+","+j);
//	            	   
//	            	   int dis=Grid.getBFSPesudoDistance(new int[]{this.agent_plan.get(index).x,this.agent_plan.get(index).y} , new int[]{i,j},this.myGraph , this.id);
//	            	   if(dis<closest){
//	            		   	closest=dis;
//	            		   	this.currentGoal=new Goal('z', this.color ,new int[]{i,j});
//	            	   		//break;
//	            	   }
//	            	}
//	            
//	            }
//	            		
//	            			
//	            
//	            }
//	        	if(this.currentGoal!=null)
//	        		break;
//	    }
//	    	
//	    
//	    //start creating the plan
//	    boolean aware_others=true;
//		boolean aware_sa=false;
//		Node rescueState=null;
//		int index_ahead=0;
//				
//		
//		if (index!=0){
//			
//			rescueState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
//				this.solution.get(index_ahead).boxes,myGoals,myGraph,
//				this.currentBox,
//				this.currentGoal, new int[]{this.agent_plan.get(index_ahead).x,this.agent_plan.get(index_ahead).y}, this.id);
//			//System.err.println("Emtour AGENT SELF computation from index:"+index_ahead+ " to replace rest plan from "+index);
//		}else{
//			rescueState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
//					this.myInitBoxes,myGoals,myGraph,
//					this.currentBox,
//					this.currentGoal, this.init_location, this.id);
//			//System.err.println("Emtour AGENT SELF computation at inital state");
//		}
//		this.strategy= new StrategyBestFirst(new AStar(rescueState));
//		LinkedList<Node> singlesolution=new LinkedList<Node>();
//		
//		try {			
//			singlesolution=SearchRescueSingle(strategy,rescueState,index,refer_agent);
//			if(singlesolution==null){
//				aware_others=false;
//				if (index!=0){
//					index_ahead=index-1; //one step back
//					rescueState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
//						this.solution.get(index_ahead).boxes,myGoals,myGraph,
//						this.currentBox,
//						this.currentGoal, new int[]{this.agent_plan.get(index_ahead).x,this.agent_plan.get(index_ahead).y}, this.id);
//					//System.err.println("Emtour computation from index:"+index_ahead+ " to replace rest plan from "+index);
//				}else{
//					rescueState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
//							this.myInitBoxes,myGoals,myGraph,
//							this.currentBox,
//							this.currentGoal, this.init_location, this.id);
//					//System.err.println("Emtour computation at inital state");
//				}
//				this.strategy= new StrategyBestFirst(new AStar(rescueState));
//				singlesolution=SearchRescueSingle(strategy,rescueState,index,refer_agent);
//			}
//			////System.err.println("Found a detour with length:"+singlesolution.size());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			//System.err.println("Exception happened, Detour solution logic has errors!!");
//			e.printStackTrace();
//		}
//		return singlesolution;		
//	}
	
	public LinkedList<Node> createEmergencyPlan(Agent refer_agent, int index){
		HashMap<Point, Box > refersInitBoxes = refer_agent.myInitBoxes;
		HashMap<Point, Box > refersCurrentBoxes;
		Point curr_agent, current_block;
		Box block_box;
		this.currentBox=null;
		if (index>=refer_agent.solution.size()){
			refersCurrentBoxes = refer_agent.solution.getLast().boxes;
			curr_agent=refer_agent.agent_plan.getLast();
			//find the blokcing box
			current_block=refer_agent.agent_plan.getLast();
			block_box=this.solution.get(index).boxes.get(current_block);
			if(block_box!=null){
				block_box.location=new int[]{current_block.x,current_block.y};		
			}else{
				block_box=refer_agent.solution.getLast().engagedBox;
			}
			
		}else{
			refersCurrentBoxes = refer_agent.solution.get(index).boxes;
			curr_agent=refer_agent.agent_plan.get(index);
			//find the blokcing box
			current_block=refer_agent.agent_plan.get(index);
			block_box=this.solution.get(index).boxes.get(current_block);
			if(block_box!=null){
				block_box.location=new int[]{current_block.x,current_block.y};				
			}else{
				block_box=refer_agent.solution.get(index).engagedBox;
			}
			
		}
		////System.err.println("Find a blk box for agent "+refer_agent.agent_plan.get(index));
		////System.err.println("Find a blk box for agent "+this.solution.get(index).boxes.keySet());
		Set<Point> initbox_loc=refersInitBoxes.keySet();
		Set<Point> currbox_loc=refersCurrentBoxes.keySet();
		Set<Point> intersection = new HashSet<Point>(initbox_loc); // use the copy constructor
		intersection.retainAll(currbox_loc);
		for (Point init_box: initbox_loc){
			if (!intersection.contains(init_box)){
				int key=((init_box.x+ init_box.y)*(init_box.x+ init_box.y + 1))/2 + init_box.y;
				this.myGraph.get(key).setAgentLock(this.id, false);
				////System.err.println("Unset lock at :"+init_box.toString());
			}
		}

		for (Point curr_box: currbox_loc){
			if (!intersection.contains(curr_box)){
				int key=((curr_box.x+ curr_box.y)*(curr_box.x+ curr_box.y + 1))/2 + curr_box.y;
				this.myGraph.get(key).setAgentLock(this.id, true);
				////System.err.println("Set lock at :"+curr_box.toString());
			}
		}
		
		int agent_key=((curr_agent.x+ curr_agent.y)*(curr_agent.x+ curr_agent.y + 1))/2 + curr_agent.y;
		this.myGraph.get(agent_key).setAgentLock(this.id, true);
		

		
		this.currentBox=block_box;
		////System.err.println("Set Current block box at :"+block_box.toString());
		this.currentGoal=null;
		//this.location=new int[]{curr_agent.x,curr_agent.y};
		///find a free destination
		int closest=Grid.LOCK_THRESHOLD;
	    for(int i=0; i<this.realfrees.length; i++) {
	        for(int j=0; j<realfrees[i].length; j++) {
	            Point free_loc=new Point(i,j);
	            Point free_n=new Point(i-1,j);
	            Point free_s=new Point(i+1,j);
	            Point free_e=new Point(i,j+1);
	            Point free_w=new Point(i,j-1);
	            if(realfrees[i][j]  && !refer_agent.agent_plan.contains(free_loc) && !refersCurrentBoxes.keySet().contains(free_loc)){
	            	if((realfrees[i-1][j] && !refer_agent.agent_plan.contains(free_n) && !refersCurrentBoxes.keySet().contains(free_n)) ||
	            	   (realfrees[i+1][j] && !refer_agent.agent_plan.contains(free_s) && !refersCurrentBoxes.keySet().contains(free_s)) ||
	            	   (realfrees[i][j+1] && !refer_agent.agent_plan.contains(free_e) && !refersCurrentBoxes.keySet().contains(free_e)) ||
	            	   (realfrees[i][j-1] && !refer_agent.agent_plan.contains(free_w) && !refersCurrentBoxes.keySet().contains(free_w))){
	            		////System.err.println("INspect "+i+","+j);
	            	   
	            	   int dis=Grid.getBFSPesudoDistance(new int[]{this.agent_plan.get(index).x,this.agent_plan.get(index).y} , new int[]{i,j},this.myGraph , this.id);
	            	   if(dis<closest){
	            		   	closest=dis;
	            		   	this.currentGoal=new Goal(Character.toLowerCase(block_box.id), this.color ,new int[]{i,j});
	            	   		//break;
	            	   }
	            	}
	            
	            }
	            		
	            			
	            
	            }
	        	if(this.currentGoal!=null)
	        		break;
	    }
	    
//	    //System.err.println("Find a clean area for agent "+this.currentGoal.toString());
//	    //System.err.println("Set Current block box at :"+block_box.toString());
//	    //System.err.println("Set Current Agent at :"+this.agent_plan.get(index));
		
	    
	    //start creating the plan
	    boolean aware_others=true;
		boolean aware_sa=false;
		Node rescueState=null;
		int index_ahead=0;
		
		
//		if(index>=1 && Math.abs(refer_agent.solution.get(index).engagedBox.location[0]-block_box.location[0])+Math.abs(refer_agent.solution.get(index).engagedBox.location[1]-block_box.location[1])<1){
//			index_ahead=index-1; //one step back
//			if(!this.solution.get(index).engagedBox.equals(this.solution.get(index_ahead).engagedBox)){
//				this.currentBox=this.solution.get(index_ahead).engagedBox;
//			}
//		}
//		else{
		index_ahead=index;
		//}
		

		
		
		if (index!=0){
			
			rescueState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
				this.solution.get(index_ahead).boxes,myGoals,myGraph,
				this.currentBox,
				this.currentGoal, new int[]{this.agent_plan.get(index_ahead).x,this.agent_plan.get(index_ahead).y}, this.id);
			//System.err.println("Emtour computation from index:"+index_ahead+ " to replace rest plan from "+index);
		}else{
			rescueState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
					this.myInitBoxes,myGoals,myGraph,
					this.currentBox,
					this.currentGoal, this.init_location, this.id);
			//System.err.println("Emtour computation at inital state");
		}
		this.strategy= new StrategyBestFirst(new AStar(rescueState));
		LinkedList<Node> singlesolution=new LinkedList<Node>();
		
		try {			
			singlesolution=SearchRescueSingle(strategy,rescueState,index,refer_agent);
			if(singlesolution==null){
				aware_others=false;
				if (index!=0){
					index_ahead=index-1; //one step back
					rescueState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
						this.solution.get(index_ahead).boxes,myGoals,myGraph,
						this.currentBox,
						this.currentGoal, new int[]{this.agent_plan.get(index_ahead).x,this.agent_plan.get(index_ahead).y}, this.id);
					//System.err.println("Emtour computation from index:"+index_ahead+ " to replace rest plan from "+index);
				}else{
					rescueState = new Node(aware_others,aware_sa, null,RandomWalkClient.all_walls, 
							this.myInitBoxes,myGoals,myGraph,
							this.currentBox,
							this.currentGoal, this.init_location, this.id);
					//System.err.println("Emtour computation at inital state");
				}
				this.strategy= new StrategyBestFirst(new AStar(rescueState));
				singlesolution=SearchRescueSingle(strategy,rescueState,index,refer_agent);
			}
			////System.err.println("Found a detour with length:"+singlesolution.size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//System.err.println("Exception happened, Detour solution logic has errors!!");
			e.printStackTrace();
		}
		return singlesolution;
	}

	
	//Single Astar logic
	public Node SearchSingle(Strategy strategy) throws IOException {
		System.err.format("Search starting with strategy Single %s.\n", strategy.toString());
		strategy.addToFrontier(this.initialState);
		int iterations = 0;
		while (true) {
            if (iterations == 1000) {
				//System.err.println(strategy.searchStatus());
				iterations = 0;
			}

			if (strategy.frontierIsEmpty()) {
				//System.err.println("Frontier is Empty Now. Weird?");
				return null;
			}
			
			if(strategy.countExplored()>=200000 && (this.plan_mode==SAFE_MODE || this.plan_mode==MA_SAFE_MODE)){
				//System.err.println("Planning stuck. Weird?");
				return null;
			}
			
			if(strategy.countExplored()>=400000 && (this.plan_mode==SAFE_SECONDAEY_MODE || this.plan_mode==MA_SECONDAEY_RISK_MODE) ){
				//System.err.println("Planning stuck. Weird?");
				return null;
			}

			Node leafNode = strategy.getAndRemoveLeaf();
			
			if (leafNode.isSingleGoalState()) {
							
				boolean valid=true;
				//I have to validate the extracted plan here as well. make sure the next plan can continue
				valid=validateCurrentPlan(currentGoal,new int[]{leafNode.agentRow,leafNode.agentCol});
				//if it is the last goal to finish, then does not matter where to place agent next
				if(this.plan_mode!=SAFE_MODE && this.plan_mode!=MA_SAFE_MODE){
					valid=true;
				}
				
				if(valid)
				{			
					leafNode.extractSolution();
					this.plan.addAll(leafNode.action_plan);
					this.agent_plan.addAll(leafNode.agent_plan);
					this.solution.addAll(leafNode.solution_plan);
					for(Point agent_loc:leafNode.agent_plan){
						this.agent_start_plan.add(new int[]{this.location[0],this.location[1]});
						this.agent_rescue_plan.add(false);
					}
					
					this.location=new int[]{leafNode.agentRow,leafNode.agentCol};
					this.myBoxes=leafNode.boxes;
					//update box locations for next subplan
					for (Point box_loc : this.myBoxes.keySet())
						this.myBoxes.get(box_loc).location=new int[]{box_loc.x,box_loc.y};
					
					System.err.println("Strategy status is: "+strategy.searchStatus());
					return leafNode;
				}
			}

			strategy.addToExplored(leafNode);
			for (Node n : leafNode.getExpandedNodes()) { // The list of expanded nodes is shuffled randomly; see Node.java.
				if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {	
					////System.err.println("Explore Node:"+n.boxes.keySet()+","+n.agentRow+"-"+n.agentCol);
					strategy.addToFrontier(n);
				}
			}
			iterations++;
		}
	}

	
	
	public LinkedList<Node> SearchRescueSingle(Strategy strategy, Node resumeState, int index, Agent refer_agent) throws IOException {
		System.err.format("Rescue Search starting with strategy Single %s.\n", strategy.toString());
		strategy.addToFrontier(resumeState);

		
		int iterations = 0;
		while (true) {
            if (iterations == 1000) {
				//System.err.println(strategy.searchStatus());
				iterations = 0;
			}

			if (strategy.frontierIsEmpty()) {
				//System.err.println("Frontier is Empty Now. Weird?");
				return null;
			}

			Node leafNode = strategy.getAndRemoveLeaf();
			if (leafNode.isSingleRescueGoalState(refer_agent)) {

					leafNode.extractSolution();
					//System.err.println("!!!!Detour Rescue path:"+leafNode.action_plan);
					//System.err.println("Original path:"+this.plan);
					int old_plan_length=this.plan.size();
					int offset=0;
					for(int detou_step=0; detou_step<leafNode.agent_plan.size();detou_step++){
						offset=index+detou_step;
						if(offset<old_plan_length){
							this.agent_plan.set(offset, leafNode.agent_plan.get(detou_step));
							this.plan.set(offset, leafNode.action_plan.get(detou_step));
							this.solution.set(offset, leafNode.solution_plan.get(detou_step));
							this.agent_start_plan.set(offset,this.agent_start_plan.get(detou_step));
							this.agent_rescue_plan.set(offset, true);
						}else{
							this.agent_plan.addLast(leafNode.agent_plan.get(detou_step));
							this.plan.addLast(leafNode.action_plan.get(detou_step));
							this.solution.addLast(leafNode.solution_plan.get(detou_step));	
							this.agent_start_plan.addLast(this.agent_start_plan.getLast());
							this.agent_rescue_plan.addLast(true);
						}
					}
					
					
					////System.err.println("Changed path:"+this.plan);
					
					//TEST LOGIC: abandoned old unavalibble steps.
					int tmp_plan_length=this.agent_plan.size();
					
					for(int detour_abandon=tmp_plan_length-1; detour_abandon>(index+leafNode.agent_plan.size()-1);detour_abandon--){
							this.agent_plan.removeLast();
							this.plan.removeLast();
							this.solution.removeLast();
							this.agent_start_plan.removeLast();
							this.agent_rescue_plan.removeLast();
					}
					
					this.location=new int[]{leafNode.agentRow,leafNode.agentCol};
					this.myBoxes=leafNode.boxes;
					//update box locations for next subplan
					for (Point box_loc : this.myBoxes.keySet())
						this.myBoxes.get(box_loc).location=new int[]{box_loc.x,box_loc.y};
					
					for(int rescue_update=index; rescue_update>=0; rescue_update--){
					if(this.solution.get(rescue_update).currentBox!=null && this.solution.get(rescue_update).currentBox.id== this.myBoxes.get(new Point(this.currentGoal.location[0],this.currentGoal.location[1])).id){
						////System.err.println("Whats up!!!"+lightest_agent.agent_plan.get(k));
						this.agent_rescue_plan.set(rescue_update, true);
					}
					}
					
					
					////System.err.println("Cleaned path:"+this.plan);
					return leafNode.solution_plan;
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
	
	//Single Astar Detour logic
	public LinkedList<Node> SearchDetourSingle(Strategy strategy, Node resumeState, int index) throws IOException {
			System.err.format("Detour Search starting with strategy Single %s.\n", strategy.toString());
			strategy.addToFrontier(resumeState);

			
			int iterations = 0;
			while (true) {
	            if (iterations == 1000) {
					//System.err.println(strategy.searchStatus());
					iterations = 0;
				}

				if (strategy.frontierIsEmpty()) {
					//System.err.println("Frontier is Empty Now. Weird?");
					return null;
				}

				Node leafNode = strategy.getAndRemoveLeaf();
				if (leafNode.isSingleDetourGoalState()) {
					boolean valid;
					//I have to validate the extracted plan here as well. make sure the next plan can continue
					valid=validateCurrentPlan(currentGoal,new int[]{leafNode.agentRow,leafNode.agentCol});
					//if it is the last goal to finish, then does not matter where to place agent next
					
					if(valid)
					{
						
						
						leafNode.extractSolution();
						//System.err.println("!!!!Detour path:"+leafNode.action_plan);
						////System.err.println("Original path:"+this.plan);
						int old_plan_length=this.plan.size();
						int offset=0;
						for(int detou_step=0; detou_step<leafNode.agent_plan.size();detou_step++){
							offset=index+detou_step;
							if(offset<old_plan_length){
								this.agent_plan.set(offset, leafNode.agent_plan.get(detou_step));
								this.plan.set(offset, leafNode.action_plan.get(detou_step));
								this.solution.set(offset, leafNode.solution_plan.get(detou_step));
								this.agent_start_plan.set(offset,new int[]{this.location[0],this.location[1]});
								this.agent_rescue_plan.set(offset, false);
							}else{
								this.agent_plan.addLast(leafNode.agent_plan.get(detou_step));
								this.plan.addLast(leafNode.action_plan.get(detou_step));
								this.solution.addLast(leafNode.solution_plan.get(detou_step));	
								this.agent_start_plan.addLast(new int[]{this.location[0],this.location[1]});
								this.agent_rescue_plan.addLast(false);
							}
						}
						////System.err.println("Changed path:"+this.plan);
						
						//TEST LOGIC: abandoned old unavalibble steps.
						int tmp_plan_length=this.agent_plan.size();
						
						for(int detour_abandon=tmp_plan_length-1; detour_abandon>(index+leafNode.agent_plan.size()-1);detour_abandon--){
								this.agent_plan.removeLast();
								this.plan.removeLast();
								this.solution.removeLast();
								this.agent_start_plan.removeLast();
								this.agent_rescue_plan.removeLast();
						}
						
						this.location=new int[]{leafNode.agentRow,leafNode.agentCol};
						this.myBoxes=leafNode.boxes;
						//update box locations for next subplan
						for (Point box_loc : this.myBoxes.keySet())
							this.myBoxes.get(box_loc).location=new int[]{box_loc.x,box_loc.y};
						
						////System.err.println("Cleaned path:"+this.plan);
						return leafNode.solution_plan;
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
		
	//Single Astar logic
	public LinkedList<Node> SearchDetourRestSingle(Strategy strategy) throws IOException {
			System.err.format("Search starting with strategy Single %s.\n", strategy.toString());
			strategy.addToFrontier(this.initialState);
			
			int iterations = 0;
			while (true) {
	            if (iterations == 1000) {
					//System.err.println(strategy.searchStatus());
					iterations = 0;
				}

				if (strategy.frontierIsEmpty()) {
					//System.err.println("Frontier is Empty Now. Weird?");
					return null;
				}

				Node leafNode = strategy.getAndRemoveLeaf();
				if (leafNode.isSingleGoalState()) {
					boolean valid;
					//I have to validate the extracted plan here as well. make sure the next plan can continue
					valid=validateCurrentPlan(currentGoal,new int[]{leafNode.agentRow,leafNode.agentCol});
					//if it is the last goal to finish, then does not matter where to place agent next
					
					if(valid)
					{
						
						leafNode.extractSolution();
						this.plan.addAll(leafNode.action_plan);
						this.agent_plan.addAll(leafNode.agent_plan);
						this.solution.addAll(leafNode.solution_plan);
						for(Point agent_loc:leafNode.agent_plan){
							this.agent_start_plan.add(new int[]{this.location[0],this.location[1]});		
							this.agent_rescue_plan.add(false);
						}
						
						this.location=new int[]{leafNode.agentRow,leafNode.agentCol};
						this.myBoxes=leafNode.boxes;
						//update box locations for next subplan
						for (Point box_loc : this.myBoxes.keySet())
							this.myBoxes.get(box_loc).location=new int[]{box_loc.x,box_loc.y};
						
						return leafNode.solution_plan;
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

		
	//Find next safe box/goal logic
	public void findNextSafeBoxGoal(boolean sa_mode) {
			
		TreeMap<Integer,ArrayList<Goal>> unfinished_goals= new TreeMap<Integer,ArrayList<Goal>>();
		TreeMap<Integer,Box> unfinished_boxes= new TreeMap<Integer,Box>();  //there may have box in same distance(arraylist might be used later)
		for (Goal a_goal : myGoals.values()){
			Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
			if(myBoxes.containsKey(a_goal_loc) && Character.toUpperCase(a_goal.id)==myBoxes.get(a_goal_loc).id){
				//System.err.println("This box is in place:"+a_goal_loc.toString());
				continue;
			}
			int agent2goal=RandomWalkClient.initial_level_grid.getBFSDistance(a_goal.location,this.location);
			////System.err.println(a_goal.id+" to agent"+agent2goal);
			if(!unfinished_goals.containsKey(agent2goal))
				unfinished_goals.put(agent2goal, new ArrayList<Goal>());
			unfinished_goals.get(agent2goal).add(a_goal);
		}
		
		
		this.currentGoal=null;
		this.secondaryGoal=null;
		this.currentGoal=unfinished_goals.firstEntry().getValue().get(0); //if no next target goal is valid based on the hueristic below in "validateNextGoal"
		int closet_goal_dis=unfinished_goals.firstEntry().getKey();
		//also register a secondary goal/box pair for backup plan.	
		this.secondaryGoal=unfinished_goals.lastEntry().getValue().get(0);
			
		////System.err.println("Default is "+unfinished_goals.firstEntry().getValue().toString()+ " The distance:"+unfinished_goals.firstEntry().getKey());
		Entry<Integer,ArrayList<Goal>> closest_goals=null;
		while(unfinished_goals.keySet().size()!=0){
			
			closest_goals=unfinished_goals.pollFirstEntry();
			closet_goal_dis=closest_goals.getKey();
			
			boolean valid=true;
			for (int i=0;i< closest_goals.getValue().size(); i++){
				Goal closest_goal=closest_goals.getValue().get(i);
				
				////System.err.println("Inspect "+closest_goal.toString()+" validaity");
				if(!sa_mode)
					valid=validateNextGoal(closest_goal);
				else
					valid=validateSANextGoal(closest_goal);
				
				if(valid){
					this.currentGoal=closest_goal;
					////System.err.println("Current Goal is "+ this.secondaryGoal.toString());
					break;
				}else{
				//	//System.err.println("Current Goal" +closest_goal.toString()+"  is not valid");
				}
				
			}
			if(valid){
				break;
			}
		}
		
		
		

		
		
		int currentDist=Integer.MAX_VALUE;
		int secondDist=Integer.MAX_VALUE;
		for(Box a_box: myBoxes.values()){
			Point a_box_loc = new Point(a_box.location[0],a_box.location[1]);
			if(myGoals.containsKey(a_box_loc) && Character.toLowerCase(a_box.id)==myGoals.get(a_box_loc).id){
				////System.err.println("This box is in place:"+a_box_loc.toString());
				continue;
			}
			int box2goal=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,this.currentGoal.location);
			int box2sgoal=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,this.secondaryGoal.location);
			if(Character.toLowerCase(a_box.id)==this.currentGoal.id && box2goal<=currentDist ){
				
//				if(box2goal==currentDist && this.currentBox!=null){
//					int box2agent=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,this.location);
//					int curbox2agent=RandomWalkClient.initial_level_grid.getBFSDistance(this.currentBox.location,this.location);
//					if(box2agent>curbox2agent)
//						continue;
//				}
				currentDist=box2goal;
				this.currentBox=a_box;
			}
			if(Character.toLowerCase(a_box.id)==this.secondaryGoal.id && box2sgoal<=secondDist){
				secondDist=box2sgoal;
				this.secondaryBox=a_box;				
			}
		}
		
	}
	
	//FInd next close box/goal logic
	public void findNextClosestBoxGoal() {
		int currentDist=0;
		for(Box a_box: myBoxes.values()){
			Point a_box_loc = new Point(a_box.location[0],a_box.location[1]);
			if(myGoals.containsKey(a_box_loc) && Character.toLowerCase(a_box.id)==myGoals.get(a_box_loc).id){
				//System.err.println("This box is in place:"+a_box_loc.toString());
				continue;
			}
			if(a_box.zombie){
				////System.err.println("This box is Zombie in place:"+a_box_loc.toString());
				continue;
			}
			int box2agent=RandomWalkClient.initial_level_grid.getBFSDistance(a_box.location,this.location);
			if(box2agent>currentDist ){
				currentDist=box2agent;
				this.currentBox=a_box;
			}
		}
		
		int currentDist2=Integer.MAX_VALUE;
		for (Goal a_goal : myGoals.values()){
			Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
			if(myBoxes.containsKey(a_goal_loc) && Character.toUpperCase(a_goal.id)==myBoxes.get(a_goal_loc).id){
				continue;
			}
			int box2goal=RandomWalkClient.initial_level_grid.getBFSDistance(a_goal.location,this.currentBox.location);
			if(box2goal<currentDist2 && Character.toLowerCase(this.currentBox.id)==a_goal.id){
				currentDist2=box2goal;
				this.currentGoal=a_goal;
			}
		}
		
		
	
	}
	
	
	public boolean validateSANextGoal(Goal closest_goal){
		Set<Point> box_locs=new HashSet<Point>(this.myBoxes.keySet());
		this.myGraph.get(closest_goal.hashCode()).setLock(true);
		boolean valid=true;

		for (Goal a_goal : myGoals.values()){
			//test goal count		
			Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
			if((myBoxes.containsKey(a_goal_loc) && Character.toUpperCase(a_goal.id)==myBoxes.get(a_goal_loc).id) || a_goal.equals(closest_goal) ){
				//box_locs.remove(a_goal_loc);
				this.myGraph.get(a_goal.hashCode()).setLock(true);
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
					int sudobox2goal=Grid.getSABFSPesudoDistance(this.myBoxes.get(a_box_loc).location,a_goal.location,this.myGraph);
					if (sudobox2goal<=currentDist){
						currentDist=sudobox2goal;
						clsbox2other=a_box_loc;
					}
				}
				
			}
			
			////System.err.println("Agent at "+this.location[0]+","+this.location[1]+" locked goal: "+closest_goal.toString()+" inspect another goal "+a_goal.toString()+" and its closest box to itself is "+clsbox2other.toString()+" and the distance between this another goal and this box becomes "+currentDist+" The distance between agent to this box "+ Grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location,this.myGraph));
			////System.err.println(clsbox2other+" to"+ this.location[0]+","+this.location[1]+"  "+Grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location,this.myGraph,this.id));		
			int sudoDist=Grid.getSABFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location,this.myGraph);
			if((sudoDist+currentDist)>=Grid.LOCK_THRESHOLD){
				////System.err.println("Box "+clsbox2other.toString()+ "unreach "+RandomWalkClient.initial_level_grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location));
				valid=false;	
				break;
				
			}

			//box_locs.remove(clsbox2other);
			
		}
		
		
		this.myGraph.get(closest_goal.hashCode()).setLock(false);
		
		for (Goal a_goal : myGoals.values()){
			Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
			if((myBoxes.containsKey(a_goal_loc) && Character.toUpperCase(a_goal.id)==myBoxes.get(a_goal_loc).id) || a_goal.equals(closest_goal) ){
				//box_locs.remove(a_goal_loc);
				this.myGraph.get(a_goal.hashCode()).setLock(false);
				continue;
			}			
		}
		
		return valid;	
	}
	
	public boolean validateNextGoal(Goal closest_goal){
		
		Set<Point> box_locs=new HashSet<Point>(this.myBoxes.keySet());
		this.myGraph.get(closest_goal.hashCode()).setAgentLock(this.id, true);
		boolean valid=true;

		for (Goal a_goal : myGoals.values()){
			//test goal count		
			Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
			if((myBoxes.containsKey(a_goal_loc) && Character.toUpperCase(a_goal.id)==myBoxes.get(a_goal_loc).id) || a_goal.equals(closest_goal) ){
				//box_locs.remove(a_goal_loc);
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
					if (sudobox2goal<=currentDist){
						currentDist=sudobox2goal;
						clsbox2other=a_box_loc;
					}
				}
				
			}
			
			////System.err.println("Agent at "+this.location[0]+","+this.location[1]+" locked goal: "+closest_goal.toString()+" inspect another goal "+a_goal.toString()+" and its closest box to itself is "+clsbox2other.toString()+" and the distance between this another goal and this box becomes "+currentDist+" The distance between agent to this box "+ Grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location,this.myGraph));
			////System.err.println(clsbox2other+" to"+ this.location[0]+","+this.location[1]+"  "+Grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location,this.myGraph,this.id));		
			int sudoDist=Grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location,this.myGraph,this.id);
			if((sudoDist+currentDist)>=Grid.LOCK_THRESHOLD){
				////System.err.println("Box "+clsbox2other.toString()+ "unreach "+RandomWalkClient.initial_level_grid.getBFSPesudoDistance(this.myBoxes.get(clsbox2other).location,this.location));
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
		int completeness=1;
		for (Goal a_goal : myGoals.values()){
			Point a_goal_loc = new Point(a_goal.location[0],a_goal.location[1]);
			if((myBoxes.containsKey(a_goal_loc) && Character.toUpperCase(a_goal.id)==myBoxes.get(a_goal_loc).id) || a_goal.equals(closest_goal) ){
				completeness++;
				continue;
			}
			////System.err.println("I inspect "+closest_goal.toString()+" and other goal:"+a_goal.toString()+ " The distance:"+RandomWalkClient.initial_level_grid.getBFSPesudoDistance(a_goal.location,this.location));
			if(RandomWalkClient.initial_level_grid.getBFSPesudoDistance(a_goal.location,agent_location)>=Grid.LOCK_THRESHOLD){
				valid=false;
				break;
			}
		}
		
		
//		if(completeness==(myGoals.size()-1))
//			valid=true;
		
		if(this.restGoals.containsKey(new Point(agent_location[0],agent_location[1])))
			valid=false;
//		if(this.myGoals.containsKey(new Point(agent_location[0],agent_location[1])))
//			valid=false;
		
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
		//System.err.println("Found " + color + " Goal " + id + " Location " + location[0]+","+location[1]);
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
	boolean zombie;
	int freedom;
	
	public Box( char id, String color , int[] location) {
		////System.err.println("Found " + color + " Box " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
		this.claimed=false;
		this.zombie=false;
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

