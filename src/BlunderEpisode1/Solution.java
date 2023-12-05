package BlunderEpisode1;

import java.util.*;

/**
 * The 9 rules of the new Blunder system:
 *
 * Blunder starts from the place indicated by the @ symbol on the map and heads SOUTH.
 * Blunder finishes his journey and dies when he reaches the suicide booth marked $.
 * Obstacles that Blunder may encounter are represented by # or X.
 * When Blunder encounters an obstacle, he changes direction using the following priorities: SOUTH, EAST, NORTH and WEST. So he first tries to go SOUTH, if he cannot, then he will go EAST, if he still cannot, then he will go NORTH, and finally if he still cannot, then he will go WEST.
 * Along the way, Blunder may come across path modifiers that will instantaneously make him change direction. The S modifier will make him turn SOUTH from then on, E, to the EAST, N to the NORTH and W to the WEST.
 * The circuit inverters (I on map) produce a magnetic field which will reverse the direction priorities that Blunder should choose when encountering an obstacle. Priorities will become WEST, NORTH, EAST, SOUTH. If Blunder returns to an inverter I, then priorities are reset to their original state (SOUTH, EAST, NORTH, WEST).
 * Blunder can also find a few beers along his path (B on the map) that will give him strength and put him in “Breaker” mode. Breaker mode allows Blunder to destroy and automatically pass through the obstacles represented by the character X (only the obstacles X). When an obstacle is destroyed, it remains so permanently and Blunder maintains his course of direction. If Blunder is in Breaker mode and passes over a beer again, then he immediately goes out of Breaker mode. The beers remain in place after Blunder has passed.
 * 2 teleporters T may be present in the city. If Blunder passes over a teleporter, then he is automatically teleported to the position of the other teleporter and he retains his direction and Breaker mode properties.
 * Finally, the space characters are blank areas on the map (no special behavior other than those specified above).
 *
 * Extra: If Blunder cannot reach the suicide booth because he is indefinitely looping, then your program must only display LOOP.
 */

class Solution {

    private static State[][] states;
    private static boolean loopIdentified = false;

    private static class Teleporter {
        private int x, y;

        public Teleporter(int x, int y) {
            this.x = x;
            this.y = y;
        }
        public boolean isAt(int x, int y) {
            return this.x == x && this.y == y;
        }
        public int[] getPosition() {
            return new int[]{x, y};
        }
    }

    static class State {
        Move lastMove;
        int gridchanges;
        boolean breakermode;
        boolean inverted;

        public State(Move move, int gridchanges, boolean breakermode, boolean inverted) {
            this.lastMove = move;
            this.gridchanges = gridchanges;
            this.breakermode = breakermode;
            this.inverted = inverted;
        }

        public boolean equals(State other) {
            return other != null
                    &&  this.getClass() == other.getClass()
                    &&  this.lastMove.equals(other.lastMove)
                    &&  this.inverted == other.inverted
                    &&  this.breakermode == other.breakermode
                    &&  this.gridchanges == other.gridchanges
                    ;
        }
    }

    private static class Grid {

        private char[][] grid;
        int lengthX;
        int lengthY;

       List  teleporters;// 0 or 2
        private int gridChangesByBreakerMode;

        public Grid(char[][] grid, int lengthY, int lengthX) {
            this.grid = grid;
            this.lengthX = lengthX;
            this.lengthY = lengthY;
            this.gridChangesByBreakerMode = 0;
            identifyTeleporters();
        }

        public int[] getStartPostion() {
            //Rule 1
            for (int y = 0; y < lengthY; ++y) {
                for(int x = 0; x < lengthX; ++x) {
                    if (grid[y][x] == '@') {
                        return new int[]{x,y};
                    }
                }
            }
            return new int[]{-1,-1};
        }

        public char get(int x, int y) {
            if (x < 0 || y < 0 || lengthX <= x || lengthY <= y) { //Illegal spot outside grid
                return '#';
            }
            return grid[y][x];
        }

