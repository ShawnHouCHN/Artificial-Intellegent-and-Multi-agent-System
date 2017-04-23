package sampleclients;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import sampleclients.Command.type;
import sampleclients.Agent;

public class Node {
	private static final Random RND = new Random(1);


	public int agentRow;
	public int agentCol;
	public int[] agent_loc;
	//public char[][] boxes;
	public boolean[][] walls;
	public HashMap<Point,Box> boxes;
	public HashMap<Point,Goal> goals;
	public Node parent;
	public Command action;

	
	//testing logic 
	Box currentBox;
	Goal currentGoal;
	
	private int g;
	
	private int _hash = 0;

	public Node(Node parent) {
		this.parent = parent;
		if (parent == null) {
			this.g = 0;
		} else {
			this.g = parent.g() + 1;
			this.walls = parent.walls;
			this.goals = parent.goals;
			//this.boxes = new char[parent.boxes.length][parent.boxes[0].length];
			this.boxes = new HashMap<Point, Box>(parent.boxes);
			this.currentGoal=parent.currentGoal;
			this.currentBox=parent.currentBox;
		}

	}
	
	
	
	public Node(Node parent, boolean[][] walls, HashMap<Point,Box> boxes,HashMap<Point,Goal> goals,int[] agent_loc ) {
		if (parent == null) {
			this.g = 0;
		} else {
			this.g = parent.g() + 1;
		}
		this.parent = parent;
		this.walls = walls;
		this.boxes = boxes;
		this.goals = goals;
		this.agent_loc = agent_loc;
		this.agentRow = agent_loc[0];
		this.agentCol = agent_loc[1];
	}


	public Node(Node parent, boolean[][] walls, HashMap<Point,Box> boxes,HashMap<Point,Goal> goals, Box currentBox, Goal currentGoal, int[] agent_loc ) {
		if (parent == null) {
			this.g = 0;
		} else {
			this.g = parent.g() + 1;
		}
		this.parent = parent;
		this.walls = walls;
		this.boxes = boxes;
		this.goals = goals;
		this.agent_loc = agent_loc;
		this.agentRow = agent_loc[0];
		this.agentCol = agent_loc[1];
		this.currentBox=currentBox;
		this.currentGoal=currentGoal;
		
	}
	
	
	public int g() {
		return this.g;
	}

	public boolean isInitialState() {
		return this.parent == null;
	}
	
	//old isgoalstate function for integrated A* planning
	public boolean isGoalState() {
		for (Goal g : this.goals.values()){
			char g_chr= g.id;
			int[] g_loc=g.location;
			if(g_chr>0){
				Point goalbox= new Point(g_loc[0],g_loc[1]);
				if (!boxes.containsKey(goalbox))
						return false;
				else if(boxes.containsKey(goalbox) && Character.toLowerCase(boxes.get(goalbox).id)!=g_chr){
						return false;
				}					
			}
		}
		return true;
	}
	
	//new isgoalstate function for single A* planning
	public boolean isSingleGoalState() {
		if (Arrays.equals(this.currentBox.location,this.currentGoal.location))
			return true;
		else
			return false;
	}	
	
	
	
