# Mancala Game

This is a text-based Mancala game implemented in Java.

## How to Compile

To compile the game, ensure you have the Java Development Kit (JDK) installed and run the following command in the project root directory:

```bash
javac *.java
```

## How to Run

To start the game in interactive mode, run:

```bash
java MancalaGame
```

To run the exhaustive search mode, run:

```bash
java MancalaGame --search
```

To limit search output to a specific depth (number of moves) and see forced win/tie outcomes, run:

```bash
java MancalaGame --search --depth <n>
```

## Game Modes
- **Interactive Mode**: Play against the computer (Random strategy).
- **Search Mode**: Exhaustively explores all possible games using minimax and identifies forced winning strategies. Supports depth-limited output and reports progress.

## Game Rules
- The game is played between a User and a Computer.
- Each player has 6 holes and one Mancala.
- Players take turns moving stones from one of their holes counter-clockwise.
- If the last stone lands in your Mancala, you get an extra turn.
- If the last stone lands in an empty hole on your side, you capture the stones in the opposite hole.
- The game ends when one player's holes are all empty.
- The player with the most stones in their Mancala at the end wins.
