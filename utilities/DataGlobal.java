package utilities;

import datastructures.DFSCode;

import java.util.*;

public class DataGlobal {

    // 存储上一个时间的频繁边，以及其支持度的ids
    public static Map<String, List<Integer>[]> FrequentEdgeLast = new HashMap<>();

    //记录频繁子图的数量
    public static int patternCount = 0;

    /** The list of frequent subgraphs found by the last execution */
//    public static List<FrequentSubgraph> frequentSubgraphs = new ArrayList<FrequentSubgraph>();

    public static Map<DFSCode, Set<Integer>> frequentSubgraphs = new HashMap<>();

    public static Map<DFSCode, List<Set<Integer>>> maxFSMs = new HashMap<>();
    public static Map<DFSCode, Set<Integer>> InfrequentSubgraphs = new HashMap<>();

    //存储上次的伪极大子图
    public static List<DFSCode> PseudoMaxSubLast = new ArrayList<>();


}
