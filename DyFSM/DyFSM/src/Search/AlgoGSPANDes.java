package Search;

import datastructures.*;
import tools.MemoryLogger;
import utilities.DataGlobal;
import utilities.Settings;

import javax.swing.text.html.HTMLDocument;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Zhaoming Chen
 */
public class AlgoGSPANDes {

    /**
     * runtime of the most recent execution
     */
    private long runtime;

    /**
     * runtime of the most recent execution
     */
    private double maxmemory;

    /**
     * number of graph in the input database
     */
    private int graphCount;

    private Map<String, List<Integer>[]> FrequentEdge;

    private List<Graph> graphDB;

    private int startId;
    private int endId;

    private int isomCount;

    //the key of the map is child and the value is its parent in the enumeration tree
    private Map<DFSCode, DFSCode> Child_Parent;

    //all the frequent subgraphs in this loop
    private Map<DFSCode, Set<Integer>> frequentSubgraphs;
    //new frequent subgraph to add
    private Map<DFSCode, Set<Integer>> frequentSubgraphsToadd;

    //the new maxFSMs
    private List<DFSCode> maxFSMtoAdd;
    private List<DFSCode> maxFSMnew;

    public AlgoGSPANDes() throws Exception {

    }

    /**
     * Run the GSpan algorithm
     *
     * @throws IOException            if error while writing to file
     * @throws ClassNotFoundException
     */

    public void runAlgorithm(int endgID) throws Exception {

        // if maximum size is 0
        if (Settings.maxNumberOfEdges <= 0) {
            return;
        }


        runtime = 0;
        maxmemory = 0;
        graphCount = 0;
        FrequentEdge = new HashMap<>();
        graphDB = new ArrayList<>();

        startId = endgID - Settings.IncSize;
        endId = endgID;

        isomCount = 0;
        Child_Parent = new HashMap<>();
        frequentSubgraphs = new HashMap<>();
        frequentSubgraphsToadd = new HashMap<>();

        maxFSMtoAdd = new ArrayList<>();
        maxFSMnew = new ArrayList<>();

        // Initialize the tool to check memory usage
        MemoryLogger.getInstance().reset();

        // Record the start time
        Long t1 = System.currentTimeMillis();

        // read graphs
        graphDB = readGraphs();

        // mining
        gSpan();

        // check the memory usage
        MemoryLogger.getInstance().checkMemory();

        Long t2 = System.currentTimeMillis();

        runtime = t2 - t1;

        maxmemory = MemoryLogger.getInstance().getMaxMemory();

        DataGlobal.FrequentEdgeLast.putAll(FrequentEdge);

        if (Settings.FirstTime){
            keepFSM();
        }else{
            UpdateMFS();
        }


        DataGlobal.patternCount = this.frequentSubgraphs.size();
    }


    private void keepFSM() throws IOException, ClassNotFoundException {
        //最终的maxFSM
        for (DFSCode c : this.maxFSMnew) {
            DataGlobal.maxFSMs.remove(c);
        }

        //更新其对应的graphids
        for (DFSCode max : DataGlobal.maxFSMs.keySet()) {
            if (max.size() == 1) {
                DataGlobal.maxFSMs.get(max).add(this.frequentSubgraphs.get(max));
            } else {
                DFSCode c = max.copy();
                for (int i = 0; i < c.size(); i++) {
                    DataGlobal.maxFSMs.get(max).add(i, null);
                }
                DataGlobal.maxFSMs.get(max).set(c.size() - 1, this.frequentSubgraphs.get(c));
                while (!this.Child_Parent.get(c).isEmpty()) {
                    c.remove();
                    DataGlobal.maxFSMs.get(max).set(c.size() - 1, this.frequentSubgraphs.get(c));
                }
            }
        }
    }

    private void UpdateMFS(){
        //Code and 它的相应的ids
        Map<DFSCode, List<Set<Integer>>> maxFSMtoAdd = new HashMap<>();
        //先删，遍历新加入发max，消除是其前缀的所有的max
        for (DFSCode c : this.maxFSMnew){
            Iterator iteratorMax = DataGlobal.maxFSMs.keySet().iterator();
            while (iteratorMax.hasNext()){
                DFSCode max = (DFSCode) iteratorMax.next();
                if (!DataGlobal.maxFSMs.containsKey(c)){
                    if (max.isPrefix(c)){
                        List<Set<Integer>> graphids = new ArrayList<>();
                        graphids.addAll(DataGlobal.maxFSMs.get(c).subList(0,c.size()-1));
                        maxFSMtoAdd.put(c, graphids);
                        iteratorMax.remove();
                    }
                }
            }
        }
        //后加
        DataGlobal.maxFSMs.putAll(maxFSMtoAdd);
    }

