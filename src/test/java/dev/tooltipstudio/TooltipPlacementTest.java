package dev.tooltipstudio;

import dev.tooltipstudio.render.TooltipPlacement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TooltipPlacementTest {
    @Test void zeroOffsetPreservesOldPlacementIncludingOffscreenPreferredPositions() {
        for (int screen : new int[]{320, 640, 1920}) {
            for (int size : new int[]{80, screen - 8}) {
                for (int preferred : new int[]{-30, 0, 4, 150, screen + 20}) {
                    assertEquals(Math.max(4, Math.min(screen - size - 4, preferred)),
                            TooltipPlacement.offset(preferred, size, screen, 0));
                }
            }
        }
    }

    @Test void positiveAndNegativeOffsetsMoveByExactGuiPixelsWhenThereIsRoom() {
        assertEquals(124, TooltipPlacement.offset(100, 120, 640, 24));
        assertEquals(80, TooltipPlacement.offset(100, 120, 640, -20));
        // visualSize has already been scaled by the renderer; the offset itself stays unchanged.
        assertEquals(124, TooltipPlacement.offset(100, 60, 640, 24));
    }

    @Test void allEdgesStayVisibleAndOffsetsCanMoveAwayFromClampedPositions() {
        assertEquals(4, TooltipPlacement.offset(100, 120, 640, -4096));
        assertEquals(516, TooltipPlacement.offset(100, 120, 640, 4096));
        assertEquals(24, TooltipPlacement.offset(-100, 120, 640, 20));
        assertEquals(496, TooltipPlacement.offset(900, 120, 640, -20));
        assertEquals(4, TooltipPlacement.offset(100, 632, 640, -20));
        assertEquals(4, TooltipPlacement.offset(100, 632, 640, 20));
    }
}
