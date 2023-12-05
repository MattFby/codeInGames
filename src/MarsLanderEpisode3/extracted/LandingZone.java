package MarsLanderEpisode3.extracted;

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
     * @param x
     * @param acceptableDistanceFromZoneGivenSpeed - Adds an extra acceptable area to the left or right (negative) the spaceship may consider landing given speed and path.
     * @return
     */
    public boolean isXWithinLandingZone(int x, int acceptableDistanceFromZoneGivenSpeed) {
        return Math.min(xa, xa + acceptableDistanceFromZoneGivenSpeed) < x && x < Math.max(xb, xb + acceptableDistanceFromZoneGivenSpeed);
    }

    public int getDistanceLandingSiteXFrom(int x) {
        int horizontalDistance = Math.abs(x - xMid);
        if (horizontalDistance - xRangeFromMid < 0) {
            return 0;
        }
        return horizontalDistance;
    }

    public boolean isLandingZone(int x, int y) {
        return isXWithinLandingZone(x) && this.y == y;
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