    /**
     * Read graph from the input file
     *
     * @return a list of input graph from the input graph database
     * @throws IOException if error reading or writing to file
     */

    private List<Graph> readGraphs() throws IOException {

        // 存储所有的边及其支持的图的ids
        Map<String, List<Integer>[]> EdgeSupportIds = new HashMap<>();

        String path = Settings.inPath;
        if (Settings.DEBUG_MODE) {
            System.out.println("start reading graphs...");
        }
        int i = -1;
        List<Graph> graphDatabase = new ArrayList<>();
        while(i < this.endId){
            BufferedReader br = new BufferedReader(new FileReader(new File(path)));

            String line = br.readLine();
            Boolean hasNextGraph = (line != null) && line.startsWith("t");

            // For each graph of the graph database
            while (hasNextGraph) {
                i++;
                hasNextGraph = false;
                int gId = i;

                if (gId > this.endId) break;

                Map<Integer, Vertex> vMap = new HashMap<>();
                while ((line = br.readLine()) != null && !line.startsWith("t")) {

                    String[] items = line.split(" ");

                    if (line.startsWith("v")) {
                        // If it is a vertex
                        int vId = Integer.parseInt(items[1]);
                        int vLabel = Integer.parseInt(items[2]);
                        vMap.put(vId, new Vertex(vId, vLabel));
                    } else if (line.startsWith("e")) {
                        // If it is an edge
                        int v1 = Integer.parseInt(items[1]);
                        int v2 = Integer.parseInt(items[2]);
                        int eLabel = Integer.parseInt(items[3]);
                        Edge e = new Edge(v1, v2, eLabel);

                        int LabelV1 = vMap.get(v1).getLabel();
                        int LabelV2 = vMap.get(v2).getLabel();
                        EdgeCount(EdgeSupportIds, LabelV1, LabelV2, eLabel, gId);

//                    System.out.println(v1 + " " + v2 + " " + vMap.get(v1).id + " " + vMap.get(v2).id);
                        vMap.get(v1).addEdge(e);
                        vMap.get(v2).addEdge(e);
                    }
                }
                graphDatabase.add(new Graph(gId, vMap));

                if (line != null) {
                    hasNextGraph = true;
                }
            }

            br.close();
        }


        if (Settings.DEBUG_MODE) {
            System.out.println("read successfully, totally " + graphDatabase.size() + " graphs");
        }

        graphCount = graphDatabase.size();

        SolveFrequentChangedEdge(EdgeSupportIds);

        return graphDatabase;
    }

    /**
     * 在读取数据的时候，进行 EdgeSupport 的信息加入
     *
     * @param LabelV1
     * @param LabelV2
     * @param eLabel
     * @param gId
     */
    private void EdgeCount(Map<String, List<Integer>[]> EdgeSupportIds, int LabelV1, int LabelV2, int eLabel, int gId) {
        String edgeString;
        if (LabelV1 <= LabelV2)
            edgeString = LabelV1 + "_" + eLabel + "_" + LabelV2;
        else
            edgeString = LabelV2 + "_" + eLabel + "_" + LabelV1;

        if (!EdgeSupportIds.containsKey(edgeString)) EdgeSupportIds.put(edgeString, null);

        List[] EdgeInf_SupportIds = EdgeSupportIds.get(edgeString);

        if (EdgeInf_SupportIds == null) {
            EdgeInf_SupportIds = new ArrayList[2];
            EdgeInf_SupportIds[0] = new ArrayList();
            EdgeInf_SupportIds[1] = new ArrayList();

            int vtemp;
            if (LabelV1 > LabelV2) {
                vtemp = LabelV2;
                LabelV2 = LabelV1;
                LabelV1 = vtemp;
            }//保证前小后大
            EdgeInf_SupportIds[0].add(LabelV1);
            EdgeInf_SupportIds[0].add(LabelV2);
            EdgeInf_SupportIds[0].add(eLabel);

            EdgeInf_SupportIds[1].add(gId);

            EdgeSupportIds.put(edgeString, EdgeInf_SupportIds);
        } else {
            if (!EdgeInf_SupportIds[1].contains(gId)) EdgeInf_SupportIds[1].add(gId);
            EdgeSupportIds.put(edgeString, EdgeInf_SupportIds);
        }
    }


