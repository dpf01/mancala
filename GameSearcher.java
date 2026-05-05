import java.util.*;

public class GameSearcher {
    private static class SearchEntry {
        GameResult result;
        int depth;

        SearchEntry(GameResult result, int depth) {
            this.result = result;
            this.depth = depth;
        }
    }

    private Map<BoardState, SearchEntry> memo = new HashMap<>();
    private int maxDepthLimit = -1;
    private long nodesVisited = 0;
    private long lastProgressReport = System.currentTimeMillis();

    public enum GameResult {
        P2_WINS, TIE, P1_WINS;

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
     */
    private static class BoardState {
        private final long low;
        private final long high;

        BoardState(Board board, int playerIndex) {
            long l = 0;
            for (int i = 0; i < 10; i++) {
                l |= ((long) (board.getPits(i) & 0x3F)) << (i * 6);
            }
            this.low = l;
            long h = 0;
            for (int i = 10; i < 14; i++) {
                h |= ((long) (board.getPits(i) & 0x3F)) << ((i - 10) * 6);
            }
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
            long combined = low ^ high;
            return (int) (combined ^ (combined >>> 32));
        }
    }

    public GameSearcher(int maxDepthLimit) {
        this.maxDepthLimit = maxDepthLimit;
    }

    public void run() {
        Board board = new Board();
        int endDepth = (maxDepthLimit == -1) ? 50 : maxDepthLimit;

        System.out.println("Starting Iterative Deepening Search...");
        for (int d = 1; d <= endDepth; d++) {
            nodesVisited = 0;
            long start = System.currentTimeMillis();
            GameResult result = dfs(board, 1, new StringBuilder(), d, GameResult.P2_WINS, GameResult.P1_WINS);
            long end = System.currentTimeMillis();
            
            System.out.printf("Depth %d complete. Result: %-15s | Nodes: %-10d | Time: %dms\n", 
                d, result.toForcedString(), nodesVisited, (end - start));
            
            if (result == GameResult.P1_WINS && maxDepthLimit == -1) {
                // If we found a forced win for the whole game, we could technically stop,
                // but ID is usually used for time-limited search or best-move finding.
            }
        }
    }

    private GameResult evaluate(Board board) {
        int score1 = board.getPlayer1Score();
        int score2 = board.getPlayer2Score();
        if (score1 > score2) return GameResult.P1_WINS;
        if (score2 > score1) return GameResult.P2_WINS;
        return GameResult.TIE;
    }

    private GameResult dfs(Board board, int playerIndex, StringBuilder moveSequence, int remainingDepth, GameResult alpha, GameResult beta) {
        nodesVisited++;
        if (nodesVisited % 1000000 == 0) {
            reportProgress(moveSequence.toString());
        }

        BoardState stateKey = new BoardState(board, playerIndex);
        SearchEntry cached = memo.get(stateKey);
        if (cached != null && cached.depth >= remainingDepth) {
            return cached.result;
        }

        // Terminal states
        if (board.getPlayer1Score() > 24) return GameResult.P1_WINS;
        if (board.getPlayer2Score() > 24) return GameResult.P2_WINS;

        if (board.isGameOver()) {
            int[] currentPits = new int[Board.TOTAL_PITS];
            board.copyPitsTo(currentPits);
            Board finalBoard = new Board(currentPits);
            finalBoard.collectRemaining();
            
            if (finalBoard.getPlayer1Score() > finalBoard.getPlayer2Score()) return GameResult.P1_WINS;
            if (finalBoard.getPlayer2Score() > finalBoard.getPlayer1Score()) return GameResult.P2_WINS;
            return GameResult.TIE;
        }

        // Depth limit
        if (remainingDepth == 0) {
            return evaluate(board);
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

        boolean pruned = false;
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
            
            GameResult result = dfs(board, nextPlayer, moveSequence, remainingDepth - 1, alpha, beta);
            
            moveSequence.setLength(currentLen);
            board.copyPitsFrom(savedPits);

            if (playerIndex == 1) {
                if (bestResult == null || result.ordinal() > bestResult.ordinal()) {
                    bestResult = result;
                }
                if (bestResult.ordinal() > alpha.ordinal()) {
                    alpha = bestResult;
                }
            } else {
                if (bestResult == null || result.ordinal() < bestResult.ordinal()) {
                    bestResult = result;
                }
                if (bestResult.ordinal() < beta.ordinal()) {
                    beta = bestResult;
                }
            }

            if (alpha.ordinal() >= beta.ordinal()) {
                pruned = true;
                break;
            }
        }

        if (!pruned) {
            memo.put(stateKey, new SearchEntry(bestResult, remainingDepth));
        }

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
