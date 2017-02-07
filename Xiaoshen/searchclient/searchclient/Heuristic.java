package searchclient;

import java.util.Comparator;

import searchclient.NotImplementedException;

public abstract class Heuristic implements Comparator<Node> {
	
	public Heuristic(Node initialState) {
		// Here's a chance to pre-process the static parts of the level.
	}

	public int h(Node n) {
		
		int totalDistance = 0;
		int minDist = 0;
		
		// Get box
		for(int i = 1; i < n.MAX_ROW - 1; i++) {
			// Get row
			for(int j = 1; j < n.MAX_COL - 1; j++) {
				// Get column
				
				char box = n.boxes[i][j];
				
				if('A' <= box && box <= 'Z') {
					minDist = Integer.MAX_VALUE;
					
					box = Character.toLowerCase(box);
					
					for(int k = 1; k < n.MAX_ROW - 1; k++) {
						for(int l = 1; l < n.MAX_COL - 1; l++) {
							char goal = n.goals[k][l];
							if('a' <= goal && goal <= 'z' && box == goal) {
								int row = i - k;
								int col = j - l;
								
								minDist = Math.min((Math.abs(row) + Math.abs(col)), minDist);
							}
						}
					}
					
					totalDistance += minDist;
				}
			}
		}

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
