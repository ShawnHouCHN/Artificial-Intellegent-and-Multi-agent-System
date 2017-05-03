package sampleclients;

import java.awt.Point;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import sampleclients.Command.dir;
import sampleclients.Command.type;
import sampleclients.Strategy;
import sampleclients.Heuristic.AStar;
import sampleclients.Strategy.StrategyBestFirst;
import sampleclients.Vertex;
import sampleclients.Node;
import sampleclients.Agent;

public class RandomWalkClient {
	private static Random rand = new Random();
	private BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
	public TreeMap<Character, Agent > all_agents = new TreeMap<Character, Agent >();
	public Comparator<Agent> agent_plan_comparator = new PlanLengthComparator();
	public PriorityQueue<Agent> ranked_all_agents = new PriorityQueue<Agent>(agent_plan_comparator);
	public HashMap<String,ArrayList<Agent>> color_agents=new HashMap<String,ArrayList<Agent>>();
	public List< Goal > all_goals = new ArrayList< Goal >();
	public List< Box > all_boxes = new ArrayList< Box >();
	public HashMap<Integer,Vertex> initial_graph=new HashMap<Integer,Vertex>();
	public static boolean[][] all_walls=null;
	boolean[][] all_frees=null;
	boolean[][] real_frees=null;
	public static Grid initial_level_grid=null; //this is the grid to save all distance between any pair of locations 
		
	public List<String> conflicts = null;

	
	public static int NOOP_LOW_THRESHOLD = 3; //set to 3 
	public static int NOOP_HIGH_THRESHOLD = 5; //set to 3 
	
	public RandomWalkClient() throws IOException {
		readMap();
	}

