import java.util.*;

public class GameSearcher {
    private Map<String, GameResult> memo = new HashMap<>();
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

    public GameSearcher(int targetDepth) {
        this.targetDepth = targetDepth;
    }

    public void run() {
        Board board = new Board();
        dfs(board, 1, "");
        System.out.println("Search complete. Total nodes visited: " + nodesVisited);
    }

    private GameResult dfs(Board board, int playerIndex, String moveSequence) {
        nodesVisited++;
        if (nodesVisited % 100000 == 0) {
            reportProgress(moveSequence);
        }

        String stateKey = board.getStateKey() + "|" + playerIndex;
        if (memo.containsKey(stateKey)) {
            return memo.get(stateKey);
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
            Board finalBoard = new Board(board.getPitsArray());
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

        for (int move : validMoves) {
            Board nextBoard = new Board(board.getPitsArray());
            boolean extraTurn = nextBoard.move(move, playerIndex);
            
            char moveChar;
            if (playerIndex == 1) {
                moveChar = (char) ('A' + move);
            } else {
                moveChar = (char) ('a' + (move - 7));
            }

            int nextPlayer = extraTurn ? playerIndex : (playerIndex == 1 ? 2 : 1);
            GameResult result = dfs(nextBoard, nextPlayer, moveSequence + moveChar);

            if (bestResult == null) {
                bestResult = result;
            } else {
                if (playerIndex == 1) {
                    // P1 wants P1_WINS > TIE > P2_WINS
                    if (result == GameResult.P1_WINS || (result == GameResult.TIE && bestResult == GameResult.P2_WINS)) {
                        bestResult = result;
                    }
                } else {
                    // P2 wants P2_WINS > TIE > P1_WINS
                    if (result == GameResult.P2_WINS || (result == GameResult.TIE && bestResult == GameResult.P1_WINS)) {
                        bestResult = result;
                    }
                }
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
        if (now - lastProgressReport > 5000) { // Report every 5 seconds if nodes keep being visited
            System.err.println("Progress: " + nodesVisited + " nodes visited. Current path: " + currentPath);
            lastProgressReport = now;
        }
    }
}