    /**
     * Prune the infrequent edges
     */
    private void SolveFrequentChangedEdge(Map<String, List<Integer>[]> EdgeSupportIds) {
        if (DataGlobal.FrequentEdgeLast.isEmpty()) {
            Iterator<Entry<String, List<Integer>[]>> Edge_Infs = EdgeSupportIds.entrySet().iterator();
            while (Edge_Infs.hasNext()) {
                Entry<String, List<Integer>[]> Edge_Inf = Edge_Infs.next();
                int support = Edge_Inf.getValue()[1].size();
                if (support >= Settings.minSup) {
                    FrequentEdge.put(Edge_Inf.getKey(), Edge_Inf.getValue());
                }
            }
        } else {//说明有上一次的频繁边的信息存在
            Iterator<Entry<String, List<Integer>[]>> Edge_Infs = EdgeSupportIds.entrySet().iterator();
            while (Edge_Infs.hasNext()) {
                Entry<String, List<Integer>[]> Edge_Inf = Edge_Infs.next();
                int support = Edge_Inf.getValue()[1].size();
                if (support >= Settings.minSup) {
                    if (DataGlobal.FrequentEdgeLast.containsKey(Edge_Inf.getKey())) {
                        int supportLast = DataGlobal.FrequentEdgeLast.get(Edge_Inf.getKey())[1].size();
                        if (supportLast != support)
                            FrequentEdge.put(Edge_Inf.getKey(), Edge_Inf.getValue());
                    } else {
                        FrequentEdge.put(Edge_Inf.getKey(), Edge_Inf.getValue());
                    }
                }
            }
        }
    }


    /**
     * Initial call of the depth-first search
     *
     * @throws IOException            exception if error writing/reading to file
     * @throws ClassNotFoundException if error casting a class
     */
    private void gSpan() throws Exception {

//        for (Graph g : graphDB) {
//            g.precalculateVertexList();
//        }

        if (Settings.ELIMINATE_INFREQUENT_EDGES) {
            RemoveUnchangedEdged(graphDB);
        }

        if (Settings.DEBUG_MODE) {
            System.out.println("Precalculating information...");
        }

        // Create a set with all the graph ids
        Set<Integer> graphIdsDB = new HashSet<Integer>();
        for (Graph g : graphDB) {

            if (g.getAllVertices() == null || g.getAllVertices().length != 0) {

                graphIdsDB.add(g.getId());

                // Precalculate the list of neighbors of each vertex
                g.precalculateVertexNeighbors();

                // Precalculate the list of vertices having each label
                g.precalculateLabelsToVertices();
            } else {
                if (Settings.DEBUG_MODE) {
                    System.out.println("EMPTY GRAPHS REMOVED");
                }
                //emptyGraphsRemoved++;
            }
        }

        System.out.println("Starting depth-first search...");
        // Start the depth-first search

        Recover();

//        Judge();

        if (Settings.FirstTime) {
            gSpanDFS(new DFSCode(), graphIdsDB, null);
            Settings.FirstTime = false;
        } else {
            gSpanInc(new DFSCode(), graphIdsDB);
        }

    }