	//updated get expanded nodes;
	public ArrayList<Node> getExpandedNodes() {
		ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.every.length);
		Point oagent_point = new Point(this.agentRow,this.agentCol);
		for (Command c : Command.every) {
			// Determine applicability of action
			if (c.actType == type.NoOp) {
				continue;
			}
			int newAgentRow = this.agentRow + Command.dirToRowChange(c.dir1);
			int newAgentCol = this.agentCol + Command.dirToColChange(c.dir1);
			Point nagent_point = new Point(newAgentRow,newAgentCol);
			if (c.actType == type.Move) {
				// Check if there's a wall or box on the cell to which the agent is moving
				if (this.cellIsFree(nagent_point)) {
					Node n = new Node(this);
					n.action = c;
					n.agentRow = newAgentRow;
					n.agentCol = newAgentCol;
					n.boxes = this.boxes;
					expandedNodes.add(n);
				}
			} else if (c.actType == type.Push) {
				// Make sure that there's actually a box to move
				if (this.boxAt(nagent_point)) {
					int newBoxRow = newAgentRow + Command.dirToRowChange(c.dir2);
					int newBoxCol = newAgentCol + Command.dirToColChange(c.dir2);
					Point nbox_point = new Point(newBoxRow,newBoxCol);
					// .. and that new cell of box is free
					if (this.cellIsFree(nbox_point)) {
						Node n = this.ChildNode();
						n.action = c;
						n.agentRow = newAgentRow;
						n.agentCol = newAgentCol;
						n.boxes.put(nbox_point, this.boxes.get(nagent_point));
						n.boxes.remove(nagent_point);
						expandedNodes.add(n);
						
						//extra logic update currentbox location
						if(nagent_point.x==this.currentBox.location[0] && nagent_point.y==this.currentBox.location[1])
							n.currentBox=new Box(this.currentBox.id, this.currentBox.color,new int[]{nbox_point.x,nbox_point.y});
					}
				}
			} else if (c.actType == type.Pull) {
				// Cell is free where agent is going
				if (this.cellIsFree(nagent_point)) {
					int boxRow = this.agentRow + Command.dirToRowChange(c.dir2);
					int boxCol = this.agentCol + Command.dirToColChange(c.dir2);
					// .. and there's a box in "dir2" of the agent
					Point nbox_point = new Point(boxRow,boxCol);
					if (this.boxAt(nbox_point)) {
						Node n = this.ChildNode();
						n.action = c;
						n.agentRow = newAgentRow;
						n.agentCol = newAgentCol;
						n.boxes.put(oagent_point, this.boxes.get(nbox_point));
						n.boxes.remove(nbox_point);
						expandedNodes.add(n);
						
						//extra logic update currentbox location
						if(nbox_point.x==this.currentBox.location[0] && nbox_point.y==this.currentBox.location[1])
							n.currentBox=new Box(this.currentBox.id, this.currentBox.color,new int[]{oagent_point.x,oagent_point.y});
					}
				}
			}
		}
		//Collections.shuffle(expandedNodes, RND);
		return expandedNodes;
	}

	private boolean cellIsFree(Point agent) {
		return !this.walls[agent.x][agent.y] && !this.boxes.containsKey(agent);
	}

	private boolean boxAt(Point agent) {
		return this.boxes.containsKey(agent) && this.boxes.get(agent).id>0;
	}

	private Node ChildNode() {
		Node copy = new Node(this);
		return copy;
	}

	public LinkedList<Node> extractPlan() {
		LinkedList<Node> plan = new LinkedList<Node>();
		Node n = this;
		while (!n.isInitialState()) {
			plan.addFirst(n);
			n = n.parent;
		}
		return plan;
	}

	@Override
	public int hashCode() {
		if (this._hash == 0) {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.agentCol;
			result = prime * result + this.agentRow;
			result = prime * result + this.boxes.hashCode();
			result = prime * result + this.goals.hashCode();
			result = prime * result + Arrays.deepHashCode(this.walls);
			this._hash = result;
		}
		return this._hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (this.agentRow != other.agentRow || this.agentCol != other.agentCol)
			return false;
		Iterator<Entry<Point, Box>> box_it = this.boxes.entrySet().iterator();
		while(box_it.hasNext()){
			Map.Entry<Point,Box> box = (Map.Entry<Point,Box>)box_it.next();
			if(!other.boxes.containsKey(box.getKey()))
				return false;
			if(!other.boxes.containsValue(box.getValue()))
				return false;
		}
		//compare box and goal collection for equality
		
		
		if (!Arrays.deepEquals(this.walls, other.walls))
			return false;
		
		return true;
	}

//	@Override
//	public String toString() {
//		StringBuilder s = new StringBuilder();
//		for (int row = 0; row < this.goals.length; row++) {
//			if (!this.walls[row][0]) {
//				break;
//			}
//			for (int col = 0; col < this.goals[0].length; col++) {
//				if (this.boxes[row][col] > 0) {
//					s.append(this.boxes[row][col]);
//				} else if (this.goals[row][col] > 0) {
//					s.append(this.goals[row][col]);
//				} else if (this.walls[row][col]) {
//					s.append("+");
//				} else if (row == this.agentRow && col == this.agentCol) {
//					s.append("0");
//				} else {
//					s.append(" ");
//				}
//			}
//			s.append("\n");
//		}
//		return s.toString();
//	}

}