Name: Ashutosh Jambhale
Student ID: 1657201
Partner: Alex Jensen (1620449)

The REcompile program implements a regular expression compiler. 
It converts regex patterns into Non-Deterministic Finite Automata (NFA) with support for basic operators.

References:
1) Thompson's Construction: https://www.baeldung.com/java-finite-automata
2) NFA Design: https://www.geeksforgeeks.org/designing-non-deterministic-finite-automata-set-4/
3) Princeton NFA: https://algs4.cs.princeton.edu/code/edu/princeton/cs/algs4/NFA.java.html

How to Use It:
Compile the code: javac REcompile.java
Run it: java REcompile "pattern"

Output Format:
Each NFA state is printed as: state-number,type,next1,next2

Supported Grammar:
EXPR    → CONCAT ('|' CONCAT)*
CONCAT  → FACTOR+
FACTOR  → ATOM ('*'|'+'|'?')
ATOM    → Literal | . | ( EXPR ) | \ AnyChar
Literal → any non-special character (except |*+?.()\)
AnyChar → any single character

Methods:
main: Entry point for the program, handling command-line input and initiating regex compilation.

REcompile: Constructor that initializes the compiler with the input regex pattern.

compile: Builds the NFA from the regex and turns it into a text output.

Connect:Links one state to another in the NFA.

parseExpr:Reads the regex to handle “or” (|) parts(ex: a|b).

parseconcat:Reads parts of the regex that are stuck together (ex: ab).

parseFactor:Reads a small regex piece that might repeat (ex:a*).

parseAtom:Reads the smallest regex piece (ex: a letter, .).

constructAlternationPath:Builds an NFA for choosing between two regex parts (ex:a|b).

createRepeatedFragment:Makes an NFA for repeating a regex piece (ex: a*).

createLiteral:Makes a single NFA state for a letter or escaped character.

createWildcard: Makes a single NFA state for a wildcard (.).

newState:Makes a new NFA state with a unique number.

swapStates:Changes the states list and updates all links. Updates all next1 and next2 pointers to point to the right states

peek:Looks at the next character without moving forward.

consume: Grabs the next character and moves forward.

hasMore:Checks if there are more characters to read.

isMeta: Checks if a character is a special symbol (like |, *, etc.).

How It Works:
1)Takes one regex (e.g., "a|b"). If wrong, shows error and stops.
2)prepares empty state list.
3)Makes state 0.
4)Breaks regex into pieces (letters, ., |, *, etc.) using grammar rules.
5)Creates states for letters (a), wildcards (.), “or” (|), or repeats (*, +, ?).
6)Connects states to form the NFA map.
7)Makes a final “accept” state and links the NFA’s end to it.
8)Prints all states.

Example of how this code works:
Command: java REcompile "a|b"
Steps:
1)Sees "a|b", accepts it.
2)Sets position (POS) to 0.
3)Makes state 0.
4)Calls parseconcat for a → parseAtom makes state 1.
5)Sees |, calls parseconcat for b → parseFactor → parseAtom makes state 3.
6)Uses constructAlternationPath to combine a and b. Creates state 2: 2,BR,1,3 (branch to a and b)and creates state 4: 4,BR,-1,-1
7)Uses swapStates to reorder states (ensures branch state 2 follows logically).
8)Connect Pieces (compile, connect).