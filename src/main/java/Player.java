import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    static final int SEED_BASE_COST = 0;
    static final int[] GROW_BASE_COST = new int[] { 1, 3, 7 };
    static final int COMPLETE_BASE_COST = 4;
    static final int[] RICHNESS_BONUS = new int[] { 0, 0, 2, 4 };
    static final int LAST_DAY = 23;

    static final int SEED = 0, SMALL = 1, MEDIUM = 2, LARGE = 3;

    final static Random rand = new Random(47);

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Cell[] cells = new Cell[in.nextInt()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Cell(in.nextInt(), in.nextInt(),
                    in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());
        }

        Cell.fillRings(cells);

        final State state = new State(cells);

        // game loop
        while (true) {
            state.update(in.nextInt(), in.nextInt(),
                    in.nextInt(), in.nextInt(),
                    in.nextInt(), in.nextInt(),
                    in.nextInt() != 0
            );

            int numberOfTrees = in.nextInt(); // the current amount of trees
            for (int i = 0; i < numberOfTrees; i++) {
                state.addTree(in.nextInt(), in.nextInt(), in.nextInt() != 0, in.nextInt() != 0);
            }

            String[] legalActions = new String[in.nextInt()];

            if (in.hasNextLine()) {
                in.nextLine();
            }

            for (int i = 0; i < legalActions.length; i++) {
                legalActions[i] = in.nextLine(); // try printing something from here to start with
            }

            System.err.println("Initial state -->");
            state.print();

            state.save();
            for (String action : legalActions) {
                state.restore();
                System.err.println("After action " + action + " -->");
                state.applyAction(action);
                state.print();
            }

            System.out.println(legalActions[rand.nextInt(legalActions.length)]);
        }
    }

    static class Cell {
        final int index, richness;
        final int[] neighbors = new int[6];

        final Set<Cell> firstRing = new HashSet<>();
        final Set<Cell> secondRing = new HashSet<>();
        final Set<Cell> secondCircle = new HashSet<>();
        final Set<Cell> thirdRing = new HashSet<>();
        final Set<Cell> thirdCircle = new HashSet<>();

        Cell(int index, int richness, int... neighbors) {
            this.index = index;
            this.richness = richness;
            System.arraycopy(neighbors, 0, this.neighbors, 0, 6);
        }

        static void fillRings(Cell[] cells) {
            for (Cell cell : cells) {
                for (int neighborIndex : cell.neighbors) {
                    if (neighborIndex > -1)
                        cell.firstRing.add(cells[neighborIndex]);
                }

                for (Cell neighbor : cell.firstRing) {
                    for (int neighborIndex : neighbor.neighbors) {
                        if (neighborIndex > -1) {
                            Cell neighbor2 = cells[neighborIndex];
                            if (neighbor2 != cell && !cell.firstRing.contains(neighbor2))
                                cell.secondRing.add(neighbor2);
                        }
                    }
                }

                cell.secondCircle.addAll(cell.firstRing);
                cell.secondCircle.addAll(cell.secondRing);

                for (Cell neighbor : cell.secondRing) {
                    for (int neighborIndex : neighbor.neighbors) {
                        if (neighborIndex > -1) {
                            Cell neighbor3 = cells[neighborIndex];
                            if (!cell.secondCircle.contains(neighbor3))
                                cell.thirdRing.add(neighbor3);
                        }
                    }
                }

                cell.thirdCircle.addAll(cell.secondCircle);
                cell.thirdCircle.addAll(cell.thirdRing);
            }
        }

        static Cell neighbor(Cell[] cells, int cell, int dir) {
            int neighborIndex = cells[cell].neighbors[dir];
            return neighborIndex > -1 ? cells[neighborIndex] : null;
        }

        @Override
        public String toString() {
            return "cell#" + index;
        }
    }

    static class State {
        final Cell[] cells;
        int day, sunDir, nutrients;

        Bot myBot = new Bot(true);
        Bot oppBot = new Bot(false);

        final Map<Integer, Tree> treeMap = new HashMap<>(37);

        // backup
        int dayB, sunDirB, nutrientsB;
        Bot myBotB, oppBotB;
        final Map<Integer, Tree> treeMapB = new HashMap<>(37);

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

        void save() {
            dayB = day;
            sunDirB = sunDir;
            nutrientsB = nutrients;
            myBotB = myBot.copy();
            oppBotB = oppBot.copy();
            treeMapB.clear();
            fillTreeMap(treeMapB, myBotB, oppBotB);
        }

        void restore() {
            day = dayB;
            sunDir = sunDirB;
            nutrients = nutrientsB;
            myBot = myBotB.copy();
            oppBot = oppBotB.copy();
            treeMap.clear();
            fillTreeMap(treeMap, myBot, oppBot);
        }

        void applyAction(String action) {
            String[] actParts = action.split(" ");
            switch (actParts[0]) {
                case "WAIT":
                    myBot.isWaiting = true;
                    break;
                case "SEED":
                    Tree tree = treeMap.get(Integer.parseInt(actParts[1]));
                    tree.seed(Integer.parseInt(actParts[2]));
                    break;
                case "GROW":
                    treeMap.get(Integer.parseInt(actParts[1])).grow();
                    break;
                case "COMPLETE":
                    treeMap.get(Integer.parseInt(actParts[1])).complete();
                    break;
            }
        }

        void addTree(int cellIndex, int size, boolean isMine, boolean isDormant) {
            (isMine ? myBot : oppBot).addTree(cellIndex, size, isDormant);
        }

        Cell neighbor(int cell, int dir) {
            return Cell.neighbor(cells, cell, dir);
        }

        void print() {
            myBot.print();
//            oppBot.print();
        }

        private void fillTreeMap(Map<Integer, Tree> treeMap, Bot bot1, Bot bot2) {
            for (Tree tree : bot1.trees)
                treeMap.put(tree.cell.index, tree);
            for (Tree tree : bot2.trees)
                treeMap.put(tree.cell.index, tree);
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

            void complete() {
                Bot owner = owner();
                owner.score += nutrients + RICHNESS_BONUS[cell.richness];
                owner.sun -= COMPLETE_BASE_COST;
                nutrients--;
                owner.trees.remove(this);
                treeMap.remove(cell.index);
            }

            void grow() {
                Bot owner = owner();
                owner.sun -= growCost();
                size++;
                isDormant = true;
            }

            void seed(int targetCell) {
                Bot owner = owner();
                owner.sun -= seedCost();
                owner.addTree(targetCell, 0, true);
                isDormant = true;
            }

            int growCost() {
                return GROW_BASE_COST[size] + owner().getTreeCount(size + 1);
            }

            int seedCost() {
                return SEED_BASE_COST + owner().getTreeCount(0);
            }

            Bot owner() {
                return isMine ? myBot : oppBot;
            }

            Tree copy() {
                return new Tree(cell.index, size, isMine, isDormant);
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

            String act() {
                int[] treeCounts = getTreeCounts();
                for (Tree tree : trees) {
                    if (tree.cell.richness == 3) {
                        if (tree.size == LARGE) {
                            if (sun >= 4)
                                return "COMPLETE " + tree.cell.index;
                            else
                                return "WAIT";
                        } else {
                            if (sun >= tree.growCost())
                                return "GROW " + tree.cell.index;
                            else
                                return "WAIT";
                        }
                    }
                }
                return "";
            }

            void addTree(int cellIndex, int size, boolean isDormant) {
                Tree tree = new Tree(cellIndex, size, isMine, isDormant);
                trees.add(tree);
                treeMap.put(cellIndex, tree);
            }

            int getTreeCount(int size) {
                int count = 0;
                for (Tree tree : trees) {
                    if (tree.size == size)
                        count++;
                }
                return count;
            }

            int[] getTreeCounts() {
                int[] counts = new int[4];
                for (Tree tree : trees) {
                    counts[tree.size]++;
                }
                return counts;
            }

            float getFinalScore() {
                int sunScore = sun / 3;
                return score + sunScore + 0.01f * trees.size();
            }

            int maxSun() {
                int maxSun = sun, day = State.this.day;
                while (++day <= LAST_DAY) {
                    int sunDir = day % 6;
                    int[] shadows = new int[cells.length];
                    setShadows(shadows, myBot.trees, sunDir);
                    setShadows(shadows, oppBot.trees, sunDir);

                    for (Tree tree : trees) {
                        if (tree.size > shadows[tree.cell.index])
                            maxSun += tree.size;
                    }
                }
                return maxSun;
            }

            void print() {
                System.err.println("score = " + score);
                System.err.println("sun points = " + sun);
                int[] treeCounts = getTreeCounts();
                System.err.println("seeds = " + treeCounts[0]);
                System.err.println("small trees = " + treeCounts[1]);
                System.err.println("medium trees = " + treeCounts[2]);
                System.err.println("large trees = " + treeCounts[3]);
                System.err.println("theoretical max sun points = " + maxSun());
            }

            Bot copy() {
                Bot clone = new Bot(isMine);
                clone.update(sun, score, isWaiting);
                List<Tree> cloneTrees = clone.trees;
                for (Tree tree : trees) {
                    Tree cloneTree = tree.copy();
                    cloneTrees.add(cloneTree);
                }
                return clone;
            }

            private void setShadows(int[] shadows, List<Tree> trees, int sunDir) {
                for (Tree tree : trees) {
                    int n = tree.size;
                    Cell cell = tree.cell;
                    while (n-- > 0 && cell != null) {
                        cell = neighbor(cell.index, sunDir);
                        if (cell != null && tree.size > shadows[cell.index])
                            shadows[cell.index] = tree.size;
                    }
                }
            }
        }
    }
}