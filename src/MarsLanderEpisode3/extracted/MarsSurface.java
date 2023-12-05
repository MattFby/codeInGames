package MarsLanderEpisode3.extracted;

import java.util.ArrayList;
import java.util.List;

class MarsSurface {

    static final int Y_BORDER_MAX = 3000;
    static final int X_BORDER_MAX = 7000;
    private static final short UNREACHABLE_CONSTANT = 10001; //Longest walk is along borders: 3000+7000 => 10 000 +1
    private static final short NOT_CALCULATED_CONSTANT = 0; //Cannot decide distance to this place.

    List<Point> surfacePoints;
    LandingZone landingZone;
    boolean[][] isSurface;
    short[][] preCalculatedDistanceToLandingZone;
    private short[][] heightAboveSurface;


    MarsSurface() {}

    public void addPoint(Point p){
        if (surfacePoints == null) { surfacePoints = new ArrayList<>();}
        surfacePoints.add(p);
    }

    public void build() {
        landingZone();
        buildSurface();
        buildDistanceGrid();
        buildHeightGrid();
    }

    public void buildSurface() {
        isSurface = new boolean[Y_BORDER_MAX][X_BORDER_MAX];
        for(int i = 1; i < surfacePoints.size(); ++i) {
            int yb = surfacePoints.get(i).y();
            int ya = surfacePoints.get(i-1).y();
            int xb = surfacePoints.get(i).x();
            int xa = surfacePoints.get(i-1).x();

            double dy_dx = ((double)(yb-ya)) / (xb-xa);
            int y, x;

//            System.err.println("yb:" + yb + " ya:" + ya + " xb:" + xb + " xa:" + xa + " dydx:" + dy_dx);

            double xiStepLength = (-1 <= dy_dx && dy_dx <= 1) ? 1 : 1.0/Math.abs(dy_dx);
            for (double xi = 0; xi < Math.abs(xb - xa); xi += xiStepLength) {
                if (xa < xb) {
                    y = (int)Math.round(ya + xi * dy_dx);
                    x = xa + (int)xi;
                } else {
                    y = (int)Math.round(yb + xi * dy_dx);
                    x = xb + (int)xi;
                }
                isSurface[y][x] = true;
            }
        }
    }

    public void buildDistanceGrid() {
        preCalculatedDistanceToLandingZone = new short[Y_BORDER_MAX][X_BORDER_MAX];
        addLandingZoneToDistanceGrid();
        //Must start from landingzone
        //idea is to build upwards starting from landinzone mid
        for (int y = landingZone().y(); y < Y_BORDER_MAX; ++y) {
            for(int x = landingZone().xMid(); x < X_BORDER_MAX; ++x) {
                calculateDistanceUsingKnownDistances(x,y);
            }
            for(int x = landingZone().xMid(); -1 < x; --x) {
                calculateDistanceUsingKnownDistances(x,y);
            }
        }

        //Now go from top down to handle any backsides of hills/missed gaps
        for (int y = Y_BORDER_MAX-1; 0 <= y ; --y) {
            for(int x = X_BORDER_MAX-1; 0 <= x ; --x) {
                calculateDistanceUsingKnownDistances(x,y);
            }
        }
    }

    public void buildHeightGrid() {
        heightAboveSurface = new short[X_BORDER_MAX][Y_BORDER_MAX];
        for (int y = 0; y < Y_BORDER_MAX-1; ++y) {
            for (int x = 0; x < X_BORDER_MAX; ++x) {
                if (isSurface[y][x]) {
                    heightAboveSurface[x][y+1] = 1;
                }
            }
        }
        for (int x = 0; x < X_BORDER_MAX; ++x) {
            for (int y = 1; y < Y_BORDER_MAX; ++y) {
                if (heightAboveSurface[x][y] == 0 && heightAboveSurface[x][y-1] != 0) {
                    heightAboveSurface[x][y] = (short) (heightAboveSurface[x][y - 1] + 1);
                }
            }
        }
    }

    public boolean isGroundContact(int x, int y) {
        return (getHeightFromSurface(x,y) < 1);
    }


    public int getHeightFromSurface(int x, int y) {
        return heightAboveSurface[x][y]-1; //ground is at 1 for init purposes.
    }

    private void addLandingZoneToDistanceGrid() {
        for (int landingZoneX = landingZone().xa(); landingZoneX <= landingZone.xb(); ++landingZoneX) {
            preCalculatedDistanceToLandingZone[landingZone.y()][landingZoneX] = 1;
        }
    }

