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
- Enhanced computer strategy: Asynchronous tree search using minimax with alpha-beta pruning and iterative deepening. The computer searches while the human is thinking and uses cache-only lookups for quick responses. If the computer starts the game, its first move is random.
- Cache Management: The searcher uses a thread-safe `ConcurrentHashMap` with a size limit to balance performance and memory usage.
- Player Resets: Players are reset at the start of each game to initialize state (like the first move flag) and clear caches.
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

### Exhaustive Search Mode (New)
- Goal: Explore all possible game states using Depth-First Search (DFS) and Minimax.
- Player 1 (User side) always goes first.
- Move Representation:
  - Player 1: Uppercase 'A' through 'F'.
  - Player 2: Lowercase 'a' through 'f'.
- Pruning/Stopping Conditions:
  - If a player's mancala contains more than 24 stones (majority of 48).
  - If a game state has already been visited (memoization using a Map).
- Output: 
  - Move sequences for completed games and the outcome.
  - If `--depth` is specified, only intermediate nodes at that depth are shown with their forced outcomes.
- State Annotation: Minimax algorithm identifies nodes where a player can force a win (`P1_CAN_FORCE_WIN`, `P2_CAN_FORCE_WIN`, `CAN_FORCE_TIE`).
- Progress: Periodically reports nodes visited and current path to `stderr`.

## Search Evolution Roadmap
The future of the game search capabilities is outlined in [SEARCH_EVOLUTION.md](SEARCH_EVOLUTION.md). 

### Progress
- [x] **Implement Alpha-Beta Pruning:** Optimized the minimax search by pruning branches that cannot influence the final decision.
- [x] **Introduce Iterative Deepening:** Provides immediate results at increasing depths, improving the user experience and search efficiency.
- [x] **Heuristic Evaluation:** Implemented a score-based evaluation for non-terminal leaf nodes.
- [x] **Transposition Table (Memoization) Fixes:** Updated the cache to handle search depth and pruning state correctly.
- [x] **Playback Mode:** Added `--play-string` flag to execute move sequences and verify board states.
- [x] **Heuristic Fix:** Corrected extra-turn potential calculation to use modulo 13 (accounting for skipped opponent mancala).
- [ ] **Principal Variation Reporting:** Planned.

## Implementation Details

### Board State
A board state can be represented as a record or a string to facilitate memoization.

### Search Logic
- A recursive DFS function will explore all valid moves for the current player.
- It will track the move sequence.
- It will store the results of each state (Win/Loss/Draw) to determine if a player can force a win.