    /**
     * Create the pruning matrix
     */
    private void RemoveUnchangedEdged(List<Graph> graphDB) {

        // REMOVE INFREQUENT EDGES
        if (Settings.ELIMINATE_INFREQUENT_EDGES) {
            // CALCULATE THE SUPPORT OF EACH ENTRY
            for (Graph g : graphDB) {
                g.precalculateVertexList();
                Vertex[] vertices = g.getAllVertices();

                List<Integer> verticesToRmove = new ArrayList<>();
                for (int i = 0; i < vertices.length; i++) {
                    Vertex v1 = vertices[i];
                    int labelV1 = v1.getLabel();

                    Iterator<Edge> iter = v1.getEdgeList().iterator();
                    while (iter.hasNext()) {
                        Edge edge = iter.next();
                        int v2 = edge.another(v1.getId());
                        int labelV2 = g.getVLabel(v2);
                        int eLabel = edge.getEdgeLabel();

                        String edgeString;
                        if (labelV1 <= labelV2) {
                            edgeString = labelV1 + "_" + eLabel + "_" + labelV2;
                        } else {
                            edgeString = labelV2 + "_" + eLabel + "_" + labelV1;
                        }

                        if (!FrequentEdge.containsKey(edgeString)) {//不包含就说明没变化或不频繁
                            iter.remove();
                        }
                    }
                    if (v1.getEdgeList().isEmpty()) verticesToRmove.add(i);
                }
                for (int i : verticesToRmove) {
                    g.vMap.remove(i);
                }
                g.precalculateVertexList();
            }
        }
    }

    private void Recover() throws IOException, ClassNotFoundException {
        for (DFSCode c : DataGlobal.maxFSMs.keySet()) {
            DFSCode parentCode = c.copy();

            while (!this.frequentSubgraphs.containsKey(parentCode) && parentCode.size() != 0) {
                Set<Integer> graphids = DataGlobal.maxFSMs.get(c).get(parentCode.size() - 1);
                this.frequentSubgraphs.put(parentCode.copy(), graphids);
                DFSCode child = parentCode.copy();
                parentCode.remove();
                DFSCode parent = parentCode.copy();
                Child_Parent.put(child, parent);
            }
        }
    }

//    private void Judge(){
//        int i = 0;
//        for (DFSCode c : this.frequentSubgraphs.keySet()){
//            Set<Integer> graphIds1 = this.frequentSubgraphs.get(c);
//            Set<Integer> graphIds2 = DataGlobal.frequentSubgraphs.get(c);
//            System.out.println(i++);
//            if (!graphIds1.equals(graphIds2))
//                break;
//        }
//    }

    /**
     * Recursive method to perform the depth-first search
     *
     * @param c the current DFS code
     * @throws IOException            exception if error writing/reading to file
     * @throws ClassNotFoundException if error casting a class
     */
    private void gSpanDFS(DFSCode c, Set<Integer> graphIdsDB, List<DFSCode> CanMaxToRemove) throws Exception {
        // If we have reached the maximum size, we do not need to extend this graph
        if (c.size() == Settings.maxNumberOfEdges - 1) {
            return;
        }


        // Find all the extensions of this graph, with their support values
        // They are stored in a map where the key is an extended edge, and the value is
        // the
        // is the list of graph ids where this edge extends the current subgraph c.
        List<Integer> supportCount = new ArrayList<>();
        supportCount.add(0);

        Map<ExtendedEdge, Set<Integer>> extensions = rightMostPathExtensions(c, graphIdsDB, supportCount);

        //避免使用不频繁的
        if (!c.isEmpty() && supportCount.get(0) < Settings.minSup) {
            this.frequentSubgraphs.remove(c);
            CanMaxToRemove.remove(c);
            DFSCode c_child = c.copy();
            DFSCode c_parent = c.copy();
            c_parent.remove();
            this.Child_Parent.remove(c_child, c_parent);
//            this.frequentCount--;
            return;
        }

        int FreDesCount = 0;
        // For each extension
//		if (extensions != null) {
        for (Entry<ExtendedEdge, Set<Integer>> entry : extensions.entrySet()) {
            // Get the support
            Set<Integer> newGraphIDs = entry.getValue();
            int sup = newGraphIDs.size();

            // Create the new DFS code of this graph
            DFSCode newC = c.copy();
            ExtendedEdge extension = entry.getKey();
            newC.add(extension);

            if (isCanonical(newC)) {
                // if the support is enough
                if (sup >= Settings.minSup) {
                    FreDesCount++;
                    Set<Integer> SupportValue = new HashSet<>();
                    SupportValue.add(sup);
                    this.frequentSubgraphs.put(newC, SupportValue);

//                    DataGlobal.frequentSubgraphs.put(newC, newGraphIDs);

                    // Try to extend this graph to generate larger frequent subgraphs
//                    this.frequentCount++;

                    this.Child_Parent.put(newC, c);

                    gSpanDFS(newC, newGraphIDs, CanMaxToRemove);
                } else {
                    Set<Integer> SupportValue = new HashSet<>();
                    SupportValue.add(sup);
                    DataGlobal.InfrequentSubgraphs.put(newC, SupportValue);
                }
            }
        }

        if (!c.isEmpty()) {
            if (FreDesCount == 0) {//说明为极大子图
//                this.maxFSMtoAdd.add(c);
                DataGlobal.maxFSMs.put(c, new ArrayList<>());
                //DataGlobal.maxFSMs.put(c,newParentIds);
            }
        }
        // check the memory usage
        MemoryLogger.getInstance().checkMemory();
    }


