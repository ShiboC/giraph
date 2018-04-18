/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph;

import org.apache.giraph.conf.LongConfOption;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.BasicComputation;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.worker.WorkerContext;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.NullWritable;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the basic Pregel shortest paths implementation.
 */
@Algorithm(
        name = "Shortest paths",
        description = "Finds all shortest paths from a selected vertex"
)
public class SimpleShortestPathsComputationEdge extends BasicComputation<
        IntWritable, DoubleWritable, NullWritable, DoubleWritable> {
    /**
     * The shortest paths id
     */
    public static final LongConfOption SOURCE_ID =
            new LongConfOption("SimpleShortestPathsVertex.sourceId", 1,
                    "The shortest paths id");
    /**
     * Class logger
     */
    private static final Logger LOG =
            Logger.getLogger(SimpleShortestPathsComputationEdge.class);

    /**
     * Is this vertex the source id?
     *
     * @param vertex Vertex
     * @return True if the source id
     */
    private boolean isSource(Vertex<IntWritable, ?, ?> vertex) {
        return vertex.getId().get() == SOURCE_ID.get(getConf());

    }

    @Override
    public void compute(
            Vertex<IntWritable, DoubleWritable, NullWritable> vertex,
            Iterable<DoubleWritable> messages) throws IOException {
        if (getSuperstep() == 0) {
            vertex.setValue(new DoubleWritable(Double.MAX_VALUE));
        }
        WorkerContext wc = getWorkerContext();
        //set superstep to kill
//        ArrayList<Long> superstepToKillList=new ArrayList<Long>();
        String[] superstepToKillString = getConf().getSuperstepToKill().split(",");
        if (getSuperstep() == 0) {
            for (int i = 0; i < superstepToKillString.length; i++) {
                wc.superstepToKillSet.add(Long.parseLong(superstepToKillString[i]));
            }
        }

        //set workerindex to kill
        ArrayList<Integer> workerindexToKillList = new ArrayList<Integer>();
//        System.out.println("wc stkset:"+wc.superstepToKillSet.toString());
//        if (wc.superstepToKillSet.contains(-1l)) {
//
//            wc.superstepToKillSet.remove(-1l);
//        }

        String[] workerindexToKillString = getConf().getWorkerindexToKill().split(",");

        for (int i = 0; i < workerindexToKillString.length; i++) {
            workerindexToKillList.add(Integer.parseInt(workerindexToKillString[i]));
        }
//        System.out.println( "kill step set before:"+wc.superstepToKillSet);
//        System.out.println( "kill index set before:"+workerindexToKillList.toString());
//
//        System.out.println("wc:"+wc.getMyWorkerIndex() + ";" +wc.getSuperstep());
//        System.out.println("attemp id .id: "+getContext().getTaskAttemptID()+";"+getContext().getTaskAttemptID().getId());
        if (wc.superstepToKillSet.contains(wc.getSuperstep()) && workerindexToKillList.contains(wc.getMyWorkerIndex())) {
            wc.superstepToKillSet.remove(wc.getSuperstep());

            System.exit(-1);
        }
//        System.out.println( "killset after:"+wc.superstepToKillSet);

//        long rs=wc.getRestartSuperstep();
//        System.out.println("restartsuperstep:"+rs);
        double minDist = isSource(vertex) ? 0d : Double.MAX_VALUE;
        for (DoubleWritable message : messages) {
            minDist = Math.min(minDist, message.get());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Vertex " + vertex.getId() + " got minDist = " + minDist +
                    " vertex value = " + vertex.getValue());
        }
        if (minDist < vertex.getValue().get()) {
            vertex.setValue(new DoubleWritable(minDist));
            for (Edge<IntWritable, NullWritable> edge : vertex.getEdges()) {
                double distance = minDist + 1;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Vertex " + vertex.getId() + " sent to " +
                            edge.getTargetVertexId() + " = " + distance);
                }
                sendMessage(edge.getTargetVertexId(), new DoubleWritable(distance));
            }
        }
        vertex.voteToHalt();
    }
}