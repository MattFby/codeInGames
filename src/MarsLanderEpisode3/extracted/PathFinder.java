package MarsLanderEpisode3.extracted;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class PathFinder {

    static final int MAX_BREAK_ANGLE = 45;

    PathRocketElement root;
    PathRocketElement current;
    final MarsSurface marsSurface;

    int depth = 0;
    final int childrenGenerationPerTurn = 10000;
    int responsesGiven = 0;
    boolean isLanded = false;


    PathFinder(RocketState rocketState, MarsSurface marsSurface) {
        this.marsSurface = marsSurface;
        createRootElement(rocketState, marsSurface);
    }

    private void createRootElement(RocketState rocketState, MarsSurface marsSurface) {
        root = new PathRocketElement(rocketState, 0, null);
        root.setDistanceToLandingZone(marsSurface.getMinDistanceToLandingZone(rocketState.x(), rocketState.y()));
        root.depthIndex = 0;
        current = root;
    }

    public void findPath() {
        generateChildrenTreeFrom(current);
        root.removeUnwantedChildren();
    }

    /**
     * Greedy Depth first approach with limited generation at a time due to time issues.
     */
    public void generateChildrenTreeFrom(PathRocketElement element) {
        current = element;
        if (!isLanded) {
            for (int childGenerationIndex = 0; childGenerationIndex < childrenGenerationPerTurn; ++childGenerationIndex ) {
                generateChildrenOf(current);
                while (current.children.isEmpty()) {
                    current = current.parent;
                    current.children.remove(current.getBestChild());
                }
                current = current.getBestChild();
                if (current.rocketState.status() == RocketStatus.LANDED) {
                    isLanded = true;
                    break;
                }
            }
        }
    }

    public void generateChildrenOf(PathRocketElement parentElement) {
        LinkedList<RocketState> angleRocketStates = new LinkedList<>();
        generateChildrenWithAvailableAnglesFrom(parentElement, angleRocketStates);
        generateChildrenWithAvailableThrustsUsingAngles(parentElement, angleRocketStates);
        calculateChildrenNextCoordinatesUsingAngleAndThrust(parentElement);
        validateChildrenOf(parentElement);
        sortChildrenOf(parentElement);
    }

    private static void generateChildrenWithAvailableThrustsUsingAngles(PathRocketElement parent, LinkedList<RocketState> angleRocketStates) {
        angleRocketStates.stream().forEach( (child) -> {
            RocketState accelerate = getClone(child);
            if (accelerate.incrementThrust()) {
                parent.addChild(accelerate);
            }
            RocketState deccelerate = getClone(child);
            if (deccelerate.decrementThrust()) {
                parent.addChild(deccelerate);
            }
            parent.addChild(child); //Same thrust
        });
    }

    private static RocketState getClone(RocketState rocketState) {
        try {
            return (RocketState) rocketState.clone();
        } catch (CloneNotSupportedException e) {
            System.err.println("failed to clone rocketstate, trying copying strategy");
            return rocketState.copy();
        }
    }

    private static void generateChildrenWithAvailableAnglesFrom(PathRocketElement parent, LinkedList<RocketState> angleRocketStates) {
        RocketState sameAngle = getClone(parent.rocketState);
        angleRocketStates.add(sameAngle);

        RocketState rocket = getClone(parent.rocketState);
        if (rocket.rotateLeft()) {
            angleRocketStates.add(rocket);
        }
        rocket = getClone(parent.rocketState);
        if (rocket.rotateRight()) {
            angleRocketStates.add(rocket);
        }
    }

    private void calculateChildrenNextCoordinatesUsingAngleAndThrust(PathRocketElement parent) {
        for (PathRocketElement child : parent.children) {
            child.rocketState.calculateNewSpeedAndPosition();
            if (validateRocketPathAndSetStatus(child.rocketState)) {
                child.setDistanceToLandingZone(marsSurface.getMinDistanceToLandingZone(child.rocketState.x(), child.rocketState.y()));
            } else {
                child.setDistanceToLandingZone(Integer.MAX_VALUE);
            }
        }
    }
    private void validateChildrenOf(PathRocketElement parent) {
        parent.children.stream().filter((child) ->
                    isRequirementsMet(child.rocketState)
        ).collect(Collectors.toList());
    }

    private void sortChildrenOf(PathRocketElement parent) {
        List<PathRocketElement> landedChild = parent.children.stream().filter(child -> child.rocketState.status() == RocketStatus.LANDED).collect(Collectors.toList());
        if (!landedChild.isEmpty()) {
            parent.children = landedChild;
        } else {
            parent.children.stream().sorted((child1, child2) -> (Math.min(child1.distanceToLandingZone, child2.distanceToLandingZone)));
        }
    }

    private boolean isRequirementsMet(RocketState rocket) {
        return isWithinVerticalSpeedLimit(rocket.verticalSpeed())
            && isWithinHorizontalSpeedLimit(rocket.horizontalSpeed())
            && isRocketStatusOk(rocket.status())
            ;
    }

    private static boolean isWithinHorizontalSpeedLimit(int horizontalSpeed) {
        return horizontalSpeed < RocketState.HORIZONTAL_SPEED_LIMIT;
    }

    private static boolean isWithinVerticalSpeedLimit(int verticalSpeed) {
        return verticalSpeed < RocketState.VERTICAL_SPEED_LIMIT;
    }

    public RocketState getNextRocket() {
        PathRocketElement child = root.children.get(0);
        root = child;
        responsesGiven += 1;
        return child.rocketState;
    }

    public boolean validateRocketPathAndSetStatus(RocketState rocketState) {
        int ya = rocketState.yLatestStepOrigin();
        int xa = rocketState.xLatestStepOrigin();
        int yb = rocketState.y();
        int xb = rocketState.x();

        double dy_dx = (double)  rocketState.verticalSpeed()/ rocketState.horizontalSpeed();
        double xiStepLength = (-1 <= dy_dx && dy_dx <= 1) ? 1 : 1.0 / Math.abs(dy_dx); //No teleporting.

        if (!isEndDestinationOk(rocketState) && !acceptableDistanceFromLandingZoneForPathEvaluation(rocketState)) {
            rocketState.setStatus(getRocketStatusAt(xb, yb, rocketState));
            return false;
        }
        return validatePathAndUpdateStatus(xa, xb, ya, yb, dy_dx, xiStepLength, rocketState);
    }

    private boolean acceptableDistanceFromLandingZoneForPathEvaluation(RocketState rocketState) {
        return marsSurface.isYInLandingZoneVertically(rocketState.y()) &&
                marsSurface.landingZone.isXWithinLandingZone(rocketState.x(), (rocketState.horizontalSpeed() < 0) ?
                                RocketState.HORIZONTAL_SPEED_LIMIT_FOR_LANDING* RocketState.TO_RIGHT : RocketState.HORIZONTAL_SPEED_LIMIT_FOR_LANDING* RocketState.TO_LEFT);
    }

    public boolean validatePathAndUpdateStatus(int xa, int xb, int ya, int yb, double dy_dx, double xiStepLength, RocketState rocketState) {
        int x, y;
        for (double xi = 0; xi < Math.abs(xb - xa); xi += xiStepLength) {
            if (xa < xb) {
                y = (int) Math.round(ya + xi * dy_dx);
                x = xa + (int) xi;
            } else {
                y = (int) Math.round(yb + xi * dy_dx);
                x = xb + (int) xi;
            }
            RocketStatus newStatus = getRocketStatusAt(x, y, rocketState);
            if (newStatus != rocketState.status()) {
                rocketState.setStatus(newStatus);
                break;
            }
        }
        return isRocketStatusOk(rocketState.status());
    }

    public boolean isEndDestinationOk(RocketState rocketState) {
        return isRocketStatusOk(getRocketStatusAt(rocketState.x(), rocketState.y(), rocketState));
    }

    public boolean isRocketStatusOk(RocketStatus state) {
        return state == RocketStatus.FLYING
                || state == RocketStatus.LANDED;
    }

    public boolean isLostInSpace(int x, int y) {
        return !marsSurface.isWithinMapsBorders(x,y);
    }

    public RocketStatus getRocketStatusAt(int x, int y, RocketState rocketState) {
        RocketStatus newState = rocketState.status();
        if (isLostInSpace(x, y)) {
            newState = RocketStatus.LOST_IN_SPACE;
        }
        else if (marsSurface.isGroundContact(x, y)) {
            if ( marsSurface.isXInLandingZoneHorizontally(x)
                    && marsSurface.isYInLandingZoneVertically(y)
                    && rocketState.isLandingCriteriasMet()
            ) {
                newState = RocketStatus.LANDED;
            } else {
                newState = RocketStatus.CRASHED;
            }
        }
        return newState;
    }

    public int calculateHorizontalBreakingAngle(int hSpeed, int y, int vSpeed) {
        if (isTimeToPrepareForLanding(y, vSpeed)) return RocketState.LANDING_ANGLE;
        if (hSpeed > RocketState.MAX_THRUST * Math.sin(Math.toRadians(MAX_BREAK_ANGLE * RocketState.TO_LEFT)))
            return MAX_BREAK_ANGLE * RocketState.TO_LEFT;
        if (hSpeed < RocketState.MAX_THRUST * Math.sin(Math.toRadians(MAX_BREAK_ANGLE * RocketState.TO_RIGHT)))
            return MAX_BREAK_ANGLE * RocketState.TO_RIGHT;
        return (int) Math.toDegrees(Math.asin((hSpeed) / (double) RocketState.MAX_THRUST));
    }

    public boolean isTimeToPrepareForLanding(int y, int vSpeed) {
        return marsSurface.landingZone.metersFromLandingzoneVertically(y) < (RocketState.MAX_ROTATION / MAX_BREAK_ANGLE) * vSpeed + 1; //Landing within less than possible rotations to landing position.
    }

}