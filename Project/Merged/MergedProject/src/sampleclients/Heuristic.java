package sampleclients;

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

import sampleclients.Agent;
import sampleclients.Grid;
import sampleclients.Command.type;


public abstract class Heuristic implements Comparator<Node> {
	
	public HashMap<Point,Goal> initgoals;
	HashMap<Point,Box> initboxes;
	Set<Point> intersection;
	public Heuristic(Node initialState) {
		// Here's a chance to pre-process the static parts of the level.
		this.initgoals=initialState.goals;
		this.initboxes=initialState.boxes;
	}
	
//	public int h(Node n){
//		return RandomWalkClient.level_grid.getBFSDistance(n.currentBox.location,n.currentGoal.location);
//	}

	//old hueristic of all 
	public int h(Node n) {
//		return (RandomWalkClient.initial_level_grid.getBFSDistance(n.agent_loc,n.currentBox.location)+RandomWalkClient.initial_level_grid.getBFSDistance(n.currentGoal.location,n.currentBox.location)-1);
		
		Set<Point> box_locs=new HashSet<Point>(n.boxes.keySet());
		Set<Point> goal_locs= new HashSet<Point>(this.initgoals.keySet());
		intersection = new HashSet<Point>(goal_locs);
		intersection.retainAll(box_locs);
		box_locs.removeAll(intersection);
		goal_locs.removeAll(intersection);
		
		int sumdistance=0;
		int dis=0;
		
		int newdis=0;
		int clsboxx=0;
		int clsboxy=0;
		
		Iterator<Point> ite_goal = goal_locs.iterator();
		Point agoal;
		int bonus=0;
		while(ite_goal.hasNext()){
			dis=Integer.MAX_VALUE;
			//min_agent_to_box=Integer.MAX_VALUE;
			clsboxx=0;
			clsboxy=0;
			agoal=ite_goal.next();
			if(agoal.x==n.currentGoal.location[0] && agoal.y==n.currentGoal.location[1]){
				bonus=RandomWalkClient.initial_level_grid.getBFSDistance(n.agent_loc,n.currentBox.location)+RandomWalkClient.initial_level_grid.getBFSDistance(n.currentGoal.location,n.currentBox.location)-1;
				continue;
			}
			Iterator<Point> ite_box = box_locs.iterator();
			Point abox=null;
			Point aclsbox=null;
			while(ite_box.hasNext()){
				abox=ite_box.next();
				if(this.initgoals.get(agoal).id==Character.toLowerCase(n.boxes.get(abox).id)){					
					newdis=RandomWalkClient.initial_level_grid.getBFSDistance(new int[]{abox.x,abox.y}, new int[]{agoal.x,agoal.y});
					if(newdis<dis){
						dis=newdis;
						clsboxx=abox.x;
						clsboxy=abox.y;				
					}
				}
			}
			
			aclsbox=new Point(clsboxx,clsboxy);
			box_locs.remove(aclsbox);
			sumdistance=sumdistance+dis;
		}
		
		return (sumdistance+bonus);

		

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
