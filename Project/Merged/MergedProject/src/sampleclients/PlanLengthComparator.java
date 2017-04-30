package sampleclients;

import java.util.Comparator;

public class PlanLengthComparator implements Comparator<Agent> {

	//agent has shorter(smaller) plan will be put on top of the binary heap
	@Override
	public int compare(Agent arg0, Agent arg1) {
		// TODO Auto-generated method stub
		if (arg0.solution.size() < arg1.solution.size())
			return -1;
		if (arg0.solution.size() > arg1.solution.size())
			return 1;
		return 0;
	}
	
}
