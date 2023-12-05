package MarslanderEpisode2;

import java.util.*;

class Player {
    static final double Y_BORDER_MAX = 3000.0;

    static final double MAX_THRUST = 4;

    static final int MAX_SPEED_ALLOWED_FOR_LANDING_VERTICALLY = -40; //(- means falling)
    static final int MAX_SPEED_ALLOWED_FOR_LANDING_HORIZONTALLY = 20; //(- means right)
    static final int MAX_BREAK_ANGLE = 45;
    static final int ANGLE_ROTATION_MAX = 15;
    static final int TO_LEFT = 1;
    static final int TO_RIGHT = -1;

    static int prevLandX, prevLandY, landingSiteXa = 0, landingSiteXb = 0, landingSiteXmid, landingSiteH = 0, landingSiteRangeFromMid;

    static final boolean debug = true;

    public static int calculateNextAngle(int x, int y, int hSpeed, int vSpeed) {
        int angle;
        if (x < landingSiteXa) {
            //"need to go more right"
            if (hSpeed < MAX_SPEED_ALLOWED_FOR_LANDING_HORIZONTALLY * TO_LEFT) {
                angle = -45;
            } else if (hSpeed > 2 * MAX_SPEED_ALLOWED_FOR_LANDING_HORIZONTALLY * TO_LEFT) { //TO FAST
                angle = 30;
            } else {
                angle = 0;
            }
        }
        else if (x > landingSiteXb) {
            //"need to go more left";
            if (hSpeed > MAX_SPEED_ALLOWED_FOR_LANDING_HORIZONTALLY * TO_RIGHT) {
                angle = 45;
            } else if (hSpeed < 2 * MAX_SPEED_ALLOWED_FOR_LANDING_HORIZONTALLY * TO_RIGHT) { //TO FAST
                angle = -30;
            } else {
                angle = 0;
            }
        } else {
            angle  = calculateHorizontalBreakingAngle(hSpeed, y, vSpeed);
        }
        return angle;
    }

    private static int calculateHorizontalBreakingAngle(int hSpeed, int y, int vSpeed) {
        if (isTimeToPrepareForLanding(y, vSpeed)) return 0;
        if( hSpeed > MAX_THRUST*Math.sin(Math.toRadians(MAX_BREAK_ANGLE * TO_LEFT)) ) return MAX_BREAK_ANGLE * TO_LEFT;
        if( hSpeed < MAX_THRUST*Math.sin(Math.toRadians(MAX_BREAK_ANGLE * TO_RIGHT)) ) return MAX_BREAK_ANGLE * TO_RIGHT;
        return (int)Math.toDegrees(Math.asin((hSpeed)/MAX_THRUST));
    }

    public static boolean isTimeToPrepareForLanding(int y, int vSpeed) {
        return getDistanceLandingSiteYfrom(y) < (ANGLE_ROTATION_MAX / MAX_BREAK_ANGLE) * vSpeed +1; //Landing within less than possible rotations to landing position.
    }

    public static int calculateNextPower(int givenAngle, int x, int y, int vSpeed) {
        int nextPower;

        if (Math.abs(givenAngle) > 10) {
            nextPower = (int) MAX_THRUST;
        }
        else if (vSpeed < MAX_SPEED_ALLOWED_FOR_LANDING_VERTICALLY +2) { //+2 for margin
            nextPower = (int) MAX_THRUST;
        }
        else if (vSpeed > 1) {
            nextPower = 3;
        }
        else if (vSpeed > 0 && Y_BORDER_MAX - y < 100) {
            nextPower = 0;
        }
        else if (getDistanceLandingSiteXFrom(x) > 0) { //maintain mode
            nextPower = (int) MAX_THRUST;
        }
        else {
            nextPower = (int) MAX_THRUST/2;
        }
        return nextPower;
    }

    public static int getDistanceLandingSiteYfrom(int y) {
        return y-landingSiteH;
    }

    public static int getDistanceLandingSiteXFrom(int x) {
        int horizontalDistance = Math.abs(x - landingSiteXmid);
        if (horizontalDistance-landingSiteRangeFromMid < 0) {
            return 0;
        }
        return horizontalDistance;
    }

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int surfaceN = in.nextInt(); // the number of points used to draw the surface of Mars.

        prevLandX = in.nextInt();
        prevLandY = in.nextInt();

        for (int i = 1; i < surfaceN; ++i) {
            int landX = in.nextInt(); // X coordinate of a surface point. (0 to 6999)
            int landY = in.nextInt(); // Y coordinate of a surface point. By linking all the points together in a sequential fashion, you form the surface of Mars.

            if (landY == prevLandY) {
                landingSiteXa = prevLandX;
                landingSiteXb = landX;
                landingSiteH  = landY;
            }
            prevLandY = landY;
            prevLandX = landX;
        }
        landingSiteXmid = landingSiteXa+(landingSiteXb-landingSiteXa)/2;
        landingSiteRangeFromMid = (landingSiteXb-landingSiteXa)/2;

        if(debug) {
            System.err.println("landingsite: " + landingSiteXa + " <= x <= " + landingSiteXb);
            System.err.println("landingsiteMid: " + landingSiteXmid + " +- " + landingSiteRangeFromMid);
        }

        while (true) {
            int x = in.nextInt();
            int y = in.nextInt();
            int hSpeed = in.nextInt(); // the horizontal speed (in m/s), can be negative.
            int vSpeed = in.nextInt(); // the vertical speed (in m/s), can be negative.
            int fuel = in.nextInt(); // the quantity of remaining fuel in liters.
            int rotate = in.nextInt(); // the rotation angle in degrees (-90 to 90).
            int power = in.nextInt(); // the thrust power (0 to 4).

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");
            int angle = calculateNextAngle(x, y, hSpeed, vSpeed);
            int thrust = calculateNextPower(angle, x, y, vSpeed);
            System.out.println(angle + " " + thrust); // rotate power. rotate is the desired rotation angle. power is the desired thrust power.
        }
    }
}

/*
    public static int getPredictedVerticalSpeedGiven(int angle, int power, int vSpeed) {
        double predictedNewSpeed = vSpeed - gravity + power*Math.cos(Math.toRadians(angle));
        return (int) predictedNewSpeed;
    }

    public static int getPredictedHorizontalSpeedGiven(int angle, int power, int hSpeed) {
        double predictedNextHorizontalSpeed = hSpeed + power * Math.sin(Math.toRadians(angle));
        return (int) predictedNextHorizontalSpeed;
    }

enum horizontalState {
    Accelerate,
    Maintain,
    Deccelerate;
}

enum verticalState {
    Accelerate,
    Maintain,
    Fall,
    Off
}

    static final int left = 1;
    static final int right = -1;
    static final double angleStep = 15.0;
    static final int powerStep = 1;
    static final double gravity = 3.711; //m/s^2
    static final double minX = 0.0;
    static final double maxX = 7000.0;
    static final double minY = 0.0;
    static final double angleMax = 90.0;
    static final int maxHSpeedForLanding = 20;

 */