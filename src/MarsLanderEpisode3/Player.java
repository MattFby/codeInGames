package MarsLanderEpisode3;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


enum RocketState {
    FLYING("FLYING"),
    LANDED("LANDED"),
    LOST_IN_SPACE("LOST_IN_SPACE"),
    CRASHED("CRASHED");

    private final String desc;

    RocketState(String desc) {
        this.desc = desc;
    }

    public String toString() {
        return this.desc;
    }
}

class Point {
    protected final int x, y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public int x() {return x;}
    public int y() {return y;}
}

class LandingZone {
    protected final int xa, xb, y, xMid, xRangeFromMid;

    LandingZone(int xa, int xb, int y) {
        this.xa = xa;
        this.xb = xb;
        this.y = y;

        xMid = xa+(xb-xa)/2;
        xRangeFromMid = (xb-xa)/2;
    }

    public int xa() {return xa;}
    public int xb() {return xb;}
    public int y() {return y;}
    public int xMid() {return xMid;}
    public int xRangeFromMid() { return xRangeFromMid;}

    public boolean isXWithinLandingZone(int x) {
        return xa < x && x < xb;
    }

    /**
     * Distance from landing zone height
     * positive == above, negative == underneath.
     */
    public int metersFromLandingzoneVertically(int y) {
        return y - this.y;
    }

    public void print() {
        System.err.println("landingSite: " + xa + " <= x <= " + xb +" , y:" + y);
        System.err.println("landingSiteMid: " + xMid + " +- " + xRangeFromMid);
    }
}

class MarsSurface {

    static final int Y_BORDER_MAX = 3000;
    static final int X_BORDER_MAX = 7000;
    private static final short UNREACHABLE_CONSTANT = 10001; //Longest walk is along borders: 3000+7000 => 10 000 +1

    List<Point> surfacePoints;
    LandingZone landingZone;
    boolean[][] isSurface;
    short[][] distanceToLandingZone;
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
        distanceToLandingZone = new short[Y_BORDER_MAX][X_BORDER_MAX];
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

    public int getHeightFromSurface(int x, int y) {
        return heightAboveSurface[x][y]-1; //ground is at 1 for init purposes.
    }

    private void addLandingZoneToDistanceGrid() {
        for (int landingZoneX = landingZone().xa(); landingZoneX <= landingZone.xb(); ++landingZoneX) {
            distanceToLandingZone[landingZone.y()][landingZoneX] = 1;
        }
    }

    private void calculateDistanceUsingKnownDistances(int x, int y) {
        if (distanceToLandingZone[y][x] != 0) {
            return; //already calculated
        }
        if (!isSurface[y][x]) {
            distanceToLandingZone[y][x] = (short) (getMinValueNotZeroFromNeighbors(x,y) + 1);
        } else {
            distanceToLandingZone[y][x] = UNREACHABLE_CONSTANT;
        }
    }

    private short getMinValueNotZeroFromNeighbors(int x, int y) {
        short minDist = UNREACHABLE_CONSTANT;
        if (isWithinBorder(x-1, y)) {
            minDist = getMinDistNotZero(minDist, distanceToLandingZone[y][x - 1]);
        }
        if (isWithinBorder(x+1, y)) {
                minDist = getMinDistNotZero(minDist, distanceToLandingZone[y][x + 1]);
        }
        if (isWithinBorder(x, y-1)) {
            minDist = getMinDistNotZero(minDist, distanceToLandingZone[y-1][x]);
        }
        return minDist;
    }

    public boolean isWithinBorder(int x, int y) {
        return -1 < x && x < X_BORDER_MAX && -1 < y && y < Y_BORDER_MAX;
    }

