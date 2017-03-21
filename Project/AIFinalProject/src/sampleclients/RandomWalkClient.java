package sampleclients;

import java.io.*;
import java.util.*;


public class RandomWalkClient {
	private static Random rand = new Random();
	private BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
	public List< Agent > agents = new ArrayList< Agent >();
	public List< Goal > goals = new ArrayList< Goal >();
	public List< Box > boxes = new ArrayList< Box >();
	public HashMap<String,Vertex> dij_graph=new HashMap<String,Vertex>();
	boolean[][] walls=null;
	boolean[][] frees=null;
	Grid grid=null; //this is the grid to save all distance between any pair of locations 
	//agent class 
	public class Agent {
		char id;
		String color;
		int[] location;
		public List< Goal > myGoals = new ArrayList< Goal >();
		public List< Box > myBoxes = new ArrayList< Box >();
		public Agent( char id, String color , int[] location) {
			System.err.println("Found " + color + " agent " + id + " Location " + location[0]+","+location[1]);
			this.id = id;
			this.color=color;
			this.location=location;
		}

		public String act() {
			int randomN = rand.nextInt( Command.every.length );
			System.err.println(Command.every[randomN].toString());
			return "NoOp";
		}

		public void findMyGoals(List< Goal > goals){
			for (int i = 0; i < goals.size(); i++){
				if(goals.get(i).color.equals(this.color)){
					myGoals.add(goals.get(i));
				}
			}
		}
		public void findMyBoxes(List< Box > boxes){
			for (int i = 0; i < boxes.size(); i++){
				if(boxes.get(i).color.equals(this.color)){
					myBoxes.add(boxes.get(i));
				}
			}
		}
		public String printMyBoxes(){
			String s="";
			for(int i=0; i<myBoxes.size();i++){
				s+=myBoxes.get(i).id+"\n";
			}
			System.err.println("my ID: "+id+" my boxes: "+s);
			return s;
		}
		public String printMyGoals(){
			String s="";
			for(int i=0; i<myGoals.size();i++){
				s+=myGoals.get(i).id+"\n";
			}
			System.err.println("my ID: "+id+" my goals: "+s);
			return s;
		} 
	}
	
	/*****************************************************************************/
	
	//goal class
	public class Goal{
		char id;
		String color; //kept for finding links between box and goal
		int[] location;

		public Goal( char id, String color , int[] location) {
			System.err.println("Found " + color + " Goal " + id + " Location " + location[0]+","+location[1]);
			this.id = id;
			this.color=color;
			this.location=location;
		}
	}
	
	/*****************************************************************************/
	
	
	//box class
	
	public class Box{
		char id;
		String color;
		int[] location;
		public Box( char id, String color , int[] location) {
			System.err.println("Found " + color + " Box " + id + " Location " + location[0]+","+location[1]);
			this.id = id;
			this.color=color;
			this.location=location;
		}
	}
	
	
	
	
	public RandomWalkClient() throws IOException {
		readMap();
	}

	private void readMap() throws IOException {
		Map< Character, String > colors = new HashMap< Character, String >();

		String line, color;
		
		//compute the dimension of the level file in the first place.
		int[] dimension=this.getLevelDimension(in);
		walls = new boolean[dimension[0]][dimension[1]];
		frees = new boolean[dimension[0]][dimension[1]]; //pesudo free cell, everywhere other than walls
		//char[][] goals = new char[dimension[0]][dimension[1]];
		/*****************************************************************/
		
		// Read lines specifying colors
		int lineN=0;
		while ( ( line = in.readLine() ).matches( "^[a-z]+:\\s*[0-9A-Z](,\\s*[0-9A-Z])*\\s*$" ) ) {
			line = line.replaceAll( "\\s", "" );
			color = line.split( ":" )[0];

			for ( String id : line.split( ":" )[1].split( "," ) )
				colors.put( id.charAt( 0 ), color );
		}

		// Read lines specifying level layout
		while ( !line.equals( "" ) ) {
			for ( int i = 0; i < line.length(); i++ ) {
				char id = line.charAt( i );
				
				//if the chr is a wall
				if (id == '+') { // Wall.
					walls[lineN][i] = true; //lineN is the row, i is the column
				}
				
				//if the chr is an agent
				else if ( '0' <= id && id <= '9' ){
					System.err.println("Found agent "+id);
					agents.add( new Agent(id, colors.get(id),new int[]{lineN,i}));
					frees[lineN][i] = true;
				}
				
				//save all goals
				else if('a' <= id && id <= 'z'){
					System.err.println("Found Goal "+id);
					goals.add( new Goal(id, colors.get(Character.toUpperCase(id)),new int[]{lineN,i}));
					frees[lineN][i] = true;
				}
				
				//save all boxes
				else if ('A' <= id && id <= 'Z'){
					System.err.println("Found box "+id);
					boxes.add( new Box(id, colors.get(id),new int[]{lineN,i}));
					frees[lineN][i] = true;
				}
			}

			
			line = in.readLine();
			lineN++;
			
		}
		
		
		for (int i = 0; i<agents.size(); i++){
			agents.get(i).findMyBoxes(boxes);
			agents.get(i).printMyBoxes();
			agents.get(i).findMyGoals(goals);
			agents.get(i).printMyGoals();

		}
		
		createDistanceMap(); // creat distance map between every pair of locations in the level file.
	}

	public boolean update() throws IOException {
		String jointAction = "[";

		for ( int i = 0; i < agents.size() - 1; i++ )
			jointAction += agents.get( i ).act() + ",";
		
		jointAction += agents.get( agents.size() - 1 ).act() + "]";

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
		for (int frow = 1; frow < frees.length-1; frow++) {
			for (int fcol = 1; fcol < frees[0].length-1; fcol++) {
				if(frees[frow][fcol]){
					//do dijkstra mapping below
					Vertex dj_vertex= new Vertex(frow,fcol);
					//four directions
					if(!dij_graph.containsKey(dj_vertex.toString())){
						dij_graph.put(dj_vertex.toString(), dj_vertex);	
						Vertex dj_adj_vertex;
						if (frees[frow-1][fcol]){
							dj_adj_vertex = new Vertex(frow-1,fcol);
							dj_vertex.setEdge(dj_adj_vertex);
							//dij_graph.get(dj_vertex).add(dj_adj_vertex);
						}
						if (frees[frow+1][fcol]){
							dj_adj_vertex = new Vertex(frow+1,fcol);
							dj_vertex.setEdge(dj_adj_vertex);
							//dij_graphdj_adj_vertex);
						}
						if (frees[frow][fcol-1]){
							dj_adj_vertex = new Vertex(frow,fcol-1);
							dj_vertex.setEdge(dj_adj_vertex);
							//dij_graph.get(dj_vertex).add(dj_adj_vertex);
						}
						if (frees[frow][fcol+1]){
							dj_adj_vertex = new Vertex(frow,fcol+1);
							dj_vertex.setEdge(dj_adj_vertex);
						}
						
										
					}
				
				}
			}
		}
		
		//do dijkstra computation
		Grid grid= new Grid(dij_graph);
		grid.BFSMapping();
		System.err.format("This distance is "+Grid.matrix.get("1,3,3,2")+"\n");
		
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
