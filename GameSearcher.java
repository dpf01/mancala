import java.util.*;

public class GameSearcher {
    private static class Result {
        GameResult gameResult;
        String pv;

        Result(GameResult gameResult, String pv) {
            this.gameResult = gameResult;
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

    private Map<BoardState, SearchEntry> memo = new HashMap<>();
    private int maxDepthLimit = -1;
    private long nodesVisited = 0;
    private long lastProgressReport = System.currentTimeMillis();

    public enum GameResult {
        P2_WINS, TIE, P1_WINS;

        public String toForcedString() {
            switch (this) {
                case P1_WINS: return "P1_ADVANTAGE";
                case P2_WINS: return "P2_ADVANTAGE";
                default: return "EQUAL";
            }
        }
    }

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

        System.out.println("Starting Iterative Deepening Search with PV tracking...");
        for (int d = 1; d <= endDepth; d++) {
            nodesVisited = 0;
            long start = System.currentTimeMillis();
            Result res = dfs(board, 1, d, GameResult.P2_WINS, GameResult.P1_WINS);
            long end = System.currentTimeMillis();
            
            System.out.printf("D%2d: %-12s | PV: %-20s | Nodes: %-8d | %dms\n", 
                d, res.gameResult.toForcedString(), res.pv, nodesVisited, (end - start));
        }
    }

    private GameResult evaluate(Board board) {
        int p1Score = board.getPlayer1Score();
        int p2Score = board.getPlayer2Score();
        
        // Count stones in pits (weighted less than mancala)
        double p1Pits = 0;
        for (int i = 0; i < 6; i++) p1Pits += board.getPits(i);
        double p2Pits = 0;
        for (int i = 7; i < 13; i++) p2Pits += board.getPits(i);

        // Evaluation considers score difference and stone count on board
        double eval = (p1Score - p2Score) + (p1Pits - p2Pits) * 0.1;

        if (eval > 0.1) return GameResult.P1_WINS;
        if (eval < -0.1) return GameResult.P2_WINS;
        return GameResult.TIE;
    }

    private Result dfs(Board board, int playerIndex, int remainingDepth, GameResult alpha, GameResult beta) {
        nodesVisited++;

        BoardState stateKey = new BoardState(board, playerIndex);
        SearchEntry cached = memo.get(stateKey);
        if (cached != null && cached.depth >= remainingDepth) {
            return cached.result;
        }

        // Terminal states
        if (board.getPlayer1Score() > 24) return new Result(GameResult.P1_WINS, "");
        if (board.getPlayer2Score() > 24) return new Result(GameResult.P2_WINS, "");

        if (board.isGameOver()) {
            int[] currentPits = new int[Board.TOTAL_PITS];
            board.copyPitsTo(currentPits);
            Board finalBoard = new Board(currentPits);
            finalBoard.collectRemaining();
            
            if (finalBoard.getPlayer1Score() > finalBoard.getPlayer2Score()) return new Result(GameResult.P1_WINS, "");
            if (finalBoard.getPlayer2Score() > finalBoard.getPlayer1Score()) return new Result(GameResult.P2_WINS, "");
            return new Result(GameResult.TIE, "");
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

        GameResult bestGameResult = null;
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
                if (bestGameResult == null || res.gameResult.ordinal() > bestGameResult.ordinal()) {
                    bestGameResult = res.gameResult;
                    bestPV = moveChar + res.pv;
                }
                if (bestGameResult.ordinal() > alpha.ordinal()) {
                    alpha = bestGameResult;
                }
            } else {
                // Minimizer: P2
                if (bestGameResult == null || res.gameResult.ordinal() < bestGameResult.ordinal()) {
                    bestGameResult = res.gameResult;
                    bestPV = moveChar + res.pv;
                }
                if (bestGameResult.ordinal() < beta.ordinal()) {
                    beta = bestGameResult;
                }
            }

            if (alpha.ordinal() >= beta.ordinal()) {
                pruned = true;
                break;
            }
        }

        Result finalResult = new Result(bestGameResult, bestPV);
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
