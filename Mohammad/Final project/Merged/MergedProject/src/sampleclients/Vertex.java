package sampleclients;

import java.util.ArrayList;
import java.util.HashMap;

public class Vertex {
	public static int MAX_ROW = 70;
    final public int x;
    final public int y;
    private ArrayList<Vertex> edges;
    private int distanceFromSource;
    private boolean visited;
    private boolean lock;
    private HashMap<Character, Boolean> locks;
	public Vertex(int x, int y) {
		// TODO Auto-generated constructor stub
		super();
		this.x=x;
		this.y=y;
		this.edges = new ArrayList<Vertex>();
		this.distanceFromSource= Integer.MAX_VALUE;
		this.visited=false;
		this.lock=false;
		this.locks=new HashMap<Character, Boolean>();
	}


	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	
	

	public ArrayList<Vertex> getEdges() {
		return this.edges;
	}

	public void setEdge(Vertex edge) {
		this.edges.add(edge);
	}
	
	public void removeEdge(Vertex edge) {
		this.edges.remove(edge);
	}

	public int getDistanceFromSource() {
		return distanceFromSource;
	}

	public void setDistanceFromSource(int distanceFromSource) {
		this.distanceFromSource = distanceFromSource;
	}
	
	public void setLock(boolean lock){
		this.lock=lock;
	}
	
	public void setAgentLock(char agent_id, boolean lock){
		this.locks.put(agent_id, lock);
	}
	
	public boolean getAgentLock(char agent_id){
		if(this.locks.containsKey(agent_id)){
			return this.locks.get(agent_id);
		}else{
			return false;
		}
	}
	
	public boolean getLock(){
		return this.lock;
	}

    public boolean equals(Object o) {
    	Vertex c = (Vertex) o;
        return c.x == x && c.y == y;
    }
    
    


    public int hashCode() {
        return ((x + y)*(x + y + 1))/2 + y;
    }
    
    public String toString(){
    	return x+","+y;
    }

}
