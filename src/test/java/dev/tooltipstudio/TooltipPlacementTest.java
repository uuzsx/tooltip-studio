package dev.tooltipstudio;

import dev.tooltipstudio.render.TooltipPlacement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TooltipPlacementTest {
    @Test void cursorRelativeOffsetsHaveEqualGapsOnBothSides() {
        int screen = 640, size = 180, mouseRightSide = 70, mouseLeftSide = 570;
        for (int offset : new int[]{-8, 0, 12, 24}) {
            int right = TooltipPlacement.horizontal(mouseRightSide + 12, size, screen, mouseRightSide, offset, true);
            int left = TooltipPlacement.horizontal(mouseLeftSide - 12 - size, size, screen, mouseLeftSide, offset, true);
            assertEquals(12 + offset, right - mouseRightSide);
            assertEquals(12 + offset, mouseLeftSide - (left + size));
        }
    }

    @Test void screenModeRetainsLegacyDirectionAndZeroOffsetIsUnchanged() {
        for (int mouse : new int[]{70, 570}) {
            int preferred = mouse == 70 ? mouse + 12 : mouse - 12 - 180;
            for (int offset : new int[]{-24, 0, 24})
                assertEquals(TooltipPlacement.offset(preferred, 180, 640, offset),
                        TooltipPlacement.horizontal(preferred, 180, 640, mouse, offset, false));
            assertEquals(TooltipPlacement.offset(preferred, 180, 640, 0),
                    TooltipPlacement.horizontal(preferred, 180, 640, mouse, 0, true));
        }
    }

    @Test void cursorSideComesFromOriginalPlacementEvenWhenClampingStraddlesMouse() {
        // This tooltip is clamped to x=4 even though its natural side is left.
        assertEquals(24, TooltipPlacement.horizontal(4, 500, 640, 200, -20, true));
        assertEquals(4, TooltipPlacement.horizontal(4, 500, 640, 200, 20, true));
        assertEquals(4, TooltipPlacement.horizontal(4, 632, 640, 200, -4096, true));
    }

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
