import java.util.Scanner;

public class HumanPlayer extends Player {
    private Scanner scanner;

    public HumanPlayer(String name, int playerIndex) {
        super(name, playerIndex);
        this.scanner = new Scanner(System.in);
    }

    @Override
    public int getMove(Board board) {
        while (true) {
            System.out.print(name + ", choose a hole (A-F): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.length() != 1 || input.charAt(0) < 'A' || input.charAt(0) > 'F') {
                System.out.println("Invalid input. Please enter A, B, C, D, E, or F.");
                continue;
            }

            int holeIndex = input.charAt(0) - 'A';
            if (board.getPits(holeIndex) == 0) {
                System.out.println("That hole is empty. Choose another.");
                continue;
            }

            return holeIndex;
        }
    }
}
