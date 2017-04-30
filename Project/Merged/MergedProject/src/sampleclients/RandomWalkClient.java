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
	public static Grid initial_level_grid=null; //this is the grid to save all distance between any pair of locations 
		
	public List<String> conflicts = null;

	
	public static int NOOP_LOW_THRESHOLD = 2; //set to 3 
	public static int NOOP_HIGH_THRESHOLD = 5; //set to 3 
	
	public RandomWalkClient() throws IOException {
		readMap();
	}

	private void readMap() throws IOException {
		Map< Character, String > colors = new HashMap< Character, String >();		
		String line, color;
		
		//compute the dimension of the level file in the first place.
		int[] dimension=this.getLevelDimension(in);
		all_walls = new boolean[dimension[0]][dimension[1]];
		all_frees = new boolean[dimension[0]][dimension[1]]; //pesudo free cell, everywhere other than walls

		
		/*****************************Read In the Level Layout************************************/
		// Read lines specifying colors
		int lineN=0;
		while ( ( line = in.readLine() ).matches( "^[a-z]+:\\s*[0-9A-Z](,\\s*[0-9A-Z])*\\s*$" ) ) {
			line = line.replaceAll( "\\s", "" );
			color = line.split( ":" )[0];

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
					Agent new_agent =new Agent(id, colors.get(id),new int[]{lineN,i});
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
					all_goals.add(new Goal(id, colors.get(Character.toUpperCase(id)),new int[]{lineN,i}));
					all_frees[lineN][i] = true;
				}
				
				//save all boxes
				else if ('A' <= id && id <= 'Z'){
					System.err.println("Found box "+id);
					all_boxes.add(new Box(id, colors.get(id),new int[]{lineN,i}));
					all_frees[lineN][i] = true;
				}else if (id == ' ') {
					all_frees[lineN][i] = true;
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
		
		
		
		/*****************************Create Plan For Each Legal Agent***********************************/
		for (Character agent_id : all_agents.keySet()){
			Agent a_agent=all_agents.get(agent_id);
			int[] init_agent_loc=a_agent.location;
			
			a_agent.setInitialGraph(this.initial_graph);
			
			a_agent.setInitialWalls(all_walls);
			
			a_agent.findMyBoxes(all_boxes,color_agents);

			a_agent.findMyGoals(all_goals);

			
			//			for (Node a: a_agent.solution){
//				System.err.println("The boxes at this momnet:"+a.boxes.keySet().toString());
//			}
		}
		
		for (Character agent_id : all_agents.keySet()){
			Agent a_agent=all_agents.get(agent_id);
			System.err.println("Successful Create plan for agent "+a_agent.id+ ": with length="+a_agent.createPlan());
			System.err.println("Location of agent:"+a_agent.id+ " along the plan is:"+a_agent.agent_plan.toString());
			
			ranked_all_agents.add(a_agent);	
			
		}
		
		/*************************************************************************************************/
		
		
		/*****************************Detect Conflicts among agents' plans********************************/
		resolveConflicts();
		/*************************************************************************************************/
	
	}
	
	public void resolveConflicts(){
		while(ranked_all_agents.size()!=0){
			Agent lightest_agent=ranked_all_agents.poll();
			for (Entry<Character, Agent> agentA: all_agents.entrySet()){
				Agent subject_agent=agentA.getValue();
				int detour_length=0;
				if(subject_agent.id==lightest_agent.id)
					continue;
				else{
					int i=0;
					while(i < subject_agent.solution.size()){
						Point subject_agent_loc=subject_agent.agent_plan.get(i);
						Box subject_engaged_box=subject_agent.solution.get(i).currentBox;
						//if subject agent at this step is in the quarantine zone.
						if(isInQuarantineZone(lightest_agent,subject_agent_loc,subject_engaged_box, i)){	
							System.err.println("Agent :"+subject_agent.id+" enters quarantine zone of "+lightest_agent.id+" at index:"+i+" at location: "+subject_agent.agent_plan.get(i).toString());
							int waiting_time=lightest_agent.plan.size()-i;
							//if(isGoalInQuarantineZone())
							//Do a replan + noops. 
							subject_agent.createDetourPlan(lightest_agent, i);
											
//							
//							for(int wait=0;wait<waiting_time;wait++){
//								subject_agent.plan.add(i, new Command());
//								subject_agent.agent_plan.add(i,subject_agent.agent_plan.get(i));
//								//subject_agent.solution.add(i,subject_agent.solution.get(i));
//							}
							//break;
						}
						i++;
					}
				}
				if(detour_length==0)
					break;
			}
			
		}
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
		 
		
		Point engaged_box_loc=new Point(subject_engaged_box.location[0], subject_engaged_box.location[1]);
		if(lightest_agent_path.contains(subject_agent_loc) || lightest_agent_boxes.keySet().contains(subject_agent_loc) || lightest_agent_goals.keySet().contains(subject_agent_loc)){
			return true;
		}else if(lightest_agent_path.contains(engaged_box_loc) || lightest_agent_boxes.keySet().contains(engaged_box_loc) || lightest_agent_goals.keySet().contains(engaged_box_loc)){
			return true;
		}
		else{
			return false;
		}
	}
	
	public void detectConflicts() {

		for (Entry<Character, Agent> agentA: all_agents.entrySet()){
			Agent agent_one=agentA.getValue();
			
			for (Entry<Character, Agent> agentB: all_agents.entrySet()){
				Agent agent_two = agentB.getValue();
				
				if(agent_one.id >= agent_two.id)
					continue;
				
				try {
					int i=0;
					
					while(i < Math.max(agent_one.agent_plan.size(), agent_two.agent_plan.size())) {
						//agent one = agent with smaller id / agent two = agent with greater id.
						Point agentone_loc=agent_one.agent_plan.get(i);
						Point agentone_next_loc=agent_one.agent_plan.get(i+1);
						Point agenttwo_loc=agent_two.agent_plan.get(i); 
						Point agenttwo_next_loc=agent_two.agent_plan.get(i+1);
						HashMap<Point, Box> agentone_iboxes=agent_one.solution.get(i).boxes;
						HashMap<Point, Box> agenttwo_iboxes=agent_two.solution.get(i).boxes;
						Box agentone_currentbox=agent_one.solution.get(i).currentBox;
						Box agenttwo_currentbox=agent_two.solution.get(i).currentBox;
						Command agentone_command=agent_one.solution.get(i).action;
						Command agenttwo_command=agent_two.solution.get(i).action;
						//Case 1: both agents go to same loc/ or an agent next move walks into another agent current place
						if((agentone_loc.equals(agenttwo_loc) || agentone_loc.equals(agenttwo_next_loc) || agentone_next_loc.equals(agenttwo_loc))) {
							System.err.println("CONFLICT DETECTED BETWEEN AGENTS: " + agent_one.id + " & " + agent_two.id + ", @ " + agent_two.agent_plan.get(i) + " AT INDEX " + i);												
							int waiting_time=noopAndWaitConflict(agent_one, agent_two, i);					
							
							//if waiting time == MAX THRESHOLD, it means one agent has terminated. do replan to put that agent to a spare location.
							//this feed
							
							
							//else
							// Add NoOp
							for(int wait=0;wait<3;wait++){
								agent_one.plan.add(i, new Command());
								agent_one.agent_plan.add(i,agent_one.agent_plan.get(i));
								agent_one.solution.add(i,agent_one.solution.get(i));
							}
							
							//########
							
							
							
							System.err.println("AGENT " + agent_one.id + ", NEW PLAN (Two agents, same locs): " + agent_one.plan.toString() + "\n");
							
						}
						
						
//						//Case 4, agent two goes to a stationary box belonged to agent one
//						else if(agentone_iboxes.containsKey(agenttwo_loc) || agentone_iboxes.containsKey(agenttwo_next_loc)){
//							System.err.println("CONFLICT DETECTED BETWEEN AGENTS: "  + agent_two.id + " runs into " + agent_one.id+ " Boxes " + agent_one.agent_plan.get(i) + " AT INDEX " + i);
//						
//							//wait and see for a moment and then detour for agent two
//						
//						}
//						
//						//Case 5, agent one goes to a stationary box belonged to agent two
//						else if(agenttwo_iboxes.containsKey(agentone_loc) || agenttwo_iboxes.containsKey(agentone_next_loc)){
//							System.err.println("CONFLICT DETECTED BETWEEN AGENTS: "  + agent_one.id + " runs into " + agent_two.id+ " Boxes " + agent_two.agent_plan.get(i) + " AT INDEX " + i);
//						}
						i++;
						
					} 
					
				}catch(Exception e) {
						// OUT OF BOUNDS
						System.err.println("OUT OF BOUNDS");
					}
					
				}
				
			}
		
		
	}

	//recursive code checking whether there is a conflict in the further steps
//	public int noopAndReviewConflict(Agent defendant, Agent complainant, int step_index){
//		int waiting_length;
//		int com_rest=complainant.agent_plan.size()-step_index;
//		while((step_index+NOOP_LOW_THRESHOLD)<=complainant.agent_plan.size()){
//			Point def_agent_loc=defendant.agent_plan.get(step_index);
//			Point com_agent_loc=complainant.agent_plan.get(step_index+waiting_steps);	
//			if(def_agent_loc.equals(com_agent_loc)){
//				waiting_length=NOOP_LOW_THRESHOLD*2;
//				return noopAndReviewConflict(waiting_length, defendant, complainant, step_index);
//			}else{
//				return waiting_steps;
//			}
//		}
//		
//
//		return 0;
//	}
	
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
