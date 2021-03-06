package org.ixxi.bridginess;

import java.io.IOException;
import static java.lang.Boolean.FALSE;
import static java.lang.Math.log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.EdgeIterable;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.HierarchicalDirectedGraph;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.Node;
import org.gephi.statistics.plugin.ChartUtils;

import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.TempDirUtils;
import org.gephi.utils.TempDirUtils.TempDir;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Lookup;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openide.util.Exceptions;


/**
 *
 * @author PJ@IXXI, MJ@MedialabSciencesPO, MM@IXXI
 */


public class Bridginess implements Statistics, LongTask {
    
    public static final String BETWEENNESS = "betweenness"; //betweenness centrality
    //public static final String BRIDGINESSMSP = "bridginessmsp"; //minimum SPs
    public static final String BRIDGINESSMSP3 = "bridginessmsp3"; //minimum len 3 SPs
    public static final String BRIDGINESSMSP4 = "bridginessmsp4"; //minimum len 4 SPs
    public static final String BRIDGINESSMSP5 = "bridginessmsp5"; //minimum len 5 SPs
    public static final String BRIDGINESSEXN = "bridginessexn"; //exclude neighbors
    
    public static final String NODEENTROPY = "nodeentropy";
    public static final String STIRLING = "nodestirling";
    
    //from "gephi-0.8.2/modules/StatisticsPlugin/src/main/java/org/gephi/statistics/plugin/Modularity.java"
    public static final String MODULARITY_CLASS = "modularity_class";
    //* */
    private double[] betweenness;
    //private double[] bridginessMSP;
    private double[] bridginessMSP3;
    private double[] bridginessMSP4;
    private double[] bridginessMSP5;
    private double[] bridginessEXN;
    
    private double[] nodeentropy; 
    private double[] stirling;
    
    private double avgBridginess;
    
  
    
    private int N;
    
    private boolean isDirected;
    
    private ProgressTicket progress;
    
    private boolean isCanceled;
    private int shortestPaths;
    //private boolean isNormalized; //let's forget normalization for now
    
  
    private int exclNeighbors; //neighbors bridginess
    private int minPathLength; //path bridginess
    

