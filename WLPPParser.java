import java.util.*;
import java.math.*;
import java.lang.*;
import java.nio.*;

/** A sample main class demonstrating the use of the Lexer.
 * This main class just outputs each line in the input, followed by
 * the tokens returned by the lexer for that line.
 *
 * @version 071011.0
 */


public class WLPPParser {
    
    boolean errorOccurred = false;
    
    public static final void main(String[] args) {
        new WLPPParser().run();
    }
    
    private Lexer lexer = new Lexer();
    
    private void run() {
        Scanner in = new Scanner(System.in);
        
        
        
        // FIRST PASS
        while(!errorOccurred && in.hasNextLine()) {
            
            String line = in.nextLine();
            
            // Scan the line into an array of tokens.
            Token[] tokens;
            tokens = lexer.scan(line);
            
            if (tokens.length <= 0) continue;
            
            for (int i = 0; i < tokens.length; i++) {
                Token token = tokens[i];
                Kind kind = token.kind;
                
                System.out.println ("" + token.kind + " " + token.lexeme);
                
            }
        }
    }
}

/** The various kinds of tokens. */
enum Kind {
ID,         // Opcode or identifier (use of a label)
NUM,     // Hexadecimal integer
INT,
SINGLECHAR,   // Register number
DOUBLECHAR,
LPAREN,
RPAREN,
LBRACE,
RBRACE,
RETURN,
IF,
ELSE,
WHILE,
PRINTLN,
WAIN,
BECOMES,
EQ,
NE,
LT,
GT,
LE,
GE,
PLUS,
MINUS,
STAR,
SLASH,
PCT,
COMMA,
SEMI,
NEW,
DELETE,
LBRACK,
RBRACK,
AMP,
NULL,
WHITESPACE; // Whitespace
}

/** Representation of a token. */
class Token {
    public Kind kind;     // The kind of token.
    public String lexeme; // String representation of the actual token in the
                          // source code.
    
    public Token(Kind kind, String lexeme) {
        this.kind = kind;
        this.lexeme = lexeme;
    }
    public String toString() {
        return kind+" {"+lexeme+"}";
    }
    /** Returns an integer representation of the token. For tokens of kind
     * INT (decimal integer constant) and HEXINT (hexadecimal integer
     * constant), returns the integer constant. For tokens of kind
     * REGISTER, returns the register number.
     */
    public int toInt() {
        if(kind == Kind.NUM) return parseLiteral(lexeme, 10, 32);
        //else if(kind == Kind.HEXINT) return parseLiteral(lexeme.substring(2), 16, 32);
        //else if(kind == Kind.REGISTER) return parseLiteral(lexeme.substring(1), 10, 5);
        else {
            System.err.println("ERROR in to-int conversion.");
            System.exit(1);
            return 0;
        }
    }
    private int parseLiteral(String s, int base, int bits) {
        BigInteger x = new BigInteger(s, base);
        if(x.signum() > 0) {
            if(x.bitLength() > bits) {
                System.err.println("ERROR in parsing: constant out of range: "+s);
                System.exit(1);
            }
        } else if(x.signum() < 0) {
            if(x.negate().bitLength() > bits-1
               && x.negate().subtract(new BigInteger("1")).bitLength() > bits-1) {
                System.err.println("ERROR in parsing: constant out of range: "+s);
                System.exit(1);
            }
        }
        return (int) (x.longValue() & ((1L << bits) - 1));
    }
}

