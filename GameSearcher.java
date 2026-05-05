import java.util.*;

public class GameSearcher {
    private Map<String, GameResult> memo = new HashMap<>();

    public enum GameResult {
        P1_WINS, P2_WINS, TIE
    }

    public void run() {
        Board board = new Board();
        dfs(board, 1, "");
    }

    private GameResult dfs(Board board, int playerIndex, String moveSequence) {
        String stateKey = board.getStateKey() + "|" + playerIndex;
        if (memo.containsKey(stateKey)) {
            return memo.get(stateKey);
        }

        // Check if game is already decided by score
        if (board.getPlayer1Score() > 24) {
            System.out.println(moveSequence + " -> User wins (Score: " + board.getPlayer1Score() + "-" + board.getPlayer2Score() + ")");
            return GameResult.P1_WINS;
        }
        if (board.getPlayer2Score() > 24) {
            System.out.println(moveSequence + " -> Computer wins (Score: " + board.getPlayer1Score() + "-" + board.getPlayer2Score() + ")");
            return GameResult.P2_WINS;
        }

        if (board.isGameOver()) {
            Board finalBoard = new Board(board.getPitsArray());
            finalBoard.collectRemaining();
            GameResult result;
            if (finalBoard.getPlayer1Score() > finalBoard.getPlayer2Score()) {
                result = GameResult.P1_WINS;
                System.out.println(moveSequence + " -> User wins (" + finalBoard.getPlayer1Score() + "-" + finalBoard.getPlayer2Score() + ")");
            } else if (finalBoard.getPlayer2Score() > finalBoard.getPlayer1Score()) {
                result = GameResult.P2_WINS;
                System.out.println(moveSequence + " -> Computer wins (" + finalBoard.getPlayer1Score() + "-" + finalBoard.getPlayer2Score() + ")");
            } else {
                result = GameResult.TIE;
                System.out.println(moveSequence + " -> Tie (" + finalBoard.getPlayer1Score() + "-" + finalBoard.getPlayer2Score() + ")");
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

        boolean canP1ForceWin = false;
        boolean canP2ForceWin = false;
        boolean canForceTie = false;

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

            if (result == GameResult.P1_WINS) canP1ForceWin = true;
            else if (result == GameResult.P2_WINS) canP2ForceWin = true;
            else canForceTie = true;
        }

        GameResult finalResult;
        if (playerIndex == 1) {
            if (canP1ForceWin) finalResult = GameResult.P1_WINS;
            else if (canForceTie) finalResult = GameResult.TIE;
            else finalResult = GameResult.P2_WINS;
        } else {
            if (canP2ForceWin) finalResult = GameResult.P2_WINS;
            else if (canForceTie) finalResult = GameResult.TIE;
            else finalResult = GameResult.P1_WINS;
        }

        memo.put(stateKey, finalResult);
        return finalResult;
    }
}
