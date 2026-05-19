import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ComputerPlayer extends Player {
    private Random random;
    private GameSearcher searcher;
    private Thread searchThread;
    private boolean isFirstMoveOfGame = true;

    public ComputerPlayer(String name, int playerIndex) {
        super(name, playerIndex);
        this.random = new Random();
        this.searcher = new GameSearcher(-1); // No depth limit by default
    }

    @Override
    public void reset() {
        this.isFirstMoveOfGame = true;
    }

    @Override
    public void startThinking(Board board, int currentPlayerIndex) {
        if (searchThread != null && searchThread.isAlive()) {
            stopThinking();
        }
        
        // Use a copy of the board for searching
        int[] pits = new int[Board.TOTAL_PITS];
        board.copyPitsTo(pits);
        Board boardCopy = new Board(pits);
        
        searchThread = new Thread(() -> {
            searcher.run(boardCopy, currentPlayerIndex, false);
        });
        searchThread.start();
    }

    @Override
    public void stopThinking() {
        if (searcher != null) {
            searcher.stop();
        }
        if (searchThread != null) {
            try {
                searchThread.join(100); // Wait a bit for it to stop
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public int getMove(Board board) {
        if (isFirstMoveOfGame) {
            isFirstMoveOfGame = false;
            return getRandomMove(board);
        }

        // If we were already thinking, give it a moment to finish the current depth
        if (searchThread != null && searchThread.isAlive()) {
            try {
                Thread.sleep(500); // Wait briefly for current depth
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stopThinking();
        }

        // Now do a cache-only lookup for the current state.
        int move = searcher.getBestMoveFromCache(board, playerIndex);
        
        if (move == -1 || board.getPits(move) == 0) {
            // Fallback to random if search didn't find a valid move in cache
            move = getRandomMove(board);
        }

        char holeLabel = (char) ('A' + (move < 7 ? move : move - 7));
        System.out.println(name + " chooses hole " + holeLabel);
        return move;
    }

    public int getRandomMove(Board board) {
        List<Integer> validMoves = new ArrayList<>();
        int start = (playerIndex == 1) ? 0 : 7;
        int end = (playerIndex == 1) ? 5 : 12;
        for (int i = start; i <= end; i++) {
            if (board.getPits(i) > 0) {
                validMoves.add(i);
            }
        }
        return validMoves.get(random.nextInt(validMoves.size()));
    }
}