        private void identifyTeleporters() {
            if (teleporters == null) { teleporters = new ArrayList<>(); }
            for (int y = 0; y < lengthY; ++y) {
                for (int x = 0; x < lengthX; ++x) {
                    if (grid[y][x] == 'T') {
                        teleporters.add(new Teleporter(x,y));
                    }
                }
            }
        }

        public void printGrid() {
            for (int i = 0; i < lengthY; ++i) {
                System.err.println(grid[i]); //print as error as this doesnt have any impact on challenge result in CodeInGames
            }
        }

        public int[] getTeleportationDestination(int x, int y) {
            if (teleporters.size() < 1) {
                return new int[]{x,y}; //same spot - no teleportation
            }
            //There's either 0 or 2 teleporters available according to assignment definition.
            Teleporter teleporter1 = (Teleporter) teleporters.get(0);
            Teleporter teleporter2 = (Teleporter) teleporters.get(1);
            if (teleporter1.isAt(x, y)) {
                return teleporter2.getPosition();
            } else if (teleporter2.isAt(x, y)) {
                return teleporter1.getPosition();
            } else {
                return new int[]{x, y}; //same spot - no teleportation
            }
        }

        public void destroyWallIfExistsAt(int x, int y) {
            if (grid[y][x] == 'X'){
                grid[y][x] = ' ';
                ++gridChangesByBreakerMode;
            }
        }
    }

    public static enum Move {
        NORTH(0,-1, "NORTH"),
        EAST(1,0, "EAST"),
        SOUTH(0,1, "SOUTH"),
        WEST(-1,0, "WEST"),
        UNKNOWN (0, 0, "UNKNOWN");

        private int x, y;
        private String name;

        Move(int x, int y, String printableName) {
            this.x = x;
            this.y = y;
            this.name = printableName;
        }

        public int x() {return this.x;}
        public int y() {return this.y;}
        public String toString() {return name;}
    }

    private static class Blunder {
        final List normalMoveOrder = new ArrayList<>(){{ //RULE 4
            add(Move.SOUTH);
            add(Move.EAST);
            add(Move.NORTH);
            add(Move.WEST);
        }};
        Move priorityMove = Move.SOUTH; //RULE 1
        Move previousMove = Move.SOUTH; // RULE 4

        private boolean breakerMode;
        private boolean invertedDirections;
        private boolean isFinished;


        private int positionX, positionY;

        Blunder(int[] coordinates) {
            this(coordinates[0], coordinates[1]);
        }
        Blunder(int x, int y) {
           this.positionX = x;
           this.positionY = y;
           this.breakerMode = false;
           this.invertedDirections = false;
           this.isFinished = false;
        }

        public String getNextMoveInText(Grid grid) {
            if (isFinished) {
                return "Finished";
            }
            Move nextMove = getNextMove(grid);
            makeMove(nextMove);
            readGridSpecialEffect(grid);
            return nextMove.toString();
        }

        private void makeMove(Move move) {
            this.positionX += move.x;
            this.positionY += move.y;
            this.previousMove = move;
        }

        public boolean isBreakerMode() {return breakerMode;}

        public void toggleBreakerMode() {
            this.breakerMode = !this.breakerMode;
        }

        public void toggleInvertedDirections() {
            this.invertedDirections = !this.invertedDirections;
        }


        private Move getNextMove(Grid grid) {
            if (priorityMove != Move.UNKNOWN &&
                    iCanMoveTo(grid.get(positionX + priorityMove.x, positionY + priorityMove.y))) {
                return priorityMove;
            } else {
                if (priorityMove != Move.UNKNOWN && priorityMove != previousMove) {
                    previousMove = Move.UNKNOWN; //impossible prioritymove registered cancelling continuation.
                }
                priorityMove = Move.UNKNOWN;
                if (iCanContinueSameDirection(grid, previousMove)) { //Rule 4 continue until next obstacle
                    return previousMove;
                }
                if (invertedDirections) { //RULE 6
                    for (int i = normalMoveOrder.size()-1; i > -1 ; --i) {
                        Move move = (Move) normalMoveOrder.get(i);
                        if (iCanMoveTo(grid.get(positionX + move.x, positionY + move.y))) {
                            return move;
                        }
                    }
                } else { //Rule 5
                    for (int i = 0; i < normalMoveOrder.size(); ++i) {
                        Move move = (Move) normalMoveOrder.get(i);
                        if (iCanMoveTo(grid.get(positionX + move.x, positionY + move.y))) {
                            return move;
                        }
                    }
                }
            }
            return Move.UNKNOWN;
        }

