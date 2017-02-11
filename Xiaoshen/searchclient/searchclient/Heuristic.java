package searchclient;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.TreeMap;

import searchclient.NotImplementedException;

public abstract class Heuristic implements Comparator<Node> {
	
	HashMap<Point,Character> goallist;
	char[][] initboxes;
	public Heuristic(Node initialState) {
		// Here's a chance to pre-process the static parts of the level.
		this.goallist= new HashMap<Point, Character>();
		for(int i = 1; i < initialState.goals.length - 1; i++) {
			for(int j = 1; j < initialState.goals[0].length - 1; j++) {
				if(initialState.goals[i][j]!=0){
					goallist.put(new Point(i,j),initialState.goals[i][j]);
				}
			}
		}
		
		this.initboxes=initialState.boxes;
		
		
		
	}

	public int h(Node n) {
		
		int agentrow = n.agentRow;
		int agentcol = n.agentCol;
		char[] cloestbox;
		TreeMap<Integer,char[]> boxes_tree=new TreeMap<Integer,char[]>();
		
		
		Entry<Integer,char[]> abox;
		int distance=0;
		int comdistance=0;
		int sumdistance=0;
		
		//if(n.boxes.equals(this.initboxes)){
		//	sumdistance=sumdistance+100;
		//}
		
		// Get box
		for(int i = 1; i < n.boxes.length - 1; i++) {
			// Get row
			for(int j = 1; j < n.boxes[0].length - 1; j++) {
				// Get column
				if(n.boxes[i][j]!=0){
					Point currentbox=new Point(i,j);
					if (goallist.get(currentbox)!=null && goallist.get(currentbox)==n.boxes[i][j]){
						goallist.remove(currentbox);
						sumdistance=sumdistance-100;
					}else
					{
						cloestbox=new char[3];
						cloestbox[0]=(char)i;
						cloestbox[1]=(char)j;
						cloestbox[2]=n.boxes[i][j];
						boxes_tree.put((int)(Math.abs(i-agentrow)+Math.abs(j-agentcol)),cloestbox);
					}
				}
				
			}
		}
		

		while(!boxes_tree.isEmpty()){
			abox=boxes_tree.pollFirstEntry();
			PriorityQueue<Integer> dis_to_goals=new PriorityQueue<Integer>();
			for(Entry<Point,Character> agoal : this.goallist.entrySet()){				
				if(agoal.getValue()==Character.toLowerCase(abox.getValue()[2])){
					distance=Math.abs(agoal.getKey().x-(int)abox.getValue()[0])+Math.abs(agoal.getKey().y-(int)abox.getValue()[1]);
					dis_to_goals.add(distance);
				}
			}
			comdistance=dis_to_goals.poll();
			sumdistance=sumdistance+comdistance;
		}
		
		return comdistance;
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