	private void readMap() throws IOException {
		Map< Character, String > colors = new HashMap< Character, String >();		
		String line, color=null;
		
		//compute the dimension of the level file in the first place.
		int[] dimension=this.getLevelDimension(in);
		all_walls = new boolean[dimension[0]][dimension[1]];
		all_frees = new boolean[dimension[0]][dimension[1]]; //pesudo free cell, everywhere other than walls
		real_frees= new boolean[dimension[0]][dimension[1]];
		
		/*****************************Read In the Level Layout************************************/
		// Read lines specifying colors
		int lineN=0;
		while ( ( line = in.readLine() ).matches( "^[a-z]+:\\s*[0-9A-Z](,\\s*[0-9A-Z])*\\s*$" ) ) {
			line = line.replaceAll( "\\s", "" );
			color = line.split( ":" )[0];
			//System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!I read color "+color);
			for ( String id : line.split( ":" )[1].split( "," ) ){
				colors.put( id.charAt( 0 ), color );
			}
		}

		
		
		// Read lines specifying level layout
		while ( !line.equals( "" ) ) {
			for ( int i = 0; i < line.length(); i++ ) {
				char id = line.charAt( i );
				
				//if the chr is a wall
				if (id == '+') { // Wall.
					all_walls[lineN][i] = true; //lineN is the row, i is the column
				}
				
				//if the chr is an agent
				else if ( '0' <= id && id <= '9' ){
					System.err.println("Found agent "+id);
					Agent new_agent;
					if (color==null)
						new_agent=new Agent(id, "blue",new int[]{lineN,i});
					else
						new_agent=new Agent(id, colors.get(id),new int[]{lineN,i});
					all_agents.put(id,new_agent);
					//categorize agents by color
					if(!color_agents.containsKey(new_agent.color))
						color_agents.put(new_agent.color, new ArrayList<Agent>());					
					color_agents.get(new_agent.color).add(new_agent);
					//	
					
					all_frees[lineN][i] = true;
				}
				
				//save all goals
				else if('a' <= id && id <= 'z'){
					System.err.println("Found Goal "+id);
					if (color==null)
						all_goals.add(new Goal(id, "blue",new int[]{lineN,i}));
					else
						all_goals.add(new Goal(id, colors.get(Character.toUpperCase(id)),new int[]{lineN,i}));
					all_frees[lineN][i] = true;
				}
				
				//save all boxes
				else if ('A' <= id && id <= 'Z'){
					System.err.println("Found box "+id);
					if (color==null)
						all_boxes.add(new Box(id, "blue",new int[]{lineN,i}));
					else
						all_boxes.add(new Box(id, colors.get(id),new int[]{lineN,i}));
					all_frees[lineN][i] = true;
				}else if (id == ' ') {
					all_frees[lineN][i] = true;
					real_frees[lineN][i] = true;
				}else{
					System.err.println("Error, read invalid level character: " + (int) i);
					System.exit(1);
				}
			}

			
			line = in.readLine();
			lineN++;
			
		}
		/*******************************************************************************************/
		
		
		
		/*****************************Create Grid BFS Distance Heuristic****************************/
		createDistanceMap(); // creat distance map between every pair of locations in the level file.
		initial_level_grid=new Grid(this.initial_graph);
		
		/******************************************************************************************/
		
		
		

		
		//SA case
		if(all_agents.size()==1){
			Agent the_agent=all_agents.get("0".charAt(0));
			the_agent.setInitialGraph(this.initial_graph);
			
			the_agent.setInitialWalls(all_walls);
			
			the_agent.findAllBoxesGoals(all_boxes,all_goals);
			System.err.println("SA Successful Create plan for agent "+the_agent.id+ ": with length="+the_agent.createInitSAPlan());
		}
		
		else{
		//MA case
			/*****************************Create Plan For Each Legal Agent***********************************/
			for (Character agent_id : all_agents.keySet()){
				Agent a_agent=all_agents.get(agent_id);
				int[] init_agent_loc=a_agent.location;
				
				a_agent.setInitialGraph(this.initial_graph);
				
				a_agent.setInitialWalls(all_walls);
				
				a_agent.findMyBoxes(all_boxes,color_agents);
				//a_agent.printMyBoxes();
				a_agent.findMyGoals(all_goals);

			}			
			
			for (Character agent_id : all_agents.keySet()){
				Agent a_agent=all_agents.get(agent_id);
				System.err.println("MA Successful Create plan for agent "+a_agent.id+ ": with length="+a_agent.createInitPlan());
				System.err.println("Location of agent:"+a_agent.id+ " along the plan is:"+a_agent.subplanboard.values().toString());
				
				ranked_all_agents.add(a_agent);	
				
			}
			
			/*************************************************************************************************/
			
			
		/*****************************Detect Conflicts among agents' plans********************************/
		System.err.println("Start resolving conflicts!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		resolveConflicts();
		/*************************************************************************************************/
		}
		
		//Test
		for (Character agent_id : all_agents.keySet()){
			Agent a_agent=all_agents.get(agent_id);
			//System.err.println("Successful Create plan for agent "+a_agent.id+ ": with length="+a_agent.createInitPlan());
			System.err.println("Resolved Location of agent:"+a_agent.id+ " init:"+a_agent.init_location[0]+","+a_agent.init_location[1]+" along the plan is:"+a_agent.plan);
		}		
		
	
	}
	
	public void resolveConflicts(){
		HashMap<Character, Agent> resolved_agent= new HashMap<Character, Agent>();
		while(ranked_all_agents.size()!=0){
			Agent lightest_agent=ranked_all_agents.poll();
			
			//a agent has no tasks
			if (lightest_agent.solution.size()==0)
				continue;
			
			resolved_agent.put(lightest_agent.id, lightest_agent);
			for (Entry<Character, Agent> agentA: all_agents.entrySet()){
				Agent subject_agent=agentA.getValue();
				if(subject_agent.id==lightest_agent.id || resolved_agent.containsKey(subject_agent.id))
					continue;
				
				
				else{			
					int i=0; //iterate through every other subject agent, make sure resolve the conflict when they enter lightest way.

					while(i < subject_agent.plan.size() && subject_agent.plan.get(i).actType!=type.NoOp){
						
						//get some current details (box, engaged box, goal,agent location)
						Point subject_agent_loc=subject_agent.agent_plan.get(i);
						Box subject_engaged_box=subject_agent.solution.get(i).engagedBox;
						HashMap<Point,Box> subject_boxes=subject_agent.solution.get(i).boxes;
						//Goal subject_curr_goal=subject_agent.solution.get(i).currentGoal;
//						
//						if(isBoxBlockQuarantineZone(lightest_agent,subject_agent,i)){
//							System.err.println("Agent :"+lightest_agent.id+" Hit a box");
//						}
					
						//if subject agent with/without box enters quarantine zone.
						if(isInQuarantineZone(lightest_agent,subject_agent_loc,subject_engaged_box, i)){	
							
							System.err.println("Agent :"+subject_agent.id+" enters quarantine zone of lightest agent "+lightest_agent.id+" at index:"+i+" at location: "+subject_agent.agent_plan.get(i).toString());
							
							if(isGoalBlockQuarantineZone(lightest_agent,subject_agent, i)){
								System.err.println("It is a blocking goal! stop going:");
								int waiting_time=lightest_agent.plan.size();
								for(int wait=0;wait<waiting_time;wait++){
									subject_agent.plan.add(i, new Command());							
									subject_agent.agent_plan.add(i,subject_agent.agent_plan.get(i));
									subject_agent.solution.add(i,subject_agent.solution.get(i));	
								}
								break;
							}						
							
							//if the subject agent/box enters a safe cell in QuarantineZone of lightest agent
							else if(!isInOccupiedZone(lightest_agent,subject_agent,i)){
								System.err.println("It is not an occupied cell! keep going:");
							}
							
							//if the subject agent/box enters a dangerous cell in QuarantineZone of lightest agent
							else{		
								
								LinkedList<Node> singlesolution=subject_agent.createDetourPlan(lightest_agent, i);								
								
								//if there is no detour
								if(singlesolution==null || singlesolution.size()==0){									
									//the lightest box so far will block another subject's way at this step of that subject, so the lightest agent should halt the execution. 
									int light_enter=0;
									
									//know when lightest going into this subject qurantine zone 
									for(int j=0;j<=i;j++)
										if(isInQuarantineZone(subject_agent,lightest_agent.agent_plan.get(j),lightest_agent.solution.get(j).engagedBox, j)){
											light_enter=j;	
											break;
										}
									
									int waiting_time=subject_agent.plan.size();
									for(int wait=0;wait<waiting_time;wait++){
										lightest_agent.plan.add(light_enter, new Command());							
										lightest_agent.agent_plan.add(light_enter,lightest_agent.agent_plan.get(light_enter));
										lightest_agent.solution.add(light_enter,lightest_agent.solution.get(light_enter));										
									}
									System.err.println("It is not a valid lightest agent! find next:");
									//System.err.println("Now the lighest agent"+lightest_agent.id+" plan is:"+lightest_agent.plan);
									break;
								}
								
								//if there is a detour
								else{
									subject_agent.createDetourAltPlan();
								}
							
							}
							System.err.println("+++++++++++++++++++++++++++++++++++++Resolve 1 >.< +++++++++++++++++++++++++++++");

						}
						i++;
					}
				}
			}
			
		}
	}
	
	
	public boolean isInOccupiedZone(Agent lightest_agent, Agent subject_agent, int index){
		HashMap<Point, Box> lightest_last_boxes;
		HashMap<Point, Box> lightest_agent_boxes;
		HashMap<Point, Box> lightest_next_boxes;
		HashMap<Point, Goal> lightest_agent_goals;
		HashMap<Point, Goal> lightest_next_goals;
		LinkedList<Point> lightest_agent_path=lightest_agent.agent_plan;
		Box lightest_agent_engaged_box=null;
		Box subject_agent_engaged_box=null;
		Point subject_engaged_box_loc=null;
		Point lightest_engaged_box_loc=null;
		Point lightest_last_loc=null, lightest_agent_loc=null, lightest_next_loc=null;
		Point subject_last_loc=null, subject_agent_loc=null, subject_next_loc=null;
		
		if(index>=lightest_agent.solution.size()-1){
			lightest_last_boxes=lightest_agent.solution.getLast().boxes;
			lightest_agent_boxes=lightest_agent.solution.getLast().boxes;
			lightest_next_boxes=lightest_agent.solution.getLast().boxes;
			lightest_agent_goals=lightest_agent.solution.getLast().goals;
			lightest_next_goals=lightest_agent.solution.getLast().goals;
//			lightest_agent_engaged_box=lightest_agent.solution.getLast().engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.getLast();
			lightest_next_loc=lightest_agent.agent_plan.getLast();
			lightest_last_loc=lightest_agent.agent_plan.getLast();
			
		}
		else if(index==0){
			lightest_agent_boxes=lightest_agent.solution.get(index).boxes;
			lightest_last_boxes=lightest_agent.myInitBoxes;
			lightest_next_boxes=lightest_agent.solution.get(index+1).boxes;
			lightest_agent_goals=lightest_agent.solution.get(index).goals;
			lightest_next_goals=lightest_agent.solution.get(index+1).goals;
//			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
			lightest_next_loc=lightest_agent.agent_plan.get(index+1);
			lightest_last_loc=new Point(lightest_agent.init_location[0],lightest_agent.init_location[1]);
		}
		else{
			lightest_last_boxes=lightest_agent.solution.get(index-1).boxes;
			lightest_agent_boxes=lightest_agent.solution.get(index).boxes;
			lightest_next_boxes=lightest_agent.solution.get(index+1).boxes;
			lightest_agent_goals=lightest_agent.solution.get(index).goals;
			lightest_next_goals=lightest_agent.solution.get(index+1).goals;
//			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_last_loc=lightest_agent.agent_plan.get(index-1);
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
			lightest_next_loc=lightest_agent.agent_plan.get(index+1);
		}
		
		subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
		
		
		if(index>=subject_agent.agent_plan.size()-1){
			subject_agent_loc=subject_agent.agent_plan.getLast();
			subject_last_loc=subject_agent.agent_plan.getLast();
			subject_next_loc=subject_agent.agent_plan.getLast();
		}
		if(index==0){
			subject_agent_loc=subject_agent.agent_plan.get(index);
			subject_last_loc=new Point(subject_agent.init_location[0], subject_agent.init_location[1]);
			subject_next_loc=subject_agent.agent_plan.get(index+1);
		}
		else{
			subject_agent_loc=subject_agent.agent_plan.get(index);
			subject_last_loc=subject_agent.agent_plan.get(index-1);
			//subject_next_loc=subject_agent.agent_plan.get(index+1);
		}
		
		if(subject_agent_engaged_box!=null)
			subject_engaged_box_loc=new Point(subject_agent_engaged_box.location[0], subject_agent_engaged_box.location[1]);	

		//System.err.println("Subject agent:"+ subject_agent.id+ ","+subject_agent_loc+" engagedbox:"+subject_engaged_box_loc+" lightest agent: "+lightest_agent.id+" "+lightest_agent_loc+" engagedbox:"+lightest_engaged_box_loc);
		//System.err.println("Check:"+subject_last_loc+subject_agent_loc+subject_next_loc+"------"+lightest_last_loc+lightest_agent_loc+lightest_next_loc);
		//System.err.println("Subject agent:"+subject_agent.id+" at:"+subject_agent.agent_plan.get(index)+" Lightest agent:"+lightest_agent.id+" at:"+lightest_agent.agent_plan.get(index));
		if(!lightest_agent_goals.keySet().contains(subject_agent_loc) && 
		   !lightest_agent_goals.keySet().contains(subject_engaged_box_loc) && 
		   !lightest_agent_boxes.keySet().contains(subject_agent_loc) && 
		   !lightest_agent_boxes.keySet().contains(subject_engaged_box_loc) &&
		   !lightest_next_goals.keySet().contains(subject_agent_loc) &&
		   !lightest_next_goals.keySet().contains(subject_engaged_box_loc) &&
		   !lightest_next_boxes.keySet().contains(subject_agent_loc) &&
		   !lightest_next_boxes.keySet().contains(subject_engaged_box_loc) &&
		   !lightest_last_boxes.keySet().contains(subject_agent_loc) &&
		   !lightest_last_boxes.keySet().contains(subject_engaged_box_loc) &&
		   !subject_agent_loc.equals(lightest_agent_loc) &&
		   !subject_agent_loc.equals(lightest_next_loc) &&
		   !subject_last_loc.equals(lightest_agent_loc))
			return false;
		
		
		
		else 
			return true;
		
		
	}
	
	public boolean isInQuarantineZone(Agent lightest_agent, Point subject_agent_loc, Box subject_engaged_box, int index){
		HashMap<Point, Box> lightest_agent_boxes;
		HashMap<Point, Goal> lightest_agent_goals;
		LinkedList<Point> lightest_agent_path=lightest_agent.agent_plan;
		if(index>=lightest_agent.solution.size()){
			lightest_agent_boxes=lightest_agent.solution.getLast().boxes;
			lightest_agent_goals=lightest_agent.solution.getLast().goals;
		}else{
			lightest_agent_boxes=lightest_agent.solution.get(index).boxes;
			lightest_agent_goals=lightest_agent.solution.get(index).goals;
		}
		 
		Point engaged_box_loc=null;
		if (subject_engaged_box!=null)
			engaged_box_loc=new Point(subject_engaged_box.location[0], subject_engaged_box.location[1]);
		
		if(lightest_agent_path.contains(subject_agent_loc) || lightest_agent_boxes.keySet().contains(subject_agent_loc) || lightest_agent_goals.keySet().contains(subject_agent_loc)){
			return true;
		}
		
		else if(engaged_box_loc!=null && (lightest_agent_path.contains(engaged_box_loc) || lightest_agent_boxes.keySet().contains(engaged_box_loc) || lightest_agent_goals.keySet().contains(engaged_box_loc))){
			System.err.println("Ligtest is "+lightest_agent.id+" A engaged box at"+ engaged_box_loc+ " of subject is pushed into the qurantinze zone at index:"+index);
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean isBoxBlockQuarantineZone(Agent lightest_agent, Agent subject_agent, int index){
		return subject_agent.solution.get(index).boxes.containsKey(lightest_agent.agent_plan.get(index+1));
	}
	
	
	public boolean isGoalBlockQuarantineZone(Agent lightest_agent, Agent subject_agent, int index){
		Goal subject_curr_goal=subject_agent.solution.get(index).currentGoal;
		Goal lightest_curr_goal;
		int[] agent_start_loc;
		if (index<lightest_agent.plan.size()){
			lightest_curr_goal=lightest_agent.solution.get(index).currentGoal;
			agent_start_loc=lightest_agent.agent_start_plan.get(index);
		}else{
			lightest_curr_goal=lightest_agent.solution.getLast().currentGoal;
			agent_start_loc=lightest_agent.agent_start_plan.getLast();
		}
		
		
//		System.err.println("Agent :"+subject_agent.id+" current goal at: "+
//		subject_agent.currentGoal.toString()+" blocked lightest agent "+lightest_agent.id+","+lightest_curr_loc+" to reach: "+lightest_curr_goal+
//		"##DIS:"+initial_level_grid.getBFSPesudoDistance(lightest_curr_goal.location, lightest_agent.init_location, lightest_agent.myGraph));
		
		
		lightest_agent.myGraph.get(subject_curr_goal.hashCode()).setLock(true);
		if (initial_level_grid.getBFSPesudoDistance(lightest_curr_goal.location,agent_start_loc, lightest_agent.myGraph)>=Grid.LOCK_THRESHOLD){
			lightest_agent.myGraph.get(subject_curr_goal.hashCode()).setLock(false);
			return true;
		}else{
			lightest_agent.myGraph.get(subject_curr_goal.hashCode()).setLock(false);
			return false;
		}
		
	}
	
	
	
	//recursive code checking whether there is a conflict in the further steps
	public int noopAndWaitConflict(Agent defendant, Agent complainant, int step_index){
		int waiting_length=0;
		Point def_agent_loc=defendant.agent_plan.get(step_index);
		for(int start=step_index+NOOP_LOW_THRESHOLD; start<=(step_index+NOOP_HIGH_THRESHOLD); start++){
			Point com_agent_loc=complainant.agent_plan.get(start);
			if(com_agent_loc.equals(def_agent_loc))
				waiting_length=start-step_index+NOOP_LOW_THRESHOLD;
		}
		
		return waiting_length;
	}
	
	
	
	
	public boolean update() throws IOException {
		String jointAction = "[";

		for (Character agent_id : all_agents.keySet()) 
			jointAction += all_agents.get( agent_id ).act() + ",";
		
		jointAction = jointAction.substring(0,jointAction.length()-1)  + "]";

		// Place message in buffer
		System.out.println( jointAction );
		
		// Flush buffer
		System.out.flush();

		// Disregard these for now, but read or the server stalls when its output buffer gets filled!
		String percepts = in.readLine();
		System.err.println( "percepts: "+ percepts );
		if ( percepts == null )
			return false;

		return true;
	}
	
	public void createDistanceMap(){
		//Put all free cells into a map
		for (int frow = 1; frow < all_frees.length-1; frow++) {
			for (int fcol = 1; fcol < all_frees[0].length-1; fcol++) {
				if(all_frees[frow][fcol]){
					//do dijkstra mapping below
					Vertex dj_vertex= new Vertex(frow,fcol);
					//four directions
					if(!initial_graph.containsKey(dj_vertex.hashCode())){
						initial_graph.put(dj_vertex.hashCode(), dj_vertex);	
						Vertex dj_adj_vertex;
						if (all_frees[frow-1][fcol]){
							dj_adj_vertex = new Vertex(frow-1,fcol);
							dj_vertex.setEdge(dj_adj_vertex);
							//dij_graph.get(dj_vertex).add(dj_adj_vertex);
						}
						if (all_frees[frow+1][fcol]){
							dj_adj_vertex = new Vertex(frow+1,fcol);
							dj_vertex.setEdge(dj_adj_vertex);
							//dij_graphdj_adj_vertex);
						}
						if (all_frees[frow][fcol-1]){
							dj_adj_vertex = new Vertex(frow,fcol-1);
							dj_vertex.setEdge(dj_adj_vertex);
							//dij_graph.get(dj_vertex).add(dj_adj_vertex);
						}
						if (all_frees[frow][fcol+1]){
							dj_adj_vertex = new Vertex(frow,fcol+1);
							dj_vertex.setEdge(dj_adj_vertex);
						}
						
										
					}
				
				}
			}
		}
				
	}
	
	public int[] getLevelDimension(BufferedReader serverMessages) throws IOException{
		int longest=0, row=0, line_length=0;
		serverMessages.mark(10000);
		String line = serverMessages.readLine();
		while (!line.equals("")) {
			line = serverMessages.readLine();
			line_length=line.length();
			if (line_length>=longest){
				longest=line_length;
			}
			row++;
		}
		int[] dimension={row,longest};
		serverMessages.reset();
		return dimension;	
	}
	

	public static void main( String[] args ) {

		// Use stderr to print to console
		System.err.println( "Hello from RandomWalkClient. I am sending this using the error outputstream" );
		for (int i=0; i < Command.every.length;i ++){
			System.err.println( Command.every[i]);
		}
		try {
			RandomWalkClient client = new RandomWalkClient();
			int counter=0;
			while ( client.update() ){
				counter++;
			}
		System.err.println( "Updates:"+ counter );
		} catch ( IOException e ) {
			// Got nowhere to write to probably
		}
	}
}
