package datastructures;

import java.util.Objects;
import java.util.Set;

/**
 * This is a frequent subgraph. It stores the (1) DFS code of the subgraph,
 * (2) the support of the subgraph, and (3) the set of graphs where the 
 * subgraph appears.
 * 
 * @author Zhaoming Chen
 */
public class FrequentSubgraph implements Comparable<FrequentSubgraph> {
	
	/** dfs code */
    public DFSCode dfsCode;
    
    /** the ids of graphs where the subgraph appears */
    public Set<Integer> setOfGraphsIDs;
    
    /** the support of the subgraph */
    public int support;
    
    /**
     * Constructor
     * @param dfsCode a dfs code
     * @param setOfGraphsIDs the ids of graphs where the subgraph appears
     * @param support the support of the subgraph
     */
    public FrequentSubgraph(DFSCode dfsCode, Set<Integer> setOfGraphsIDs, int support){
    	this.dfsCode = dfsCode;
    	this.setOfGraphsIDs = setOfGraphsIDs;
    	this.support = support;
    }

    /**
     * Compare this subgraph with another subgraph
     * @param o another subgraph
     * @return 0 if equal, -1 if smaller, 1 if larger (in terms of support).
     */
    public int compareTo(FrequentSubgraph o) {
		if(o == this){
			return 0;
		}
		long compare =  this.support - o.support;
		if(compare > 0){
			return 1;
		}
		if(compare < 0){
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FrequentSubgraph that = (FrequentSubgraph) o;
		return this.dfsCode.equals(that.dfsCode);
	}

}
