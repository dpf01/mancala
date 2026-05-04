import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ComputerPlayer extends Player {
    private Random random;

    public ComputerPlayer(String name, int playerIndex) {
        super(name, playerIndex);
        this.random = new Random();
    }

    @Override
    public int getMove(Board board) {
        List<Integer> validMoves = new ArrayList<>();
        // Computer side is 7-12
        for (int i = 7; i <= 12; i++) {
            if (board.getPits(i) > 0) {
                validMoves.add(i);
            }
        }

        int move = validMoves.get(random.nextInt(validMoves.size()));
        char holeLabel = (char) ('A' + (move - 7));
        System.out.println(name + " chooses hole " + holeLabel);
        return move;
    }
}
