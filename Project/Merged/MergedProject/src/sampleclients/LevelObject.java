package sampleclients;

import java.awt.Point;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import sampleclients.Heuristic.AStar;
import sampleclients.RandomWalkClient;
import sampleclients.Strategy.StrategyBestFirst;

//agent class

/*****************************************************************************/
public class LevelObject {
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
	
	public LevelObject( char id, String color , int[] location) {
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
		return this.solution.poll().action.toString();
	}

	public void findMyGoals(List< Goal > goals){
		for (int i = 0; i < goals.size(); i++){
			if(goals.get(i).color.equals(this.color)){
				myGoals.put(new Point(goals.get(i).location[0],goals.get(i).location[1]), goals.get(i));
			}
		}
	}
	public void findMyBoxes(List< Box > boxes){
		for (int i = 0; i < boxes.size(); i++){
			if(boxes.get(i).color.equals(this.color)){
				myBoxes.put(new Point(boxes.get(i).location[0],boxes.get(i).location[1]), boxes.get(i));
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
	
	public int createPlan(){ //RETURN LIST OF COMMAND OBEJCTS
		//For all Goals, find closest through heuristic distance map
		
		//brand new logic here using A*//
		this.initialState= new Node(null,RandomWalkClient.walls, myBoxes,myGoals,this.location);
		strategy= new StrategyBestFirst(new AStar(initialState));
		try {
			this.solution=Search(strategy);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Exception happened, solution cannot be found!!");
			e.printStackTrace();
		}
		

		return this.solution.size();
	}


//	public void reachTheBox(Box box) {	//RETURN LIST OF COMMAND OBJECTS LEADING AGENT TO THE BOX		
//		int[] tmp_location=this.location;
//		while(true){
//			//System.err.println("ID:"+this.id+ "   Plan:"+plan.toString());
//			if (level_grid.getBFSDistance(tmp_location,box.location)==1) {
//				
//				break;
//			 }
//			Command step=getDirection(tmp_location,box.location);
//			switch (step.dir1){
//			 case N:  tmp_location[0] = tmp_location[0]-1; break;
//			 case W:  tmp_location[1] = tmp_location[1]-1; break;
//			 case E:  tmp_location[1] = tmp_location[1]+1; break;
//			 case S:  tmp_location[0] = tmp_location[0]+1; break;
//			 default: break;
//			}
//			plan.add(step);	
//			
//			
//		}
//	}
	
//	public void reachTheGoal(Box box, Goal goal) { //RETURN LIST OF COMMAND OBJECTS LEADING BOX TO THE GOAL		
//		int[] boxCurrentLocation = goal.location;
//		int[] agentCurrentLocation;
//		getDirection(goal.location, box.location);
//
//	}
	
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

}

/*****************************************************************************/

//goal class
class Goal{
	char id;
	String color; //kept for finding links between box and goal
	int[] location;

	public Goal( char id, String color , int[] location) {
		System.err.println("Found " + color + " Goal " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
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
	public Box( char id, String color , int[] location) {
		System.err.println("Found " + color + " Box " + id + " Location " + location[0]+","+location[1]);
		this.id = id;
		this.color=color;
		this.location=location;
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

