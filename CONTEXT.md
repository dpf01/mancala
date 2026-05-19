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
- Advanced Computer Strategy:
  - Asynchronous tree search (thinks during human turn).
  - Rapid response using cache lookups and shallow search fallback.
  - Random first move if starting the game.
- Session Management:
  - Alternate who goes first in subsequent games.
  - Player state reset between games.

## Design & Structure

### Classes
- `MancalaGame`: Main entry point. Handles the game loop, player turns, and overall flow.
- `Board`: Represents the board state (14 pits: 2 mancalas + 12 holes) and handles movement logic.
- `Player`: Abstract base class for players.
  - `HumanPlayer`: Handles CLI input.
  - `ComputerPlayer`: Manages background search thread and move selection.
- `GameSearcher`: Implements the iterative deepening minimax search with alpha-beta pruning and a transposition table.

### Search Architecture
- **Iterative Deepening:** Explores the game tree to increasing depths, providing reliable best-move suggestions even if interrupted.
- **Transposition Table:** Thread-safe `ConcurrentHashMap` caching `SearchResult` objects for board states.
- **Cache Management:**
  - **Unreachable State Pruning:** Removes states with lower mancala scores than the current root.
  - **Depth-Based Pruning:** Removes low-depth entries when capacity (500k) is reached.
- **Asynchronous Execution:** Search runs in a background thread during the human player's thinking time.
- **Fallback Search:** Performs a quick depth-limited search (depth 8) if a cache miss occurs during the computer's turn.

### Board Representation
An array of integers of size 14:
- Index 0-5: Player 1 holes.
- Index 6: Player 1 mancala.
- Index 7-12: Player 2 holes.
- Index 13: Player 2 mancala.

### Search Mode
- Triggered by `--search` flag.
- Exhaustively explores the game tree from a starting state.
- Supports `--depth` and `--play-string` for targeted analysis.
- Reports score, Principal Variation (PV), nodes visited, and time per depth.
