package searchclient;

import java.util.ArrayList;

public class Vertex {
    final public int x;
    final public int y;
    private ArrayList<Vertex> edges;
    private int distanceFromSource;
    private boolean visited;
    
	public Vertex(int x, int y) {
		// TODO Auto-generated constructor stub
		super();
		this.x=x;
		this.y=y;
		this.edges = new ArrayList<Vertex>();
		this.distanceFromSource= Integer.MAX_VALUE;
		this.visited=false;
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
	
	

	public int getDistanceFromSource() {
		return distanceFromSource;
	}

	public void setDistanceFromSource(int distanceFromSource) {
		this.distanceFromSource = distanceFromSource;
	}
	

    public boolean equals(Object o) {
    	Vertex c = (Vertex) o;
        return c.x == x && c.y == y;
    }
    
    


    public int hashCode() {
        return new Integer(x + "0" + y);
    }
    
    public String toString(){
    	return x+","+y;
    }

}