    private void calculateDistanceUsingKnownDistances(int x, int y) {
        if (preCalculatedDistanceToLandingZone[y][x] != 0) {
            return; //already calculated
        }
        if (!isSurface[y][x]) {
            //TODO: change calculations to handle backside of hilly conditions
            preCalculatedDistanceToLandingZone[y][x] = (short) (getMinDistanceValueFromNeighbors(x,y) + 1);
        } else {
            preCalculatedDistanceToLandingZone[y][x] = UNREACHABLE_CONSTANT;
        }
    }

    /**
     * Checks the neighbor value and returns the lowest value available.
     * If x,y isSurface it will return Unreachable constant
     * @param x
     * @param y
     * @return
     */
    private short getMinDistanceValueFromNeighbors(int x, int y) {
        short minDist = NOT_CALCULATED_CONSTANT;
        if (isSurface[y][x] && !isLandingZone(x,y)) {
            return UNREACHABLE_CONSTANT;
        }
        if (isWithinMapsBorders(x-1, y)) {
            minDist = getMinDistNotZeroOrNegative(minDist, preCalculatedDistanceToLandingZone[y][x - 1]);
        }
        if (isWithinMapsBorders(x+1, y)) {
            minDist = getMinDistNotZeroOrNegative(minDist, preCalculatedDistanceToLandingZone[y][x + 1]);
        }
        if (isWithinMapsBorders(x, y-1)) {
            minDist = getMinDistNotZeroOrNegative(minDist, preCalculatedDistanceToLandingZone[y-1][x]);
        }
        if (isWithinMapsBorders(x, y+1)) {
            minDist = getMinDistNotZeroOrNegative(minDist, preCalculatedDistanceToLandingZone[y+1][x]);
        }
        return minDist;
    }

    private boolean isLandingZone(int x, int y) {
        return landingZone.isLandingZone(x,y);
    }

    public boolean isWithinMapsBorders(int x, int y) {
        return -1 < x && x < X_BORDER_MAX && -1 < y && y < Y_BORDER_MAX;
    }

    /**
     * This will evaluate a and b and return the smallest positive integer (1 <= x).
     * NOT_CALCULATED_CONSTANT if both a and b is within the range(Short.MIN_VALUE, 0).
     * @param a
     * @param b
     * @return
     */
    private short getMinDistNotZeroOrNegative(short a, short b) {
        if (a > 0 && b > 0) {
            return (short)Math.min(a, b);
        } else {
            if (a > 0) {
                return a;
            } else if (b > 0) {
                return b;
            } else {
                return NOT_CALCULATED_CONSTANT;
            }
        }
    }

    public LandingZone landingZone() {
        if (landingZone == null) {
            for (int p = 1; p < surfacePoints.size(); ++p) {
                if (surfacePoints.get(p-1).y == surfacePoints.get(p).y) {
                    landingZone = new LandingZone(surfacePoints.get(p-1).x, surfacePoints.get(p).x, surfacePoints.get(p).y);
                }
            }
        }
        return landingZone;
    }

    public short getMinDistanceToLandingZone(int x, int y){
        return preCalculatedDistanceToLandingZone[y][x];
    }

    /**
     * Landing means touchdown (y = 0), however calculations of position may miss the 0, and return for instance -2.
     * Assumptions:
     * A landing requires the ship to be at landingzone height or passing through it.
     * The landing will be from above going downwards.
     * A limited acceptable distance from landingzone is required to catch landing where calculating rocket new coordinates jumps over 0.
     * I've chosen to go for the vertical speed limit as a reasonable distance, hence the interval [-40 0] is acceptable
     *
     * Note, this may miss landing going in reverse direction. (upwards)
     */
    public boolean isYInLandingZoneVertically(int y) {
        int metersFromY = landingZone.metersFromLandingzoneVertically(y);
        return RocketState.VERTICAL_SPEED_LIMIT_FOR_LANDING < metersFromY && metersFromY < 1;
    }

    /**
     * X is wide, harder to miss,
     * if needed a safezone can be added.
     * @param x
     * @return
     */
    public boolean isXInLandingZoneHorizontally(int x) {
        return landingZone.isXWithinLandingZone(x);
    }

    public void printState() {
        if (surfacePoints != null && surfacePoints.size() > 0) {
            for (Point point : surfacePoints) {
                System.err.print("[" + point.x() + "," + point.y() +" ] ");
            }
            System.err.println();
        }
        if (landingZone != null) {
            System.err.println("Landingzone: " + landingZone.xa() + " <= x<= " + landingZone.xb() + " , y:" + landingZone.y());
        }
        if (isSurface != null) {
            System.err.println("surfaceGrid calculated");
        }
        if (heightAboveSurface != null) {
            System.err.println("heightGrid calculated");
        }
        if (preCalculatedDistanceToLandingZone != null) {
            System.err.println("distanceGrid calculated");
        }
    }
}