/** Lexer -- reads an input line, and partitions it into a list of tokens. */
class Lexer {
    public Lexer() {
        CharSet whitespace = new Chars("\t\n\r ");
        CharSet letters = new Chars(
                                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        CharSet lettersDigits = new Chars(
                                          "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        CharSet digits = new Chars("0123456789");
        CharSet hexDigits = new Chars("0123456789ABCDEFabcdef");
        CharSet oneToNine = new Chars("123456789");
        
        CharSet singleCharacters = new Chars("(){}+-*%,;[]&");
        CharSet doubleCharacters = new Chars("=<>");
        CharSet notCharacter = new Chars("!");
        CharSet slashCharacter = new Chars("/");
        CharSet zeroCharacter = new Chars("0");
        
        CharSet all = new AllChars();
        
        table = new Transition[] {
            new Transition(State.START, whitespace, State.WHITESPACE),
            new Transition(State.START, letters, State.ID),
            new Transition(State.START, zeroCharacter, State.ZERO),
            new Transition(State.START, oneToNine, State.NUM),
            new Transition(State.START, singleCharacters, State.SINGLECHAR),
            new Transition(State.START, doubleCharacters, State.EQ),
            new Transition(State.START, slashCharacter, State.SLASH),
            new Transition(State.START, notCharacter, State.NOT),
            
            new Transition(State.EQ, new Chars("="), State.DOUBLECHAR),
            
            new Transition(State.NOT, new Chars("="), State.DOUBLECHAR),
            
            new Transition(State.ZERO, lettersDigits, State.ERROR),
            
            new Transition(State.NUM, digits, State.NUM),
            
            new Transition(State.ID, lettersDigits, State.ID),
            
            new Transition(State.SLASH, new Chars("/"), State.COMMENT),
            
            new Transition(State.COMMENT, all, State.COMMENT)
        };
    }
    /** Partitions the line passed in as input into an array of tokens.
     * The array of tokens is returned.
     */
    public Token[] scan( String input ) {
        List<Token> ret = new ArrayList<Token>();
        if(input.length() == 0) return new Token[0];
        int i = 0;
        int startIndex = 0;
        State state = State.START;
        while(true) {
            Transition t = null;
            if(i < input.length()) t = findTransition(state, input.charAt(i));
            if(t == null) {
                // no more transitions possible
            	if (state == State.ERROR) {
                    System.err.println("ERROR in lexing after reading 0");
                    System.exit(1);
            	}
            	if(!state.isFinal()) {
                    System.err.println("ERROR in lexing after reading "+input.substring(0, i));
                    System.exit(1);
                }
                if( state.kind != Kind.WHITESPACE ) {
                    Token token = new Token(state.kind,
                                         input.substring(startIndex, i));
                    if (token.kind == Kind.ID) {
                        
                        if (token.lexeme.equals("return"))
                            token.kind = Kind.RETURN;
                        else if (token.lexeme.equals("if"))
                            token.kind = Kind.IF;
                        else if (token.lexeme.equals("else"))
                            token.kind = Kind.ELSE;
                        else if (token.lexeme.equals("while"))
                            token.kind = Kind.WHILE;
                        else if (token.lexeme.equals("println"))
                            token.kind = Kind.PRINTLN;
                        else if (token.lexeme.equals("wain"))
                            token.kind = Kind.WAIN;
                        else if (token.lexeme.equals("int"))
                            token.kind = Kind.INT;
                        else if (token.lexeme.equals("new"))
                            token.kind = Kind.NEW;
                        else if (token.lexeme.equals("delete"))
                            token.kind = Kind.DELETE;
                        else if (token.lexeme.equals("NULL"))
                            token.kind = Kind.NULL;
                        
                    } else if (token.kind == Kind.SINGLECHAR) {
                        
                        if (token.lexeme.equals("("))
                            token.kind = Kind.LPAREN;
                        else if (token.lexeme.equals(")"))
                            token.kind = Kind.RPAREN;
                        else if (token.lexeme.equals("{"))
                            token.kind = Kind.LBRACE;
                        else if (token.lexeme.equals("}"))
                            token.kind = Kind.RBRACE;
                        else if (token.lexeme.equals("="))
                            token.kind = Kind.BECOMES;
                        else if (token.lexeme.equals("<"))
                            token.kind = Kind.LT;
                        else if (token.lexeme.equals(">"))
                            token.kind = Kind.GT;
                        else if (token.lexeme.equals("+"))
                            token.kind = Kind.PLUS;
                        else if (token.lexeme.equals("-"))
                            token.kind = Kind.MINUS;
                        else if (token.lexeme.equals("*"))
                            token.kind = Kind.STAR;
                        else if (token.lexeme.equals("/"))
                            token.kind = Kind.SLASH;
                        else if (token.lexeme.equals("%"))
                            token.kind = Kind.PCT;
                        else if (token.lexeme.equals(","))
                            token.kind = Kind.COMMA;
                        else if (token.lexeme.equals(";"))
                            token.kind = Kind.SEMI;
                        else if (token.lexeme.equals("["))
                            token.kind = Kind.LBRACK;
                        else if (token.lexeme.equals("]"))
                            token.kind = Kind.RBRACK;
                        else if (token.lexeme.equals("&"))
                            token.kind = Kind.AMP;
                        
                    } else if (token.kind == Kind.DOUBLECHAR) {
                        
                        if (token.lexeme.equals("=="))
                            token.kind = Kind.EQ;
                        else if (token.lexeme.equals("!="))
                            token.kind = Kind.NE;
                        else if (token.lexeme.equals("<="))
                            token.kind = Kind.LE;
                        else if (token.lexeme.equals(">="))
                            token.kind = Kind.GE;
                        
                    }

                    ret.add(token);
                }
                startIndex = i;
                state = State.START;
                if(i >= input.length()) break;
            } else {
                state = t.toState;
                i++;
            }
        }
        return ret.toArray(new Token[ret.size()]);
    }
    
    ///////////////////////////////////////////////////////////////
    // END OF PUBLIC METHODS
    ///////////////////////////////////////////////////////////////
    
    private Transition findTransition(State state, char c) {
        for( int j = 0; j < table.length; j++ ) {
            Transition t = table[j];
            if(t.fromState == state && t.chars.contains(c)) {
                return t;
            }
        }
        return null;
    }
    
    private static enum State {
        START(null),
        ID(Kind.ID),
        EQ(Kind.SINGLECHAR),
        ZERO(Kind.NUM),
        DOUBLECHAR(Kind.DOUBLECHAR),
        SINGLECHAR(Kind.SINGLECHAR),
        NOT(null),
        NUM(Kind.NUM),
        ERROR(null),
        SLASH(Kind.SLASH),
        COMMENT(Kind.WHITESPACE),
        WHITESPACE(Kind.WHITESPACE);
        State(Kind kind) {
            this.kind = kind;
        }
        Kind kind;
        boolean isFinal() {
            return kind != null;
        }
    }
    
    private interface CharSet {
        public boolean contains(char newC);
    }
    private class Chars implements CharSet {
        private String chars;
        public Chars(String chars) { this.chars = chars; }
        public boolean contains(char newC) {
            return chars.indexOf(newC) >= 0;
        }
    }
    private class AllChars implements CharSet {
        public boolean contains(char newC) {
            return true;
        }
    }
    
    private class Transition {
        State fromState;
        CharSet chars;
        State toState;
        Transition(State fromState, CharSet chars, State toState) {
            this.fromState = fromState;
            this.chars = chars;
            this.toState = toState;
        }
    }
    private Transition[] table;
}