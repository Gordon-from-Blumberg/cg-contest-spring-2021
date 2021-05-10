import java.util.*;

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

    static final int SEED = 0, SMALL_TREE = 1, MEDIUM_TREE = 2, LARGE_TREE = 3;
    static final int UNUSABLE = 0, POOR_CELL = 1, MEDIUM_CELL = 2, RICH_CELL = 3;

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

            System.out.println(state.myBot.act());
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

        State copy() {
            State clone = new State(cells);
            copyTo(clone);
            return clone;
        }

        void copyTo(State clone) {
            clone.update(day, nutrients, myBot.sun, myBot.score, oppBot.sun, oppBot.score, oppBot.isWaiting);
            treeMap.values().forEach(tree -> clone.addTree(tree.cell.index, tree.size, tree.isMine, tree.isDormant));
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

        boolean isFreeRichestPresent() {
            for (int i = 0; i < 7; i++) {
                if (cells[i].richness == RICH_CELL && !treeMap.containsKey(i))
                    return true;
            }
            return false;
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

            Cell richestCellToSeed() {
                Cell richest = null;
                for (Cell cell : seedCells()) {
                    if (cell.richness == UNUSABLE || treeMap.containsKey(cell.index))
                        continue;

                    if (richest == null || richest.richness < cell.richness) {
                        richest = cell;
                        if (richest.richness == RICH_CELL)
                            break;
                    }
                }

                return richest;
            }

            Set<Cell> seedCells() {
                return size == LARGE_TREE ? cell.thirdCircle
                        : size == MEDIUM_TREE ? cell.secondCircle
                        : size == SMALL_TREE ? cell.firstRing
                        : Collections.emptySet();
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

                boolean isFreeRichestPresent = isFreeRichestPresent();
                System.err.println("isFreeRichestPresent = " + isFreeRichestPresent);
                Tree toSeed = null, toGrow = null, toComplete = null;
                Cell seedTarget = null;
                for (Tree tree : trees) {
                    if (tree.isDormant)
                        continue;

                    if (tree.cell.richness == RICH_CELL) {
                        if (tree.size == LARGE_TREE) {
                            System.err.println("Tree on rich cell#" + tree.cell.index + " is large");
                            if (toComplete == null) {
                                toComplete = tree;
                            }
                        } else if (toGrow == null && tree.size < LARGE_TREE) {
                            toGrow = tree;
                        }

                        if (isFreeRichestPresent) {
                            Cell richestCellToSeed = tree.richestCellToSeed();
                            if (richestCellToSeed != null && richestCellToSeed.richness == RICH_CELL) {
                                toSeed = tree;
                                seedTarget = richestCellToSeed;
                            }
                        }

                    } else {
                        if (seedTarget == null || toGrow == toSeed) {
                            Cell richestCellToSeed = tree.richestCellToSeed();
                            if (richestCellToSeed != null
                                    && (seedTarget == null || richestCellToSeed.richness > seedTarget.richness)) {
                                toSeed = tree;
                                seedTarget = richestCellToSeed;
                            }
                        }
                        if (toSeed != tree
                                && tree.size < LARGE_TREE
                                && (toGrow == null || toGrow.cell.richness < tree.cell.richness))
                            toGrow = tree;
                    }
                }

                if (!isFreeRichestPresent && toComplete != null && treeCounts[LARGE_TREE] > 1 && treeCounts[MEDIUM_TREE] > 1) {
                    System.err.println("No free richest cell and can complete -> complete");
                    return sun >= COMPLETE_BASE_COST ? "COMPLETE " + toComplete.cell.index : "WAIT";
                }

                if (day == LAST_DAY && toComplete != null && sun >= COMPLETE_BASE_COST) {
                    System.err.println("Last day and can complete -> complete");
                    return "COMPLETE " + toComplete.cell.index;
                }

                if (toSeed != null
                        && (seedTarget.richness == RICH_CELL || toGrow == null || seedTarget.richness > toGrow.cell.richness)) {
                    return sun >= treeCounts[SEED] ? "SEED " + toSeed.cell.index + " " + seedTarget.index : "WAIT";
                } else if (toGrow != null) {
                    return sun >= toGrow.growCost() ? "GROW " + toGrow.cell.index : "WAIT";
                }

                return "WAIT";
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
                System.err.println("seeds = " + treeCounts[SEED]);
                System.err.println("small trees = " + treeCounts[SMALL_TREE]);
                System.err.println("medium trees = " + treeCounts[MEDIUM_TREE]);
                System.err.println("large trees = " + treeCounts[LARGE_TREE]);
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

        enum Gene {
            WAIT {
                @Override
                String act(State state) {
                    return "WAIT";
                }
            },

            SEED {
                @Override
                String act(State state) {
                    final Map<Integer, Tree> treeMap = state.treeMap;
                    for (final Tree tree : state.myBot.trees) {
                        if (tree.isDormant)
                            continue;

                        for (final Cell cellToSeed : tree.seedCells()) {
                            if (cellToSeed.richness > UNUSABLE && !treeMap.containsKey(cellToSeed.index)) {
                                return state.myBot.sun >= tree.seedCost()
                                        ? "SEED " + tree.cell.index + " " + cellToSeed.index
                                        : "WAIT";
                            }
                        }
                    }
                    return "WAIT";
                }
            },

            SEED_ON_RICH {
                @Override
                String act(State state) {
                    final Map<Integer, Tree> treeMap = state.treeMap;
                    for (final Tree tree : state.myBot.trees) {
                        if (tree.isDormant)
                            continue;

                        for (final Cell cellToSeed : tree.seedCells()) {
                            if (cellToSeed.richness == RICH_CELL && !treeMap.containsKey(cellToSeed.index)) {
                                return state.myBot.sun >= tree.seedCost()
                                        ? "SEED " + tree.cell.index + " " + cellToSeed.index
                                        : "WAIT";
                            }
                        }
                    }
                    return "WAIT";
                }
            },

            SEED_OUT_OF_SHADOWS {
                @Override
                String act(State state) {
                    return "WAIT";
                }
            },

            SEED_TO_SHADOW_OPP {
                @Override
                String act(State state) {
                    return "WAIT";
                }
            },

            SEED_ON_MEDIUM {
                @Override
                String act(State state) {
                    final Map<Integer, Tree> treeMap = state.treeMap;
                    for (final Tree tree : state.myBot.trees) {
                        if (tree.isDormant)
                            continue;

                        for (final Cell cellToSeed : tree.seedCells()) {
                            if (cellToSeed.richness == MEDIUM_CELL && !treeMap.containsKey(cellToSeed.index)) {
                                return state.myBot.sun >= tree.seedCost()
                                        ? "SEED " + tree.cell.index + " " + cellToSeed.index
                                        : "WAIT";
                            }
                        }
                    }
                    return "WAIT";
                }
            },

            SEED_ON_POOR {
                @Override
                String act(State state) {
                    final Map<Integer, Tree> treeMap = state.treeMap;
                    for (final Tree tree : state.myBot.trees) {
                        if (tree.isDormant)
                            continue;

                        for (final Cell cellToSeed : tree.seedCells()) {
                            if (cellToSeed.richness == POOR_CELL && !treeMap.containsKey(cellToSeed.index)) {
                                return state.myBot.sun >= tree.seedCost()
                                        ? "SEED " + tree.cell.index + " " + cellToSeed.index
                                        : "WAIT";
                            }
                        }
                    }
                    return "WAIT";
                }
            },

            GROW {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (tree.size < LARGE_TREE) {
                            return state.myBot.sun >= tree.growCost() ? "GROW " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            GROW_ON_RICH {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (tree.size < LARGE_TREE && tree.cell.richness == RICH_CELL) {
                            return state.myBot.sun >= tree.growCost() ? "GROW " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            GROW_OUT_OF_SHADOWS {
                @Override
                String act(State state) {

                    return "WAIT";
                }
            },

            GROW_TO_SHADOW_OPP {
                @Override
                String act(State state) {
                    return "WAIT";
                }
            },

            GROW_ON_MEDIUM {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (tree.size < LARGE_TREE && tree.cell.richness == MEDIUM_CELL) {
                            return state.myBot.sun >= tree.growCost() ? "GROW " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            GROW_ON_POOR {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (tree.size < LARGE_TREE && tree.cell.richness == POOR_CELL) {
                            return state.myBot.sun >= tree.growCost() ? "GROW " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            COMPLETE {
                @Override
                String act(State state) {
                    return "WAIT";
                }
            },

            COMPLETE_RICH {
                @Override
                String act(State state) {
                    return "WAIT";
                }
            },

            COMPLETE_MEDIUM {
                @Override
                String act(State state) {
                    return "WAIT";
                }
            },

            COMPLETE_POOR {
                @Override
                String act(State state) {
                    return "WAIT";
                }
            };

            abstract String act(State state);
        }
    }
}