package searchclient;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import searchclient.Command.Type;

public class Node {
	private static final Random RND = new Random(1);

	public static int MAX_ROW = 70;
	public static int MAX_COL = 70;

	public int agentRow;
	public int agentCol;

	public boolean[][] walls;
	//public char[][] boxes;
	public HashMap<Point,Character> boxes;
	public char[][] goals;
	public Node parent;
	public Command action;

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
			this.boxes = new HashMap<Point, Character>(parent.boxes);
		}

	}
	
	
	public Node(Node parent,boolean[][] walls,HashMap<Point,Character> boxes,char[][] goals,int agentRow, int agentCol ) {

		if (parent == null) {
			this.g = 0;
		} else {
			this.g = parent.g() + 1;
		}
		this.parent = parent;
		this.walls = walls;
		this.boxes = boxes;
		this.goals = goals;
		this.agentRow = agentRow;
		this.agentCol = agentCol;
	}

	public int g() {
		return this.g;
	}

	public boolean isInitialState() {
		return this.parent == null;
	}

	public boolean isGoalState() {
		for (int row = 1; row < this.goals.length - 1; row++) {
			for (int col = 1; col < this.goals[0].length - 1; col++) {
				char g = goals[row][col];
				if(g>0){
					Point goalbox= new Point(row,col);
					if (!boxes.containsKey(goalbox))
							return false;
					else if(boxes.containsKey(goalbox) && Character.toLowerCase(boxes.get(goalbox))!=g){
							return false;
					}					
				}
			}
		}
		return true;
	}

	public ArrayList<Node> getExpandedNodes() {
		ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.EVERY.length);
		Point oagent_point = new Point(this.agentRow,this.agentCol);
		for (Command c : Command.EVERY) {
			// Determine applicability of action
			int newAgentRow = this.agentRow + Command.dirToRowChange(c.dir1);
			int newAgentCol = this.agentCol + Command.dirToColChange(c.dir1);
			Point nagent_point = new Point(newAgentRow,newAgentCol);
			if (c.actionType == Type.Move) {
				// Check if there's a wall or box on the cell to which the agent is moving
				if (this.cellIsFree(nagent_point)) {
					Node n = new Node(this);
					n.action = c;
					n.agentRow = newAgentRow;
					n.agentCol = newAgentCol;
					n.boxes = this.boxes;
					expandedNodes.add(n);
				}
			} else if (c.actionType == Type.Push) {
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
					}
				}
			} else if (c.actionType == Type.Pull) {
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
					}
				}
			}
		}
		Collections.shuffle(expandedNodes, RND);
		return expandedNodes;
	}

	private boolean cellIsFree(Point agent) {
		return !this.walls[agent.x][agent.y] && !this.boxes.containsKey(agent);
	}

	private boolean boxAt(Point agent) {
		return this.boxes.containsKey(agent) && this.boxes.get(agent)>0;
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
			result = prime * result + Arrays.deepHashCode(this.boxes.values().toArray());
			result = prime * result + Arrays.deepHashCode(this.goals);
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
		Iterator<Entry<Point, Character>> box_it = this.boxes.entrySet().iterator();
		while(box_it.hasNext()){
			 Map.Entry<Point,Character> box = (Map.Entry<Point,Character>)box_it.next();
			if(!other.boxes.containsKey(box.getKey()))
				return false;
			if(other.boxes.get(box.getKey())!=box.getValue())
				return false;
		}
//		if (!this.boxes.keySet().equals(other.boxes.keySet()))
//			return false;
		if (!Arrays.deepEquals(this.goals, other.goals))
			return false;
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