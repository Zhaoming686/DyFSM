package datastructures;

import datastructures.Edge;
import datastructures.Vertex;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author Zhaoming Chen
 */
public class Graph {

	/** the graph ID */
    private int id;
    
    /** Map each vertex id to its corresponding datastructures.Vertex object */
    public Map<Integer, Vertex> vMap;
    
    /** Map each vertex id to the ids of its neighbors */
    private Map<Integer, Vertex[]> neighborCache;

    /** The list of vertices */
    Vertex[] vertices;
    
    /** Map each vertex label to the list of vertex ids having this label */
    Map<Integer, int[]> mapLabelToVertexIDs;

    /** Number of edges */
	private int edgeCount = 0;
    
	/** empty vertex list */
	private static final Vertex[] EMPTY_VERTEX_LIST = new Vertex[0];
	
	/** empty integer array */
	private static final int[] EMPTY_INTEGER_ARRAY = new int[0];
	
	/**
	 * Remove infrequent label from this graph
	 * @param label the label
	 */
	public void removeInfrequentLabel(int label) {
		
		Iterator<Entry<Integer, Vertex>> iter = vMap.entrySet().iterator();
		while (iter.hasNext()) {
			Vertex vertex = (Vertex) iter.next().getValue();
			if(vertex.getLabel() == label){
				iter.remove();
			}
		}
		
		for(Vertex vertex: vMap.values()){
			Iterator<Edge> it = vertex.getEdgeList().iterator();
			while (it.hasNext()) {
				Edge edge = (Edge) it.next();
				if(vMap.get(edge.v1) == null || vMap.get(edge.v2) == null){
					it.remove();
				}
			}
		}
	}

    /**
     * Constructor
     * @param id  a graph id
     * @param vMap a map of vertex id to corresponding datastructures.Vertex object
     */
    public Graph(int id, Map<Integer, Vertex> vMap) {
        this.id = id;
        this.vMap= vMap;
    }


    public void SetGraphId(int id){
        this.id = id;
    }

	/**
     * Constructor
     * @param c  a dfs code
     */
    public Graph(DFSCode c) {
        this.vMap = new HashMap<>();
        for (ExtendedEdge ee : c.getEeL()) {
            int v1 = ee.getV1();
            int v2 = ee.getV2();
            int v1L = ee.getvLabel1();
            int v2L = ee.getvLabel2();
            int eL = ee.getEdgeLabel();
            
            Edge e = new Edge(v1, v2, eL);
            if (vMap.get(v1) == null)
                vMap.put(v1, new Vertex(v1, v1L));
            if (vMap.get(v2) == null)
                vMap.put(v2, new Vertex(v2, v2L));
            vMap.get(v1).addEdge(e);
            vMap.get(v2).addEdge(e);
        }
        this.id = -1;

    	precalculateVertexList();
        precalculateVertexNeighbors();
        precalculateLabelsToVertices();
    }

    

    /**
     * For optimization purposes, precalculate the list of neighbors of each vertex.
     */
	public void precalculateVertexNeighbors() {
		
        neighborCache = new HashMap<>();

        List<Vertex> neighbors = new ArrayList<Vertex>();
        
        // For each vertex
        for(Entry<Integer, Vertex> entry: vMap.entrySet()){
        	int vertexID = entry.getKey();
        	Vertex vertex = entry.getValue();
        	List<Edge> vertexEdgeList = vertex.getEdgeList();

        	// For each edge
            for (Edge e : vertexEdgeList) {
            	Vertex vertexNeighboor = vMap.get(e.another(vertexID));
                neighbors.add(vertexNeighboor);
            }
            
            // Convert to array
            Vertex [] arrayNeighbors = new Vertex[neighbors.size()];
            for(int i =0; i< neighbors.size(); i++){
            	arrayNeighbors[i] = neighbors.get(i);
            }
            
            // Sort the array
            Arrays.sort(arrayNeighbors);
            
            neighborCache.put(vertexID, arrayNeighbors);
            edgeCount += neighbors.size();
            neighbors.clear();
        }
        edgeCount = edgeCount / 2;
	}

    public Map<Integer, Vertex> getvMap() {
        return vMap;
    }

    /**
     * For optimization purposes, precalculate the list of vertices in this graph.
     */
	public void precalculateVertexList() {
		
		vertices = new Vertex[vMap.size()];

        // For each vertex
        int j = 0;
        for(Entry<Integer, Vertex> entry: vMap.entrySet()){
        	Vertex vertex = entry.getValue();
            
            // Add the vertex to the precalculated array of vertices
            vertices[j] = vertex;
            j++;
        }
	}
	
