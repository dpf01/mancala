public abstract class Player {
    protected String name;
    protected int playerIndex;

    public Player(String name, int playerIndex) {
        this.name = name;
        this.playerIndex = playerIndex;
    }

    public abstract int getMove(Board board);

    public void startThinking(Board board, int currentPlayerIndex) {}

    public void stopThinking() {}

    public void reset() {}

    public String getName() {
        return name;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }
}
