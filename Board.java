import java.util.Arrays;

public class Board {
    public static final int PITS_PER_SIDE = 6;
    public static final int TOTAL_PITS = 14;
    public static final int PLAYER1_MANCALA = 6;
    public static final int PLAYER2_MANCALA = 13;

    private int[] pits;
    private StringBuilder moveHistory = new StringBuilder();

    public Board() {
        pits = new int[TOTAL_PITS];
        reset();
    }

    public Board(int[] pits) {
        this.pits = Arrays.copyOf(pits, TOTAL_PITS);
    }

    public void setMoveHistory(String history) {
        this.moveHistory = new StringBuilder(history);
    }

    public String getPlayString() {
        return moveHistory.toString();
    }

    public void recordMove(int holeIndex, int playerIndex) {
        char moveChar;
        if (playerIndex == 1) {
            moveChar = (char) ('A' + holeIndex);
        } else {
            moveChar = (char) ('a' + (holeIndex - 7));
        }
        moveHistory.append(moveChar);
    }

    public void copyPitsTo(int[] dest) {
        System.arraycopy(pits, 0, dest, 0, TOTAL_PITS);
    }

    public void copyPitsFrom(int[] src) {
        System.arraycopy(src, 0, pits, 0, TOTAL_PITS);
    }

    public String getStateKey() {
        return Arrays.toString(pits);
    }

    public void reset() {
        moveHistory = new StringBuilder();
        for (int i = 0; i < TOTAL_PITS; i++) {
            if (i == PLAYER1_MANCALA || i == PLAYER2_MANCALA) {
                pits[i] = 0;
            } else {
                pits[i] = 4;
            }
        }
    }

    public int getPits(int index) {
        return pits[index];
    }

    /**
     * Executes a move for the given player starting from the specified hole.
     * @param holeIndex The index of the hole to empty (0-5 for P1, 7-12 for P2).
     * @param playerIndex 1 for User, 2 for Computer.
     * @return true if the player gets an extra turn.
     */
    public boolean move(int holeIndex, int playerIndex) {
        int stones = pits[holeIndex];
        pits[holeIndex] = 0;

        int currentIndex = holeIndex;
        int opponentMancala = (playerIndex == 1) ? PLAYER2_MANCALA : PLAYER1_MANCALA;
        int playerMancala = (playerIndex == 1) ? PLAYER1_MANCALA : PLAYER2_MANCALA;

        while (stones > 0) {
            currentIndex = (currentIndex + 1) % TOTAL_PITS;
            if (currentIndex == opponentMancala) {
                continue;
            }
            pits[currentIndex]++;
            stones--;
        }

        // Rule: If the last piece lands in player's mancala, they go again.
        if (currentIndex == playerMancala) {
            return true;
        }

        // Rule: Capture
        // If last piece lands in player's empty hole, capture adjacent opponent's pieces.
        if (pits[currentIndex] == 1 && isPlayerSide(currentIndex, playerIndex)) {
            int oppositeIndex = 12 - currentIndex;
            if (pits[oppositeIndex] > 0) {
                pits[playerMancala] += pits[currentIndex] + pits[oppositeIndex];
                pits[currentIndex] = 0;
                pits[oppositeIndex] = 0;
            }
        }

        return false;
    }

    private boolean isPlayerSide(int index, int playerIndex) {
        if (playerIndex == 1) {
            return index >= 0 && index < PLAYER1_MANCALA;
        } else {
            return index > PLAYER1_MANCALA && index < PLAYER2_MANCALA;
        }
    }

    public boolean isGameOver() {
        return isSideEmpty(0, 5) || isSideEmpty(7, 12);
    }

    private boolean isSideEmpty(int start, int end) {
        for (int i = start; i <= end; i++) {
            if (pits[i] > 0) return false;
        }
        return true;
    }

    public void collectRemaining() {
        for (int i = 0; i < 6; i++) {
            pits[PLAYER1_MANCALA] += pits[i];
            pits[i] = 0;
        }
        for (int i = 7; i < 13; i++) {
            pits[PLAYER2_MANCALA] += pits[i];
            pits[i] = 0;
        }
    }

    public void display() {
        System.out.println("\n      " + pits[12] + "   " + pits[11] + "   " + pits[10] + "   " + pits[9] + "   " + pits[8] + "   " + pits[7]);
        System.out.println("  " + pits[13] + "                          " + pits[6]);
        System.out.println("      " + pits[0] + "   " + pits[1] + "   " + pits[2] + "   " + pits[3] + "   " + pits[4] + "   " + pits[5]);
        System.out.println("      A   B   C   D   E   F\n");
    }

    public int getPlayer1Score() {
        return pits[PLAYER1_MANCALA];
    }

    public int getPlayer2Score() {
        return pits[PLAYER2_MANCALA];
    }
}
