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
	
	
	public HashMap<Integer, Vertex> graph;
	public static HashMap<Integer,Integer> matrix;
	public List<Integer> sortedkeys;
	public Grid(HashMap<Integer, Vertex> graph){
		this.graph=graph;
		matrix=new HashMap<Integer,Integer>();
	}
	
	public void BFSMapping(){
		int dis=0;
		int key=0;
		for(int source: graph.keySet()){
			//for(int target: sortedkeys){
				//key=pairSourceTarget(source, target);
				//if(key!=0 && !Grid.matrix.containsKey(key)){
			BFSDistance(source);
					//Grid.matrix.put(key, dis);
				//}
				
			//}
			//break; //test
		}
	}
	
	public int getBFSDistance(int[] source, int[] target){
		int hash1 = ((source[0] + source[1])*(source[0] + source[1] + 1))/2 + source[1];
		int hash2 = ((target[0] + target[1])*(target[0] + target[1] + 1))/2 + target[1];
		int unionhash=Grid.pairSourceTarget(hash1, hash2);
		if(!Grid.matrix.containsKey(unionhash)){
			int dis=BFSDistance(hash1,hash2);
			Grid.matrix.put(unionhash, dis);
			return dis;
		}else{
			return Grid.matrix.get(unionhash);
		}
	}
	
	
	//abandoned function.
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
			int tem_key=pairSourceTarget(source.hashCode(),graph.get(vet).hashCode());
			Grid.matrix.put(tem_key,graph.get(vet).getDistanceFromSource());	
			for (Vertex edge:graph.get(vet.hashCode()).getEdges()){
				if(!closedset.contains(edge.hashCode()))
				{				
					graph.get(edge.hashCode()).setDistanceFromSource(graph.get(vet).getDistanceFromSource()+1);
					//int tem_key=pairSourceTarget(source.hashCode(),edge.hashCode());
					//if(!Grid.matrix.containsKey(tem_key))
					//Grid.matrix.put(tem_key,edge.getDistanceFromSource());
					frontier.addLast(edge.hashCode());
				}
			}
		}		
		
		return Integer.MAX_VALUE;
	}
	
	
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
			Grid.matrix.put(tem_key,graph.get(vet).getDistanceFromSource());	
			for (Vertex edge:graph.get(vet).getEdges()){
				if(!closedset.contains(edge.hashCode()) && !frontier.contains(edge.hashCode()) )
				{				
					graph.get(edge.hashCode()).setDistanceFromSource(graph.get(vet).getDistanceFromSource()+1);
					frontier.addLast(edge.hashCode());
				}
			}
		}		
		
		return Integer.MAX_VALUE;
	}
	
	
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
