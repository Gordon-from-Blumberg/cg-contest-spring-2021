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

    static final int FIRST_TURN = 1000 - 50;
    static final int REST_TURNS = 100 - 15;

    static final int GENES_COUNT = 100;
    static final int POPULATION = 20;
    static final int MATING_POOL_SIZE = 150;
    static final float MUTATION_CHANCE = 0.05f;

    static final Random RAND = new Random(47);

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Cell[] cells = new Cell[in.nextInt()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Cell(in.nextInt(), in.nextInt(),
                    in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());
        }

        Cell.fillRings(cells);

        int allowedDelay = FIRST_TURN;
        final State state = new State(cells);
        final State.DNA[] population = new State.DNA[POPULATION];

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
            state.setShadows(new int[cells.length]);

            String[] legalActions = new String[in.nextInt()];

            if (in.hasNextLine()) {
                in.nextLine();
            }

            for (int i = 0; i < legalActions.length; i++) {
                legalActions[i] = in.nextLine(); // try printing something from here to start with
            }

            final long endTime = System.currentTimeMillis() + allowedDelay;

            final State.DNA[] matingPool = new State.DNA[MATING_POOL_SIZE];

            int generationNumber = 0;
            int realMatingPoolSize = 0;
            State.DNA bestSolution = null;
            while (System.currentTimeMillis() < endTime) {
                generationNumber++;
                if (matingPool[0] == null) {
                    population[0] = population[0] != null ? population[0].toNextTurn() : State.DNA.random();
                    for (int i = 1; i < POPULATION; i++)
                        population[i] = State.DNA.random();
                } else {
                    for (int i = 0; i < POPULATION; i++)
                        population[i] = State.DNA.crossover(
                                matingPool[RAND.nextInt(realMatingPoolSize)],
                                matingPool[RAND.nextInt(realMatingPoolSize)]
                        );
                }

                for (int i = 0; i < POPULATION; i++) {
                    final State copy = state.copy();
                    State.Genetic.simulate(copy, population[i]);
                    State.Genetic.fitness(copy, population[i]);
                }

                int totalFitness = 0;
                for (int i = 0; i < POPULATION; i++) {
                    final int score = population[i].score;
                    totalFitness += score;
                    if (bestSolution == null || score > bestSolution.score)
                        bestSolution = population[i];
                }

                realMatingPoolSize = 0;
                for (int i = 0; i < POPULATION; i++) {
                    final State.DNA solution = population[i];
                    final int n = solution.score * MATING_POOL_SIZE / totalFitness;
                    for (int j = 0; j < n; j++)
                        matingPool[realMatingPoolSize++] = solution;
                }
            }

//            assert bestSolution != null;
//            System.err.println("generation #" + generationNumber);
//            System.err.println("best score = " + bestSolution.score);
//            System.err.println("best solution = " + bestSolution);

            System.out.println(bestSolution.firstAction);
