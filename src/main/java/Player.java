import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    final int SEED_BASE_COST = 0;
    static final int[] GROW_BASE_COST = new int[] {1, 3, 7};
    static final int[] RICHNESS_BONUS = new int[] {0, 0, 2, 4};
    static final int LAST_DAY = 23;

    final static Random rand = new Random(47);

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Cell[] cells = new Cell[in.nextInt()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Cell(in.nextInt(), in.nextInt(),
                    in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());
        }

        final State state = new State(cells);

        // game loop
        while (true) {
            state.update(in.nextInt(), in.nextInt(),
                    in.nextInt(), in.nextInt(),
                    in.nextInt(), in.nextInt(),
                    in.nextInt() != 0
            );
//            int day = in.nextInt(); // the game lasts 24 days: 0-23
//            int nutrients = in.nextInt(); // the base score you gain from the next COMPLETE action
//            int sun = in.nextInt(); // your sun points
//            int score = in.nextInt(); // your current score
//            int oppSun = in.nextInt(); // opponent's sun points
//            int oppScore = in.nextInt(); // opponent's score
//            boolean oppIsWaiting = in.nextInt() != 0; // whether your opponent is asleep until the next day

            int numberOfTrees = in.nextInt(); // the current amount of trees
            for (int i = 0; i < numberOfTrees; i++) {
                state.addTree(in.nextInt(), in.nextInt(), in.nextInt() != 0, in.nextInt() != 0);
            }

            String[] legalActions = new String[in.nextInt()];

            if (in.hasNextLine()) {
                in.nextLine();
            }

            System.err.println(state.myBot.getFinalScore());
            System.err.println(state.oppBot.getFinalScore());

            Map<String, List<Integer>> groupedActions = new HashMap<>();
            for (int i = 0; i < legalActions.length; i++) {
                legalActions[i] = in.nextLine(); // try printing something from here to start with
                String[] actionParts = legalActions[i].split(" ");
                if (!"WAIT".equals(actionParts[0])) {
                    groupedActions.computeIfAbsent(actionParts[0], s -> new ArrayList<>())
                            .add(Integer.parseInt(actionParts[1]));
                }
            }

            System.err.println(groupedActions);

            state.print();

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            System.out.println(legalActions[rand.nextInt(legalActions.length)]);
        }
    }

    static class Cell {
        final int index, richness;
        final int[] neighbors = new int[6];

        Cell(int index, int richness, int... neighbors) {
            this.index = index;
            this.richness = richness;
            System.arraycopy(neighbors, 0, this.neighbors, 0, 6);
        }
    }

    static class State {
        final Cell[] cells;
        int day, sunDir, nutrients;

        final Bot myBot = new Bot(true);
        final Bot oppBot = new Bot(false);

        final Map<Integer, Tree> treeMap = new HashMap<>();

        State(Cell[] cells) {
            this.cells = cells;
        }

        // clear tree map!!!
        void update(int day, int nutrients,
                    int sun, int score,
                    int oppSun, int oppScore,
                    boolean oppIsWaiting
        ) {
            this.day = day;
            this.sunDir = day % 6;
            this.nutrients = nutrients;

            myBot.update(sun, score);
            oppBot.update(oppSun, oppScore, oppIsWaiting);

            treeMap.clear();
        }

        void addTree(int cellIndex, int size, boolean isMine, boolean isDormant) {
            (isMine ? myBot : oppBot).addTree(cellIndex, size, isDormant);
        }

        Cell neighbor(int cell, int dir) {
            return cells[cells[cell].neighbors[dir]];
        }

        void print() {
            System.err.println("score = " + myBot.score);
            System.err.println("sun points = " + myBot.sun);
            int[] treeCounts = new int[4];
            for (Tree tree : myBot.trees) {
                treeCounts[tree.size]++;
            }
            System.err.println("seeds = " + treeCounts[0]);
            System.err.println("small trees = " + treeCounts[1]);
            System.err.println("medium trees = " + treeCounts[2]);
            System.err.println("large trees = " + treeCounts[3]);
        }

        class Tree {
            Cell cell;
            int size;
            boolean isMine, isDormant;

            Tree(int cellIndex, int size, boolean isMine, boolean isDormant) {
                this.cell = cells[cellIndex];
                this.size = size;
                this.isMine = isMine;
                this.isDormant = isDormant;
            }
        }

        class Bot {
            final boolean isMine;
            int sun, score;
            boolean isWaiting;
            List<Tree> trees = new ArrayList<>();

            Bot(boolean isMine) {
                this.isMine = isMine;
            }

            // clear trees!!!
            void update(int sun, int score) {
                this.sun = sun;
                this.score = score;
                trees.clear();
            }

            void update(int sun, int score, boolean isWaiting) {
                update(sun, score);
                this.isWaiting = isWaiting;
            }

            void addTree(int cellIndex, int size, boolean isDormant) {
                Tree tree = new Tree(cellIndex, size, isMine, isDormant);
                trees.add(tree);
                treeMap.put(cellIndex, tree);
            }

            float getFinalScore() {
                int sunScore = sun / 3;
                return score + sunScore + 0.01f * trees.size();
            }
        }
    }
}