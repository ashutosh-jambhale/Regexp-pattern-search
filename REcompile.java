/**
 * Author: Ashutosh Jambhale
 * Last Modified: 14 May 2025
 * Description: Compiles a regular expression into an NFA.
 * Outputs NFA in format: state-number,type,next1,next2
 * Grammar:
 * EXPR    → CONCAT ('|' CONCAT)*
 * CONCAT  → FACTOR+
 * FACTOR  → ATOM ('*'|'+'|'?')
 * Atom    → Literal | . | ( Expr ) | \ AnyChar
 * Literal → any non-special character (except |*+?.()\)
 * AnyChar → any single character
 * Resources:
 * 1. Baeldung's "Finite Automata in Java" - https://www.baeldung.com/java-finite-automata
 * 2. GeeksforGeeks NFA Design - https://www.geeksforgeeks.org/designing-non-deterministic-finite-automata-set-4/
 * 3. Princeton NFA Implementation - https://algs4.cs.princeton.edu/code/edu/princeton/cs/algs4/NFA.java.html
 */
import java.util.*;

public class REcompile {

    private final List<State> states = new ArrayList<>();  // Stores all NFA states
    private final String pattern;   // Input regex pattern
    private int POS;                // Current position in the pattern
    private int stateId;            // Next available state ID

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java REcompile \"pattern\"");
            System.exit(1);
        }
        
        REcompile compiler = new REcompile(args[0]);
        System.out.print(compiler.compile());
    }

    private static class Fragment {
        int start; // Starting state ID
        int end;   // Ending state ID
        Fragment(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class State {
        int id;
        String type;
        int next1;
        int next2;
        State(int id, String type, int next1, int next2) {
            this.id = id;
            this.type = type;
            this.next1 = next1;
            this.next2 = next2;
        }
        @Override
        public String toString() {
            return String.format("%d,%s,%d,%d", id, type, next1, next2);
        }
    }

    //Initializes the regex pattern
    public REcompile(String pattern) {
        this.pattern = pattern.replace("**", "*"); // Simplify undefined ** to *
        this.POS = 0;
        this.stateId = 0;
    }


    public String compile() {
        try {
            int startBranch = newState("BR", -1, -1);  // Create initial branch state
            Fragment nfa = parseExpr(); // Parse the pattern into an NFA fragment
            Connect(startBranch, nfa.start);  // Connect the start state to the NFA's entry point
    
            int acceptState = newState("BR", -1, -1);   // Create the accept state
            Connect(nfa.end, acceptState); 

            StringBuilder output = new StringBuilder(); // Build output by formatting each state
            for (State state : states) {
                output.append(state).append("\n");
            }
            return output.toString();
        } catch (Exception e) {
            System.err.println("Error in compiling pattern: " + e.getMessage());
            System.exit(1);
            return "";
        }
    }

    //Connects two states in the NFA
    private void Connect(int from, int to) {
    State state = states.get(from);
    if (!state.type.equals("BR")) { // Non-branch states point both transitions to the same state
        state.next1 = to;
        state.next2 = to;
    } 
    else { // Branch states only update unset transitions
        if (state.next1 == -1) {
            state.next1 = to;
        }
        if (state.next2 == -1) {
            state.next2 = to;
        }
    }
    }

    //Parses an expression(ex:"a|b")
    private Fragment parseExpr() {
        Fragment frag = parseconcat();   // Parse left side
        while (peek() == '|') {
            consume();
            Fragment next = parseconcat();  // Parse right side
            frag = constructAlternationPath(frag, next);
        }
        return frag;
    }

    //Parses concatenated patterns (ex:"ab")
    private Fragment parseconcat() {
        Fragment frag = parseFactor();
        while (hasMore() && !isMeta(peek())) { // Chain subsequent factors (implicit concatenation)
            Fragment next = parseFactor();
            Connect(frag.end, next.start);
            frag = new Fragment(frag.start, next.end);
        }
        return frag;
    }

    //Parses a factor, applying repetition operators (*, +, ?)
    private Fragment parseFactor() {
        Fragment frag = parseAtom();
        char op = peek();
        if (op == '*' || op == '+' || op == '?') {
            consume();
            frag = createRepeatedFragment(frag, op);
        }
        return frag;
    }

    //Parses an atom: literal, wildcard (.), escaped character, or grouped expression
    private Fragment parseAtom() {
        char c = consume();
        switch (c) {
            case '\\':  // Escape sequence
                if (!hasMore()) throw new RuntimeException("Escape character '\\' must be followed by a valid character");
                c = consume();
                return createLiteral(c);
            case '(':  // Group
                Fragment frag = parseExpr();
                if (consume() != ')') throw new RuntimeException("unclosed parenthesis");
                return frag;
            case '.':  // Wildcard
                return createWildcard();
            default:
                return createLiteral(c);
        }
    }

    //Builds an alternation (OR) path between two NFA
    private Fragment constructAlternationPath(Fragment left, Fragment right) {
        int branch = newState("BR", left.start, right.start);  // Create a branch state pointing to both fragments
        int endState = newState("BR", -1, -1);                // Create a merge state for the end of both paths
        Connect(left.end, endState);
        Connect(right.end, endState);
        int targetPOS = left.end + 1; // Reorder branch state to follow left path
        if (branch != targetPOS) {  
            swapStates(branch, targetPOS);
            branch = targetPOS;
        }
        return new Fragment(branch, endState);
    }

    //Applies repetition (*, +, ?) to a fragment, creating branch and end states
    private Fragment createRepeatedFragment(Fragment frag, char op) {
        int branch = newState("BR", frag.start, -1);
        int end = newState("BR", -1, -1);
        switch (op) { 
            case '*':  // Kleene star: 0 or more
                Connect(frag.end, branch);  // Loop back
                Connect(branch, end); // Optional skip
                break;
            case '+':  // One or more
                Connect(frag.end, branch);
                Connect(frag.end, end); // Mandatory path
                break;
            case '?':  // Zero or one
                Connect(branch, frag.start);
                Connect(branch, end);
                Connect(frag.end, end); 
                break;
        }
        return new Fragment(branch, end);
    }

    //Creates a literal state for a single character.
    private Fragment createLiteral(char c) {
        int state = newState(String.valueOf(c), -1, -1);
        return new Fragment(state, state);
    }

    //Creates NFA fragment for wildcard 
    private Fragment createWildcard() {
        int state = newState("WC", -1, -1);
        return new Fragment(state, state);
    }

    //Creates new NFA state
    private int newState(String type, int next1, int next2) {
        State state = new State(stateId++, type, next1, next2);
        states.add(state);
        return state.id;
    }

    //Swaps two states' positions in the NFA
    private void swapStates(int id1, int id2) {
        // Ensure states exist
        while (states.size() <= id1) newState("TMP", -1, -1);
        while (states.size() <= id2) newState("TMP", -1, -1);
        
        State s1 = states.get(id1);
        State s2 = states.get(id2);
        
        int temp = s1.id; // Swap IDs
        s1.id = s2.id;
        s2.id = temp;
        
        for (State state : states) {   // Update all references
            if (state.next1 == id1) state.next1 = id2;
            else if (state.next1 == id2) state.next1 = id1;
            
            if (state.next2 == id1) state.next2 = id2;
            else if (state.next2 == id2) state.next2 = id1;
        }
        
        states.set(id1, s2);  // Update list positions
        states.set(id2, s1);
    }

    private char peek() { //Peeks at next character without consuming
        return POS < pattern.length() ? pattern.charAt(POS) : '\0';
    }

    private char consume() { //Consumes and returns next character
        if (!hasMore()) throw new RuntimeException("Unexpected end of pattern");
        return pattern.charAt(POS++);
    }

    private boolean hasMore() {  //Checks if more input remains
        return POS < pattern.length();
    }

    private boolean isMeta(char c) { //Checks if a character is a meta-character (operator)
        return ")|*+?.\\".indexOf(c) != -1;
    }
}
