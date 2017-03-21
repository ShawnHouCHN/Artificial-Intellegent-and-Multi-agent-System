package sampleclients;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;


public class Grid {
	public HashMap<String, Vertex> graph;
	public static HashMap<String,Integer> matrix;
	public Grid(HashMap<String, Vertex> graph){
		this.graph=graph;
		matrix=new HashMap<String,Integer>();
	}
	
	public void BFSMapping(){
		int dis;
		for(String source: graph.keySet()){
			for(String target: graph.keySet()){
				dis=BFSDistance(source,target);
				Grid.matrix.put(source+","+target, dis);
			}
			//break; //test
		}
	}
	
	public int BFSDistance(String source, String target){
		ArrayDeque<String> frontier =new ArrayDeque<String>();
		HashSet<String> closedset = new HashSet<String>();
		if (source==target)
			return 0;
		frontier.add(source);
		graph.get(source).setDistanceFromSource(0);
		String vet=null;
		while(!frontier.isEmpty()){
			vet=frontier.pollFirst();
			closedset.add(vet);
			if (vet.equals(target))
			{				
				//System.err.format("### "+vet.toString()+" "+graph.get(vet.toString()).getEdges().toString()+" Cost:"+graph.get(vet.toString()).getDistanceFromSource());
				return graph.get(vet).getDistanceFromSource();
			}
			for (Vertex edge:graph.get(vet).getEdges()){
				if(!closedset.contains(edge.toString()))
				{				
					graph.get(edge.toString()).setDistanceFromSource(graph.get(vet).getDistanceFromSource()+1);
					//System.err.format("### "+vet.toString()+" "+edge.toString()+" "+graph.get(edge.toString()).getDistanceFromSource());
					frontier.addLast(edge.toString());
				}
			}
		}		
		
		return Integer.MAX_VALUE;
	}
	
}
