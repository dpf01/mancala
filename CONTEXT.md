# Mancala Game Design

## Requirements
- Text-based interface.
- Human vs. Computer.
- Standard Mancala rules:
  - Six holes per player.
  - Distribution counter-clockwise.
  - Skip opponent's mancala.
  - Extra turn if last piece lands in player's mancala.
  - Capture if last piece lands in player's empty hole and opponent's adjacent hole is not empty.
  - Game ends when one side is empty.
- Initial computer strategy: Random choice.
- Alternate who goes first in subsequent games (for this implementation, we will handle a single game session or a simple loop).

## Design & Structure

### Classes
- `MancalaGame`: Main entry point. Handles the game loop, player turns, and overall flow.
- `Board`: Represents the Mancala board (14 pits total: 2 mancalas + 12 holes). Handles the movement logic and state transitions.
- `Player`: Abstract class for a player.
  - `HumanPlayer`: Handles input from the user.
  - `ComputerPlayer`: Implements the random move strategy.

### Board Representation
An array of integers of size 14:
- Index 0-5: Player 1 (User) holes.
- Index 6: Player 1 (User) mancala.
- Index 7-12: Player 2 (Computer) holes.
- Index 13: Player 2 (Computer) mancala.

### Game Logic
- The `Board` class will have a `move(int holeIndex, int playerIndex)` method that returns whether the player gets another turn.
- Capturing logic and game-over detection will also reside in `Board` or be orchestrated by `MancalaGame`.
