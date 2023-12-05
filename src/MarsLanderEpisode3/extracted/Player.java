package MarsLanderEpisode3.extracted;

import java.util.Scanner;

class Player {

    static PathFinder pathFinder;

    static final boolean debug = true;
    static MarsSurface marsSurface;


    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        readInAndCreateMarsSurface(in);
        RocketState rocketState;
        do {
            rocketState = readNextStateOfRocket(in);
            rocketState = getNextRocketStepUsing(rocketState);
            printStateIfInDebug(rocketState);
            printAngleAndPowerToEditor(rocketState);
            System.err.println(rocketState.x() + " " + rocketState.y() + " " + rocketState.horizontalSpeed() + " " + rocketState.verticalSpeed() + " " + rocketState.fuel() + " " + rocketState.angle() + " " + rocketState.thrust());
        } while (rocketState != null && rocketState.status() == RocketStatus.FLYING);
    }

    private static void printStateIfInDebug(RocketState rocketState) {
        if (debug) {
            rocketState.printState();
            System.err.println("Expected shortest distance to landingzone: " + marsSurface.getMinDistanceToLandingZone(rocketState.xLatestStepOrigin(), rocketState.yLatestStepOrigin()));
            System.err.println("Expected height above surface: " + marsSurface.getHeightFromSurface(rocketState.xLatestStepOrigin(), rocketState.yLatestStepOrigin()));
            System.err.println("RocketMove is ok: " + pathFinder.validateRocketPathAndSetStatus(rocketState));
            System.err.println("Rocket is/has " + rocketState.status());
        }
    }

    private static RocketState readNextStateOfRocket(Scanner in) {
        int x = in.nextInt();
        int y = in.nextInt();
        int hSpeed = in.nextInt(); // the horizontal speed (in m/s), can be negative.
        int vSpeed = in.nextInt(); // the vertical speed (in m/s), can be negative.
        int fuel = in.nextInt(); // the quantity of remaining fuel in liters.
        int rotate = in.nextInt(); // the rotation angle in degrees (-90 to 90).
        int power = in.nextInt(); // the thrust power (0 to 4).

        return new RocketState(x, y, hSpeed, vSpeed, fuel, rotate, power);
    }

    private static void readInAndCreateMarsSurface(Scanner in) {
        marsSurface = new MarsSurface();

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
    }

    private static PathFinder getPathFinder(RocketState rocketState) {
        if (pathFinder == null ||
                !pathFinder.root.rocketState.equals(rocketState)) //values are wrong.
        {
            pathFinder = new PathFinder(rocketState, marsSurface);
        }
        return pathFinder;
    }

    private static RocketState getNextRocketStepUsing(RocketState rocketState) {
        getPathFinder(rocketState).findPath();
        return pathFinder.getNextRocket();
    }

    public static void printAngleAndPowerToEditor(RocketState rocketState) {
        System.out.println(rocketState.angle() + " " + rocketState.thrust()); // rotate power. rotate is the desired rotation angle. power is the desired thrust power.
    }
}
