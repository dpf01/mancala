import java.util.Random;
import java.util.Scanner;

public class MancalaGame {
    private Board board;
    private Player human;
    private Player computer;
    private Player currentPlayer;
    private boolean player1StartsNext;
    private Random random;
    private Scanner scanner;

    public MancalaGame() {
        this.board = new Board();
        this.human = new HumanPlayer("User", 1);
        this.computer = new ComputerPlayer("Computer", 2);
        this.random = new Random();
        this.scanner = new Scanner(System.in);
        // Randomize first player for the first game
        this.player1StartsNext = random.nextBoolean();
    }

    public void start() {
        System.out.println("Welcome to Mancala!");
        boolean playAgain = true;

        while (playAgain) {
            playGame();
            System.out.print("Do you want to play again? (y/n): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            playAgain = choice.startsWith("y");
            if (playAgain) {
                board.reset();
                // Alternate who starts next game
                // (Note: player1StartsNext was set for the game just finished, 
                // but we'll just toggle it here)
                // Actually, the requirement says "On subsequent games, alternate which player goes first."
            }
        }
        System.out.println("Thanks for playing!");
    }

    private void playGame() {
        if (player1StartsNext) {
            currentPlayer = human;
        } else {
            currentPlayer = computer;
        }
        player1StartsNext = !player1StartsNext; // Prepare for next game

        System.out.println(currentPlayer.getName() + " starts the game.");

        while (!board.isGameOver()) {
            board.display();
            boolean extraTurn = true;
            while (extraTurn && !board.isGameOver()) {
                int move = currentPlayer.getMove(board);
                extraTurn = board.move(move, currentPlayer.getPlayerIndex());
                if (extraTurn && !board.isGameOver()) {
                    board.display();
                    System.out.println(currentPlayer.getName() + " gets an extra turn!");
                }
            }
            
            if (!board.isGameOver()) {
                currentPlayer = (currentPlayer == human) ? computer : human;
            }
        }

        board.collectRemaining();
        board.display();
        System.out.println("Game Over!");
        System.out.println("Final Score - User: " + board.getPlayer1Score() + ", Computer: " + board.getPlayer2Score());

        if (board.getPlayer1Score() > board.getPlayer2Score()) {
            System.out.println("User wins!");
        } else if (board.getPlayer2Score() > board.getPlayer1Score()) {
            System.out.println("Computer wins!");
        } else {
            System.out.println("It's a tie!");
        }
    }

    public static void main(String[] args) {
        MancalaGame game = new MancalaGame();
        game.start();
    }
}
