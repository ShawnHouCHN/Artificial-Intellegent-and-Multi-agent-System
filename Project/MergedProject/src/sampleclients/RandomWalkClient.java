package sampleclients;

import java.awt.Point;
import java.io.*;
import java.util.*;

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
	public HashMap<String,ArrayList<Agent>> color_agents=new HashMap<String,ArrayList<Agent>>();
	public List< Goal > all_goals = new ArrayList< Goal >();
	public List< Box > all_boxes = new ArrayList< Box >();
	public HashMap<Integer,Vertex> initial_graph=new HashMap<Integer,Vertex>();
	public static boolean[][] all_walls=null;
	boolean[][] all_frees=null;
	public static Grid initial_level_grid=null; //this is the grid to save all distance between any pair of locations 
		
	
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
			a_agent.findMyBoxes(all_boxes,color_agents);
			//a_agent.printMyBoxes();
			a_agent.findMyGoals(all_goals);
			//a_agent.printMyGoals();
			a_agent.setInitialWalls(all_walls);
			a_agent.setInitialGraph(this.initial_graph);
			System.err.println("Create plan for agent "+a_agent.id+ ": with length="+a_agent.createPlan());
		}
		
		/******************************************************************************************/
		
	
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
