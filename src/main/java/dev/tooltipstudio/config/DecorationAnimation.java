package dev.tooltipstudio.config;

/** Consecutive equal-size frames in a PNG strip. frameTime is measured in 50 ms ticks. */
public record DecorationAnimation(int frames, Integer frameTime, String direction) {
    public int ticksPerFrame() { return frameTime == null ? 2 : frameTime; }
    public boolean horizontal() { return "horizontal".equals(direction); }

    public void validate(Style.Region first, int textureWidth, int textureHeight) {
        Style.require(frames >= 1 && frames <= 256, "animation.frames must be 1..256");
        Style.require(ticksPerFrame() >= 1 && ticksPerFrame() <= 1200, "animation.frameTime must be 1..1200 ticks");
        Style.require(direction == null || horizontal() || "vertical".equals(direction),
                "animation.direction must be vertical or horizontal");
        long right = (long) first.u() + first.width() * (horizontal() ? (long) frames : 1L);
        long bottom = (long) first.v() + first.height() * (horizontal() ? 1L : (long) frames);
        Style.require(right <= textureWidth && bottom <= textureHeight, "animation frames lie outside the texture");
    }

    public Style.Region regionAt(Style.Region first, long elapsedMillis) {
        int frame = (int) (Math.max(0, elapsedMillis) / (50L * ticksPerFrame()) % frames);
        return new Style.Region(first.u() + (horizontal() ? frame * first.width() : 0),
                first.v() + (horizontal() ? 0 : frame * first.height()), first.width(), first.height());
    }
}
