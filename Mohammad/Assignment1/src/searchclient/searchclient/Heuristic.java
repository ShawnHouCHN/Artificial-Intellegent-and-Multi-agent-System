package searchclient;

import java.util.Comparator;

import searchclient.NotImplementedException;

public abstract class Heuristic implements Comparator<Node> {
	
	public Heuristic(Node initialState) {
		// Here's a chance to pre-process the static parts of the level.
	}

	public int h(Node n) {
		
		int totalDistance = 0;
		int minDist = Integer.MAX_VALUE;
		
		// Get box
		for(int i = 1; i < n.boxes.length - 1; i++) {
			// Get row
			for(int j = 1; j < n.boxes[0].length - 1; j++) {
				// Get column
				char box = n.boxes[i][j];
				
				if(box >= 'A' && box <= 'Z') {
					box = Character.toLowerCase(box);
					
					for(int k = 1; k < n.boxes.length - 1; k++) {
						for(int l = 1; l < n.boxes[0].length - 1; l++) {
							char goal = n.goals[k][l];
							if(box == goal && goal >= 'a' && goal <= 'z') {
								int row = Math.abs(i - k);
								int col = Math.abs(j - l);
								int temp = row + col;
								
								minDist = Math.min(temp, minDist);
							}
						}
					}
					
					totalDistance += minDist;
					minDist = Integer.MAX_VALUE;
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
