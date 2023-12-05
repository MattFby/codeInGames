package MarsLanderEpisode3.extracted;

class RocketState implements Cloneable {
    private static final int MIN_TRUST = 0;
    final static int MAX_ROTATION = 90;
    final static int MAX_STEP_ROTATION = 15;
    final static int MAX_THRUST = 1;
    final static int MAX_STEP_THRUST = 1;
    final static double GRAVITY = 3.711; //m/s^2
    final static int fuelUsagePerThrustUnit = 1;

    public static final int VERTICAL_SPEED_LIMIT = 60; //Randomly set value
    public static final int HORIZONTAL_SPEED_LIMIT = 40; //Randomly set value
    final static int VERTICAL_SPEED_LIMIT_FOR_LANDING = -40;
    final static int HORIZONTAL_SPEED_LIMIT_FOR_LANDING = 20;
    final static int LANDING_ANGLE = 0;
    public static final int TO_LEFT = 1;
    public static final int TO_RIGHT = -1;

    private int fuel,
            angle,
            thrust,
            verticalSpeed,
            horizontalSpeed,
            xDestination, x0,
            yDestination, y0;

    private RocketStatus state = RocketStatus.FLYING;

    RocketState(int x, int y, int hSpeed, int vSpeed, int fuel, int rotate, int power) {
        this(x, x, y, y, hSpeed, vSpeed, fuel, rotate, power);
    }

    RocketState(int xDestination, int x0, int yDestination, int y0, int hSpeed, int vSpeed, int fuel, int rotate, int power) {
        this.xDestination = xDestination;
        this.x0 = x0;
        this.yDestination = yDestination;
        this.y0 = y0;
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
        calculateNewExpectedCoordinates();
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
        System.err.print( "x: " + xDestination + " ");
        System.err.print( "y: " + yDestination + " ");
        System.err.print( "vSpeed: " + verticalSpeed + " ");
        System.err.print( "hSpeed: " + horizontalSpeed + " ");
        System.err.print( "rotation: " + angle + " ");
        System.err.print( "thrust: " + thrust + " ");
        System.err.println( "fuel: " + fuel + " ");
    }

    public void calculateNewExpectedCoordinates() {
        x0 = xDestination;
        y0 = yDestination;
        xDestination = x0 + horizontalSpeed;
        yDestination = y0 + verticalSpeed;
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

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public RocketState copy() {
        return new RocketState(
                this.xDestination,
                this.x0,
                this.yDestination,
                this.y0,
                this.horizontalSpeed,
                this.verticalSpeed,
                this.fuel,
                this.angle,
                this.thrust
        );
    }

    /**
     * This would be origin of y if 0-1 action or move is performed,
     * but if multiple moves are performed, this will return the previous y position.
     * hence only giving the last step origin.
     */
    public int yLatestStepOrigin() {
        return y0;
    }

    /**
     * This would be origin of y if 0-1 action or move is performed,
     * but if multiple moves are performed, this will return the previous y position.
     * hence only giving the last step origin.
     */
    public int xLatestStepOrigin() {
        return x0;
    }

    public int x() {
        return xDestination;
    }

    public int y() {
        return yDestination;
    }

    public boolean isLandingCriteriasMet() {
        return  this.angle == LANDING_ANGLE
                && VERTICAL_SPEED_LIMIT_FOR_LANDING < this.verticalSpeed
                && Math.abs(this.horizontalSpeed) <= HORIZONTAL_SPEED_LIMIT_FOR_LANDING
                ;
    }

    public void setStatus(RocketStatus state) {
        this.state = state;
    }

    public RocketStatus status() {
        return state;
    }
}