	/**
     * Precalculate the list of vertices having each label
     */
	public void precalculateLabelsToVertices() {
		mapLabelToVertexIDs = new HashMap<Integer, int[]>();

		// create a temporary list to store vertex id
		List<Integer> sameIDs = new ArrayList<Integer>();

		// for each vertex
		for(int i = 0; i < vertices.length; i++){
			// get the label
			int label = vertices[i].getLabel();
			// if we did not already process that label
			if(!mapLabelToVertexIDs.containsKey(label)){
				// Find all other vertices having that label

				for(int j = i+1; j < vertices.length; j++){
					if(vertices[j].getLabel() == label){
						sameIDs.add(vertices[j].getId());
					}
				}

				// Create an array  to store the vertice IDs
				int[] verticeIDs = new int[sameIDs.size()+1];
				verticeIDs[0] = vertices[i].getId();
				for(int k = 0; k< sameIDs.size(); k++){
					verticeIDs[k+1] = sameIDs.get(k);
				}

				mapLabelToVertexIDs.put(label, verticeIDs);

				sameIDs.clear();
			}
		}
	}

	
    /**
     * Get all vertice IDs having a given label
     * @param targetLabel the label
     * @return the list of vertice IDs
     */
    public int[] findAllWithLabel(int targetLabel) {
    	int[] vertexIds = mapLabelToVertexIDs.get(targetLabel);
    	if(vertexIds == null){
    		return EMPTY_INTEGER_ARRAY;
    	}
        return vertexIds;
    }

	/**
     * Get all vertices of this graph
     * @return the set of all vertices
     */
    public Vertex[] getAllVertices() {
        return vertices;
    }
    
	/**
     * Get all vertices of this graph
     * @return the set of all vertices
     */
    public List<Vertex> getNonPrecalculatedAllVertices() {
       List<Vertex> vertices = new ArrayList<>(vMap.size());

        
        // For each vertex
        for(Entry<Integer, Vertex> entry: vMap.entrySet()){
        	Vertex vertex = entry.getValue();

            // Add the vertex to the precalculated array of vertices
        	vertices.add(vertex);
        }
        
        return vertices;
    }

    /**
     * Get all edges of this graph
     * @return the set of all edges
     */
    public Set<Edge> getAllEdges() {
        Set<Edge> edges = new HashSet<>();
//        for (Vertex v : getAllVertices())
        for (Vertex v : this.vMap.values())
            edges.addAll(v.getEdgeList());
        return edges;
    }

    public int getVLabel(int v) {
        return vMap.get(v).getLabel();
    } 

    /**
     * Get the label of the edge between two vertex
     * @param v1 the id of a vertex
     * @param v2 the id of another vertex
     * @return the label if the edge exists, or otherwise -1.
     */
    public int getEdgeLabel(int v1, int v2) {
        for (Edge e : vMap.get(v1).getEdgeList()) {
        	if (e.v1 == v1 && e.v2 == v2){
                return e.getEdgeLabel();
        	}
        	if (e.v2 == v1 && e.v1 == v2){
                return e.getEdgeLabel();
        	}
        }
        return -1;
    }

    public Edge getEdge(int v1, int v2) {
        for (Edge e : vMap.get(v1).getEdgeList()) {
            if (e.v1 == v1 && e.v2 == v2){
                return e;
            }
            if (e.v2 == v1 && e.v1 == v2){
                return e;
            }
        }
        return null;
    }

    /**
     * Get the list of vertices connected to a given vertex
     * @param v the vertex
     * @return the list of vertices
     */
    public Vertex[] getAllNeighbors(int v) {
    	Vertex[] neighboors = neighborCache.get(v);
    	if(neighboors == null){
    		return EMPTY_VERTEX_LIST;
    	}
        return neighboors;
    }

    /**
     * Check if two vertices are neighbors
     * @param v1 the first vertex
     * @param v2 the second vertex
     * @return true if they are neighbors
     */
    public boolean isNeighboring(int v1, int v2) {
    	Vertex[] neighborsOfV1 = neighborCache.get(v1);
    	
//    	for(datastructures.Vertex vertex: neighborsOfV1){
//    		if(vertex.getId() == v2){
//    			return true;
//    		}
//    	}

    	// ================ binary search ==========
		int low = 0;
		int high = neighborsOfV1.length - 1;

		while (high >= low) {
			int middle = (low + high) / 2;
			int val = neighborsOfV1[middle].getId();
			if (val == v2) {
				return true;
			}
			if (val < v2) {
				low = middle + 1;
			}
			if (val > v2) {
				high = middle - 1;
			}
		}
		return false;	
    }

    /**
     * Get the number of vertex
     * @return the number of vertex
     */
    public int getVertexCount() {
        return vertices.length;
    }
    
    /** 
     * Get the number of edges
     * @return the number of edges
     */
    public int getEdgeCount() {
//    	if(edgeCount == - 1){
//	        int num = 0;
//	        for (datastructures.Vertex v : vertices) {
//	            num += v.getEdgeList().size();
//	        }
//	        edgeCount = num/2;
//    	}
        return edgeCount;
    }


    /**
     * Get the graph id
     * @return the id
     */
    public int getId() {
        return id;
    }




}