    private short getMinDistNotZero(short a, short b) {
        if (a > 0 && b > 0) {
            return (short)Math.min(a, b);
        } else {
            if (a > 0) {
                return a;
            } else if (b > 0) {
                return b;
            } else {
                return Short.MAX_VALUE;
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
        return distanceToLandingZone[y][x];
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
        if (distanceToLandingZone != null) {
            System.err.println("distanceGrid calculated");
        }
    }
}

class Rocket {
    private static final int MIN_TRUST = 0;
    final static int MAX_ROTATION = 90;
    final static int MAX_STEP_ROTATION = 15;
    final static int MAX_THRUST = 1;
    final static int MAX_STEP_THRUST = 1;
    final static double GRAVITY = 3.711; //m/s^2
    final static int fuelUsagePerThrustUnit = 1;

    final static int VERTICAL_SPEED_LIMIT_FOR_LANDING = -40;
    final static int HORIZONTAL_SPEED_LIMIT_FOR_LANDING = 20;
    final static int LANDING_ANGLE_REQUIREMENT = 0;
    public static final int TO_LEFT = 1;
    public static final int TO_RIGHT = -1;

    private int fuel,
            angle,
            thrust,
            verticalSpeed,
            horizontalSpeed,
            x, x0,
            y, y0;

    private RocketState state = RocketState.FLYING;

    Rocket (int x, int y, int hSpeed, int vSpeed, int fuel, int rotate, int power) {
        this.x = x;
        this.x0 = x;
        this.y = y;
        this.y0 = y;
        this.horizontalSpeed = hSpeed;
        this.verticalSpeed = vSpeed;
        this.fuel = fuel;
        this.angle = rotate;
        this.thrust = power;
    }

    public void calculateNewPositionUsing(int newAngle, int newThrust) {
        System.err.println("received newAngle: " + newAngle + " and thrust:" + newThrust);
        updateAngle(validateNewAngle(newAngle));
        updateThrust(validateNewThrust(newThrust));
        calculateNewSpeedAndPosition();
    }

    public void calculateNewSpeedAndPosition() {
        calculateNewHorizontalSpeed();
        calculateNewVerticalSpeed();
        calculateFuelLeft();
        calculateNewCoordinates();
    }

    private int validateNewAngle(int newAngle) {
        if (Math.abs(this.angle - newAngle) < MAX_STEP_ROTATION+1) {
            return newAngle;
        } else {
            return (newAngle > angle) ? this.angle + MAX_STEP_ROTATION*TO_LEFT : this.angle + MAX_STEP_ROTATION*TO_RIGHT;
        }
    }

    private void updateAngle(int newAngle) {
        if (newAngle < MAX_ROTATION*TO_RIGHT) {
            this.angle = MAX_ROTATION*TO_RIGHT;
        }
        else {
            this.angle = Math.min(newAngle, MAX_ROTATION * TO_LEFT);
        }
    }

    public boolean rotateRight() {
        if (angle <= MAX_ROTATION*TO_RIGHT) {
            return false;
        }
        updateAngle(this.angle + MAX_STEP_ROTATION * TO_RIGHT);
        return true;
    }

    public boolean rotateLeft() {
        if (angle >= MAX_ROTATION * TO_LEFT) {
            return false;
        }
        updateAngle(this.angle + MAX_STEP_ROTATION * TO_LEFT);
        return true;
    }

    public boolean incrementThrust() {
        if (thrust+MAX_STEP_THRUST <= MAX_THRUST) {
            updateThrust(thrust + MAX_STEP_THRUST);
            return true;
        }
        return false;
    }

    public boolean decrementThrust() {
        if (thrust-MAX_STEP_THRUST >= MIN_TRUST) {
            updateThrust(thrust - MAX_STEP_THRUST);
            return true;
        }
        return false;
    }

    private int validateNewThrust(int newThrust) {
        if (Math.abs(newThrust - thrust) > 1) {
            if (thrust < newThrust) {
                return thrust + MAX_STEP_THRUST;
            } else if (thrust > newThrust ){
                return thrust - MAX_STEP_THRUST;
            }
        }
        return newThrust;
    }

    private void updateThrust(int newThrust) {
        if (hasFuelFor(newThrust)) {
            thrust = newThrust;
        } else {
             thrust = getMaxThrustOfRemainingFuel();
        }
    }

    private boolean hasFuelFor(int thrust) {
        return fuel >= thrust * fuelUsagePerThrustUnit;
    }

    private int getMaxThrustOfRemainingFuel() {
        return Math.min(MAX_THRUST, fuel/fuelUsagePerThrustUnit);
    }

    public void calculateNewVerticalSpeed() {
        double verticalThrust = thrust * Math.cos(Math.toRadians(angle));
        verticalSpeed = (int) Math.round(verticalSpeed - GRAVITY + verticalThrust);
    }

    public void calculateNewHorizontalSpeed() {
        horizontalSpeed = (int) Math.round(horizontalSpeed + thrust * Math.sin(Math.toRadians(this.angle)));
    }

    public void calculateFuelLeft() {
        this.fuel -= this.thrust * fuelUsagePerThrustUnit;
    }

    public void printState() {
        System.err.println("Rocket state: ");
        System.err.print( "x: " + x + " ");
        System.err.print( "y: " + y + " ");
        System.err.print( "vSpeed: " + verticalSpeed + " ");
        System.err.print( "hSpeed: " + horizontalSpeed + " ");
        System.err.print( "rotation: " + angle + " ");
        System.err.print( "thrust: " + thrust + " ");
        System.err.println( "fuel: " + fuel + " ");
    }

    public void calculateNewCoordinates() {
        x0 = x;
        y0 = y;
        x += horizontalSpeed;
        y += verticalSpeed;
    }

    public int fuel() {
        return fuel;
    }

    public int angle() {
        return angle;
    }

    public int thrust() {
        return thrust;
    }

    public int verticalSpeed() {
        return verticalSpeed;
    }

    public int horizontalSpeed() {
        return horizontalSpeed;
    }

    public Rocket clone() {
        return new Rocket(
                this.x,
                this.y,
                this.horizontalSpeed,
                this.verticalSpeed,
                this.fuel,
                this.angle,
                this.thrust
        );
    }

    public int yPrev() {
        return y0;
    }

    public int xPrev() {
        return x0;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public boolean isLandingCriteriasMet() {
        return  this.angle == LANDING_ANGLE_REQUIREMENT
                && VERTICAL_SPEED_LIMIT_FOR_LANDING < this.verticalSpeed
                && Math.abs(this.horizontalSpeed) <= HORIZONTAL_SPEED_LIMIT_FOR_LANDING
                ;
    }

    public void setState(RocketState state) {
        System.err.println("Setting state to " +  state);
        this.state = state;
    }

    public RocketState state() {
        return state;
    }
}

class PathFinder {

    PathRocketElement root;
    MarsSurface marsSurface;

    PathFinder(Rocket rocket, MarsSurface marsSurface) {
        this.marsSurface = marsSurface;
        root = new PathRocketElement(rocket, 0);
        root.setDistanceLanding(marsSurface.getMinDistanceToLandingZone(rocket.x(), rocket.y()));
    }

    public void generateChildren() {
        generateChildrenOf(root);
    }

    public void generateChildrenOf(PathRocketElement parent) {
        LinkedList<Rocket> angleRockets = new LinkedList<>();

        Rocket childSameAngle = parent.rocket.clone();
        angleRockets.add(childSameAngle);

        Rocket childIncrementAngle = parent.rocket.clone();
        if (childIncrementAngle.rotateLeft()){
            angleRockets.add(childIncrementAngle);
        }

        Rocket childDecrementAngle = parent.rocket.clone();
        if (childDecrementAngle.rotateRight()) {
            angleRockets.add(childDecrementAngle);
        };

        angleRockets.stream().forEach( (child) -> {
            Rocket rocketAccelerate = child.clone();
            if (rocketAccelerate.incrementThrust()) {
                parent.addChild(rocketAccelerate);
            }
            Rocket rocketDecelerate = child.clone();
            if (rocketDecelerate.decrementThrust()) {
                parent.addChild(rocketDecelerate);
            }
            parent.addChild(child); //Same thrust
        });
        parent.children.stream().forEach((child) -> {
                child.rocket.calculateNewSpeedAndPosition();
                child.setDistanceLanding(marsSurface.getMinDistanceToLandingZone(child.rocket.x(), child.rocket.y()));
        });
        parent.children.stream().sorted((child1, child2) -> (Math.min(child1.distanceLanding, child2.distanceLanding)));
    }

    public Rocket getNextRocket() {
        PathRocketElement child = root.children.get(0);
        root = child;
        return child.rocket;
    }
}

class PathRocketElement {
    final Rocket rocket;
    int depthIndex;
    int distanceLanding;
    List<PathRocketElement> children;

    PathRocketElement(Rocket rocket, int depthIndex) {
        this.rocket = rocket;
        this.depthIndex = depthIndex;
    }

    public void addChild(Rocket rocket) {
        if (children == null) { children = new LinkedList<>();}
        PathRocketElement child = new PathRocketElement(rocket, this.depthIndex+1);
        children.add(child);
    }

    public void setDistanceLanding(int distanceLanding) {
        this.distanceLanding = distanceLanding;
    }
}


class Player {

    static final int MAX_BREAK_ANGLE = 45;

    static PathFinder pathFinder;

    static final boolean debug = true;
    static MarsSurface marsSurface;

    public static int calculateNextAngle(int x, int y, int hSpeed, int vSpeed) {
        int angle;
        if (x < marsSurface.landingZone().xa()) {
            //"need to go more right"
            if (hSpeed < Rocket.HORIZONTAL_SPEED_LIMIT_FOR_LANDING * Rocket.TO_LEFT) {
                angle = -45;
            } else if (hSpeed > 2 * Rocket.HORIZONTAL_SPEED_LIMIT_FOR_LANDING * Rocket.TO_LEFT) { //TO FAST
                angle = 30;
            } else {
                angle = 0;
            }
        } else if (x > marsSurface.landingZone().xb()) {
            //"need to go more left";
            if (hSpeed > Rocket.HORIZONTAL_SPEED_LIMIT_FOR_LANDING * Rocket.TO_RIGHT) {
                angle = 45;
            } else if (hSpeed < 2 * Rocket.HORIZONTAL_SPEED_LIMIT_FOR_LANDING * Rocket.TO_RIGHT) { //TO FAST
                angle = -30;
            } else {
                angle = 0;
            }
        } else {
            angle = calculateHorizontalBreakingAngle(hSpeed, y, vSpeed);
        }
        return angle;
    }

    private static int calculateHorizontalBreakingAngle(int hSpeed, int y, int vSpeed) {
        if (isTimeToPrepareForLanding(y, vSpeed)) return 0;
        if (hSpeed > Rocket.MAX_THRUST * Math.sin(Math.toRadians(MAX_BREAK_ANGLE * Rocket.TO_LEFT)))
            return MAX_BREAK_ANGLE * Rocket.TO_LEFT;
        if (hSpeed < Rocket.MAX_THRUST * Math.sin(Math.toRadians(MAX_BREAK_ANGLE * Rocket.TO_RIGHT)))
            return MAX_BREAK_ANGLE * Rocket.TO_RIGHT;
        return (int) Math.toDegrees(Math.asin((hSpeed) / (double)Rocket.MAX_THRUST));
    }

    public static boolean isTimeToPrepareForLanding(int y, int vSpeed) {
        return marsSurface.landingZone.metersFromLandingzoneVertically(y) < (Rocket.MAX_ROTATION / MAX_BREAK_ANGLE) * vSpeed + 1; //Landing within less than possible rotations to landing position.
    }

    public static int calculateNextPower(int givenAngle, int x, int y, int vSpeed) {
        int nextPower;

        if (Math.abs(givenAngle) > 10) {
            nextPower = Rocket.MAX_THRUST;
        } else if (vSpeed < Rocket.VERTICAL_SPEED_LIMIT_FOR_LANDING + 2) { //+2 for margin
            nextPower = Rocket.MAX_THRUST;
        } else if (vSpeed > 1) {
            nextPower = 3;
        } else if (vSpeed > 0 && MarsSurface.Y_BORDER_MAX - y < 100) {
            nextPower = 0;
        } else if (getDistanceLandingSiteXFrom(x) > 0) { //maintain mode
            nextPower = Rocket.MAX_THRUST;
        } else {
            nextPower = Rocket.MAX_THRUST / 2;
        }
        return nextPower;
    }

    public static int getDistanceLandingSiteXFrom(int x) {
        int horizontalDistance = Math.abs(x - marsSurface.landingZone.xMid());
        if (horizontalDistance - marsSurface.landingZone.xRangeFromMid() < 0) {
            return 0;
        }
        return horizontalDistance;
    }

    public static void main(String args[]) {
        marsSurface = new MarsSurface();

        Scanner in = new Scanner(System.in);
        int surfaceN = in.nextInt(); // the number of points used to draw the surface of Mars.
        for (int i = 0; i < surfaceN; ++i) {
            int landX = in.nextInt(); // X coordinate of a surface point. (0 to 6999)
            int landY = in.nextInt(); // Y coordinate of a surface point. By linking all the points together in a sequential fashion, you form the surface of Mars.
            marsSurface.addPoint(new Point(landX, landY));
        }
        marsSurface.build();

        if (debug) {
            marsSurface.printState();
            marsSurface.landingZone.print();
        }

        while (true) {
            int x = in.nextInt();
            int y = in.nextInt();
            int hSpeed = in.nextInt(); // the horizontal speed (in m/s), can be negative.
            int vSpeed = in.nextInt(); // the vertical speed (in m/s), can be negative.
            int fuel = in.nextInt(); // the quantity of remaining fuel in liters.
            int rotate = in.nextInt(); // the rotation angle in degrees (-90 to 90).
            int power = in.nextInt(); // the thrust power (0 to 4).

            Rocket rocket = new Rocket(x, y, hSpeed, vSpeed, fuel, rotate, power);
            rocket = getNextRocketStepUsing(rocket);

            if (debug) {
                rocket.printState();
                System.err.println("Expected shortest distance to landingzone: " + marsSurface.getMinDistanceToLandingZone(x, y));
                System.err.println("Expected height above surface: " + marsSurface.getHeightFromSurface(x, y));
                System.err.println("RocketMove is ok: " + checkPathOfRocketIsOk(rocket));
                System.err.println( "Rocket is/has " + rocket.state());
            }
            printNextAngleAndThrustToEditor(rocket);
            if (rocket.state() != RocketState.FLYING) {
                break;
            }
        }
    }

    private static PathFinder getPathFinder(Rocket rocket) {
        if (pathFinder == null ) { pathFinder = new PathFinder(rocket, marsSurface); }
        return pathFinder;
    }

    private static Rocket getNextRocketStepUsing(Rocket rocket) {
        /*
        int angle = calculateNextAngle(rocket.x(), rocket.y(), rocket.horizontalSpeed(), rocket.verticalSpeed());
        int thrust = calculateNextPower(angle, rocket.x(), rocket.y(), rocket.verticalSpeed());
        rocket.calculateNewPositionUsing(angle, thrust);
        return rocket.clone();
         */
        getPathFinder(rocket).generateChildren();
        return pathFinder.getNextRocket();
    }

    public static void printNextAngleAndThrustToEditor(Rocket rocket) {
        System.out.println(rocket.angle() + " " + rocket.thrust()); // rotate power. rotate is the desired rotation angle. power is the desired thrust power.
    }

    public static boolean checkPathOfRocketIsOk(Rocket rocket) {
        int xa = rocket.xPrev();
        int xb = rocket.x();
        int ya = rocket.yPrev();
        int yb = rocket.y();
        int x, y;

        double dy_dx = (double)  rocket.verticalSpeed()/rocket.horizontalSpeed();
        double xiStepLength = (-1 <= dy_dx && dy_dx <= 1) ? 1 : 1.0 / Math.abs(dy_dx); //No teleporting.

        for (double xi = 0; xi < Math.abs(xb - xa); xi += xiStepLength) {
            if (xa < xb) {
                y = (int) Math.round(ya + xi * dy_dx);
                x = xa + (int) xi;
            } else {
                y = (int) Math.round(yb + xi * dy_dx);
                x = xb + (int) xi;
            }
            if (isRocketLanding(x, y, rocket)) {
                break;
            }
        }
        return rocket.state() != RocketState.CRASHED && rocket.state() != RocketState.LOST_IN_SPACE;
    }

    public static boolean isGroundContact(int x, int y) {
        return (marsSurface.getHeightFromSurface(x,y) < 1);
    }

    public static boolean isLostInSpace(int x, int y) {
        return !marsSurface.isWithinBorder(x,y);
    }

    public static boolean isRocketLanding(int x, int y, Rocket rocket) {
         if (isLostInSpace(x, y)) {
             rocket.setState(RocketState.LOST_IN_SPACE);
         }
         else if (isGroundContact(x, y)) {
             if ( isXInLandingZoneHorizontally(x)
                     && isYInLandingZoneVertically(y)
                     && rocket.isLandingCriteriasMet()
             ) {
                 rocket.setState(RocketState.LANDED);
             } else {
                 rocket.setState(RocketState.CRASHED);
             }
         }
         return rocket.state() != RocketState.FLYING;
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
    private static boolean isYInLandingZoneVertically(int y) {
        int metersFromY = marsSurface.landingZone().metersFromLandingzoneVertically(y);
        return Rocket.VERTICAL_SPEED_LIMIT_FOR_LANDING < metersFromY && metersFromY < 1;
    }

    private static boolean isXInLandingZoneHorizontally(int x) {
        return marsSurface.landingZone.isXWithinLandingZone(x);
    }
}