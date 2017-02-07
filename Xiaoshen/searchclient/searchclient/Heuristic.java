package searchclient;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.TreeMap;

import searchclient.NotImplementedException;

public abstract class Heuristic implements Comparator<Node> {
	
	public Heuristic(Node initialState) {
		// Here's a chance to pre-process the static parts of the level.
	}

	public int h(Node n) {
		
		int totalDistance = 0;
		int agentrow = n.agentRow;
		int agentcol = n.agentCol;
		Point cloestbox=new Point();
		Point cloestgoal=new Point();
		TreeMap<Integer,Point> boxes_tree=new TreeMap<Integer,Point>();
		TreeMap<Integer,Point> goals_tree=new TreeMap<Integer,Point>();
		// Get box
		for(int i = 1; i < n.boxes.length - 1; i++) {
			// Get row
			for(int j = 1; j < n.boxes[0].length - 1; j++) {
				// Get column
				if(n.boxes[i][j]!=0){
					cloestbox.setLocation(i, j);
					boxes_tree.put(Math.abs((i-agentrow)+(j-agentcol)),cloestbox);
				}
				
			}
		}
		
		cloestbox=boxes_tree.firstEntry().getValue();
		
		for(int i = 1; i < n.boxes.length - 1; i++) {
			for(int j = 1; j < n.boxes[0].length - 1; j++) {
				if(n.goals[i][j]!=0){
					cloestgoal.setLocation(i, j);
					goals_tree.put((int)Math.abs((cloestbox.getX()-i)+(cloestbox.getY()-j)),cloestgoal);
				}
			}
		}
		
		cloestgoal=goals_tree.firstEntry().getValue();
		totalDistance=boxes_tree.firstKey()+goals_tree.firstKey();
		return totalDistance;
	}

	public abstract int f(Node n);

	@Override
	public int compare(Node n1, Node n2) {
		return this.f(n1) - this.f(n2);
	}

	public static class AStar extends Heuristic {
		public AStar(Node initialState) {
			super(initialState);
		}

		@Override
		public int f(Node n) {
			return n.g() + this.h(n);
		}

		@Override
		public String toString() {
			return "A* evaluation";
		}
	}

	public static class WeightedAStar extends Heuristic {
		private int W;

		public WeightedAStar(Node initialState, int W) {
			super(initialState);
			this.W = W;
		}

		@Override
		public int f(Node n) {
			return n.g() + this.W * this.h(n);
		}

		@Override
		public String toString() {
			return String.format("WA*(%d) evaluation", this.W);
		}
	}

	public static class Greedy extends Heuristic {
		public Greedy(Node initialState) {
			super(initialState);
		}

		@Override
		public int f(Node n) {
			return this.h(n);
		}

		@Override
		public String toString() {
			return "Greedy evaluation";
		}
	}
}
