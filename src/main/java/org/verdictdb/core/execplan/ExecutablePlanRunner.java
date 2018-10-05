/*
 *    Copyright 2018 University of Michigan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.verdictdb.core.execplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.verdictdb.commons.VerdictDBLogger;
import org.verdictdb.connection.DbmsConnection;
import org.verdictdb.core.querying.ExecutableNodeBase;
import org.verdictdb.core.resulthandler.ExecutionResultReader;
import org.verdictdb.core.resulthandler.ExecutionTokenReader;
import org.verdictdb.exception.VerdictDBException;

public class ExecutablePlanRunner {

  private DbmsConnection conn;

  private ExecutablePlan plan;

//  private int nThreads = 1;
  
  private VerdictDBLogger log = VerdictDBLogger.getLogger(this.getClass());
  
  private List<ExecutableNodeRunner> nodeRunners = new ArrayList<>();
  
//  private Map<Integer, ExecutorService> executorPool = new HashMap<>();

  public ExecutablePlanRunner(DbmsConnection conn, ExecutablePlan plan) {
    this.conn = conn;
    this.plan = plan;
  }

  public static ExecutionTokenReader getTokenReader(DbmsConnection conn, ExecutablePlan plan) {
    return (new ExecutablePlanRunner(conn, plan)).getTokenReader();
  }

  public static ExecutionResultReader getResultReader(DbmsConnection conn, ExecutablePlan plan) {
    return (new ExecutablePlanRunner(conn, plan)).getResultReader();
  }

  public static void runTillEnd(DbmsConnection conn, ExecutablePlan plan)
      throws VerdictDBException {
    ExecutionTokenReader reader = (new ExecutablePlanRunner(conn, plan)).getTokenReader();
    while (true) {
      ExecutionInfoToken token = reader.next();
//      System.out.println("runTillEnd: " + token);
      if (token == null) {
        break;
      }
    }
  }

  public ExecutionTokenReader getTokenReader() {
    // set up to get the results
    ExecutionTokenReader reader;
    if (plan.getReportingNode() != null) {
      ExecutableNodeBase node = new ExecutableNodeBase(-1);
      node.subscribeTo((ExecutableNodeBase) plan.getReportingNode());
      reader = new ExecutionTokenReader(node.getSourceQueues().get(0));
    } else {
      reader = new ExecutionTokenReader();
    }
    
    // Run nodes: approach 1
    // The nodes whose dependencies have not been finished will terminate immediately.
    // The leaf nodes will signal its parents (also called subscribers) to make them run.
    // The signal will be propagated throughout the tree as the nodes are completed.
//    Set<Integer> groupIds = plan.getNodeGroupIDs();
//    for (int gid : groupIds) {
//      List<ExecutableNode> nodes = plan.getNodesInGroup(gid);
//      for (ExecutableNode n : nodes) {
//        ExecutableNodeRunner nodeRunner = new ExecutableNodeRunner(conn, n);
//        nodeRunner.runOnThread();
////        nodeRunner.run();
//        nodeRunners.add(nodeRunner);
//      }
//    }
    
    // Run nodes: approach 2
    // A parent node will run its children
    // The parent node terminates when all of its children indicate completion.
    
    // first, assign node runner for all nodes.
    Set<Integer> groupIds = plan.getNodeGroupIDs();
    for (int gid : groupIds) {
      List<ExecutableNode> nodes = plan.getNodesInGroup(gid);
      for (ExecutableNode n : nodes) {
        nodeRunners.add(new ExecutableNodeRunner(conn, n));
      }
    }
    
    // second, run those nodes
    ExecutableNodeBase root = (ExecutableNodeBase) plan.getReportingNode();
    ExecutableNodeRunner nodeRunner = root.getRegisteredRunner();
    nodeRunner.runThisAndDependents();
//    nodeRunners.add(nodeRunner);

    return reader;
  }

  public ExecutionResultReader getResultReader() {
    ExecutionTokenReader reader = getTokenReader();
    return new ExecutionResultReader(reader);
  }
  
  /**
   * Kill all currently running threads.
   */
  public void abort() {
    // for approach 1: setting the flag first helps properly catching exceptions.
    for (ExecutableNodeRunner nodeRunner : nodeRunners) {
      nodeRunner.setAborted();
    }
    for (ExecutableNodeRunner nodeRunner : nodeRunners) {
      nodeRunner.abort();
    }
  }

}
