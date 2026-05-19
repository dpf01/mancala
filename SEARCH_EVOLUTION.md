# Mancala Search Evolution

This document tracks the technical evolution and optimizations of the Mancala game tree search.

## 1. Search Fundamentals

### Alpha-Beta Pruning
Standard optimization for Minimax that maintains alpha (best score for maximizer) and beta (best score for minimizer) to prune branches that cannot influence the final decision.

### Iterative Deepening
Searches to increasing depths (1, 2, 3...), allowing the search to be interrupted at any time while still returning the best move found at the last completed depth.

### Principal Variation (PV) Reconstruction
Instead of storing full move strings in every cache node, the searcher reconstructs the PV by following the "best move" pointers stored in the transposition table.

## 2. Advanced Optimizations

### Transposition Table (Memoization)
Uses a thread-safe `ConcurrentHashMap` to store search results for visited board states, significantly reducing redundant calculations in the game graph.

### Memory Efficiency
- **Compact Board State:** Board pits are packed into two `long` values, minimizing memory footprint.
- **Minimal Object Allocation:** Search loop avoids list/array allocations at each node to reduce GC overhead.

### Cache Management & Pruning
- **Unreachable State Pruning:** Automatically removes states with lower mancala scores than the current game root, as they are mathematically impossible to reach.
- **Depth-Based Eviction:** Selectively removes low-depth (low computation cost) entries when the 500k-entry limit is reached.

### Asynchronous Strategy
- **Background Thinking:** The computer utilizes the human player's turn to explore future possibilities.
- **Low-Latency Response:** Prioritizes cache-only lookups and uses a shallow (depth 8) fallback search for instant decision-making.

## 3. Implementation History
- **Alpha-Beta Pruning:** Drastically reduced search space.
- **Iterative Deepening:** Added real-time feedback at increasing depths.
- **Granular Scoring:** Replaced 3-state enum with integer scores for tactical awareness.
- **Playback Mode:** Enabled verification of Principal Variations via move-string execution.
- **Heuristic Refinement:** Corrected extra-turn logic and added stone weighting.
- **Memory Optimization:** Replaced full PV string storage with `bestMove` index and packed board states.
- **Thread Safety & Stability:** Switched to `ConcurrentHashMap` and added strict cache limits.
- **Cache Effectiveness:** Refined caching to include leaf nodes and pruned branches.
- **Unreachable Pruning:** Implemented mancala-score-based cache eviction.
