package sampleclients;

import java.io.*;
import java.util.*;

public class RandomWalkClient {
	private static Random rand = new Random();
	private BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
	public List< Agent > agents = new ArrayList< Agent >();
	public List< Goal > goals = new ArrayList< Goal >();
	public List< Box > boxes = new ArrayList< Box >();

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
			return Command.every[randomN].toString();
		}

		/*
		public String createPlan(){
			-For all Goals, find closest through heuristic distance map
			-Make a pop plan from the closest goal to the agent, using the heuristic distance map to help make decisions

			*-*broadcast plan

			*-*make neccesary conflict resolution

			-run plan
		}
		*/

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
	public class Goal{
		char id;
		String color;
		int[] location;

		public Goal( char id, String color , int[] location) {
			System.err.println("Found " + color + " Goal " + id + " Location " + location[0]+","+location[1]);
			this.id = id;
			this.color=color;
			this.location=location;
		}
	}
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
		int[] dimension=this.getLevelDimension(in);
		//find the longest row and column logic
		boolean[][] walls = new boolean[dimension[0]][dimension[1]];
		boolean[][] frees = new boolean[dimension[0]][dimension[1]]; //everywhere other than walls

		Map< Character, String > colors = new HashMap< Character, String >();
		String line, color;

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

				if (id == '+') { // Wall.
					walls[lineN][i] = true;  //lineN is row, i is column;
				} 
				else if ( '0' <= id && id <= '9' ){
					agents.add( new Agent(id, colors.get(id),new int[]{lineN,i}));

					frees[lineN][i] = true;
				}
				else if(Character.isLowerCase(id)){
					System.err.println("Found Goal "+id);
					goals.add( new Goal(id, colors.get(Character.toUpperCase(id)),new int[]{lineN,i}));

					frees[lineN][i] = true;

				}
				else if (Character.isUpperCase(id)){
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