//            System.out.println(state.myBot.act());

            allowedDelay = REST_TURNS;
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
        int[] shadows;
        boolean gameOver;

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
            clone.shadows = new int[cells.length];
            System.arraycopy(shadows, 0, clone.shadows, 0, shadows.length);
            clone.gameOver = gameOver;
        }

        void applyAction(String[] actionPats, Tree tree, Bot bot) {
            switch (actionPats[0]) {
                case "WAIT":
                    bot.isWaiting = true;
                    break;
                case "SEED":
                    tree.seed(Integer.parseInt(actionPats[2]));
                    break;
                case "GROW":
                    tree.grow();
                    break;
                case "COMPLETE":
                    tree.complete();
                    break;
            }
        }

        void nextDay() {
            sunDir = ++day % 6;
            setShadows(new int[cells.length]);
            myBot.isWaiting = false;
            myBot.trees.forEach(tree -> {
                tree.isDormant = false;
                if (tree.size > shadows[tree.cell.index])
                    myBot.sun += tree.size;
            });
            oppBot.isWaiting = false;
            oppBot.trees.forEach(tree -> {
                tree.isDormant = false;
                if (tree.size > shadows[tree.cell.index])
                    oppBot.sun += tree.size;
            });
        }

        void addTree(int cellIndex, int size, boolean isMine, boolean isDormant) {
            (isMine ? myBot : oppBot).addTree(cellIndex, size, isDormant);
        }

        void setShadows(int[] shadows) {
            for (Tree tree : treeMap.values()) {
                int n = tree.size;
                Cell cell = tree.cell;
                while (n-- > 0 && cell != null) {
                    cell = neighbor(cell.index, sunDir);
                    if (cell != null && tree.size > shadows[cell.index])
                        shadows[cell.index] = tree.size;
                }
            }
            this.shadows = shadows;
        }

        Cell neighbor(int cell, int dir) {
            return Cell.neighbor(cells, cell, dir);
        }

        boolean isFreeRichestPresent() {
            for (int i = 0; i < 7; i++) {
                if (cells[i].richness == RICH_CELL && !treeMap.containsKey(i))
                    return true;
            }
            return false;
        }

        void print() {
            myBot.print();
//            oppBot.print();
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
                Tree toSeed = null, toGrow = null, toComplete = null;
                Cell seedTarget = null;
                for (Tree tree : trees) {
                    if (tree.isDormant)
                        continue;

                    if (tree.cell.richness == RICH_CELL) {
                        if (tree.size == LARGE_TREE) {
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

                if ((!isFreeRichestPresent || nutrients > 18)
                        && toComplete != null
                        && treeCounts[LARGE_TREE] > 1
                        && treeCounts[MEDIUM_TREE] > 1) {
//                    System.err.println("No free richest cell and can complete -> complete");
                    return sun >= COMPLETE_BASE_COST ? "COMPLETE " + toComplete.cell.index : "WAIT";
                }

                if (day == LAST_DAY && toComplete != null && sun >= COMPLETE_BASE_COST) {
//                    System.err.println("Last day and can complete -> complete");
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

            int getFinalScore() {
                int sunScore = sun / 3;
                return 100 * (score + sunScore) + trees.size();
            }

            int maxSun() {
                int maxSun = sun, day = State.this.day;
                while (++day <= LAST_DAY) {
                    int sunDir = day % 6;
                    int[] shadows = new int[cells.length];
//                    setShadows(shadows, myBot.trees, sunDir);
//                    setShadows(shadows, oppBot.trees, sunDir);

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
//                System.err.println("theoretical max sun points = " + maxSun());
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
        }

        static class Genetic {
            static void simulate(State state, DNA solution) {
                final Map<Integer, Tree> treeMap = state.treeMap;
                final Bot myBot = state.myBot;
                final Bot oppBot = state.oppBot;
                int geneIndex = 0;
                while (state.day <= LAST_DAY && geneIndex < GENES_COUNT) {
                    final Gene gene = solution.genes[geneIndex];
                    final String myAction = myBot.isWaiting ? "WAIT" : gene.act(state);
                    if (solution.firstAction == null)
                        solution.firstAction = myAction;
                    final String oppAction = oppBot.isWaiting ? "WAIT" : oppBot.act();

                    final String[] myActionParts = myAction.split(" ");
                    final String[] oppActionParts = oppAction.split(" ");

                    final Tree myActingTree = myActionParts.length > 1 ? treeMap.get(Integer.parseInt(myActionParts[1])) : null;
                    final Tree oppActingTree = oppActionParts.length > 1 ? treeMap.get(Integer.parseInt(oppActionParts[1])) : null;

                    if (myAction.startsWith("SEED")
                            && oppAction.startsWith("SEED")
                            && myActionParts[2].equals(oppActionParts[2])) {
                        myActingTree.isDormant = true;
                        oppActingTree.isDormant = true;
                        myBot.sun -= myActingTree.seedCost();
                        oppBot.sun -= oppActingTree.seedCost();
                    } else {
                        state.applyAction(myActionParts, myActingTree, myBot);
                        state.applyAction(oppActionParts, oppActingTree, oppBot);

                        if (myAction.startsWith("COMPLETE") && oppAction.startsWith("COMPLETE"))
                            oppBot.score++;
                    }

                    if (!myAction.equals("WAIT") || gene == Gene.WAIT)
                        geneIndex++;

                    // switch to new day
                    if (myBot.isWaiting && oppBot.isWaiting)
                        if (state.day == LAST_DAY) {
                            state.gameOver = true;
                            break;
                        } else {
                            state.nextDay();
                        }
                }
            }

            static void fitness(State state, DNA solution) {
                int score = state.myBot.getFinalScore();
                if (state.gameOver) {
                    final int oppScore = state.oppBot.getFinalScore();
                    if (score > oppScore)
                        score *= 2;
                    else if (score < oppScore)
                        score /= 2;
                }

                solution.score = score;
            }
        }

        static class DNA {
            final Gene[] genes = new Gene[GENES_COUNT];

            int score;
            String firstAction;

            static DNA random() {
               final DNA dna = new DNA();
               for (int i = 0; i < GENES_COUNT; i++)
                   dna.genes[i] = Gene.random();
               return dna;
            }

            static DNA crossover(DNA parent1, DNA parent2) {
                final DNA child = new DNA();
                for (int i = 0; i < GENES_COUNT; i++) {
                    child.genes[i] = RAND.nextFloat() < MUTATION_CHANCE
                            ? Gene.random()
                            : (RAND.nextFloat() < 0.5 ? parent1 : parent2).genes[i];
                }
                return child;
            }

            DNA toNextTurn() {
                score = 0;
                firstAction = null;
                final int n = GENES_COUNT - 1;
                for (int i = 0; i < n; i++) {
                    genes[i] = genes[i + 1];
                }
                genes[n] = Gene.random();
                return this;
            }

            @Override
            public String toString() {
                return Arrays.toString(genes);
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
                        if (!tree.isDormant && tree.size < LARGE_TREE) {
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
                        if (!tree.isDormant && tree.size < LARGE_TREE && tree.cell.richness == RICH_CELL) {
                            return state.myBot.sun >= tree.growCost() ? "GROW " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            GROW_ON_MEDIUM {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (!tree.isDormant && tree.size < LARGE_TREE && tree.cell.richness == MEDIUM_CELL) {
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
                        if (!tree.isDormant && tree.size < LARGE_TREE && tree.cell.richness == POOR_CELL) {
                            return state.myBot.sun >= tree.growCost() ? "GROW " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            GROW_SEED {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (!tree.isDormant && tree.size == Player.SEED) {
                            return state.myBot.sun >= tree.growCost() ? "GROW " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            GROW_SMALL {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (!tree.isDormant && tree.size == SMALL_TREE) {
                            return state.myBot.sun >= tree.growCost() ? "GROW " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            GROW_MEDIUM {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (!tree.isDormant && tree.size == MEDIUM_TREE) {
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

            COMPLETE {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (!tree.isDormant && tree.size == LARGE_TREE) {
                            return state.myBot.sun >= COMPLETE_BASE_COST ? "COMPLETE " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            COMPLETE_RICH {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (!tree.isDormant && tree.size == LARGE_TREE && tree.cell.richness == RICH_CELL) {
                            return state.myBot.sun >= COMPLETE_BASE_COST ? "COMPLETE " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            COMPLETE_MEDIUM {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (!tree.isDormant && tree.size == LARGE_TREE && tree.cell.richness == MEDIUM_CELL) {
                            return state.myBot.sun >= COMPLETE_BASE_COST ? "COMPLETE " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            },

            COMPLETE_POOR {
                @Override
                String act(State state) {
                    for (Tree tree : state.myBot.trees) {
                        if (!tree.isDormant && tree.size == LARGE_TREE && tree.cell.richness == POOR_CELL) {
                            return state.myBot.sun >= COMPLETE_BASE_COST ? "COMPLETE " + tree.cell.index : "WAIT";
                        }
                    }
                    return "WAIT";
                }
            };

            static final int COUNT = values().length;

            static Gene random() {
                return Gene.values()[RAND.nextInt(COUNT)];
            }

            abstract String act(State state);
        }
    }
}