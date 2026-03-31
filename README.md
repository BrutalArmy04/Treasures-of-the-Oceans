# 🌊 Treasures of the Oceans: Top Trumps (CLI Edition)

A fully functional, dynamically scaling Command Line Interface (CLI), featuring a custom 60-card deck of aquatic wildlife and three distinct tiers of mathematically-driven AI opponents.

## 📖 Overview
This project is a robust Java-based game engine designed to simulate a blind Top Trumps tournament. Players compete to win all the cards in the deck by calling out the highest stat on their current card. The engine features dynamic deck splitting, recursive tie-breaker handling, and a sophisticated AI system that scales its difficulty using memory queues and logarithmic probability.

## ✨ Key Features
* **Dynamic Deck Sizing:** Play with the full Master Deck, or request a smaller custom deck (e.g., a quick 15-card skirmish). The engine perfectly parses the math and removes leftover cards on the fly.
* **Flawless Tie-Breakers:** Built-in recursive evaluation handles multi-player ties, holding the cards in a central "pot" until a definitive winner scoops the jackpot.
* **Elimination Tracking:** A memory-safe queue system smoothly removes players who run out of cards without breaking the turn order.
* **Blind Gameplay:** Staying true to physical Top Trumps, players (both human and AI) are completely blind to the cards held by their opponents.

## 🤖 The AI Opponents
The game features three distinct AI bots, ranging from pure chaos to card-counting strategists. 

* **The Easy Bot (The Goldfish):** * **Memory:** 0 Cards
  * **Strategy:** Pure RNG. It guesses a stat completely at random. 
* **The Medium Bot (The Short-Term Thinker):** * **Memory:** 1 Full Round (`numberOfPlayers`)
  * **Strategy:** It tracks the cards played in the immediate previous round, subtracting them from the game's baseline stats to calculate a "Safety Score" before choosing its attack.
* **The Hard Bot (The Elephant):** * **Memory:** Dynamic (`log2(P) + log2(C) + log3(C/P)`)
  * **Strategy:** Scales its memory limit logarithmically based on the total number of cards and players in the game. In late-game scenarios, it practically has map-hacks, flawlessly predicting the safest statistical bet based on the remaining global pool.

## 🎮 How to Play
1. **Setup:** Launch the game and choose how many players will sit at the table (2-4).
2. **Assign Seats:** You can mix and match Human players and Bots in any configuration.
3. **The Turn:** If it is your turn, you will look at the top card of your deck and choose one of three stats: **Speed**, **Size**, or **Danger**.
4. **The Reveal:** Everyone reveals their top card. The highest number in the chosen stat wins the round and takes all the cards played.
5. **The Goal:** Eliminate all other players by taking all their cards!

## 🚀 Running the Game Locally

### Prerequisites
* Java Development Kit (JDK) 8 or higher installed on your machine.

### Compilation and Execution
1. Open your terminal or command prompt.
2. Navigate to the directory containing the `.java` files.
3. Compile the code:
   ```bash
   javac *.java

### Run the main application:
java App

### Built Using Java - Core game logic, Object-Oriented architecture, and probability math.