    public Bridginess() {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null && graphController.getModel() != null) {
            //exclNeighbors = graphController.getModel().getExclNeighbors();
            isDirected = graphController.getModel().isDirected();
        }
    }
    
    /**
     *
     * @param graphModel
     */
    
    @Override
    public void execute(GraphModel graphModel, AttributeModel attributeModel) {
        HierarchicalGraph graph = null;        
        if (isDirected) {
            graph = graphModel.getHierarchicalDirectedGraphVisible();
        } else {
            graph = graphModel.getHierarchicalUndirectedGraphVisible();
        }
        execute(graph, attributeModel);
    }
    
  public void execute(HierarchicalGraph hgraph, AttributeModel am) {
        
        isCanceled = false;
        AttributeTable nodetable = am.getNodeTable();
        AttributeColumn betweennessCol = nodetable.getColumn(BETWEENNESS);
        //AttributeColumn bridginessMSPCol = nodetable.getColumn(BRIDGINESSMSP);
        AttributeColumn bridginessMSPCol3 = nodetable.getColumn(BRIDGINESSMSP3);
        AttributeColumn bridginessMSPCol4 = nodetable.getColumn(BRIDGINESSMSP4);
        AttributeColumn bridginessMSPCol5 = nodetable.getColumn(BRIDGINESSMSP5);
        AttributeColumn bridginessEXNCol = nodetable.getColumn(BRIDGINESSEXN);
        AttributeColumn nodeentropyCol = nodetable.getColumn(NODEENTROPY);
        AttributeColumn stirlingCol = nodetable.getColumn(STIRLING);
        
        if (betweennessCol == null) {
            betweennessCol = nodetable.addColumn(BETWEENNESS, "Betweenness", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }

        //if (bridginessMSPCol == null) {
        //    bridginessMSPCol = nodetable.addColumn(BRIDGINESSMSP, "BridginessMSP", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        //}
        if (bridginessMSPCol3 == null) {
            bridginessMSPCol3 = nodetable.addColumn(BRIDGINESSMSP3, "BridginessMSP3", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (bridginessMSPCol4 == null) {
            bridginessMSPCol4 = nodetable.addColumn(BRIDGINESSMSP4, "BridginessMSP4", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (bridginessMSPCol5 == null) {
            bridginessMSPCol5 = nodetable.addColumn(BRIDGINESSMSP5, "BridginessMSP5", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        
        if (bridginessEXNCol == null) {
            bridginessEXNCol = nodetable.addColumn(BRIDGINESSEXN, "BridginessEXN", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (nodeentropyCol == null) {
            nodeentropyCol = nodetable.addColumn(NODEENTROPY, "nodeentropy", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (stirlingCol == null) {
            stirlingCol = nodetable.addColumn(STIRLING, "nodestirling", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
 

        hgraph.readLock();
        
        N = hgraph.getNodeCount();
        
        betweenness = new double[N];
        //bridginessMSP = new double[N];
        bridginessMSP3 = new double[N];
        bridginessMSP4 = new double[N];
        bridginessMSP5 = new double[N];
        bridginessEXN = new double[N];
        
        nodeentropy = new double[N];
        stirling = new double[N];
        //shortestPaths = 0;
       
        //System.out.println("Params: minPathLength " + minPathLength);
        //System.out.println("Params: exclNeighbors " + exclNeighbors);

        HashMap<Node, Integer> indicies = new HashMap<Node, Integer>();
        int index = 0;
        for (Node s : hgraph.getNodes()) {
            indicies.put(s, index);
            index++;
        }
        
        Progress.start(progress, hgraph.getNodeCount());
        int count = 0;
        
        //entropy
        //count communities pop.
        HashMap<Integer, Integer> communityPop = new HashMap<Integer, Integer>();
        HashMap<Integer, LinkedList<Node>> communityNodes = new HashMap<Integer, LinkedList<Node>>();
                      
        for (Node s : hgraph.getNodes()) {
            Integer community = (Integer) s.getNodeData().getAttributes().getValue(MODULARITY_CLASS);
            if (communityPop.containsKey(community)) {
                Integer freq = communityPop.get(community);
                freq += 1;
                communityPop.put(community, freq);
                //insert member node in existing community                
                communityNodes.get(community).addLast(s);
            } else {
                communityPop.put(community, 1);
                //insert member node in not yet existing list
                LinkedList<Node> ll = new LinkedList<Node>();
                communityNodes.put(community, ll);
                communityNodes.get(community).addLast(s);
            }
        }
        
        Integer communityNum = communityPop.size();
        System.out.println(communityNum + " communities found.");
        
        Double communityDistance[][] = new Double[communityNum][communityNum];
        Double communityDistanceInverse[][] = new Double[communityNum][communityNum];
        
        //initialize distances;
        //WARNING: presumes communities #0, #1, #2...#N (no gaps)
        for (int sComm = 0; sComm < communityNum; sComm++) {
            for (int tComm = 0; tComm < communityNum; tComm++) {
                communityDistance[sComm][tComm] = Double.POSITIVE_INFINITY;   
                communityDistanceInverse[sComm][tComm] = 0.0;   
            }
        }
  
        
        for (Node s : hgraph.getNodes()) {
            Integer sComm = (Integer) s.getNodeData().getAttributes().getValue(MODULARITY_CLASS);
            for (Node t : hgraph.getNeighbors(s)) {
                Integer tComm = (Integer) t.getNodeData().getAttributes().getValue(MODULARITY_CLASS);
                //same community, not interesting

                if (sComm.intValue() == tComm.intValue()) {
                    continue;
                }
                
                double wl_st = (double) hgraph.getEdge(s, t).getWeight();
                System.out.println("sc " + sComm + " tc " + tComm + " wl_st " + wl_st);
                
                communityDistanceInverse[sComm][tComm] += wl_st;
            }
        }

        for (int sComm = 0; sComm < communityNum; sComm++) {
            for (int tComm = 0; tComm < communityNum; tComm++) {
                communityDistance[sComm][tComm] = 1/communityDistanceInverse[sComm][tComm];
                System.out.println("commdist: s " + sComm + " t " + tComm + " dist: " + communityDistance[sComm][tComm]);
            }
        }

        
        
        
        for (Node s : hgraph.getNodes()) { 
            
           Integer sCommunity = (Integer) s.getNodeData().getAttributes().getValue(MODULARITY_CLASS);
        
           //ignore nodes from small communities
           //if (communityPop.get(sCommunity) < 100) {
           //    continue;
           //}
                      
           HashMap<Integer, Double> weightsByCommunity = new HashMap<Integer, Double>();           
           Double nodeLocalWeights = 0d;
           Double nodeTotalWeights = 0d;
           
           for (Node t : hgraph.getNeighbors(s)) {
                if (s == t) {
                    continue;
                }
               
                Integer tCommunity = (Integer) t.getNodeData().getAttributes().getValue(MODULARITY_CLASS);                
                Double weight = (double) hgraph.getEdge(s, t).getWeight();

                //overall weights (for normalization)
                nodeTotalWeights += weight;
                
                //compute cumulated weights per community
                
                if (sCommunity.intValue() == tCommunity.intValue()) {
                    nodeLocalWeights += weight;
                } else 
                    if (weightsByCommunity.containsKey(tCommunity)) {
                        Double oldWBC = weightsByCommunity.get(tCommunity);
                        weightsByCommunity.put(tCommunity, oldWBC + weight);
                    } else {
                        weightsByCommunity.put(tCommunity, weight);
                    }
                }

           
                     
           Double nodeEntropy = 0.;
           for (Map.Entry<Integer, Double> entry : weightsByCommunity.entrySet()) {
                              
               Integer comm = entry.getKey();
               System.out.println("comm " + comm + " size " + communityPop.get(comm));
               if (communityPop.get(comm) < 0) {
                   continue;
               }

               Double commWeights = entry.getValue();
              
               //System.out.println("commweights " + commWeights + " nodelocalweights " + nodeLocalWeights);
               
               Double p_iJ = commWeights/(commWeights+nodeLocalWeights);
               
               //System.out.println("p_iJ " + p_iJ);
               
               nodeEntropy += (p_iJ) * (Double) log(p_iJ);
            }

          
           Double nodeStirling = 0.;
           
           Iterator comm1iter = weightsByCommunity.entrySet().iterator();
           while (comm1iter.hasNext()) {
           
               Map.Entry comm1entry = (Map.Entry) comm1iter.next();
               Integer comm1 = (Integer) comm1entry.getKey();
               
               Double p_iJ1= (Double) comm1entry.getValue();

               Iterator comm2iter = weightsByCommunity.entrySet().iterator();
               
                   Map.Entry comm2entry = (Map.Entry) comm2iter.next();
                   Integer comm2 = (Integer) comm2entry.getKey();
                            
                   if (comm1.intValue() == comm2.intValue()) {
                       continue;
                   }
                 
                   Double p_iJ2 = (Double) comm2entry.getValue();
                   Double d_j1j2 = (double) communityDistance[comm1.intValue()][comm2.intValue()];
                   
                   nodeStirling += (p_iJ1 * p_iJ2 * d_j1j2);
               }    

           AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
           row.setValue(nodeentropyCol, nodeEntropy);
           row.setValue(stirlingCol, nodeStirling);
        }

        
/*
        //Brandes 2001----    
        for (Node s : hgraph.getNodes()) {
            Stack<Node> S = new Stack<Node>();
            //System.out.println("###### s is "+s);

            LinkedList<Node>[] P = new LinkedList[N];
            double[] theta = new double[N];
            int[] d = new int[N];
            for (int j = 0; j < N; j++) {
                P[j] = new LinkedList<Node>();
                theta[j] = 0;
                d[j] = -1;
            }

            int s_index = indicies.get(s);

            theta[s_index] = 1;
            d[s_index] = 0;

            LinkedList<Node> Q = new LinkedList<Node>();
            Q.addLast(s);
            while (!Q.isEmpty()) {
                Node v = Q.removeFirst();
                S.push(v);
                int v_index = indicies.get(v);

                EdgeIterable edgeIter = null;
                if (isDirected) {
                    edgeIter = ((HierarchicalDirectedGraph) hgraph).getOutEdgesAndMetaOutEdges(v);
                } else {
                    edgeIter = hgraph.getEdgesAndMetaEdges(v);
                }

                for (Edge edge : edgeIter) {
                    Node reachable = hgraph.getOpposite(v, edge);

                    int r_index = indicies.get(reachable);
                    if (d[r_index] < 0) {
                        Q.addLast(reachable);
                        d[r_index] = d[v_index] + 1;
                    }
                    if (d[r_index] == (d[v_index] + 1)) {
                        theta[r_index] = theta[r_index] + theta[v_index];
                        P[r_index].addLast(v);
                    }
                }
            }
            double reachable = 0;
            for (int i = 0; i < N; i++) {
                if (d[i] > 0) {
                    reachable++;
                }
            }
 
            shortestPaths += reachable;

            double[] delta = new double[N];
            //double[] delta_bridge_MSP = new double[N];
            double[] delta_bridge_MSP3 = new double[N];
            double[] delta_bridge_MSP4 = new double[N];
            double[] delta_bridge_MSP5 = new double[N];
            
            double[] delta_bridge_EXN = new double[N];
            ///double delta_bridge_EXN_b = 0; //BEFORE, t-1
            
             while (!S.empty()) {
                Node w = S.pop();
                
                int w_index = indicies.get(w);
                ListIterator<Node> predecessorIter = P[w_index].listIterator();                

                while (predecessorIter.hasNext()) { //w to s
                    Node predecessor = predecessorIter.next();

                    
                    int predecessor_index = indicies.get(predecessor);
                    
                    
                    delta[predecessor_index] += 
                        (theta[predecessor_index] / theta[w_index]) *
                        (1 + delta[w_index]);
                    
                    //Filter out neighbors - from the beginning to the other end                    
                    delta_bridge_EXN[predecessor_index] += 
                            (theta[predecessor_index] / theta[w_index]) *
                            delta[w_index];
               
                    
                    //Filter out too short shortest paths                     
                    //if(d[w_index] >= minPathLength) {
                    //    delta_bridge_MSP[predecessor_index] += 
                    //            (theta[predecessor_index] / theta[w_index]) *
                    //            (1 + delta[w_index]);
                    //} else {
                    //    delta_bridge_MSP[predecessor_index] += 
                    //        (theta[predecessor_index] / theta[w_index]) *
                    //        delta[w_index];  
                    //}
                    
                    //Filter out too short shortest paths 3, 4 and 5                     
                    if(d[w_index] >= 3) {
                        delta_bridge_MSP3[predecessor_index] += 
                                (theta[predecessor_index] / theta[w_index]) *
                                (1 + delta[w_index]);
                    } else {
                        delta_bridge_MSP3[predecessor_index] += 
                            (theta[predecessor_index] / theta[w_index]) *
                            delta[w_index];
                        
                    }
                    //Filter out too short shortest paths                     
                    if(d[w_index] >= 4) {
                        delta_bridge_MSP4[predecessor_index] += 
                                (theta[predecessor_index] / theta[w_index]) *
                                (1 + delta[w_index]);
                    } else {
                        delta_bridge_MSP4[predecessor_index] += 
                            (theta[predecessor_index] / theta[w_index]) *
                            delta[w_index];
                        
                    }
                    //Filter out too short shortest paths                     
                    if(d[w_index] >= 5) {
                        delta_bridge_MSP5[predecessor_index] += 
                                (theta[predecessor_index] / theta[w_index]) *
                                (1 + delta[w_index]);
                    } else {
                        delta_bridge_MSP5[predecessor_index] += 
                            (theta[predecessor_index] / theta[w_index]) *
                            delta[w_index];
                        
                    }
                      //***
                }
                              
           
                if ( d[w_index] > 1 ) {
                    bridginessEXN[w_index] += delta_bridge_EXN[w_index];
                }  
                    
                if ( w != s ) {
                    betweenness[w_index] += delta[w_index];
                    //bridginessMSP[w_index] += delta_bridge_MSP[w_index];
                    bridginessMSP3[w_index] += delta_bridge_MSP3[w_index];
                    bridginessMSP4[w_index] += delta_bridge_MSP4[w_index];
                    bridginessMSP5[w_index] += delta_bridge_MSP5[w_index];
                }
       
             }
            
            count++;
            if (isCanceled) {
                hgraph.readUnlockAll();
                return;
            }
            Progress.progress(progress, count);
        }

        //avgDist /= shortestPaths;//mN * (mN - 1.0f);

        for (Node s : hgraph.getNodes()) {
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
            int s_index = indicies.get(s);

            if (!isDirected) {             
                betweenness[s_index] /= 2;
                //bridginessMSP[s_index] /= 2;
                bridginessMSP3[s_index] /= 2;
                bridginessMSP4[s_index] /= 2;
                bridginessMSP5[s_index] /= 2;
                bridginessEXN[s_index] /= 2;
            }
            //if (isNormalized) {
            //    bridginess[s_index] /= isDirected ? (N - 1) * (N - 2) : (N - 1) * (N - 2) / 2;
            //}
            row.setValue(betweennessCol, betweenness[s_index]);  
            //row.setValue(bridginessMSPCol, bridginessMSP[s_index]);   
            row.setValue(bridginessMSPCol3, bridginessMSP3[s_index]);   
            row.setValue(bridginessMSPCol4, bridginessMSP4[s_index]);   
            row.setValue(bridginessMSPCol5, bridginessMSP5[s_index]);   
            row.setValue(bridginessEXNCol, bridginessEXN[s_index]);   
            //row.setValue(nodeentropyCol, nodeentropy[s_index]);   
        } 
*/
        hgraph.readUnlock();                
    }
  
       
   
    @Override
    public boolean cancel() {
        this.isCanceled = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
        this.progress = pt;
    }

    private String createImageFile(TempDir tempDir, double[] pVals, String pName, String pX, String pY) {
     //distribution of values
     Map<Double, Integer> dist = new HashMap<Double, Integer>();
     for (int i = 0; i < N; i++) {
         Double d = pVals[i];
         if (dist.containsKey(d)) {
             Integer v = dist.get(d);
             dist.put(d, v + 1);
         } else {
             dist.put(d, 1);
         }
     }

     //Distribution series
     XYSeries dSeries = ChartUtils.createXYSeries(dist, pName);

     XYSeriesCollection dataset = new XYSeriesCollection();
     dataset.addSeries(dSeries);

     JFreeChart chart = ChartFactory.createXYLineChart(
             pName,
             pX,
             pY,
             dataset,
             PlotOrientation.VERTICAL,
             true,
             false,
             false);
     chart.removeLegend();
     ChartUtils.decorateChart(chart);
     ChartUtils.scaleChart(chart, dSeries, FALSE);//isNormalized);
     return ChartUtils.renderChart(chart, pName + ".png");
    }

    
    @Override
    public String getReport() {
        //String htmlIMG1 = "";
        String htmlIMG13 = "";
        String htmlIMG14 = "";
        String htmlIMG15 = "";
        String htmlIMG2 = "";
        try {
            TempDir tempDir = TempDirUtils.createTempDir();
            //htmlIMG1 = createImageFile(tempDir, bridginessMSP, "MSP Bridginess Distribution", "Value", "Count");
            htmlIMG13 = createImageFile(tempDir, bridginessMSP3, "MSP3 Bridginess Distribution", "Value", "Count");
            htmlIMG14 = createImageFile(tempDir, bridginessMSP4, "MSP4 Bridginess Distribution", "Value", "Count");
            htmlIMG15 = createImageFile(tempDir, bridginessMSP5, "MSP5 Bridginess Distribution", "Value", "Count");
            htmlIMG2 = createImageFile(tempDir, bridginessEXN, "EXN Bridginess Distribution", "Value", "Count");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        String report = "<HTML> <BODY> <h1>Graph Distance  Report </h1> "
                + "<hr>"
                + "<br>"
                + "<h2> Parameters: </h2>"
                + "Network Interpretation:  " + (isDirected ? "directed" : "undirected") + "<br />"
                + "<br /> <h2> Results: </h2>"
                //+ "Diameter: " + diameter + "<br />"
                //+ "Radius: " + radius + "<br />"
                //+ "Average Path length: " + avgDist + "<br />"
                //+ "Number of shortest paths: " + shortestPaths + "<br /><br />"
                //+ htmlIMG1 + "<br /><br />"
                + htmlIMG13 + "<br /><br />"
                + htmlIMG14 + "<br /><br />"
                + htmlIMG15 + "<br /><br />"
                + htmlIMG2 
                + "<br /><br />" + "<h2> Algorithm: </h2>"
                + "Ulrik Brandes, <i>A Faster Algorithm for Betweenness Centrality</i>, in Journal of Mathematical Sociology 25(2):163-177, (2001)<br />"
                + "Extended by Pablo Jensen (IXXI), Mathieu Jacomy (SciencesPO Medialab), Matteo Morini (IXXI)"
                + "</BODY> </HTML>";

        return report;
    }

    public void setMinPathLength(int minPathLength) {
        this.minPathLength = minPathLength;
    }
    
    public int getMinPathLength() {
        return minPathLength;
    }
    
    public void setExclNeighbors(int exclNeighbors) {
        this.exclNeighbors = exclNeighbors;
    }
    
    public int getExclNeighbors() {
        return exclNeighbors;
    }

    public double getAvgBridginess() {
        return avgBridginess;
    }
    
}
