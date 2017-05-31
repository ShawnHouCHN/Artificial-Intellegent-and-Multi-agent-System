package sampleclients;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class Grid {
	
	public static int LOCK_THRESHOLD = 10000;
	public static int SA_GOAL_REACH_COST=5;
	public static int SA_MOVE_OTHER_COST=5;
	public HashMap<Integer, Vertex> graph;
	public static HashMap<Integer,Integer> matrix;
	public List<Integer> sortedkeys;
	public Grid(HashMap<Integer, Vertex> graph){
		this.graph=graph;
		matrix=new HashMap<Integer,Integer>();
	}
	
	public int getBFSPesudoDistance(int[] source, int[] target){
		int hash1 = ((source[0] + source[1])*(source[0] + source[1] + 1))/2 + source[1];
		int hash2 = ((target[0] + target[1])*(target[0] + target[1] + 1))/2 + target[1];
		int unionhash=Grid.pairSourceTarget(hash1, hash2);
		int dis=BFSDistance(hash1,hash2);
		if (dis<LOCK_THRESHOLD && !Grid.matrix.containsKey(unionhash))
			Grid.matrix.put(unionhash, dis);
		return dis;
	}
	
	
	public int getBFSPesudoDistance(int[] source, int[] target, HashMap<Integer,Vertex> agent_graph){
		int hash1 = ((source[0] + source[1])*(source[0] + source[1] + 1))/2 + source[1];
		int hash2 = ((target[0] + target[1])*(target[0] + target[1] + 1))/2 + target[1];
		int unionhash=Grid.pairSourceTarget(hash1, hash2);
		int dis=BFSDistance(hash1,hash2,agent_graph);
		if (dis<LOCK_THRESHOLD && !Grid.matrix.containsKey(unionhash))
			Grid.matrix.put(unionhash, dis);
		return dis;
	}
	
	
	public static int getSABFSPesudoDistance(int[] source, int[] target,HashMap<Integer,Vertex> myGraph){
		int hash1 = ((source[0] + source[1])*(source[0] + source[1] + 1))/2 + source[1];
		int hash2 = ((target[0] + target[1])*(target[0] + target[1] + 1))/2 + target[1];
		int unionhash=Grid.pairSourceTarget(hash1, hash2);
		int dis=BFSDistance(hash1,hash2,myGraph);
		return dis;		
	}
	
	
	
	public static int getBFSPesudoDistance(int[] source, int[] target,HashMap<Integer,Vertex> myGraph, char agent_id){
		int hash1 = ((source[0] + source[1])*(source[0] + source[1] + 1))/2 + source[1];
		int hash2 = ((target[0] + target[1])*(target[0] + target[1] + 1))/2 + target[1];
		int unionhash=Grid.pairSourceTarget(hash1, hash2);
		int dis=BFSDistance(hash1,hash2,myGraph,agent_id);
		return dis;
	}
	
	//save for the inital matrix of Grid (using universal lock)
	public int getBFSDistance(int[] source, int[] target){
		int hash1 = ((source[0] + source[1])*(source[0] + source[1] + 1))/2 + source[1];
		int hash2 = ((target[0] + target[1])*(target[0] + target[1] + 1))/2 + target[1];
		int unionhash=Grid.pairSourceTarget(hash1, hash2);
		if(!Grid.matrix.containsKey(unionhash)){
			int dis=BFSDistance(hash1,hash2);
			if (dis<LOCK_THRESHOLD)
				Grid.matrix.put(unionhash, dis);
			return dis;
		}else{
			return Grid.matrix.get(unionhash);
		}
	}
	
	
	
	public static int BFSDistance(int source, int target, HashMap<Integer, Vertex> agent_graph){
		ArrayDeque<Integer> frontier =new ArrayDeque<Integer>();
		HashSet<Integer> closedset = new HashSet<Integer>();
		HashMap<Integer,Vertex> graph= new HashMap<Integer,Vertex>(agent_graph);
		if (source==target)
			return 0;
		if (!graph.containsKey(target) || !graph.containsKey(source)){  //if source or target accident to be wall;
			return Integer.MAX_VALUE;
		}
		frontier.add(source);
		graph.get(source).setDistanceFromSource(0);
		int vet=0;
		
		while(!frontier.isEmpty()){
			vet=frontier.pollFirst();
			closedset.add(vet);
			//System.err.println("Investigate:"+graph.get(vet).toString());
			if (vet==target)
			{			
				
				return graph.get(vet).getDistanceFromSource();
			}
			int tem_key=pairSourceTarget(source,vet);
			
			if (graph.get(vet).getDistanceFromSource()<LOCK_THRESHOLD)
				Grid.matrix.put(tem_key,graph.get(vet).getDistanceFromSource());
			
			for (Vertex edge:graph.get(vet).getEdges()){
				if(!closedset.contains(edge.hashCode()) && !frontier.contains(edge.hashCode()) )
				{		
					if (graph.get(edge.hashCode()).getLock()) //if it hits this fake wall
						graph.get(edge.hashCode()).setDistanceFromSource(LOCK_THRESHOLD);
					else	
						graph.get(edge.hashCode()).setDistanceFromSource(graph.get(vet).getDistanceFromSource()+1);
					
					frontier.addLast(edge.hashCode());
				}
			}
		}		
		
		return Integer.MAX_VALUE;
	}
	
	//universed locks awared distance
	
	public int BFSDistance(int source, int target){
		ArrayDeque<Integer> frontier =new ArrayDeque<Integer>();
		HashSet<Integer> closedset = new HashSet<Integer>();
		HashMap<Integer,Vertex> graph= new HashMap<Integer,Vertex>(this.graph);
		if (source==target)
			return 0;
		if (!graph.containsKey(target) || !graph.containsKey(source)){  //if source or target accident to be wall;
			return Integer.MAX_VALUE;
		}
		frontier.add(source);
		graph.get(source).setDistanceFromSource(0);
		int vet=0;
		
		while(!frontier.isEmpty()){
			vet=frontier.pollFirst();
			closedset.add(vet);
			//System.err.println("Investigate:"+graph.get(vet).toString());
			if (vet==target)
			{			
				
				return graph.get(vet).getDistanceFromSource();
			}
			int tem_key=pairSourceTarget(source,vet);
			
			if (graph.get(vet).getDistanceFromSource()<LOCK_THRESHOLD)
				Grid.matrix.put(tem_key,graph.get(vet).getDistanceFromSource());
			
			for (Vertex edge:graph.get(vet).getEdges()){
				if(!closedset.contains(edge.hashCode()) && !frontier.contains(edge.hashCode()) )
				{		
					if (graph.get(edge.hashCode()).getLock()) //if it hits this fake wall
						graph.get(edge.hashCode()).setDistanceFromSource(LOCK_THRESHOLD);
					else	
						graph.get(edge.hashCode()).setDistanceFromSource(graph.get(vet).getDistanceFromSource()+1);
					
					frontier.addLast(edge.hashCode());
				}
			}
		}		
		
		return Integer.MAX_VALUE;
	}
	
	
	//color locks awared distancde
	
	public static int BFSDistance(int source, int target, HashMap<Integer,Vertex> myGraph, char agent_id){
		ArrayDeque<Integer> frontier =new ArrayDeque<Integer>();
		HashSet<Integer> closedset = new HashSet<Integer>();
		HashMap<Integer,Vertex> graph= new HashMap<Integer,Vertex>(myGraph);
		if (source==target)
			return 0;
		if (!graph.containsKey(target) || !graph.containsKey(source)){  //if source or target accident to be wall;
			return Integer.MAX_VALUE;
		}
		frontier.add(source);
		graph.get(source).setDistanceFromSource(0);
		int vet=0;
		
		while(!frontier.isEmpty()){
			vet=frontier.pollFirst();
			closedset.add(vet);
			//System.err.println("Investigate:"+graph.get(vet).toString());
			if (vet==target)
			{			
				return graph.get(vet).getDistanceFromSource();
			}
			int tem_key=pairSourceTarget(source,vet);
						
			for (Vertex edge:graph.get(vet).getEdges()){
				if(!closedset.contains(edge.hashCode()) && !frontier.contains(edge.hashCode()) )
				{		
					if (graph.get(edge.hashCode()).getAgentLock(agent_id)) //if it hits this fake wall
						graph.get(edge.hashCode()).setDistanceFromSource(LOCK_THRESHOLD);
					else	
						graph.get(edge.hashCode()).setDistanceFromSource(graph.get(vet).getDistanceFromSource()+1);
					
					frontier.addLast(edge.hashCode());
					
				}
				else if(closedset.contains(edge.hashCode()) && graph.get(edge.hashCode()).getDistanceFromSource()>=LOCK_THRESHOLD){
					if((graph.get(vet).getDistanceFromSource()+1)< LOCK_THRESHOLD)
						graph.get(edge.hashCode()).setDistanceFromSource(graph.get(vet).getDistanceFromSource()+1);
				}
				
			}
		}		
		
		return Integer.MAX_VALUE;
	}	
	
	
	
	//abandoned
	public void BFSDistance(int source){
		ArrayDeque<Integer> frontier =new ArrayDeque<Integer>();
		HashSet<Integer> closedset = new HashSet<Integer>();
		HashMap<Integer,Vertex> graph_copy = new HashMap<Integer,Vertex>(graph);
		frontier.add(source);
		graph_copy.get(source).setDistanceFromSource(0);
		Integer vet=0;
		while(!frontier.isEmpty()){
			vet=frontier.pollFirst();
			closedset.add(vet);
			int tem_key=pairSourceTarget(source,vet);
			if(!Grid.matrix.containsKey(tem_key))
				Grid.matrix.put(tem_key,graph_copy.get(vet).getDistanceFromSource());	
			for (Vertex edge:graph_copy.get(vet).getEdges()){
				if(!closedset.contains(edge.hashCode()))
				{				
					graph_copy.get(edge.hashCode()).setDistanceFromSource(graph_copy.get(vet).getDistanceFromSource()+1);
					frontier.addLast(edge.hashCode());
				}
			}
		}		
		
	}
	
	public static int pairSourceTarget(int source, int target){
		if(target>source)
			return ((source + target)*(source + target + 1))/2 + target;
		else if(target<source)
			return ((source + target)*(source + target + 1))/2 + source;
		else
			return 0;
			
	}
	
}
