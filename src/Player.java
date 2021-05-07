import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {

    static Cell[] cells;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        cells = new Cell[in.nextInt()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Cell(in.nextInt(), in.nextInt(),
                    in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());
            // int index = in.nextInt(); // 0 is the center cell, the next cells spiral outwards
            // int richness = in.nextInt(); // 0 if the cell is unusable, 1-3 for usable cells
            // int neigh0 = in.nextInt(); // the index of the neighbouring cell for each direction
            // int neigh1 = in.nextInt();
            // int neigh2 = in.nextInt();
            // int neigh3 = in.nextInt();
            // int neigh4 = in.nextInt();
            // int neigh5 = in.nextInt();
        }

        // game loop
        while (true) {
            int day = in.nextInt(); // the game lasts 24 days: 0-23
            int nutrients = in.nextInt(); // the base score you gain from the next COMPLETE action
            int sun = in.nextInt(); // your sun points
            int score = in.nextInt(); // your current score
            int oppSun = in.nextInt(); // opponent's sun points
            int oppScore = in.nextInt(); // opponent's score
            boolean oppIsWaiting = in.nextInt() != 0; // whether your opponent is asleep until the next day

            int numberOfTrees = in.nextInt(); // the current amount of trees
            Tree[] trees = new Tree[cells.length];
            for (int i = 0; i < numberOfTrees; i++) {
                Tree tree = new Tree(in.nextInt(), in.nextInt(), in.nextInt() != 0, in.nextInt() != 0);
                trees[tree.cell.index] = tree;
                // int cellIndex = in.nextInt(); // location of this tree
                // int size = in.nextInt(); // size of this tree: 0-3
                // boolean isMine = in.nextInt() != 0; // 1 if this is your tree
                // boolean isDormant = in.nextInt() != 0; // 1 if this tree is dormant
            }

            String[] legalActions = new String[in.nextInt()];

            if (in.hasNextLine()) {
                in.nextLine();
            }

            Map<String, List<Integer>> groupedActions = new HashMap();
            for (int i = 0; i < legalActions.length; i++) {
                legalActions[i] = in.nextLine(); // try printing something from here to start with
                String[] actionParts = legalActions[i].split(" ");
                if (!"WAIT".equals(actionParts[0])) {
                    groupedActions.computeIfAbsent(actionParts[0], s -> new ArrayList<>())
                            .add(Integer.parseInt(actionParts[1]));
                }
            }

            System.err.println(groupedActions);

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");


            // GROW cellIdx | SEED sourceIdx targetIdx | COMPLETE cellIdx | WAIT <message>

            List<Integer> completeCells = groupedActions.get("COMPLETE");
            List<Integer> growCells = groupedActions.get("GROW");
            if (completeCells != null) {
                Tree bestTree = null;
                for (int index : completeCells) {
                    Tree tree = trees[index];
                    if (bestTree == null || tree.cell.richness > bestTree.cell.richness)
                        bestTree = tree;
                }
                System.out.println("COMPLETE " + bestTree.cell.index);
            } else if (growCells != null) {
                Tree bestTree = null;
                for (int index : growCells) {
                    Tree tree = trees[index];
                    if (bestTree == null || tree.cell.richness > bestTree.cell.richness)
                        bestTree = tree;
                }
                System.out.println("GROW " + bestTree.cell.index);
            } else {
                System.out.println("WAIT");
            }
        }
    }

    static class Cell {
        int index, richness;
        int[] neighbors = new int[6];

        Cell(int index, int richness, int... neigbors) {
            this.index = index;
            this.richness = richness;
            System.arraycopy(neigbors, 0, this.neighbors, 0, 6);
        }
    }

    static class Tree {
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
}