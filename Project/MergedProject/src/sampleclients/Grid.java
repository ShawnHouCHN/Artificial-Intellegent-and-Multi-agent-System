package sampleclients;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;


public class Grid {
	
	
	public HashMap<Integer, Vertex> graph;
	public static HashMap<Integer,Integer> matrix;
	public Grid(HashMap<Integer, Vertex> graph){
		this.graph=graph;
		matrix=new HashMap<Integer,Integer>();
	}
	
	public void BFSMapping(){
		int dis=0;
		int key=0;
		for(Integer source: graph.keySet()){
			for(Integer target: graph.keySet()){
				key=pairSourceTarget(source, target);
				if(key!=0){
					dis=BFSDistance(graph.get(source),graph.get(target));
					Grid.matrix.put(key, dis);
				}
				
			}
			//break; //test
		}
	}
	
	public int BFSDistance(Vertex source, Vertex target){
		ArrayDeque<Integer> frontier =new ArrayDeque<Integer>();
		HashSet<Integer> closedset = new HashSet<Integer>();
		if (source==target)
			return 0;
		frontier.add(source.hashCode());
		graph.get(source.hashCode()).setDistanceFromSource(0);
		Integer vet=0;
		while(!frontier.isEmpty()){
			vet=frontier.pollFirst();
			closedset.add(vet);
			if (vet==target.hashCode())
			{				
				
				return graph.get(vet).getDistanceFromSource();
			}
			for (Vertex edge:graph.get(vet.hashCode()).getEdges()){
				if(!closedset.contains(edge.hashCode()))
				{				
					graph.get(edge.hashCode()).setDistanceFromSource(graph.get(vet).getDistanceFromSource()+1);
					//System.err.format("### "+vet.toString()+" "+edge.toString()+" "+graph.get(edge.toString()).getDistanceFromSource());
					frontier.addLast(edge.hashCode());
				}
			}
		}		
		
		return Integer.MAX_VALUE;
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
