import java.util.*;

public class GameSearcher {
    private Map<BoardState, GameResult> memo = new HashMap<>();
    private int targetDepth = -1;
    private long nodesVisited = 0;
    private long lastProgressReport = System.currentTimeMillis();

    public enum GameResult {
        P1_WINS, P2_WINS, TIE;

        public String toForcedString() {
            switch (this) {
                case P1_WINS: return "P1_CAN_FORCE_WIN";
                case P2_WINS: return "P2_CAN_FORCE_WIN";
                default: return "CAN_FORCE_TIE";
            }
        }
    }

    /**
     * Compact representation of the board state for memoization.
     * Packs 14 pits (max 48 stones each -> 6 bits) and player index into two longs.
     */
    private static class BoardState {
        private final long low;
        private final long high;

        BoardState(Board board, int playerIndex) {
            long l = 0;
            // First 10 pits (0-9)
            for (int i = 0; i < 10; i++) {
                l |= ((long) (board.getPits(i) & 0x3F)) << (i * 6);
            }
            this.low = l;
            long h = 0;
            // Remaining 4 pits (10-13)
            for (int i = 10; i < 14; i++) {
                h |= ((long) (board.getPits(i) & 0x3F)) << ((i - 10) * 6);
            }
            // Player index (1 or 2)
            h |= (long) (playerIndex - 1) << 24;
            this.high = h;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BoardState)) return false;
            BoardState that = (BoardState) o;
            return low == that.low && high == that.high;
        }

        @Override
        public int hashCode() {
            // XOR folding for a decent hash
            long combined = low ^ high;
            return (int) (combined ^ (combined >>> 32));
        }
    }

    public GameSearcher(int targetDepth) {
        this.targetDepth = targetDepth;
    }

    public void run() {
        Board board = new Board();
        // Alpha-Beta: P1 (Maximizer) wants P1_WINS, P2 (Minimizer) wants P2_WINS.
        // Initial Alpha = P2_WINS (worst for P1), Initial Beta = P1_WINS (worst for P2).
        dfs(board, 1, new StringBuilder(), GameResult.P2_WINS, GameResult.P1_WINS);
        System.out.println("Search complete. Total nodes visited: " + nodesVisited);
    }

    private GameResult dfs(Board board, int playerIndex, StringBuilder moveSequence, GameResult alpha, GameResult beta) {
        nodesVisited++;
        if (nodesVisited % 1000000 == 0) {
            reportProgress(moveSequence.toString());
        }

        BoardState stateKey = new BoardState(board, playerIndex);
        GameResult cached = memo.get(stateKey);
        if (cached != null) {
            return cached;
        }

        // Check if game is already decided by score
        if (board.getPlayer1Score() > 24) {
            if (targetDepth == -1) {
                System.out.println(moveSequence + " -> P1_WINS (Score: " + board.getPlayer1Score() + "-" + board.getPlayer2Score() + ")");
            }
            return GameResult.P1_WINS;
        }
        if (board.getPlayer2Score() > 24) {
            if (targetDepth == -1) {
                System.out.println(moveSequence + " -> P2_WINS (Score: " + board.getPlayer1Score() + "-" + board.getPlayer2Score() + ")");
            }
            return GameResult.P2_WINS;
        }

        if (board.isGameOver()) {
            // We need a copy to collect remaining without affecting the current branch's board
            int[] currentPits = new int[Board.TOTAL_PITS];
            board.copyPitsTo(currentPits);
            Board finalBoard = new Board(currentPits);
            finalBoard.collectRemaining();
            
            GameResult result;
            if (finalBoard.getPlayer1Score() > finalBoard.getPlayer2Score()) {
                result = GameResult.P1_WINS;
            } else if (finalBoard.getPlayer2Score() > finalBoard.getPlayer1Score()) {
                result = GameResult.P2_WINS;
            } else {
                result = GameResult.TIE;
            }
            
            if (targetDepth == -1) {
                System.out.println(moveSequence + " -> " + result + " (" + finalBoard.getPlayer1Score() + "-" + finalBoard.getPlayer2Score() + ")");
            }
            
            memo.put(stateKey, result);
            return result;
        }

        List<Integer> validMoves = new ArrayList<>();
        int start = (playerIndex == 1) ? 0 : 7;
        int end = (playerIndex == 1) ? 5 : 12;
        for (int i = start; i <= end; i++) {
            if (board.getPits(i) > 0) {
                validMoves.add(i);
            }
        }

        GameResult bestResult = null;
        int[] savedPits = new int[Board.TOTAL_PITS];
        board.copyPitsTo(savedPits);

        for (int move : validMoves) {
            boolean extraTurn = board.move(move, playerIndex);
            
            char moveChar;
            if (playerIndex == 1) {
                moveChar = (char) ('A' + move);
            } else {
                moveChar = (char) ('a' + (move - 7));
            }

            int nextPlayer = extraTurn ? playerIndex : (playerIndex == 1 ? 2 : 1);
            int currentLen = moveSequence.length();
            moveSequence.append(moveChar);
            
            GameResult result = dfs(board, nextPlayer, moveSequence, alpha, beta);
            
            moveSequence.setLength(currentLen); // Backtrack string
            board.copyPitsFrom(savedPits); // Backtrack board

            if (playerIndex == 1) {
                if (bestResult == null || result.ordinal() < bestResult.ordinal()) {
                    bestResult = result;
                }
                // Update alpha (best for P1)
                if (bestResult.ordinal() < alpha.ordinal()) {
                    alpha = bestResult;
                }
            } else {
                if (bestResult == null || result.ordinal() > bestResult.ordinal()) {
                    bestResult = result;
                }
                // Update beta (best for P2)
                if (bestResult.ordinal() > beta.ordinal()) {
                    beta = bestResult;
                }
            }

            // Alpha-Beta Pruning
            if (beta.ordinal() <= alpha.ordinal()) {
                break;
            }
        }

        if (moveSequence.length() == targetDepth) {
            System.out.println(moveSequence + " -> " + bestResult.toForcedString());
        }

        memo.put(stateKey, bestResult);
        return bestResult;
    }

    private void reportProgress(String currentPath) {
        long now = System.currentTimeMillis();
        if (now - lastProgressReport > 5000) {
            System.err.println("Progress: " + nodesVisited + " nodes visited. Current path: " + currentPath);
            lastProgressReport = now;
        }
    }
}
