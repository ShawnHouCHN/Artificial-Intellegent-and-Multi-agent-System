package searchclient;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import searchclient.Memory;
import searchclient.Strategy.*;
import searchclient.Heuristic.*;
import searchclient.Vertex;

public class SearchClient {
	public Node initialState;
	public static int MAX_ROW = 70;
	public static int MAX_COL = 70;
	
	public int[] getLevelDimension(BufferedReader serverMessages) throws IOException{
		int longest=0, row=0, line_length=0;
		serverMessages.mark(10000);
		String line = serverMessages.readLine();
		while (!line.equals("")) {
			line = serverMessages.readLine();
			line_length=line.length();
			if (line_length>=longest){
				longest=line_length;
			}
			row++;
		}
		int[] dimension={row,longest};
		serverMessages.reset();
		return dimension;	
	}
	
	public SearchClient(BufferedReader serverMessages) throws Exception {
		// Read lines specifying colors
		int[] dimension=this.getLevelDimension(serverMessages);
		//System.err.println(dimension[0]+" "+dimension[1]);
		String line = serverMessages.readLine();
		if (line.matches("^[a-z]+:\\s*[0-9A-Z](\\s*,\\s*[0-9A-Z])*\\s*$")) {
			System.err.println("Error, client does not support colors.");
			System.exit(1);
		}

		int row = 0;
		int agentRow=0;
		int agentCol=0;
		boolean agentFound = false;
		
		
		//find the longest row and column logic
		boolean[][] walls = new boolean[dimension[0]][dimension[1]];
		boolean[][] frees = new boolean[dimension[0]][dimension[1]]; //everywhere other than walls
		char[][] goals = new char[dimension[0]][dimension[1]];
		
		HashMap<Point,Character> boxes = new HashMap<Point,Character>();
		final HashMap<String,Vertex> dij_graph=new HashMap<String,Vertex>();

		while (!line.equals("")) {
			for (int col = 0; col < line.length(); col++) {
				char chr = line.charAt(col);

				if (chr == '+') { // Wall.
					walls[row][col] = true;
				} else if ('0' <= chr && chr <= '9') { // Agent.
					if (agentFound) {
						System.err.println("Error, not a single agent level");
						System.exit(1);
					}
					agentFound = true;
					agentRow = row;
					agentCol = col;
					frees[row][col] = true;
				} else if ('A' <= chr && chr <= 'Z') { // Box.
					boxes.put(new Point(row,col),chr);
					frees[row][col] = true;
				} else if ('a' <= chr && chr <= 'z') { // Goal.
					goals[row][col] = chr;
					frees[row][col] = true;
				} else if (chr == ' ') {
					// Free space.
					frees[row][col] = true;
				} else {
					System.err.println("Error, read invalid level character: " + (int) chr);
					System.exit(1);
				}
				
				
			}
			line = serverMessages.readLine();
			row++;
		}
		
		
		//Put all free cells into a map
		for (int frow = 1; frow < frees.length-1; frow++) {
			for (int fcol = 1; fcol < frees[0].length-1; fcol++) {
				if(frees[frow][fcol]){
					//do dijkstra mapping below
					Vertex dj_vertex= new Vertex(frow,fcol);
					//four directions
					if(!dij_graph.containsKey(dj_vertex.toString())){
						dij_graph.put(dj_vertex.toString(), dj_vertex);	
						Vertex dj_adj_vertex;
						if (frees[frow-1][fcol]){
							dj_adj_vertex = new Vertex(frow-1,fcol);
							dj_vertex.setEdge(dj_adj_vertex);
							//dij_graph.get(dj_vertex).add(dj_adj_vertex);
						}
						if (frees[frow+1][fcol]){
							dj_adj_vertex = new Vertex(frow+1,fcol);
							dj_vertex.setEdge(dj_adj_vertex);
							//dij_graphdj_adj_vertex);
						}
						if (frees[frow][fcol-1]){
							dj_adj_vertex = new Vertex(frow,fcol-1);
							dj_vertex.setEdge(dj_adj_vertex);
							//dij_graph.get(dj_vertex).add(dj_adj_vertex);
						}
						if (frees[frow][fcol+1]){
							dj_adj_vertex = new Vertex(frow,fcol+1);
							dj_vertex.setEdge(dj_adj_vertex);
						}
						
										
					}
				
				}
			}
		}
		
		//do dijkstra computation
		Grid grid= new Grid(dij_graph);
		grid.BFSMapping();
		
	
		this.initialState = new Node(null,walls,boxes,goals,agentRow,agentCol);
		//System.err.format("This distance is "+Grid.matrix.get("1,3,3,2")+"\n");
		//System.err.format("This coor a is "+a.toString()+" a's neighbours"+dij_graph.get("1,2").getEdges()+ "\n");
		//run an algorithm to map constant distance matrix
		
		
		
	}

	public LinkedList<Node> Search(Strategy strategy) throws IOException {
		System.err.format("Search starting with strategy %s.\n", strategy.toString());
		strategy.addToFrontier(this.initialState);

		int iterations = 0;
		while (true) {
            if (iterations == 1000) {
				System.err.println(strategy.searchStatus());
				iterations = 0;
			}

			if (strategy.frontierIsEmpty()) {
				return null;
			}

			Node leafNode = strategy.getAndRemoveLeaf();
			if (leafNode.isGoalState()) {
				return leafNode.extractPlan();
			}

			strategy.addToExplored(leafNode);
			for (Node n : leafNode.getExpandedNodes()) { // The list of expanded nodes is shuffled randomly; see Node.java.
				if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {					
					strategy.addToFrontier(n);
				}
			}
			iterations++;
		}
	}
	


	public static void main(String[] args) throws Exception {
		BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

		// Use stderr to print to console
		System.err.println("SearchClient initializing. I am sending this using the error output stream.");

		// Read level and create the initial state of the problem
		SearchClient client = new SearchClient(serverMessages);

        Strategy strategy;
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "-bfs":
                    strategy = new StrategyBFS();
                    break;
                case "-dfs":
                    strategy = new StrategyDFS();
                    break;
                case "-astar":
                    strategy = new StrategyBestFirst(new AStar(client.initialState));
                    break;
                case "-wastar":
                    // You're welcome to test WA* out with different values, but for the report you must at least indicate benchmarks for W = 5.
                    strategy = new StrategyBestFirst(new WeightedAStar(client.initialState, 5));
                    break;
                case "-greedy":
                    strategy = new StrategyBestFirst(new Greedy(client.initialState));
                    break;
                default:
                    strategy = new StrategyBFS();
                    System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
            }
        } else {
            strategy = new StrategyBFS();
            System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
        }

		LinkedList<Node> solution;
		try {
			solution = client.Search(strategy);
		} catch (OutOfMemoryError ex) {
			System.err.println("Maximum memory usage exceeded.");
			solution = null;
		}

		if (solution == null) {
			System.err.println(strategy.searchStatus());
			System.err.println("Unable to solve level.");
			System.exit(0);
		} else {
			System.err.println("\nSummary for " + strategy.toString());
			System.err.println("Found solution of length " + solution.size());
			System.err.println(strategy.searchStatus());

			for (Node n : solution) {
				String act = n.action.toString();
				System.out.println(act);
				String response = serverMessages.readLine();
				if (response.contains("false")) {
					System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, act);
					System.err.format("%s was attempted in \n%s\n", act, n.toString());
					break;
				}
			}
		}
	}
}
