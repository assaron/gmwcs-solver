import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.ifmo.ctddev.gmwcs.graph.Edge;
import ru.ifmo.ctddev.gmwcs.graph.Graph;
import ru.ifmo.ctddev.gmwcs.graph.Node;
import ru.ifmo.ctddev.gmwcs.graph.Unit;
import ru.ifmo.ctddev.gmwcs.solver.BicomponentSolver;
import ru.ifmo.ctddev.gmwcs.solver.RLTSolver;
import ru.ifmo.ctddev.gmwcs.solver.SolverException;
import ru.ifmo.ctddev.gmwcs.solver.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static ru.ifmo.ctddev.gmwcs.solver.Utils.sum;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GMWCSTests {
    public static final int SEED = 20160309;
    public static final int TESTS_PER_SIZE = 300;
    public static final int MAX_SIZE = 16;
    public static final int RANDOM_TESTS = 2200;
    public static final Integer DEBUG_TEST = null;
    private List<TestCase> tests;
    private BicomponentSolver solver;
    private ReferenceSolver referenceSolver;
    private Random random;

    public GMWCSTests() {
        random = new Random(SEED);
        this.solver = new BicomponentSolver(new RLTSolver());
        tests = new ArrayList<>();
        referenceSolver = new ReferenceSolver();
        if (System.getProperty("skipTests") != null) {
            System.exit(0);
        }
        makeConnectedGraphs();
        makeUnconnectedGraphs();
    }

    @Test
    public void test01_empty() throws SolverException {
        if (DEBUG_TEST != null) {
            return;
        }
        Graph graph = new Graph();
        List<Unit> res = solver.solve(graph);
        if (!(res == null || res.isEmpty())) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void test02_connected() {
        int allTests = MAX_SIZE * TESTS_PER_SIZE;
        if (DEBUG_TEST != null) {
            if (DEBUG_TEST < allTests) {
                check(tests.get(DEBUG_TEST), DEBUG_TEST);
            } else {
                return;
            }
        } else {
            for (int i = 0; i < allTests; i++) {
                TestCase test = tests.get(i);
                System.out.print("\rTest(connected) no. " + (i + 1) + "/" + tests.size());
                System.out.print(": n = " + test.n() + ", m = " + test.m() + "       ");
                System.out.flush();
                check(test, i);
            }
        }
        System.out.println();
    }

    @Test
    public void test03_random() {
        int allTests = MAX_SIZE * TESTS_PER_SIZE;
        if (DEBUG_TEST != null) {
            if (DEBUG_TEST < allTests) {
                return;
            } else {
                check(tests.get(DEBUG_TEST), DEBUG_TEST);
            }
        } else {
            for (int i = allTests; i < tests.size(); i++) {
                TestCase test = tests.get(i);
                System.out.print("\rTest(random) no. " + (i) + "/" + tests.size());
                System.out.print(": n = " + test.n() + ", m = " + test.m() + "       ");
                System.out.flush();
                check(test, i);
            }
        }
        System.out.println();
    }

    private void check(TestCase test, int num) {
        List<Unit> expected = referenceSolver.solve(test.graph(), Collections.emptyList());
        List<Unit> actual = null;
        try {
            solver.suppressOutput();
            actual = solver.solve(test.graph());
        } catch (SolverException e) {
            System.out.println();
            Assert.assertTrue(num + "\n" + e.getMessage(), false);
        } catch (UnsatisfiedLinkError e) {
            System.err.println();
            System.err.println("java.library.path must point to the directory containing the CPLEX shared library\n" +
                    "try invoking java with java -Djava.library.path=...");
            System.exit(1);
        }
        try {
            if (Math.abs(sum(expected) - sum(actual)) > 0.1) {
                System.err.println("Expected: " + sum(expected) + ", but actual: "
                        + sum(actual));
                Utils.toXdot(test.graph(), expected, actual);
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private void makeConnectedGraphs() {
        for (int size = 1; size <= MAX_SIZE; size++) {
            List<Integer> edgesCount = new ArrayList<>();
            for (int i = 0; i < TESTS_PER_SIZE; i++) {
                if (size == 1) {
                    edgesCount.add(0);
                } else {
                    int upper = Math.min((size * (size - 1)) / 2 + 1, MAX_SIZE);
                    upper -= size - 1;
                    edgesCount.add(random.nextInt(upper));
                }
            }
            Collections.sort(edgesCount);
            for (int count : edgesCount) {
                Graph graph = new Graph();
                Node[] nodes = fillNodes(graph, size);
                List<Integer> seq = new ArrayList<>();
                for (int j = 0; j < size; j++) {
                    seq.add(j);
                }
                Collections.shuffle(seq, random);
                for (int j = 0; j < size - 1; j++) {
                    graph.addEdge(nodes[seq.get(j)], nodes[seq.get(j + 1)], new Edge(j + 1, random.nextInt(16) - 8));
                }
                fillEdgesRandomly(graph, count, nodes, size);
                tests.add(new TestCase(graph));
            }
        }
    }

    private void makeUnconnectedGraphs() {
        for (int i = 0; i < RANDOM_TESTS; i++) {
            int n = random.nextInt(MAX_SIZE) + 1;
            int m = Math.min((n * (n - 1)) / 2, random.nextInt(MAX_SIZE));
            Graph graph = new Graph();
            Node[] nodes = fillNodes(graph, n);
            fillEdgesRandomly(graph, m, nodes, 1);
            tests.add(new TestCase(graph));
        }
    }

    private Node[] fillNodes(Graph graph, int size) {
        Node[] nodes = new Node[size];
        for (int j = 0; j < size; j++) {
            nodes[j] = new Node(j + 1, random.nextInt(16) - 8);
            graph.addVertex(nodes[j]);
        }
        return nodes;
    }

    private void fillEdgesRandomly(Graph graph, int count, Node[] nodes, int offset) {
        int size = graph.vertexSet().size();
        for (int j = 0; j < count; j++) {
            int u = random.nextInt(size);
            int v = random.nextInt(size);
            if (u == v || graph.getEdge(nodes[v], nodes[u]) != null) {
                j--;
                continue;
            }
            graph.addEdge(nodes[u], nodes[v], new Edge(offset + j, random.nextInt(16) - 8));
        }
    }
}