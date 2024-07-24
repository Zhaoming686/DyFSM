package utilities;

public class Settings {

    //FirstTime
    public static boolean FirstTime = true;

    //增量大小
    public static int IncSize = 200;

    //支持度阈值
    public static int minSup = 150;

    /** if true, debug mode is activated */
    public static boolean DEBUG_MODE = false;

    /** eliminate infrequent edges from graphs */
    public static boolean ELIMINATE_INFREQUENT_EDGES = true; // strategy in Gspan paper

    /** apply edge count pruning strategy */
    public static boolean EDGE_COUNT_PRUNING = true;

    /** Output the ids of graph containing each frequent subgraph */
    public static boolean outputGraphIds = true;

    /** maximum number of edges in each frequent subgraph */
    public static int maxNumberOfEdges = Integer.MAX_VALUE;

    // set the input and output file path
    public static String inPath = "D:/Users/zmche/Desktop/DyFSM/DyFSM/src/datasets/Chemical_340.txt";
    public static String outPath = ".//output.txt";

}