    private void gSpanInc(DFSCode c, Set<Integer> graphIdsDB) throws Exception {
        /**先在增量数据集中增长*/
        //首先找出增量ids
        Set<Integer> graphIdsInc = new HashSet<>();
        for (int id : graphIdsDB) {
            if (id >= startId + 1 && id <= endId) {
                graphIdsInc.add(id);
            }
        }

        gSpanDFSDes(new DFSCode(), graphIdsInc);

    }

    /**
     * 增量数据集的增长
     *
     * @param c
     * @param graphIdsInc
     */

    private void gSpanDFSDes(DFSCode c, Set<Integer> graphIdsInc) throws Exception {
        // If we have reached the maximum size, we do not need to extend this graph
        if (c.size() == Settings.maxNumberOfEdges - 1) {
            return;
        }

        Map<ExtendedEdge, Set<Integer>> extensions = rightMostPathExtensions(c, graphIdsInc, null);

//        if (supportCount < Settings)

        // For each extension
//		if (extensions != null) {
//        int number = 0;
        for (Entry<ExtendedEdge, Set<Integer>> entry : extensions.entrySet()) {
            // Get the support
            Set<Integer> newGraphIDs = entry.getValue();
            // Create the new DFS code of this graph
            DFSCode newC = c.copy();
            ExtendedEdge extension = entry.getKey();
            newC.add(extension);

            //非边界频繁子图的处理，省略计算的部分
            if (this.frequentSubgraphs.containsKey(newC)) {
                int sup = 0;
                for (int value : this.frequentSubgraphs.get(newC)) sup = value;

                sup -= newGraphIDs.size();

                if (sup >= Settings.minSup) {
                    Set<Integer> SupportValue = new HashSet<>();
                    SupportValue.add(sup);
                    this.frequentSubgraphsToadd.put(newC,SupportValue);
                    gSpanDFSDes(newC, newGraphIDs);
                }
            }

            // check the memory usage
            MemoryLogger.getInstance().checkMemory();
        }
    }

