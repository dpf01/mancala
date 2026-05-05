import java.util.*;

public class GameSearcher {
    private static final int WIN_SCORE = 10000;
    private static final int LOSS_SCORE = -10000;

    private static class Result {
        int score;
        String pv;

        Result(int score, String pv) {
            this.score = score;
            this.pv = pv;
        }
    }

    private static class SearchEntry {
        Result result;
        int depth;

        SearchEntry(Result result, int depth) {
            this.result = result;
            this.depth = depth;
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

    private Map<BoardState, SearchEntry> memo = new HashMap<>();
    private int maxDepthLimit = -1;
    private long nodesVisited = 0;
    private long lastProgressReport = System.currentTimeMillis();

    public GameSearcher(int maxDepthLimit) {
        this.maxDepthLimit = maxDepthLimit;
    }

    public void run(Board initialBoard, int initialPlayerIndex) {
        int endDepth = (maxDepthLimit == -1) ? 50 : maxDepthLimit;

        System.out.println("Starting Granular Iterative Deepening Search...");
        initialBoard.display();
        System.out.println("Initial Player: P" + initialPlayerIndex);

        for (int d = 1; d <= endDepth; d++) {
            nodesVisited = 0;
            long start = System.currentTimeMillis();
            Result res = dfs(initialBoard, initialPlayerIndex, d, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
            long end = System.currentTimeMillis();
            
            System.out.printf("D%2d: Score: %5d | PV: %-25s | Nodes: %-8d | %dms\n", 
                d, res.score, res.pv, nodesVisited, (end - start));
        }
    }

    private int evaluate(Board board) {
        int p1Score = board.getPlayer1Score();
        int p2Score = board.getPlayer2Score();
        
        // Weighting factors
        int mancalaWeight = 100;
        int pitWeight = 10;
        int extraTurnPotentialWeight = 5;

        int score = (p1Score - p2Score) * mancalaWeight;

        // Pit stones contribute but are less secure
        for (int i = 0; i < 6; i++) score += board.getPits(i) * pitWeight;
        for (int i = 7; i < 13; i++) score -= board.getPits(i) * pitWeight;

        // Bonus for potential extra turns (last stone landing in mancala)
        for (int i = 0; i < 6; i++) {
            int stones = board.getPits(i);
            if (stones > 0 && (i + stones) % 13 == 6) {
                score += extraTurnPotentialWeight;
            }
        }
        for (int i = 7; i < 13; i++) {
            int stones = board.getPits(i);
            // For P2, mancala is index 13, which is 0 mod 13.
            if (stones > 0 && (i + stones) % 13 == 0) {
                score -= extraTurnPotentialWeight;
            }
        }

        return score;
    }

    private Result dfs(Board board, int playerIndex, int remainingDepth, int alpha, int beta) {
        nodesVisited++;

        BoardState stateKey = new BoardState(board, playerIndex);
        SearchEntry cached = memo.get(stateKey);
        if (cached != null && cached.depth >= remainingDepth) {
            return cached.result;
        }

        // Terminal states
        if (board.getPlayer1Score() > 24) return new Result(WIN_SCORE + board.getPlayer1Score(), "");
        if (board.getPlayer2Score() > 24) return new Result(LOSS_SCORE - board.getPlayer2Score(), "");

        if (board.isGameOver()) {
            int[] currentPits = new int[Board.TOTAL_PITS];
            board.copyPitsTo(currentPits);
            Board finalBoard = new Board(currentPits);
            finalBoard.collectRemaining();
            
            if (finalBoard.getPlayer1Score() > finalBoard.getPlayer2Score()) {
                return new Result(WIN_SCORE + finalBoard.getPlayer1Score(), "");
            } else if (finalBoard.getPlayer2Score() > finalBoard.getPlayer1Score()) {
                return new Result(LOSS_SCORE - finalBoard.getPlayer2Score(), "");
            } else {
                return new Result(0, "");
            }
        }

        // Depth limit
        if (remainingDepth == 0) {
            return new Result(evaluate(board), "");
        }

        List<Integer> validMoves = new ArrayList<>();
        int start = (playerIndex == 1) ? 0 : 7;
        int end = (playerIndex == 1) ? 5 : 12;
        for (int i = start; i <= end; i++) {
            if (board.getPits(i) > 0) validMoves.add(i);
        }

        int bestScore = (playerIndex == 1) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        String bestPV = "";
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
            
            Result res = dfs(board, nextPlayer, remainingDepth - 1, alpha, beta);
            board.copyPitsFrom(savedPits);

            if (playerIndex == 1) {
                // Maximizer: P1
                if (res.score > bestScore) {
                    bestScore = res.score;
                    bestPV = moveChar + res.pv;
                }
                alpha = Math.max(alpha, bestScore);
            } else {
                // Minimizer: P2
                if (res.score < bestScore) {
                    bestScore = res.score;
                    bestPV = moveChar + res.pv;
                }
                beta = Math.min(beta, bestScore);
            }

            if (alpha >= beta) {
                pruned = true;
                break;
            }
        }

        Result finalResult = new Result(bestScore, bestPV);
        if (!pruned) {
            memo.put(stateKey, new SearchEntry(finalResult, remainingDepth));
        }

        return finalResult;
    }

    private void reportProgress(String currentPath) {
        long now = System.currentTimeMillis();
        if (now - lastProgressReport > 5000) {
            System.err.println("Progress: " + nodesVisited + " nodes visited. Current path: " + currentPath);
            lastProgressReport = now;
        }
    }
}
