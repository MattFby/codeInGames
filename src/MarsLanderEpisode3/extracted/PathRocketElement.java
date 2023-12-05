package MarsLanderEpisode3.extracted;

import java.util.LinkedList;
import java.util.List;

class PathRocketElement {
    final RocketState rocketState;
    int depthIndex;
    int distanceToLandingZone;

    final PathRocketElement parent;
    List<PathRocketElement> children;

    PathRocketElement(RocketState rocketState, int depthIndex, PathRocketElement parent) {
        this.rocketState = rocketState;
        this.depthIndex = depthIndex;
        this.parent = parent;
    }

    public void addChild(RocketState rocketState) {
        if (children == null) { children = new LinkedList<>();}
        PathRocketElement child = new PathRocketElement(rocketState, this.depthIndex+1, this);
        children.add(child);
    }

    public void setDistanceToLandingZone(int distanceLanding) {
        this.distanceToLandingZone = distanceLanding;
    }

    public void removeUnwantedChildren() {
        if (children != null) {
            for (PathRocketElement child : children) {
                child.removeUnwantedChildren();
                if (child.children != null && child.children.isEmpty()) {
                    children.remove(child);
                }
            }
        }
    }

    public PathRocketElement getParent() {
        return parent;
    }

    public PathRocketElement getBestChild() {
        return children.get(0);
    }
}