        private boolean iCanContinueSameDirection(Grid grid, Move lastMove) {
            return previousMove != Move.UNKNOWN && iCanMoveTo(grid.get(positionX + lastMove.x(), positionY + lastMove.y()));
        }

        private boolean iCanMoveTo(final char gridbox) {
            if (gridbox == '#' //RULE 3
                 || (gridbox == 'X' && !breakerMode)  //RULE 3 && RULE 7
            ) {
                return false;
            }
            return true;
        }

        private void readGridSpecialEffect(Grid grid) {
            char newPosition = grid.get(positionX, positionY);

            switch (Character.toUpperCase(newPosition)) {
                case '$': //RULE 2
                    isFinished = true;
                    break;
                case 'S': //RULE 5
                    priorityMove = Move.SOUTH;
                    break;
                case 'E': //RULE 5
                    priorityMove = Move.EAST;
                    break;
                case 'N': //RULE 5
                    priorityMove = Move.NORTH;
                    break;
                case 'W': //RULE 5
                    priorityMove = Move.WEST;
                    break;
                case 'I': //RULE 6
                    toggleInvertedDirections();
                    break;
                case 'B': //RULE 7
                    toggleBreakerMode();
                    break;
                case 'T': //RULE 8
                    int[] teleportPosition = grid.getTeleportationDestination(positionX, positionY);
                    this.positionX = teleportPosition[0];
                    this.positionY = teleportPosition[1];
                    break;
                default:
                    break;
            }
        }
    }

    private static boolean isLoopIdentified(Blunder blunder, Grid grid) {
        //State består av antal kartförändringar, riktning, position, breakermode, inverted
        if (states == null) {
            states = new State[grid.lengthY][grid.lengthX];
            loopIdentified = false;
        }
        State currentState = new State(blunder.previousMove, grid.gridChangesByBreakerMode, blunder.breakerMode, blunder.invertedDirections);
        State previousStateInCoordinate = states[blunder.positionY][blunder.positionX];
        if (currentState.equals(previousStateInCoordinate)) {
            loopIdentified = true;
        } else {
            states[blunder.positionY][blunder.positionX] = currentState;
        }
        return loopIdentified;
    }

    private static void handleBreakerModeGridChanges(Grid grid, Blunder blunder) {
        //Rule 7
        if (blunder.isBreakerMode()) {
            grid.destroyWallIfExistsAt(blunder.positionX, blunder.positionY);
            grid.printGrid();
        }
    }

    public static Grid readGridFromInput() {
        Scanner in = new Scanner(System.in);
        int L = in.nextInt();
        int C = in.nextInt();
        in.nextLine();
        char[][] grid = new char[L][C];

        for (int i = 0; i < L; i++) {
            String line = in.nextLine();
            line = line.trim();
            grid[i] = line.toCharArray();
        }
        return new Grid(grid, L, C);
    }

    public static void main(String args[]) {
        Grid grid = readGridFromInput();
        Blunder blunder = new Blunder(grid.getStartPostion());
        List<String> moves = new LinkedList<>();
        do {
            moves.add(blunder.getNextMoveInText(grid));
            handleBreakerModeGridChanges(grid, blunder); //blunder is not allowed to change map.
        } while (!blunder.isFinished && !isLoopIdentified(blunder, grid));
        if (loopIdentified) {
            System.out.println("LOOP");
        } else {
            for (String move : moves) {
                System.out.println(move);
            }
        }
    }
}
