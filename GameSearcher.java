import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameSearcher {
    private static final int WIN_SCORE = 10000;
    private static final int LOSS_SCORE = -10000;

    public static class Result {
        public int score;
        public int bestMove; // Store the best move index

        public Result(int score, int bestMove) {
            this.score = score;
            this.bestMove = bestMove;
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

        int getP1Mancala() {
            return (int) ((low >>> 36) & 0x3F);
        }

        int getP2Mancala() {
            return (int) ((high >>> 18) & 0x3F);
        }
    }

    private static final int MAX_CACHE_SIZE = 500000;
    private final Map<BoardState, SearchEntry> memo = new ConcurrentHashMap<>();
    private int maxDepthLimit = -1;
    private long nodesVisited = 0;
    private long lastProgressReport = System.currentTimeMillis();
    private volatile boolean stopped = false;
    private volatile int bestMoveSoFar = -1;
    private volatile int rootP1Mancala = 0;
    private volatile int rootP2Mancala = 0;

    public GameSearcher(int maxDepthLimit) {
        this.maxDepthLimit = maxDepthLimit;
    }

    public void stop() {
        this.stopped = true;
    }

    public void clearCache() {
        memo.clear();
    }

    public int getBestMoveSoFar() {
        return bestMoveSoFar;
    }

    public void run(Board initialBoard, int initialPlayerIndex, boolean verbose) {
        stopped = false;
        bestMoveSoFar = -1;
        this.rootP1Mancala = initialBoard.getPlayer1Score();
        this.rootP2Mancala = initialBoard.getPlayer2Score();
        int endDepth = (maxDepthLimit == -1) ? 50 : maxDepthLimit;

        if (verbose) {
            System.out.println("Starting Granular Iterative Deepening Search...");
        }

        for (int d = 1; d <= endDepth && !stopped; d++) {
            nodesVisited = 0;
            long start = System.currentTimeMillis();
            Result res = dfs(initialBoard, initialPlayerIndex, d, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
            long end = System.currentTimeMillis();
            
            if (!stopped && res.bestMove != -1) {
                bestMoveSoFar = res.bestMove;
                
                if (verbose) {
                    String pv = reconstructPV(initialBoard, initialPlayerIndex, d);
                    System.out.printf("D%2d: Score: %5d | PV: %-25s | Nodes: %-8d | %dms\n", 
                        d, res.score, pv, nodesVisited, (end - start));
                }
            }
        }
    }

    private String reconstructPV(Board board, int playerIndex, int depth) {
        StringBuilder pv = new StringBuilder();
        int[] savedPits = new int[Board.TOTAL_PITS];
        board.copyPitsTo(savedPits);
        
        int currentPlayer = playerIndex;
        for (int i = 0; i < depth; i++) {
            BoardState stateKey = new BoardState(board, currentPlayer);
            SearchEntry entry = memo.get(stateKey);
            if (entry == null || entry.result.bestMove == -1) break;
            
            int move = entry.result.bestMove;
            char moveChar;
            if (currentPlayer == 1) {
                moveChar = (char) ('A' + move);
            } else {
                moveChar = (char) ('a' + (move - 7));
            }
            pv.append(moveChar);
            
            boolean extraTurn = board.move(move, currentPlayer);
            if (!extraTurn) {
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
            }
            if (board.isGameOver()) break;
        }
        
        board.copyPitsFrom(savedPits);
        return pv.toString();
    }

    public int getBestMove(Board board, int playerIndex, int depth) {
        stopped = false;
        this.rootP1Mancala = board.getPlayer1Score();
        this.rootP2Mancala = board.getPlayer2Score();
        Result res = dfs(board, playerIndex, depth, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
        return res.bestMove;
    }

    public int getCacheSize() {
        return memo.size();
    }

    public static class RawEntry {
        public int score;
        public int bestMove;
        public int depth;

        RawEntry(int score, int bestMove, int depth) {
            this.score = score;
            this.bestMove = bestMove;
            this.depth = depth;
        }
    }

    public RawEntry getRawEntryFromCache(Board board, int playerIndex) {
        BoardState stateKey = new BoardState(board, playerIndex);
        SearchEntry entry = memo.get(stateKey);
        if (entry != null) {
            return new RawEntry(entry.result.score, entry.result.bestMove, entry.depth);
        }
        return null;
    }

    public String getCacheSummary() {
        if (memo.isEmpty()) return "Cache is empty.";
        
        Map<Integer, Integer> depthCounts = new TreeMap<>();
        Map<Integer, Integer> moveCounts = new TreeMap<>();
        int minScore = Integer.MAX_VALUE;
        int maxScore = Integer.MIN_VALUE;
        
        for (SearchEntry entry : memo.values()) {
            depthCounts.put(entry.depth, depthCounts.getOrDefault(entry.depth, 0) + 1);
            int move = entry.result.bestMove;
            moveCounts.put(move, moveCounts.getOrDefault(move, 0) + 1);
            minScore = Math.min(minScore, entry.result.score);
            maxScore = Math.max(maxScore, entry.result.score);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Cache Summary (Size: ").append(memo.size()).append(")\n");
        sb.append("    Depth Dist: ").append(depthCounts).append("\n");
        sb.append("    Move Dist: ");
        for (Map.Entry<Integer, Integer> me : moveCounts.entrySet()) {
            int move = me.getKey();
            String label = (move == -1) ? "None" : (move < 7 ? ""+(char)('A'+move) : ""+(char)('a'+(move-7)));
            sb.append(label).append(":").append(me.getValue()).append(" ");
        }
        sb.append("\n    Score Range: [").append(minScore).append(", ").append(maxScore).append("]");
        return sb.toString();
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

    private void cacheResult(BoardState stateKey, Result result, int depth) {
        if (stopped) return;
        if (memo.size() >= MAX_CACHE_SIZE) {
            pruneCache();
        }
        memo.put(stateKey, new SearchEntry(result, depth));
    }

    private void pruneCache() {
        int targetSize = (int) (MAX_CACHE_SIZE * 0.75);

        // Phase 1: Remove "the past" (unreachable states)
        Iterator<Map.Entry<BoardState, SearchEntry>> it = memo.entrySet().iterator();
        while (it.hasNext() && memo.size() > targetSize) {
            BoardState state = it.next().getKey();
            if (state.getP1Mancala() < rootP1Mancala || state.getP2Mancala() < rootP2Mancala) {
                it.remove();
            }
        }

        if (memo.size() <= targetSize) return;

        // Phase 2: Depth-based pruning
        for (int d = 0; d <= 4; d++) {
            it = memo.entrySet().iterator();
            while (it.hasNext() && memo.size() > targetSize) {
                if (it.next().getValue().depth <= d) {
                    it.remove();
                }
            }
            if (memo.size() <= targetSize) return;
        }

        // Phase 3: Fallback arbitrary pruning
        it = memo.entrySet().iterator();
        while (it.hasNext() && memo.size() > targetSize) {
            it.next();
            it.remove();
        }
    }

    public Result dfs(Board board, int playerIndex, int remainingDepth, int alpha, int beta) {
        nodesVisited++;

        BoardState stateKey = new BoardState(board, playerIndex);
        SearchEntry cached = memo.get(stateKey);
        if (cached != null && cached.depth >= remainingDepth) {
            return cached.result;
        }

        // Terminal states
        if (board.getPlayer1Score() > 24) {
            Result res = new Result(WIN_SCORE + board.getPlayer1Score(), -1);
            cacheResult(stateKey, res, remainingDepth);
            return res;
        }
        if (board.getPlayer2Score() > 24) {
            Result res = new Result(LOSS_SCORE - board.getPlayer2Score(), -1);
            cacheResult(stateKey, res, remainingDepth);
            return res;
        }

        if (board.isGameOver()) {
            int[] currentPits = new int[Board.TOTAL_PITS];
            board.copyPitsTo(currentPits);
            Board finalBoard = new Board(currentPits);
            finalBoard.collectRemaining();
            
            Result res;
            if (finalBoard.getPlayer1Score() > finalBoard.getPlayer2Score()) {
                res = new Result(WIN_SCORE + finalBoard.getPlayer1Score(), -1);
            } else if (finalBoard.getPlayer2Score() > finalBoard.getPlayer1Score()) {
                res = new Result(LOSS_SCORE - finalBoard.getPlayer2Score(), -1);
            } else {
                res = new Result(0, -1);
            }
            cacheResult(stateKey, res, remainingDepth);
            return res;
        }

        // Depth limit
        if (remainingDepth == 0) {
            Result res = new Result(evaluate(board), -1);
            // We usually don't cache depth 0 leaves in simple alpha-beta to save space,
            // but for Mancala it might be useful. Let's cache them for now.
            cacheResult(stateKey, res, 0);
            return res;
        }

        int start = (playerIndex == 1) ? 0 : 7;
        int end = (playerIndex == 1) ? 5 : 12;

        int bestScore = (playerIndex == 1) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int bestMove = -1;
        int[] savedPits = new int[Board.TOTAL_PITS];
        board.copyPitsTo(savedPits);

        boolean cutoff = false;
        for (int move = start; move <= end; move++) {
            if (stopped) break;
            if (board.getPits(move) == 0) continue;

            boolean extraTurn = board.move(move, playerIndex);
            int nextPlayer = extraTurn ? playerIndex : (playerIndex == 1 ? 2 : 1);
            
            Result res = dfs(board, nextPlayer, remainingDepth - 1, alpha, beta);
            board.copyPitsFrom(savedPits);

            if (playerIndex == 1) {
                // Maximizer: P1
                if (res.score > bestScore) {
                    bestScore = res.score;
                    bestMove = move;
                }
                alpha = Math.max(alpha, bestScore);
            } else {
                // Minimizer: P2
                if (res.score < bestScore) {
                    bestScore = res.score;
                    bestMove = move;
                }
                beta = Math.min(beta, bestScore);
            }

            if (alpha >= beta) {
                cutoff = true;
                break;
            }
        }

        Result finalResult = new Result(bestScore, bestMove);
        // We cache even if cutoff occurred, as the bestMove found so far is still useful.
        cacheResult(stateKey, finalResult, remainingDepth);

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
