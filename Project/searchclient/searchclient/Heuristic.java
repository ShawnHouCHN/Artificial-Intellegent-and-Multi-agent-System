package searchclient;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;


import searchclient.NotImplementedException;

public abstract class Heuristic implements Comparator<Node> {
	
	public HashMap<Point,Character> goallist;
	HashMap<Point,Character> initboxes;
	Set<Point> intersection;
	public Heuristic(Node initialState) {
		// Here's a chance to pre-process the static parts of the level.
		this.goallist= new HashMap<Point, Character>();
		for(int i = 1; i < initialState.goals.length; i++) {
			for(int j = 1; j < initialState.goals[0].length; j++) {
				if(initialState.goals[i][j]!=0){
					goallist.put(new Point(i,j),initialState.goals[i][j]);
				}
			}
		}
		
		this.initboxes=initialState.boxes;
	
		
		
	}

	public int h(Node n) {
		

		Set<Point> box_locs=new HashSet<Point>(n.boxes.keySet());
		Set<Point> goal_locs= new HashSet<Point>(this.goallist.keySet());
		intersection = new HashSet<Point>(goal_locs);
		intersection.retainAll(box_locs);
		box_locs.removeAll(intersection);
		goal_locs.removeAll(intersection);
		int sumdistance=0;
		int dis=0;
		// Get box
		
		int newdis=0;
		int agent_to_box=0;
		int min_agent_to_box=0;
		int clsboxx=0;
		int clsboxy=0;
		
		
		//System.err.format(" "+n.agentRow+n.agentCol+"  "+n.boxes.keySet().toString()+"\n");
		//System.err.format(" Boxes are "+box_locs.toString());
		Iterator<Point> ite_goal = goal_locs.iterator();
		Point agoal;
		while(ite_goal.hasNext()){
			dis=Integer.MAX_VALUE;
			//min_agent_to_box=Integer.MAX_VALUE;
			clsboxx=0;
			clsboxy=0;
			agoal=ite_goal.next();
			Iterator<Point> ite_box = box_locs.iterator();
			Point abox=null;
			Point aclsbox=null;
			while(ite_box.hasNext()){
				abox=ite_box.next();
				if(this.goallist.get(agoal)==Character.toLowerCase(n.boxes.get(abox))){					
					newdis=Grid.matrix.get(abox.x+","+abox.y+","+agoal.x+","+agoal.y);
					if(newdis<=dis){
						dis=newdis;
						clsboxx=abox.x;
						clsboxy=abox.y;				
						//agent_to_box=Grid.matrix.get(n.agentRow+","+n.agentCol+","+abox.x+","+abox.y)-1;
					}
				}
			}
			aclsbox=new Point(clsboxx,clsboxy);
			box_locs.remove(aclsbox);
			//System.err.format(" closet box to goal "+agoal.x+agoal.y+" is "+clsboxx+clsboxy);
			//System.err.format(" \n ");
			int comdistance=dis;
			sumdistance=sumdistance+comdistance;
		}
		
		return sumdistance;
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
