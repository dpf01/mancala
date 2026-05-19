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
        if (searcher != null) {
            searcher.clearCache();
        }
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
                // Wait briefly for the thread to recognize the stop signal.
                // This reduces delay in the interactive loop.
                searchThread.join(20); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public int getMove(Board board) {
        int move;
        if (isFirstMoveOfGame) {
            isFirstMoveOfGame = false;
            move = getRandomMove(board);
        } else {
            // Stop background thinking before move selection
            stopThinking();

            // Cache-only lookup
            move = searcher.getBestMoveFromCache(board, playerIndex);
            
            if (move == -1 || board.getPits(move) == 0) {
                // If cache miss or illegal move, do a very quick shallow search.
                // This should be fast because many nodes are already in the cache.
                move = searcher.getBestMove(board, playerIndex, 8);
            }
            
            if (move == -1 || board.getPits(move) == 0) {
                // Final fallback to random
                move = getRandomMove(board);
            }
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