    private Map<ExtendedEdge, Set<Integer>> rightMostPathExtensions(DFSCode c, Set<Integer> graphIds, List<Integer> supportCount) {

        Map<ExtendedEdge, Set<Integer>> extensions = new HashMap<>();

        // if the DFS code is empty (WE START FROM AN EMPTY GRAPH)
        if (c.isEmpty()) {
            for (Integer graphId : graphIds) {
                Graph g = graphDB.get(graphId);

                if (Settings.EDGE_COUNT_PRUNING && c.size() >= g.getEdgeCount()) {
                    //pruneByEdgeCountCount++;
                    continue;
                }

                // find all distinct label tuples
                for (Vertex vertex : g.getAllVertices()) {
                    for (Edge e : vertex.getEdgeList()) {
                        int v1L = g.getVLabel(e.v1);
                        int v2L = g.getVLabel(e.v2);
                        ExtendedEdge ee1;
                        if (v1L < v2L) {
                            ee1 = new ExtendedEdge(0, 1, v1L, v2L, e.getEdgeLabel());
                        } else {
                            ee1 = new ExtendedEdge(0, 1, v2L, v1L, e.getEdgeLabel());
                        }

                        // Update the set of graph ids for this pattern
                        Set<Integer> setOfGraphIDs = extensions.get(ee1);
                        if (setOfGraphIDs == null) {
                            setOfGraphIDs = new HashSet<>();
                            extensions.put(ee1, setOfGraphIDs);
                        }
                        setOfGraphIDs.add(graphId);
//                        if(setOfGraphIDs.size() > highestSupport){
//                        	highestSupport =  setOfGraphIDs.size();
//                        }
                    }
                }
            }
        } else {
            // IF THE DFS CODE IS NOT EMPTY (WE WANT TO EXTEND SOME EXISTING GRAPH)
//			int remaininggraphCount = graphIds.size();
//			int highestSupport = 0;
            int FirstV1Label = c.getEeL().get(0).getvLabel1();
            int FirstV2Label = c.getEeL().get(0).getvLabel2();
            int FirstELabel = c.getEeL().get(0).getEdgeLabel();

            int rightMost = c.getRightMost();
            // For each graph
            for (Integer graphId : graphIds) {

                Graph g = graphDB.get(graphId);

                List<Map<Integer, Integer>> isoms = subgraphIsomorphisms(c, g);
                isomCount++;

                if (supportCount != null && !isoms.isEmpty()) {
                    int value = supportCount.get(0);
                    supportCount.set(0, value + 1);
                }

                if (Settings.EDGE_COUNT_PRUNING && c.size() >= g.getEdgeCount()) {
                    //pruneByEdgeCountCount++;
                    continue;
                }

                for (Map<Integer, Integer> isom : isoms) {

                    // backward extensions from rightmost child
                    Map<Integer, Integer> invertedISOM = new HashMap<>();
                    for (Entry<Integer, Integer> entry : isom.entrySet()) {
                        invertedISOM.put(entry.getValue(), entry.getKey());
                    }
                    int mappedRM = isom.get(rightMost);
                    int mappedRMlabel = g.getVLabel(mappedRM);
                    for (Vertex x : g.getAllNeighbors(mappedRM)) {
                        Integer invertedX = invertedISOM.get(x.getId());
                        if (invertedX != null && c.onRightMostPath(invertedX) && c.notPreOfRM(invertedX)
                                && !c.containEdge(rightMost, invertedX)) {
                            // rightmost and invertedX both have correspondings in g, so label of vertices
                            // and edge all
                            // can be found by correspondings
                            if (g.getEdgeLabel(mappedRM, x.getId()) < FirstELabel) continue;
                            ExtendedEdge ee = new ExtendedEdge(rightMost, invertedX, mappedRMlabel, x.getLabel(),
                                    g.getEdgeLabel(mappedRM, x.getId()));
                            if (extensions.get(ee) == null)
                                extensions.put(ee, new HashSet<>());
                            extensions.get(ee).add(g.getId());
                        }
                    }
                    // forward extensions from nodes on rightmost path
                    Collection<Integer> mappedVertices = isom.values();
                    for (int v : c.getRightMostPath()) {
                        int mappedV = isom.get(v);
                        int mappedVlabel = g.getVLabel(mappedV);
                        for (Vertex x : g.getAllNeighbors(mappedV)) {
                            if (x.getLabel() >= FirstV1Label
                                    || (x.getLabel() == FirstV1Label && mappedVlabel >= FirstV2Label)
                                    || (x.getLabel() == FirstV1Label && mappedVlabel == FirstV2Label && g.getEdgeLabel(mappedV, x.getId()) >= FirstELabel)) {
                                if (!mappedVertices.contains(x.getId())) {
                                    ExtendedEdge ee = new ExtendedEdge(v, rightMost + 1, mappedVlabel, x.getLabel(),
                                            g.getEdgeLabel(mappedV, x.getId()));
                                    if (extensions.get(ee) == null)
                                        extensions.put(ee, new HashSet<>());
                                    Set<Integer> setOfGraphIDs = extensions.get(ee);

                                    setOfGraphIDs.add(g.getId());
                                }

//								if (setOfGraphIDs.size() > highestSupport) {
//									highestSupport = setOfGraphIDs.size();
//								}
                            }
                        }
                    }
                }

//				if (SKIP_STRATEGY && (highestSupport + remaininggraphCount < Settings.minSup)) {
//            		System.out.println("BREAK2");
//					skipStrategyCount++;
////					extensions = null;
//					break;
//				}
//				remaininggraphCount--;
            } // end FOR GRAPH ID
        }
        return extensions;
    }

