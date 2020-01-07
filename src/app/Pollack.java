package app;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import game.HuntState;
import game.Hunter;
import game.Node;
import game.NodeStatus;
import game.ScramState;

/** A solution with huntOrb optimized and scram getting out as fast as possible. */
public class Pollack extends Hunter {

    HashSet<Long> visited= new HashSet<>();
    Boolean orbHunted= false;

    /** Get to the orb in as few steps as possible. <br>
     * Once you get there, you must return from the function in order to pick it up. <br>
     * If you continue to move after finding the orb rather than returning, it will not count.<br>
     * If you return from this function while not standing on top of the orb, it will count as a
     * failure.
     *
     * There is no limit to how many steps you can take, but you will receive<br>
     * a score bonus multiplier for finding the orb in fewer steps.
     *
     * At every step, you know only your current tile's ID and the ID of all<br>
     * open neighbor tiles, as well as the distance to the orb at each of <br>
     * these tiles (ignoring walls and obstacles).
     *
     * In order to get information about the current state, use functions<br>
     * currentLocation(), neighbors(), and distanceToOrb() in HuntState.<br>
     * You know you are standing on the orb when distanceToOrb() is 0.
     *
     * Use function moveTo(long id) in HuntState to move to a neighboring<br>
     * tile by its ID. Doing this will change state to reflect your new position.
     *
     * A suggested first implementation that will always find the orb, but <br>
     * likely won't receive a large bonus multiplier, is a depth-first search. <br>
     * Some modification is necessary to make the search better, in general. */
    @Override
    public void huntOrb(HuntState state) {
        // TODO 1: Get the orb
        dfsWalk(state);
    }

    /** Recursively does a dfs walk to find the orb Chooses the neighbor with smallest distance to
     * orb first
     *
     * @param state: the current HuntState */
    public void dfsWalk(HuntState state) {
        if (state.distanceToOrb() == 0) {
            orbHunted= true;
        }
        if (orbHunted) { return; }
        Long uId= state.currentLocation();
        visited.add(uId);
        Heap<NodeStatus> neighborHeap= sortNeighbors(state.neighbors());
        while (neighborHeap.size > 0) {
            Long wId= neighborHeap.poll().getId();
            if (!visited.contains(wId) && !orbHunted) {
                state.moveTo(wId);
                dfsWalk(state);
                if (!orbHunted) { state.moveTo(uId); }
            }
        }
    }

    /** Sorts the neighbors of a given node based on distance to orb sorts the neighbors from least
     * distance to greatest distance
     *
     * @param neighbors: neighbors of the current node
     * @return A max heap with NodeStatus values and distance to orb priorities */
    public Heap<NodeStatus> sortNeighbors(Collection<NodeStatus> neighbors) {
        Heap<NodeStatus> neighborsHeap= new Heap<>(false);
        for (NodeStatus n : neighbors) {
            if (!visited.contains(n.getId())) {
                neighborsHeap.add(n, n.getDistanceToTarget());
            }
        }
        return neighborsHeap;
    }

    /** Get out the cavern before the ceiling collapses, trying to collect as <br>
     * much gold as possible along the way. Your solution must ALWAYS get out <br>
     * before time runs out, and this should be prioritized above collecting gold.
     *
     * You now have access to the entire underlying graph, which can be accessed <br>
     * through ScramState. <br>
     * currentNode() and getExit() will return Node objects of interest, and <br>
     * getNodes() will return a collection of all nodes on the graph.
     *
     * Note that the cavern will collapse in the number of steps given by <br>
     * getStepsRemaining(), and for each step this number is decremented by the <br>
     * weight of the edge taken. <br>
     * Use getStepsRemaining() to get the time still remaining, <br>
     * pickUpGold() to pick up any gold on your current tile <br>
     * (this will fail if no such gold exists), and <br>
     * moveTo() to move to a destination node adjacent to your current node.
     *
     * You must return from this function while standing at the exit. <br>
     * Failing to do so before time runs out or returning from the wrong <br>
     * location will be considered a failed run.
     *
     * You will always have enough time to scram using the shortest path from the <br>
     * starting position to the exit, although this will not collect much gold. <br>
     * For this reason, using Dijkstra's to plot the shortest path to the exit <br>
     * is a good starting solution */
    @Override
    public void scram(ScramState state) {
        // TODO 2: Get out of the cavern before it collapses, picking up gold along the way
        scramHelper(state);
    }

    /** @param state the current scram state
     *
     *              performs the operations specified by scram */
    public void scramHelper(ScramState state) {
        if (state.currentNode().equals(state.getExit())) return;
        Collection<Node> allNodes= state.allNodes();
        List<Node> outPath= new LinkedList<>();
        List<Node> exitPath= new LinkedList<>();
        Double bestScore= 0.0;
        // find node with best SP score and find SP
        for (Node n : allNodes) {
            List<Node> nPath= Path.shortest(state.currentNode(), n);
            List<Node> nExitPath= Path.shortest(n, state.getExit());
            Double nGold= 0.0;
            for (Node w : nPath) {
                nGold+= w.getTile().gold();
            }
            Integer nLength= Path.pathSum(nPath);
            Double score= nGold / (Double.valueOf(nLength) + 1);
            Boolean canReturn= Path.pathSum(nExitPath) + nLength <= state.stepsLeft();
            if (score >= bestScore && canReturn && !n.equals(state.getExit())) {
                bestScore= score;
                outPath= nPath;
                exitPath= nExitPath;
            }
        }
        // move to next Node unless none can return in which case exit
        if (outPath.size() <= 1) {
            for (Node nextExitNode : exitPath) {
                if (state.currentNode().getNeighbors().contains(nextExitNode)) {
                    state.moveTo(nextExitNode);
                }
            }
        } else {
            Integer origStepsLeft= state.stepsLeft();
            Integer detourLength= 0;
            for (Node nextNode : outPath) {
                if (state.currentNode().getNeighbors().contains(nextNode)) {
                    for (Node neighbor : state.currentNode().getNeighbors()) {
                        Integer edgeLength= neighbor.getEdge(state.currentNode()).length();
                        if (neighbor.getTile().gold() / (2 * edgeLength) > bestScore &&
                            Path.pathSum(exitPath) + Path.pathSum(outPath) + 2 * edgeLength +
                                detourLength < origStepsLeft &&
                            !outPath.contains(neighbor) && !exitPath.contains(neighbor)) {
                            Node origNode= state.currentNode();
                            state.moveTo(neighbor);
                            detourLength+= 2 * edgeLength;
                            state.moveTo(origNode);
                        }
                    }
                    state.moveTo(nextNode);
                }
            }
            scramHelper(state);
        }
    }
}
