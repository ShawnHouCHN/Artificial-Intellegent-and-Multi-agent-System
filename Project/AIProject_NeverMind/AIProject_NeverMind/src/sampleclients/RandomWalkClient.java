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
			////System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!I read color "+color);
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
					//System.err.println("Found agent "+id);
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
					//System.err.println("Found Goal "+id);
					if (color==null)
						all_goals.add(new Goal(id, "blue",new int[]{lineN,i}));
					else
						all_goals.add(new Goal(id, colors.get(Character.toUpperCase(id)),new int[]{lineN,i}));
					all_frees[lineN][i] = true;
				}
				
				//save all boxes
				else if ('A' <= id && id <= 'Z'){
					//System.err.println("Found box "+id);
					if (color==null)
						all_boxes.add(new Box(id, "blue",new int[]{lineN,i}));
					else
						all_boxes.add(new Box(id, colors.get(id),new int[]{lineN,i}));
					all_frees[lineN][i] = true;
				}else if (id == ' ') {
					all_frees[lineN][i] = true;
					real_frees[lineN][i] = true;
				}else{
					//System.err.println("Error, read invalid level character: " + (int) i);
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
			
			the_agent.setInitialFrees(real_frees);
			
			the_agent.findAllBoxesGoals(all_boxes,all_goals);
//			for(Box mybox:the_agent.myBoxes.values()){
//				if(mybox.zombie)
//					//System.err.println("Zombie "+mybox.id+" loc:"+mybox);
//			}
			the_agent.createInitSAPlan();
			System.err.println("SA Successful Create plan for agent "+the_agent.id+ ": with length="+the_agent.createInitSAPlan());
		}
		
		else{
		//MA case
			/*****************************Create Plan For Each Legal Agent***********************************/
			for (Box a_box: all_boxes){
				if(!color_agents.keySet().contains(a_box.color)){
					//all_boxes.remove(a_box);
					all_walls[a_box.location[0]][a_box.location[1]]=true;
				}
			}
			
			for (Character agent_id : all_agents.keySet()){
				Agent a_agent=all_agents.get(agent_id);
				int[] init_agent_loc=a_agent.location;
				
				a_agent.setInitialGraph(this.initial_graph);
				
				a_agent.setInitialWalls(all_walls);
				
				a_agent.setInitialFrees(real_frees);
				
				a_agent.findMyBoxes(all_boxes,color_agents);
				//a_agent.printMyBoxes();
				a_agent.findMyGoals(all_goals);

			}			
			
			int longest=0;
			for (Character agent_id : all_agents.keySet()){
				Agent a_agent=all_agents.get(agent_id);
				if(a_agent.myGoals.size()==0 || a_agent.myBoxes.size()==0){
					continue;
				}
				a_agent.createInitPlan();
				//System.err.println("MA Successful Create plan for agent "+a_agent.id+ ": with length="+a_agent.createInitPlan());
				////System.err.println("Location of agent:"+a_agent.id+ " along the plan is:"+a_agent.subplanboard.values().toString());
				
				ranked_all_agents.add(a_agent);	
				
				if(a_agent.solution.size()>longest)
					longest=a_agent.solution.size();
			}
			
			//IF AGENT HAS NO BOX OR GOAL, CREATE EMTPY PLAN FOR THEM.
			for (Character agent_id : all_agents.keySet()){
				Agent a_agent=all_agents.get(agent_id);				
				if(a_agent.myGoals.size()==0 || a_agent.myBoxes.size()==0){
					a_agent.createEmptyPlan(longest+1,0);
					//System.err.println("MA Successful Create EMPTY plan for agent "+a_agent.id+ ": with length="+a_agent.createEmptyPlan(longest+1,0));
					
				}				
			}
			
			/*************************************************************************************************/
			
			
		/*****************************Detect Conflicts among agents' plans********************************/
		//System.err.println("******************************** Start resolving conflicts!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		resolveConflicts();
		/*************************************************************************************************/
		}
		
		//Test
		for (Character agent_id : all_agents.keySet()){
			Agent a_agent=all_agents.get(agent_id);
			////System.err.println("Successful Create plan for agent "+a_agent.id+ ": with length="+a_agent.createInitPlan());
			//System.err.println("Resolved Location of agent:"+a_agent.id+ " init:"+a_agent.init_location[0]+","+a_agent.init_location[1]+" along the plan is:"+a_agent.agent_plan);
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
				
				//DONT CHECK AGAINST ITSELF
				if(subject_agent.id==lightest_agent.id)
					continue;
				
				//CHECK AGAINST PREVIOUSLY CLOSED AGENTS
				else if(resolved_agent.containsKey(subject_agent.id)){
					////System.err.println("******************************** resolving against closed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					int j=0;
					while(j< lightest_agent.plan.size()){		
						
						if(lightest_agent.plan.get(j).actType==type.NoOp){
							////System.err.println("Close J:"+j);
							j++;
							continue;
						}	
						
						else if(isIntoPreviousAgentPlan(subject_agent,lightest_agent,j) && !isOnRescue(lightest_agent,j)){
							////System.err.println("current lightest "+lightest_agent.id+" runs into "+subject_agent.id+" at index "+j+ ":"+lightest_agent.agent_plan.get(j)+lightest_agent.solution.get(j).engagedBox.toString()+"##"+subject_agent.agent_plan.get(j));
							////System.err.println("Before J:"+lightest_agent.plan);
							int waiting_time=3;
							for(int wait=0;wait<waiting_time;wait++){
								lightest_agent.plan.add(j, new Command());							
								lightest_agent.agent_plan.add(j,lightest_agent.agent_plan.get(j));
								lightest_agent.solution.add(j,lightest_agent.solution.get(j));	
								lightest_agent.agent_start_plan.add(j,lightest_agent.agent_start_plan.get(j));	
								lightest_agent.agent_rescue_plan.add(j,lightest_agent.agent_rescue_plan.get(j));
							}
							////System.err.println("After J:"+lightest_agent.plan);
						}
						
						j++;
						
					}
				}
				
				
				//CHECK AGAINST OTHER UNCLOSED AGENTS
				else{	
					////System.err.println("******************************** resolving against opened!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					////System.err.println("Lightest Agent :"+lightest_agent.id);
					int k=0;				
					//INSPECT LIGHTEST AGENT
					while(k< lightest_agent.plan.size()){
						
						
						if(lightest_agent.plan.get(k).actType==type.NoOp){
							k++;
							continue;
						}
						
						
						//IF IT IS A BLOCKING GOAL.. MOVE TO SWHERE CLOSER BUT SAFE. AND PAUSE UNTIL SUBJECT AGENT FINISH USING THE CELL.
						//XXXXXXXX

						
						if(isInQuarantineZone(subject_agent,lightest_agent, k)){
							////System.err.println("Lightest Agent :"+lightest_agent.id+" enters quarantine zone of lightest agent "+subject_agent.id+" at index:"+k+" at location: "+subject_agent.agent_plan.get(k).toString());					
//							if(isGoalBlockQuarantineZone(subject_agent,lightest_agent, k) && !isOnRescue(lightest_agent,k)){
//								//System.err.println("ALERT!!!! Lightest agent:"+lightest_agent.id+" blocking goal!!!! for subject agent "+subject_agent.id+ " at index "+k);
//							}							
//							
							
							if(isInOccupiedZone(subject_agent,lightest_agent,k)){								
								////System.err.println("Lightest agent:"+lightest_agent.id+"It is a dangerous cell at index:"+k+" at"+lightest_agent.agent_plan.get(k));
	
								//IF LIGHTEST RUNS INTO OTEHR AGENT'S BOX ON THE WAY AND NOT DETOUR CONTINUE..
								if(isInOtherAgentBoxes(subject_agent,lightest_agent,k)){
									//System.err.println("Lightest agent:"+lightest_agent.id+" enters a dangerous stationary box at index:"+k+" at"+lightest_agent.agent_plan.get(k));
									//Box block_box=subject_agent.solution.get(k).boxes.get(lightest_agent.agent_plan.get(k));								

									//if the subject agent is in rescue plan, wait for it to finish
									int m=k;
									while(isOnRescue(subject_agent,m)){
										lightest_agent.plan.add(k, new Command());							
										lightest_agent.agent_plan.add(k,lightest_agent.agent_plan.get(k));
										lightest_agent.solution.add(k,lightest_agent.solution.get(k));	
										lightest_agent.agent_start_plan.add(k,lightest_agent.agent_start_plan.get(k));	
										lightest_agent.agent_rescue_plan.add(k,lightest_agent.agent_rescue_plan.get(k));	
										m++;
									}
									
									LinkedList<Node> rescue_plan=subject_agent.createEmergencyPlan(lightest_agent, k);
//									}
//									//System.err.println("Now the agent at:"+subject_agent.location[0]+","+subject_agent.location[1]);
									
									int waiting_time=rescue_plan.size()+1;
									for(int wait=0;wait<waiting_time;wait++){
										lightest_agent.plan.add(k, new Command());							
										lightest_agent.agent_plan.add(k,lightest_agent.agent_plan.get(k));
										lightest_agent.solution.add(k,lightest_agent.solution.get(k));	
										lightest_agent.agent_start_plan.add(k,lightest_agent.agent_start_plan.get(k));	
										lightest_agent.agent_rescue_plan.add(k,lightest_agent.agent_rescue_plan.get(k));
									}
																
									
									int waiting_subject=lightest_agent.solution.size()-k-waiting_time+1;
									subject_agent.createEmptyPlan(waiting_subject,(k+rescue_plan.size()));
									
									if(subject_agent.myBoxes.size()!=0 && subject_agent.myGoals.size()!=0)
									    subject_agent.createDetourAltPlan();
									
								}
								
								
								else if(isInOtherAgent(subject_agent,lightest_agent,k)){
									System.err.println(lightest_agent.id+" Run into subject agent "+subject_agent.id+" at index:"+k+" at "+subject_agent.agent_plan.get(k));
									int waiting_time=lightest_agent.plan.size()-k+1;
									for(int wait=0;wait<waiting_time;wait++){
										lightest_agent.plan.add(k-1, new Command());							
										lightest_agent.agent_plan.add(k-1,lightest_agent.agent_plan.get(k-1));
										lightest_agent.solution.add(k-1,lightest_agent.solution.get(k-1));	
										lightest_agent.agent_start_plan.add(k-1,lightest_agent.agent_start_plan.get(k-1));	
										lightest_agent.agent_rescue_plan.add(k-1,lightest_agent.agent_rescue_plan.get(k-1));
									}
								}
								
								
							}							
						}					
						k++;
					}
					
					
					//System.err.println("Subject Agent "+subject_agent.id+" after:"+subject_agent.agent_plan);
					//System.err.println("Lightest Agent "+lightest_agent.id+" after:"+lightest_agent.agent_plan);

					
					int i=0; //iterate through every other subject agent, make sure resolve the conflict when they enter lightest way.
					//INSPECT OTHER SUBJECT AGENT
					
					while(i < subject_agent.plan.size()){	
						//System.err.println("Start Inspecting Subject Agent "+subject_agent.id);
						//this has to be reviewed !!!!!!!!
						if(subject_agent.myGoals.size()==0){
							i++;
							break;
						}
						
						
						if(subject_agent.plan.get(i).actType==type.NoOp || isOnRescue(subject_agent,i)){
							i++;
							continue;
						}
						//get some current details (box, engaged box, goal,agent location)
						Point subject_agent_loc=subject_agent.agent_plan.get(i);
						Box subject_engaged_box=subject_agent.solution.get(i).engagedBox;
						HashMap<Point,Box> subject_boxes=subject_agent.solution.get(i).boxes;

					
						//if subject agent with/without box enters quarantine zone.
						if(isInQuarantineZone(lightest_agent,subject_agent, i)){				
							////System.err.println("Subject Agent in qurantine :"+subject_agent.agent_plan.get(i));
//							
							
							
//							//check whether the lightest agent gonna use this cell
//							
							if(isGoalBlockQuarantineZone(lightest_agent,subject_agent, i) && !isOnRescue(subject_agent,i)){
								//System.err.println("It is a blocking goal to lightest agent! at index:"+i);
							
								int index_ahead=0;
								int waiting_time=lightest_agent.plan.size()-i;
								if(!isBoxBlockQuarantineZone(lightest_agent,subject_agent, i)){
									index_ahead=i-1;
									for(int wait=0;wait<waiting_time;wait++){
										subject_agent.plan.add(index_ahead, new Command());							
										subject_agent.agent_plan.add(index_ahead,subject_agent.agent_plan.get(index_ahead));
										subject_agent.solution.add(index_ahead,subject_agent.solution.get(index_ahead));
										subject_agent.agent_start_plan.add(index_ahead,subject_agent.agent_start_plan.get(index_ahead));
										subject_agent.agent_rescue_plan.add(index_ahead,subject_agent.agent_rescue_plan.get(index_ahead));
									}
									break;
								}else{
									for(int wait=0;wait<waiting_time;wait++){
										subject_agent.plan.add(i, new Command());							
										subject_agent.agent_plan.add(i,subject_agent.agent_plan.get(i));
										subject_agent.solution.add(i,subject_agent.solution.get(i));
										subject_agent.agent_start_plan.add(i,subject_agent.agent_start_plan.get(i));
										subject_agent.agent_rescue_plan.add(i,subject_agent.agent_rescue_plan.get(i));
									}
									break;									
								}
								
								
							}						
//							
							//if the subject agent/box enters a safe cell in QuarantineZone of lightest agent
							else if(!isInOccupiedZone(lightest_agent,subject_agent,i)){
								//System.err.println("It is not an occupied cell! keep going:");
							}
							
							else if(isInOccupiedZone(lightest_agent,subject_agent,i)){
								//System.err.println("Running into other occupations:");
								
								
								//int entry=i;
								
//								while(true){
//									
//								}
								if(isInOtherAgent(lightest_agent,subject_agent,i) && i<lightest_agent.plan.size()){
									//System.err.println(subject_agent.id+" Run into lightest agent "+lightest_agent.id+" at index:"+i+" at "+lightest_agent.agent_plan.get(i));
									int waiting_time=lightest_agent.plan.size()-i+1;
									for(int wait=0;wait<waiting_time;wait++){
										subject_agent.plan.add(i, new Command());							
										subject_agent.agent_plan.add(i,subject_agent.agent_plan.get(i));
										subject_agent.solution.add(i,subject_agent.solution.get(i));	
										subject_agent.agent_start_plan.add(i,subject_agent.agent_start_plan.get(i));	
										subject_agent.agent_rescue_plan.add(i,subject_agent.agent_rescue_plan.get(i));
									}
									continue;
								}
								
								LinkedList<Node> singlesolution=subject_agent.createDetourPlan(lightest_agent, i);	
								if(singlesolution==null || singlesolution.size()==0){					
									//System.err.println("No Detour!!!! at "+i);
									//the lightest box so far will block another subject's way at this step of that subject, so the lightest agent should halt the execution. 
									int light_enter=0;
									
									//know when lightest going into this subject qurantine zone 
									for(int j=0;j<=i;j++)
										if(isInQuarantineZone(subject_agent, lightest_agent, j)){
											light_enter=j;	
											break;
										}
									
									int waiting_time=subject_agent.plan.size();
									for(int wait=0;wait<waiting_time;wait++){
										lightest_agent.plan.add(light_enter, new Command());							
										lightest_agent.agent_plan.add(light_enter,lightest_agent.agent_plan.get(light_enter));
										lightest_agent.solution.add(light_enter,lightest_agent.solution.get(light_enter));										
									}
									////System.err.println("It is not a valid lightest agent! find next:");
									////System.err.println("Now the lighest agent"+lightest_agent.id+" plan is:"+lightest_agent.plan);
									break;
								}
							
								//if there is a detour
								else{
									if(subject_agent.myBoxes.size()!=0 && subject_agent.myGoals.size()!=0)
										subject_agent.createDetourAltPlan();
								}								
							}
							
						}
						i++;
					}
				}
			}
			
		}
	}
	
	
	
	
	public boolean isOnRescue(Agent subject_agent,int index){
		if(index>=subject_agent.agent_rescue_plan.size())
			return subject_agent.agent_rescue_plan.getLast();
		else
			return subject_agent.agent_rescue_plan.get(index);
	}
	
	public boolean isIntoPreviousAgentPlan(Agent subject_agent,Agent lightest_agent,int index){
		Box lightest_agent_engaged_box=null, lightest_next_engaged_box=null;
		Box subject_agent_engaged_box=null;
		Point lightest_agent_loc,lightest_agent_next_loc;
		Point subject_agent_loc;
		Point subject_agent_init;
		if(index>=lightest_agent.solution.size()-1){
			lightest_agent_engaged_box=lightest_agent.solution.getLast().engagedBox;
			lightest_next_engaged_box=lightest_agent.solution.getLast().engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.getLast();
			lightest_agent_next_loc=lightest_agent.agent_plan.getLast();
		}
		else if(index==0){
			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_next_engaged_box=lightest_agent.solution.get(index+1).engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
			lightest_agent_next_loc=lightest_agent.agent_plan.get(index+1);
		}
		else{
			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_next_engaged_box=lightest_agent.solution.get(index+1).engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
			lightest_agent_next_loc=lightest_agent.agent_plan.get(index+1);
		}
				
		
		if(index>=subject_agent.agent_plan.size()-1){
			subject_agent_engaged_box=subject_agent.solution.getLast().engagedBox;
			subject_agent_loc=subject_agent.agent_plan.getLast();
		}
		else if(index==0){
			subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
			subject_agent_loc=subject_agent.agent_plan.get(index);
		}
		else{
			subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
			subject_agent_loc=subject_agent.agent_plan.get(index);
		}	
		
		subject_agent_init=new Point(subject_agent.init_location[0],subject_agent.init_location[1]);

		
		if(lightest_agent_loc.equals(subject_agent_loc) || lightest_agent_next_loc.equals(subject_agent_loc)){
			////System.err.println("A");
			return true;
		}
		else if(subject_agent_loc.equals(new Point(lightest_agent_engaged_box.location[0],lightest_agent_engaged_box.location[1])) || subject_agent_loc.equals(new Point(lightest_next_engaged_box.location[0],lightest_next_engaged_box.location[1]))){
			////System.err.println("B");
			return true;
		}else if(lightest_agent_engaged_box.location[0]==subject_agent_engaged_box.location[0] && lightest_agent_engaged_box.location[1]==subject_agent_engaged_box.location[1] && 
				(Math.abs(lightest_agent_loc.x-lightest_agent_engaged_box.location[0])+Math.abs(lightest_agent_loc.y-lightest_agent_engaged_box.location[1]))==1){
			////System.err.println("C");
			return true;
		}
		else if(lightest_next_engaged_box.location[0]==subject_agent_engaged_box.location[0] && lightest_next_engaged_box.location[1]==subject_agent_engaged_box.location[1] && 
				(Math.abs(lightest_agent_loc.x-lightest_next_engaged_box.location[0])+Math.abs(lightest_agent_loc.y-lightest_next_engaged_box.location[1]))==1){
			////System.err.println("D");
			return true;		
		}else if(index==0 && (subject_agent_init.equals(lightest_agent_loc) || subject_agent_init.equals(lightest_agent_next_loc))){
			////System.err.println("F");		
			return true;
		}
		else
			return false;
		
	}
	
	public boolean isInOtherEngageBox(Agent subject_agent,Agent lightest_agent,int index){
		LinkedList<Point> lightest_agent_path=lightest_agent.agent_plan;
		Box lightest_last_engaged_box=null, lightest_agent_engaged_box=null,lightest_next_engaged_box=null;
		Box subject_last_engaged_box=null, subject_agent_engaged_box=null, subject_next_engaged_box=null;
		Point subject_engaged_box_loc=null;
		Point lightest_engaged_box_loc=null;
		Point lightest_agent_loc=null;
		Point subject_agent_loc=null;
		
		if(index>=lightest_agent.solution.size()-1){
			lightest_agent_engaged_box=lightest_agent.solution.getLast().engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.getLast();
			
		}
		else if(index==0){
			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
		}
		else{
			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
		}
		
				
		
		if(index>=subject_agent.agent_plan.size()-1){
			subject_agent_engaged_box=subject_agent.solution.getLast().engagedBox;
			subject_agent_loc=subject_agent.agent_plan.getLast();
		}
		else if(index==0){
			subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
			subject_agent_loc=subject_agent.agent_plan.get(index);
		}
		else{
			subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
			subject_agent_loc=subject_agent.agent_plan.get(index);
		}
		
		if(subject_agent_engaged_box!=null && lightest_agent_engaged_box!=null){
			subject_engaged_box_loc=new Point(subject_agent_engaged_box.location[0], subject_agent_engaged_box.location[1]);	
			lightest_engaged_box_loc=new Point(lightest_agent_engaged_box.location[0], lightest_agent_engaged_box.location[1]);	
		}else{
			subject_engaged_box_loc=new Point(0,0); //if engaged box does not exist, do this defualt setup
			lightest_engaged_box_loc=new Point(0,0);
		}		
		
		
		int dis=Math.abs(subject_engaged_box_loc.x-lightest_engaged_box_loc.x)+Math.abs(subject_engaged_box_loc.y-lightest_engaged_box_loc.y);
		if(dis<=1){
			//System.err.println("Agent "+subject_agent.id+" and Agent "+lightest_agent.id+"  boxes run into each other at "+subject_agent_loc+" index "+index);
			return true;
		}else{
			return false;
		}
			
		
	}
	
	public boolean isInOtherAgent(Agent subject_agent,Agent lightest_agent,int index){

		LinkedList<Point> lightest_agent_path=lightest_agent.agent_plan;
		Box lightest_agent_engaged_box=null;
		Box subject_agent_engaged_box=null;
		Point subject_engaged_box_loc=null;
		Point lightest_engaged_box_loc=null;
		Point lightest_last_loc=null, lightest_agent_loc=null, lightest_next_loc=null;
		Point subject_last_loc=null, subject_agent_loc=null, subject_next_loc=null;
		
		if(index>=lightest_agent.solution.size()-1){
			lightest_agent_engaged_box=lightest_agent.solution.getLast().engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.getLast();
			lightest_next_loc=lightest_agent.agent_plan.getLast();
			lightest_last_loc=lightest_agent.agent_plan.getLast();
			
		}
		else if(index==0){
			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
			lightest_next_loc=lightest_agent.agent_plan.get(index+1);
			lightest_last_loc=new Point(lightest_agent.init_location[0],lightest_agent.init_location[1]);
		}
		else{
			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_last_loc=lightest_agent.agent_plan.get(index-1);
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
			lightest_next_loc=lightest_agent.agent_plan.get(index+1);
		}
		
		
		
		
		if(index>=subject_agent.agent_plan.size()-1){
			subject_agent_engaged_box=subject_agent.solution.getLast().engagedBox;
			subject_agent_loc=subject_agent.agent_plan.getLast();
			subject_last_loc=subject_agent.agent_plan.getLast();
			subject_next_loc=subject_agent.agent_plan.getLast();
		}
		else if(index==0){
			subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
			subject_agent_loc=subject_agent.agent_plan.get(index);
			subject_last_loc=new Point(subject_agent.init_location[0], subject_agent.init_location[1]);
			subject_next_loc=subject_agent.agent_plan.get(index+1);
		}
		else{
			subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
			subject_agent_loc=subject_agent.agent_plan.get(index);
			subject_last_loc=subject_agent.agent_plan.get(index-1);
			subject_next_loc=subject_agent.agent_plan.get(index+1);
		}
		
		if(subject_agent_engaged_box!=null && lightest_agent_engaged_box!=null){
			subject_engaged_box_loc=new Point(subject_agent_engaged_box.location[0], subject_agent_engaged_box.location[1]);	
			lightest_engaged_box_loc=new Point(lightest_agent_engaged_box.location[0], lightest_agent_engaged_box.location[1]);	
		}else{
			subject_engaged_box_loc=new Point(0,0); //if engaged box does not exist, do this defualt setup
			lightest_engaged_box_loc=new Point(0,0);
		}
		////System.err.println("Index"+index+ " Subject Three location:"+ subject_last_loc+ ","+subject_agent_loc + ","+ subject_next_loc);
		////System.err.println("Index"+index+ " Lightest Three location:"+ lightest_last_loc+ ","+lightest_agent_loc + ","+ lightest_next_loc);
		////System.err.println("Subject agent:"+ subject_agent.id+ ","+subject_agent_loc+" engagedbox:"+subject_engaged_box_loc+" lightest agent: "+lightest_agent.id+" "+lightest_agent_loc+" engagedbox:"+lightest_engaged_box_loc);
		////System.err.println("Check:"+subject_last_loc+subject_agent_loc+subject_next_loc+"------"+lightest_last_loc+lightest_agent_loc+lightest_next_loc);
		////System.err.println("Subject agent:"+subject_agent.id+" at:"+subject_agent.agent_plan.get(index)+" Lightest agent:"+lightest_agent.id+" at:"+lightest_agent.agent_plan.get(index));
		if(!subject_agent_loc.equals(lightest_agent_loc) &&
		   !subject_agent_loc.equals(lightest_next_loc) &&
		   !subject_last_loc.equals(lightest_agent_loc) &&
		   !subject_last_loc.equals(lightest_last_loc) &&
		   !subject_agent_loc.equals(lightest_last_loc) &&
		   !subject_next_loc.equals(lightest_agent_loc) &&
		   !subject_next_loc.equals(lightest_next_loc) 
		   )
			return false;
		
		
		
		else 
			return true;	
	}
	
	public boolean isInOtherAgentBoxes(Agent subject_agent,Agent lightest_agent,int index){
		HashMap<Point, Box> lightest_agent_boxes;
		HashMap<Point, Goal> lightest_agent_goals;
		LinkedList<Point> lightest_agent_path=lightest_agent.agent_plan;
		Box lightest_agent_engaged_box=null,lightest_next_engaged_box=null;
		Box subject_agent_engaged_box=null;
		Point subject_engaged_box_loc=null;
		Point lightest_agent_loc=null;
		Point subject_agent_loc=null;
		HashMap<Point, Box> subject_agent_boxes;
		HashMap<Point, Goal> subject_agent_goals;
		
		if(index>=lightest_agent.solution.size()-1){
			lightest_agent_boxes=lightest_agent.solution.getLast().boxes;
			lightest_agent_goals=lightest_agent.solution.getLast().goals;
			lightest_agent_engaged_box=lightest_agent.solution.getLast().engagedBox;
			lightest_next_engaged_box=lightest_agent.solution.getLast().engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.getLast();
			
		}
		else if(index==0){
			lightest_agent_boxes=lightest_agent.solution.get(index).boxes;
			lightest_agent_goals=lightest_agent.solution.get(index).goals;
			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_next_engaged_box=lightest_agent.solution.get(index+1).engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
		}
		else{
			lightest_agent_boxes=lightest_agent.solution.get(index).boxes;
			lightest_agent_goals=lightest_agent.solution.get(index).goals;
			lightest_agent_engaged_box=lightest_agent.solution.get(index).engagedBox;
			lightest_next_engaged_box=lightest_agent.solution.get(index+1).engagedBox;
			lightest_agent_loc=lightest_agent.agent_plan.get(index);
		}
		
		subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
		
		
		if(index>=subject_agent.agent_plan.size()-1){
			subject_agent_loc=subject_agent.agent_plan.getLast();
			subject_agent_boxes=subject_agent.solution.getLast().boxes;
			subject_agent_goals=subject_agent.solution.getLast().goals;
			subject_agent_engaged_box=subject_agent.solution.getLast().engagedBox;
		}
		if(index==0){
			subject_agent_loc=subject_agent.agent_plan.get(index);
			subject_agent_boxes=subject_agent.solution.get(index).boxes;
			subject_agent_goals=subject_agent.solution.get(index).goals;
			subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
		}
		else{
			subject_agent_loc=subject_agent.agent_plan.get(index);
			subject_agent_boxes=subject_agent.solution.get(index).boxes;
			subject_agent_goals=subject_agent.solution.get(index).goals;
			subject_agent_engaged_box=subject_agent.solution.get(index).engagedBox;
			//subject_next_loc=subject_agent.agent_plan.get(index+1);
		}
		
		////System.err.println("Subject agent "+subject_agent.id+ " engage:"+subject_agent_engaged_box);
		////System.err.println("Subject agent:"+ subject_agent.id+ ","+subject_agent_loc+" engagedbox:"+subject_engaged_box_loc+" lightest agent: "+lightest_agent.id+" "+lightest_agent_loc+" engagedbox:"+lightest_engaged_box_loc);
		////System.err.println("Check:"+subject_last_loc+subject_agent_loc+subject_next_loc+"------"+lightest_last_loc+lightest_agent_loc+lightest_next_loc);
		////System.err.println("Subject agent:"+subject_agent.id+" at:"+subject_agent.agent_plan.get(index)+" Lightest agent:"+lightest_agent.id+" at:"+lightest_agent.agent_plan.get(index));
		if(!subject_agent_boxes.keySet().contains(lightest_agent_loc) && !subject_agent_boxes.keySet().contains(new Point(lightest_agent_engaged_box.location[0],lightest_agent_engaged_box.location[1])))
			return false;	
//		if(subject_agent_engaged_box.location[0]==lightest_agent_engaged_box.location[0] && subject_agent_engaged_box.location[1]==lightest_agent_engaged_box.location[1])
//			return false;
		else 
			return true;
			
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
		else if(index==0){
			subject_agent_loc=subject_agent.agent_plan.get(index);
			subject_last_loc=new Point(subject_agent.init_location[0], subject_agent.init_location[1]);
			subject_next_loc=subject_agent.agent_plan.get(index+1);
		}
		else{
			subject_agent_loc=subject_agent.agent_plan.get(index);
			subject_last_loc=subject_agent.agent_plan.get(index-1);
			subject_next_loc=subject_agent.agent_plan.get(index+1);
		}
		
		if(subject_agent_engaged_box!=null)
			subject_engaged_box_loc=new Point(subject_agent_engaged_box.location[0], subject_agent_engaged_box.location[1]);	
		
		////System.err.println("Index"+index+ " Subject Three location:"+ subject_last_loc+ ","+subject_agent_loc + ","+ subject_next_loc);
		////System.err.println("Index"+index+ " Lightest Three location:"+ lightest_last_loc+ ","+lightest_agent_loc + ","+ lightest_next_loc);
		////System.err.println("Subject agent:"+ subject_agent.id+ ","+subject_agent_loc+" engagedbox:"+subject_engaged_box_loc+" lightest agent: "+lightest_agent.id+" "+lightest_agent_loc+" engagedbox:"+lightest_engaged_box_loc);
		////System.err.println("Check:"+subject_last_loc+subject_agent_loc+subject_next_loc+"------"+lightest_last_loc+lightest_agent_loc+lightest_next_loc);
		////System.err.println("Subject agent:"+subject_agent.id+" at:"+subject_agent.agent_plan.get(index)+" Lightest agent:"+lightest_agent.id+" at:"+lightest_agent.agent_plan.get(index));
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
		   !subject_last_loc.equals(lightest_agent_loc) &&
		   !subject_last_loc.equals(lightest_last_loc) &&
		   !subject_agent_loc.equals(lightest_last_loc) &&
		   !subject_next_loc.equals(lightest_agent_loc) &&
		   !subject_next_loc.equals(lightest_next_loc))
			return false;
		
		
		
		else 
			return true;
		
		
	}
	
	public boolean isInQuarantineZone(Agent lightest_agent, Agent subject_agent, int index){
		
		////System.err.println("Ligtest is "+lightest_agent.id+" Subject agent is "+subject_agent.id);
		
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
		Point subject_agent_loc=null;
		//if (subject_agent.solution.get(index).engagedBox!=null)
			//engaged_box_loc=new Point(subject_agent.solution.get(index).engagedBox.location[0], subject_agent.solution.get(index).engagedBox.location[1]);
		
		if(index>=subject_agent.solution.size()){
			subject_agent_loc=subject_agent.agent_plan.getLast();
			engaged_box_loc=new Point(subject_agent.solution.getLast().engagedBox.location[0], subject_agent.solution.getLast().engagedBox.location[1]);
		}else{
			subject_agent_loc=subject_agent.agent_plan.get(index);
			engaged_box_loc=new Point(subject_agent.solution.get(index).engagedBox.location[0], subject_agent.solution.get(index).engagedBox.location[1]);
		}
		
		////System.err.println("Location is: "+lightest_agent_goals.keySet().contains(engaged_box_loc));
		////System.err.println("Location is: "+lightest_agent_boxes.keySet());
		if(lightest_agent_path.contains(subject_agent_loc) || lightest_agent_boxes.keySet().contains(subject_agent_loc) || lightest_agent_goals.keySet().contains(subject_agent_loc)){
			//System.err.println("Agent "+subject_agent.id+" enters at "+subject_agent_loc+" index "+index);
			return true;
		}
		
		else if(engaged_box_loc!=null && (lightest_agent_path.contains(engaged_box_loc) || lightest_agent_boxes.keySet().contains(engaged_box_loc) || lightest_agent_goals.keySet().contains(engaged_box_loc))){
			//System.err.println("Agent  "+subject_agent.id+"'s engaged box "+subject_agent.solution.get(index).engagedBox+" enters at "+ engaged_box_loc+" index "+index);
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean isBoxBlockQuarantineZone(Agent lightest_agent, Agent subject_agent, int index){
		//System.err.println("Subject Agent :"+subject_agent.id+" current engaged box at:"+subject_agent.solution.get(index).engagedBox+ " at index:"+index);
		Box subject_curr_box=subject_agent.solution.get(index).engagedBox;
		Goal lightest_curr_goal;
		int[] agent_start_loc;
		if (index<lightest_agent.plan.size()){
			lightest_curr_goal=lightest_agent.solution.get(index).currentGoal;
			agent_start_loc=lightest_agent.agent_start_plan.get(index);
		}else{
			lightest_curr_goal=lightest_agent.solution.getLast().currentGoal;
			agent_start_loc=lightest_agent.agent_start_plan.getLast();
		}
		
		
//		//System.err.println("Agent :"+subject_agent.id+" current goal at: "+
//		subject_agent.currentGoal.toString()+" blocked lightest agent "+lightest_agent.id+","+lightest_curr_loc+" to reach: "+lightest_curr_goal+
//		"##DIS:"+initial_level_grid.getBFSPesudoDistance(lightest_curr_goal.location, lightest_agent.init_location, lightest_agent.myGraph));
		
		
		lightest_agent.myGraph.get(subject_curr_box.hashCode()).setLock(true);
		if (initial_level_grid.getBFSPesudoDistance(lightest_curr_goal.location,agent_start_loc, lightest_agent.myGraph)>=Grid.LOCK_THRESHOLD){
			lightest_agent.myGraph.get(subject_curr_box.hashCode()).setLock(false);
			return true;
		}else{
			lightest_agent.myGraph.get(subject_curr_box.hashCode()).setLock(false);
			return false;
		}
	}
	
	
	public boolean isGoalBlockQuarantineZone(Agent lightest_agent, Agent subject_agent, int index){
		Goal subject_curr_goal=subject_agent.solution.get(index).currentGoal;
		Point subject_curr_loc=subject_agent.agent_plan.get(index);
		Goal lightest_curr_goal;
		int[] agent_start_loc;
		if (index<lightest_agent.plan.size()){
			lightest_curr_goal=lightest_agent.solution.get(index).currentGoal;
			agent_start_loc=lightest_agent.agent_start_plan.get(index);
		}else{
			lightest_curr_goal=lightest_agent.solution.getLast().currentGoal;
			agent_start_loc=lightest_agent.agent_start_plan.getLast();
		}
		
		
//		//System.err.println("Agent :"+subject_agent.id+" current goal at: "+
//		subject_agent.currentGoal.toString()+" blocked lightest agent "+lightest_agent.id+","+lightest_curr_loc+" to reach: "+lightest_curr_goal+
//		"##DIS:"+initial_level_grid.getBFSPesudoDistance(lightest_curr_goal.location, lightest_agent.init_location, lightest_agent.myGraph));
		
		
		lightest_agent.myGraph.get(subject_curr_goal.hashCode()).setLock(true);
		//lightest_agent.myGraph.get(((subject_curr_loc.x + subject_curr_loc.y)*(subject_curr_loc.x + subject_curr_loc.y + 1))/2 + subject_curr_loc.y).setLock(true);
		if (initial_level_grid.getBFSPesudoDistance(lightest_curr_goal.location,agent_start_loc, lightest_agent.myGraph)>=Grid.LOCK_THRESHOLD){
			lightest_agent.myGraph.get(subject_curr_goal.hashCode()).setLock(false);
			//lightest_agent.myGraph.get(((subject_curr_loc.x + subject_curr_loc.y)*(subject_curr_loc.x + subject_curr_loc.y + 1))/2 + subject_curr_loc.y).setLock(false);
			return true;
		}else{
			lightest_agent.myGraph.get(subject_curr_goal.hashCode()).setLock(false);
			//lightest_agent.myGraph.get(((subject_curr_loc.x + subject_curr_loc.y)*(subject_curr_loc.x + subject_curr_loc.y + 1))/2 + subject_curr_loc.y).setLock(f);
			return false;
		}
		
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
			//System.err.println( Command.every[i]);
		}
		try {
			RandomWalkClient client = new RandomWalkClient();
			int counter=0;
			while ( client.update() ){
				counter++;
			}
		//System.err.println( "Updates:"+ counter );
		} catch ( IOException e ) {
			// Got nowhere to write to probably
		}
	}
}