    /**
     * Find all isomorphisms between graph described by c and graph g each
     * isomorphism is represented by a map
     *
     * @param c a dfs code representing a subgraph
     * @param g a graph
     * @return the list of all isomorphisms
     */
    private List<Map<Integer, Integer>> subgraphIsomorphisms(DFSCode c, Graph g) {

        List<Map<Integer, Integer>> isoms = new ArrayList<>();

        // initial isomorphisms by finding all vertices with same label as vertex 0 in C
        int startLabel = c.getEeL().get(0).getvLabel1(); // only non-empty datastructures.DFSCode will be real parameter
        for (int vID : g.findAllWithLabel(startLabel)) {
            Map<Integer, Integer> map = new HashMap<>();
            map.put(0, vID);
            isoms.add(map);
        }

        // each extended edge will update partial isomorphisms
        // for forward edge, each isomorphism will be either extended or discarded
        // for backward edge, each isomorphism will be either unchanged or discarded
        for (ExtendedEdge ee : c.getEeL()) {
            int v1 = ee.getV1();
            int v2 = ee.getV2();
            int v2Label = ee.getvLabel2();
            int eLabel = ee.getEdgeLabel();

            List<Map<Integer, Integer>> updateIsoms = new ArrayList<>();
            // For each isomorphism
            for (Map<Integer, Integer> iso : isoms) {

                // Get the vertex corresponding to v1 in the current edge
                int mappedV1 = iso.get(v1);

                // If it is a forward edge extension
                if (v1 < v2) {
                    Collection<Integer> mappedVertices = iso.values();

                    // For each neighbor of the vertex corresponding to V1
                    for (Vertex mappedV2 : g.getAllNeighbors(mappedV1)) {

                        // If the neighbor has the same label as V2 and is not already mapped and the
                        // edge label is
                        // the same as that between v1 and v2.
                        if (v2Label == mappedV2.getLabel() && (!mappedVertices.contains(mappedV2.getId()))
                                && eLabel == g.getEdgeLabel(mappedV1, mappedV2.getId())) {

                            // TODO: PHILIPPE: getEdgeLabel() in the above line could be precalculated in
                            // datastructures.Graph.java ...

                            // because there may exist multiple extensions, need to copy original partial
                            // isomorphism
                            HashMap<Integer, Integer> tempM = new HashMap<>(iso.size() + 1);
                            tempM.putAll(iso);
                            tempM.put(v2, mappedV2.getId());

                            updateIsoms.add(tempM);
                        }
                    }
                } else {
                    // If it is a backward edge extension
                    // v2 has been visited, only require mappedV1 and mappedV2 are connected in g
                    int mappedV2 = iso.get(v2);
                    if (g.isNeighboring(mappedV1, mappedV2) && eLabel == g.getEdgeLabel(mappedV1, mappedV2)) {
                        updateIsoms.add(iso);
                    }
                }
            }
            isoms = updateIsoms;
        }

        // Return the isomorphisms
        return isoms;
    }

    /**
     * Check if a DFS code is canonical
     *
     * @param c a DFS code
     * @return true if it is canonical, and otherwise, false.
     */
    private boolean isCanonical(DFSCode c) {
        DFSCode canC = new DFSCode();

        for (int i = 0; i < c.size(); i++) {

            Map<ExtendedEdge, Set<Integer>> extensions = rightMostPathExtensionsFromSingle(canC, c);

            if (extensions == null) {
                break;
            }

            ExtendedEdge minEE = null;
            for (ExtendedEdge ee : extensions.keySet()) {
                if (ee.smallerThan(minEE))
                    minEE = ee;
            }

            if (minEE.smallerThan(c.getAt(i)))
                return false;
            canC.add(minEE);
        }
        return true;
    }

