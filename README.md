# Java Chess Engine

A high-performance chess engine written in **Java**, focused on **correctness**, **speed**, and **clean architecture**.

The engine implements modern techniques such as **bitboards**, **magic bitboards**, and **alpha–beta search**, with performance validated through perft testing.

---

## Features

### Engine
- Bitboard-based board representation
- Fully legal move generation
  - Castling
  - En passant
  - Promotions
- Make / unmake move system for deep search

### Move Generation
- Precomputed attack masks for non-sliding pieces
- Magic bitboards for sliding pieces (rook, bishop, queen)
- King safety validated via make/unmake

### Search
- Alpha–beta pruning
- Node counting and timing
- Nodes-per-second (NPS) tracking

### Performance
- Moves packed into a single `int`
- Allocation-free `MoveBuffer`
- Flattened data structures

### Testing
- Built-in perft framework
  - Standard perft
  - Root perft for debugging

---

## Goals

- Learn chess engine internals
- Optimize performance in Java
- Balance readability and speed
- Provide a base for experimentation

All optimizations are validated using perft.

---

## Building and Running

### Requirements
- Java **17+**
- **Maven**

### Build
```bash
mvn clean package
```

### Run
```bash
mvn exec:java
```