    private Map<ExtendedEdge, Set<Integer>> rightMostPathExtensionsFromSingle(DFSCode c, DFSCode c_old) {
        if (c.size() == c_old.size()) return null;

        Graph g = new Graph(c_old);

        int gid = g.getId();

        // Map of extended edges to graph ids
        Map<ExtendedEdge, Set<Integer>> extensions = new HashMap<>();
        Set<Integer> ids = new HashSet<>();
        ids.add(-1);

        if (c.isEmpty()) {
            // IF WE HAVE AN EMPTY SUBGRAPH THAT WE WANT TO EXTEND
            ExtendedEdge ee1 = c_old.getEeL().get(0);
            extensions.put(ee1, ids);
            // Update the set of graph ids for this pattern
            ExtendedEdge ee2temp = c_old.getEeL().get(c_old.size() - 1);
            int v1L = ee2temp.getvLabel1();
            int v2L = ee2temp.getvLabel2();
            int eL = ee2temp.getEdgeLabel();
            ExtendedEdge ee2;
            if (v1L < v2L) {
                ee2 = new ExtendedEdge(0, 1, v1L, v2L, eL);
            } else {
                ee2 = new ExtendedEdge(0, 1, v2L, v1L, eL);
            }
            // Update the set of graph ids for this pattern
            extensions.put(ee2, ids);

        } else {

            // IF WE WANT TO EXTEND A SUBGRAPH
            int rightMost = c.getRightMost();

            // Find all isomorphisms of the DFS code "c" in graph "g"
            List<Map<Integer, Integer>> isoms = subgraphIsomorphisms(c, g);

            // For each isomorphism
            for (Map<Integer, Integer> isom : isoms) {

                // backward extensions from rightmost child
                Map<Integer, Integer> invertedISOM = new HashMap<>();
                for (Entry<Integer, Integer> entry : isom.entrySet()) {
                    invertedISOM.put(entry.getValue(), entry.getKey());
                }
                int mappedRM = isom.get(rightMost);
                int mappedRMlabel = g.getVLabel(mappedRM);
                for (Vertex x : g.getAllNeighbors(mappedRM)) {
                    Integer invertedX = invertedISOM.get(x.getId());
                    if (invertedX != null && c.onRightMostPath(invertedX) && c.notPreOfRM(invertedX)
                            && !c.containEdge(rightMost, invertedX)) {
                        // rightmost and invertedX both have correspondings in g, so label of vertices
                        // and edge all
                        // can be found by correspondings
                        ExtendedEdge ee = new ExtendedEdge(rightMost, invertedX, mappedRMlabel, x.getLabel(),
                                g.getEdgeLabel(mappedRM, x.getId()));
                        if (extensions.get(ee) == null)
                            extensions.put(ee, new HashSet<>());
                        extensions.get(ee).add(g.getId());
                    }
                }
                // forward extensions from nodes on rightmost path
                Collection<Integer> mappedVertices = isom.values();
                for (int v : c.getRightMostPath()) {
                    int mappedV = isom.get(v);
                    int mappedVlabel = g.getVLabel(mappedV);
                    for (Vertex x : g.getAllNeighbors(mappedV)) {
                        if (!mappedVertices.contains(x.getId())) {
                            ExtendedEdge ee = new ExtendedEdge(v, rightMost + 1, mappedVlabel, x.getLabel(),
                                    g.getEdgeLabel(mappedV, x.getId()));
                            if (extensions.get(ee) == null)
                                extensions.put(ee, new HashSet<>());
                            extensions.get(ee).add(g.getId());
                        }
                    }
                }
            }
        }
        return extensions;
    }

    /**
     * Print statistics about the algorithm execution to System.out.
     */
    public void printStats() {
        System.out.println("=============  GSPAN v2.40 - STATS =============");
        System.out.println(" Number of graph in the input database: " + graphCount);
        System.out.println(" Frequent subgraph count : " + DataGlobal.patternCount);
//        System.out.println(" Frequent subgraph count : " + frequentCount);
        System.out.println(" Total time ~ " + runtime + "ms");
        System.out.println(" Settings.minSup : " + Settings.minSup + " graphs");
        System.out.println(" Maximum memory usage : " + maxmemory + " mb");
        System.out.println("The number of isoms : " + isomCount);

        if (Settings.DEBUG_MODE) {
            if (Settings.ELIMINATE_INFREQUENT_EDGES) {
                //	System.out.println("  Number of infrequent edge labels pruned : " + edgeRemovedByLabel);
            }
            if (Settings.EDGE_COUNT_PRUNING) {
                //	System.out.println("  Extensions skipped (edge count pruning) : " + pruneByEdgeCountCount);
            }
//			if (SKIP_STRATEGY) {
//				System.out.println("  Skip strategy count : " + skipStrategyCount);
//			}
        }
        System.out.println("===================================================");
    }

}
