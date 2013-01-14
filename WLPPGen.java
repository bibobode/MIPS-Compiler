import java.util.*;

/**
 * Starter code for CS241 assignments 9-11 for Spring 2011.
 * 
 * Based on Scheme code by Gord Cormack. Java translation by Ondrej Lhotak.
 * 
 * Version 20081105.1
 *
 * Modified June 30, 2011 by Brad Lushman
 */

public class WLPPGen {
    Scanner in = new Scanner(System.in);

    // The set of terminal symbols in the WLPP grammar.
    Set<String> terminals = new HashSet<String>(Arrays.asList("BOF", "BECOMES", 
         "COMMA", "ELSE", "EOF", "EQ", "GE", "GT", "ID", "IF", "INT", "LBRACE", 
         "LE", "LPAREN", "LT", "MINUS", "NE", "NUM", "PCT", "PLUS", "PRINTLN",
         "RBRACE", "RETURN", "RPAREN", "SEMI", "SLASH", "STAR", "WAIN", "WHILE",
         "AMP", "LBRACK", "RBRACK", "NEW", "DELETE", "NULL"));

    List<String> symbols;

    // Data structure for storing the parse tree.
    public class Tree {
        List<String> rule;
        String val;
        Tree parent;
        String type = "none";

        ArrayList<Tree> children = new ArrayList<Tree>();

        // Does this node's rule match otherRule?
        boolean matches(String otherRule) {
            return tokenize(otherRule).equals(rule);
        }
        
        Tree findChild (String name) {
        	for (int i = 0; i < this.children.size(); i++) {
        		if (this.children.get(i).val.equals(name)) return this.children.get(i);
        	}
        	return null;
        }

        public String ruleString () {
        	String str = "";
        	for (int i = 1; i < this.rule.size(); i++) {
        		str += this.rule.get(i)+ (i == this.rule.size()-1 ? "" : " ");
        	}
        	return str;
        }
    }

    // Divide a string into a list of tokens.
    List<String> tokenize(String line) {
        List<String> ret = new ArrayList<String>();
        Scanner sc = new Scanner(line);
        while (sc.hasNext()) {
            ret.add(sc.next());
        }
        return ret;
    }

    // Read and return wlppi parse tree
    Tree readParse(String lhs, Tree parent) {
        String line = in.nextLine();
        List<String> tokens = tokenize(line);
        Tree ret = new Tree();
        ret.rule = tokens;
        ret.val = tokens.get(0);
        ret.parent = parent;

        if (!terminals.contains(lhs)) {
            Scanner sc = new Scanner(line);
            sc.next(); // discard lhs
            while (sc.hasNext()) {
                String s = sc.next();
                ret.children.add(readParse(s, ret));
            }
        }
        else if (lhs.equals("ID") || lhs.equals("NUM")) {
            Tree var = new Tree();
            var.val = tokens.get(1);
            var.parent = ret;
            ret.children.add(var);
        }
        return ret;
    }

    String getType (Tree t) {
    	if (!t.type.equals("none")) { return t.type; }
    	String type = "none";
    	
    	
    	if (t.rule.size() == 2 && t.rule.get(0).equals("expr") && t.rule.get(1).equals("term")) {
    		//expr -> term
    		Tree termSubtree = t.findChild("term");
    		type = getType(termSubtree);
    	}
    	else if (t.rule.size() == 4 && t.rule.get(0).equals("expr") && t.rule.get(1).equals("expr") && t.rule.get(2).equals("PLUS") && t.rule.get(3).equals("term")) {
    		String left = getType(t.findChild("expr"));
    		String right = getType(t.findChild("term"));
    		if (left.equals("int*") && right.equals("int*")) {
    			bail ("addition of pointers");
    		}
    		else if ((left.equals("int") && right.equals("int*")) || (left.equals("int*") && right.equals("int"))) {
    			type = "int*";
    		}
    		else if (left.equals("int") && right.equals("int")) {
    			type = "int";
    		}
    	}
    	else if (t.rule.size() == 4 && t.rule.get(0).equals("expr") && t.rule.get(1).equals("expr") && t.rule.get(2).equals("MINUS") && t.rule.get(3).equals("term")) {
    		String left = getType(t.findChild("expr"));
    		String right = getType(t.findChild("term"));
    		if (left.equals("int*") && right.equals("int*")) {
    			type = "int";
    		}
    		else if (left.equals("int*") && right.equals("int")) {
    			type = "int*";
    		}
    		else if (left.equals("int") && right.equals("int")) {
    			type = "int";
    		}
    		else {
    			bail ("can't do int minus pointer");
    		}
    	}
    	
    	
    	else if (t.rule.size() == 2 && t.rule.get(0).equals("factor") && t.rule.get(1).equals("ID")) {
    		type = symbolTable.get(symbolTable.indexOf(t.findChild("ID").rule.get(1))+1);
    	}
    	else if (t.rule.size() == 2 && t.rule.get(0).equals("factor") && t.rule.get(1).equals("NUM")) {
    		type = "int";
    	}
    	else if (t.rule.size() == 2 && t.rule.get(0).equals("factor") && t.rule.get(1).equals("NULL")) {
    		type = "int*";
    	}
    	else if (t.rule.size() == 4 && t.rule.get(0).equals("factor") && t.rule.get(1).equals("LPAREN") && t.rule.get(2).equals("expr") && t.rule.get(3).equals("RPAREN")) {
    		type = getType(t.findChild("expr"));
    	}
    	else if (t.rule.size() == 3 && t.rule.get(0).equals("factor") && t.rule.get(1).equals("AMP") && t.rule.get(2).equals("lvalue")) {
    		if (getType(t.findChild("lvalue")).equals("int*")) {
    			bail("ampersand must be followed by integer");
    		}
    		type = "int*";
    	}
    	else if (t.rule.size() == 3 && t.rule.get(0).equals("factor") && t.rule.get(1).equals("STAR") && t.rule.get(2).equals("factor")) {
    		if (getType(t.findChild("factor")).equals("int")) {
    			bail("dereference of int");
    		}
    		type = "int";
    	}
    	else if (t.rule.size() == 6 && t.rule.get(0).equals("factor") && t.rule.get(1).equals("NEW") && t.rule.get(2).equals("INT") && t.rule.get(3).equals("LBRACK") && t.rule.get(4).equals("expr") && t.rule.get(5).equals("RBRACK")) {
    		if (getType(t.findChild("expr")).equals("int*")) {
    			bail("must specify integer value, not pointer");
    		}    		
    		type = "int*";
    	}
    	
    	
    	else if (t.rule.size() == 2 && t.rule.get(0).equals("term") && t.rule.get(1).equals("factor")) {
    		type = getType(t.findChild("factor"));
    	}
    	else if (t.rule.size() == 4 && t.rule.get(0).equals("term") && t.rule.get(1).equals("term") && (t.rule.get(2).equals("STAR") || t.rule.get(2).equals("SLASH") || t.rule.get(2).equals("PCT")) && t.rule.get(3).equals("factor")) {
    		String left = getType(t.findChild("term"));
    		String right = getType(t.findChild("factor"));
    		if (left.equals("int*") || right.equals("int*")) {
    			bail ("%, /, or * done on pointer(s)");
    		}
    		type = "int";
    	}
    	

    	else if (t.rule.size() == 2 && t.rule.get(0).equals("lvalue") && t.rule.get(1).equals("ID")) {
    		type = symbolTable.get(symbolTable.indexOf(t.findChild("ID").rule.get(1))+1);
    	}
    	else if (t.rule.size() == 3 && t.rule.get(0).equals("lvalue") && t.rule.get(1).equals("STAR") && t.rule.get(2).equals("factor")) {
    		if (getType(t.findChild("factor")).equals("int")) {
    			bail("dereference of int");
    		}
    		type = "int";
    	}
    	else if (t.rule.size() == 4 && t.rule.get(0).equals("lvalue") && t.rule.get(1).equals("LPAREN") && t.rule.get(2).equals("lvalue") && t.rule.get(3).equals("RPAREN")) {
    		type = getType(t.findChild("lvalue"));
    	}

    	
    	
    	else if (t.rule.size() == 3 && t.rule.get(0).equals("dcl") && t.rule.get(1).equals("type") && t.rule.get(2).equals("ID")) {
    		String temp = t.children.get(0).rule.get(1);
    		if (t.children.get(0).rule.size() == 3) {
    			temp += " "+t.children.get(0).rule.get(2);
    		}
    		
    		if (temp.equals("INT")) {
				type = "int";
			}
			else if (temp.equals("INT STAR")) {
				type = "int*";
			}
    	}
    	
    	t.type = type;
    	return type;
    }
    
    void checkTypes(Tree t) {

    	if (t.rule.size() == 6 && t.rule.get(0).equals("statement") && t.rule.get(1).equals("PRINTLN") && t.rule.get(2).equals("LPAREN") && t.rule.get(3).equals("expr") && t.rule.get(4).equals("RPAREN") && t.rule.get(5).equals("SEMI")) {
    		if (getType(t.findChild("expr")).equals("int*")) {
    			bail("can only call println on integers");
    		}
    	}
    	else if (t.rule.size() == 5 && t.rule.get(0).equals("statement") && t.rule.get(1).equals("lvalue") && t.rule.get(2).equals("BECOMES") && t.rule.get(3).equals("expr") && t.rule.get(4).equals("SEMI")) {
    		if (!((getType(t.findChild("lvalue"))).equals(getType(t.findChild("expr"))))) {
    			bail("statement's exprs must have same types");
    		}
    	}
    	else if (t.rule.size() == 6 && t.rule.get(0).equals("statement") && t.rule.get(1).equals("DELETE") && t.rule.get(2).equals("LBRACK") && t.rule.get(3).equals("RBRACK") && t.rule.get(4).equals("expr") && t.rule.get(5).equals("SEMI")) {
    		if (getType(t.findChild("expr")).equals("int")) {
    			bail("can only call delete on pointers");
    		}
    	}
    	
    	
    	else if (t.rule.size() == 4 && t.rule.get(0).equals("test") && t.rule.get(1).equals("expr") && (t.rule.get(2).equals("EQ") || t.rule.get(2).equals("NE") || t.rule.get(2).equals("LT") || t.rule.get(2).equals("LE") || t.rule.get(2).equals("GE") || t.rule.get(2).equals("GT")) && t.rule.get(3).equals("expr")) {
    		if (!((getType(t.children.get(0))).equals(getType(t.children.get(2))))) {
    			bail("test's exprs must have same types");
    		}
    	}
    	

    	else if (t.rule.size() == 6 && t.rule.get(0).equals("dcls") && t.rule.get(1).equals("dcls") && t.rule.get(2).equals("dcl") && t.rule.get(3).equals("BECOMES") && t.rule.get(4).equals("NUM") && t.rule.get(5).equals("SEMI")) {
    		if (getType(t.findChild("dcl")).equals("int*")) {
    			bail("dcls must have int");
    		}
    	}
    	else if (t.rule.size() == 6 && t.rule.get(0).equals("dcls") && t.rule.get(1).equals("dcls") && t.rule.get(2).equals("dcl") && t.rule.get(3).equals("BECOMES") && t.rule.get(4).equals("NULL") && t.rule.get(5).equals("SEMI")) {
    		if (getType(t.findChild("dcl")).equals("int")) {
    			bail("dcls must have int*");
    		}
    	}
    }
    
    List<String> symbolTable = new ArrayList<String>();
    
    // Compute symbols defined in t
    void genSymbols(Tree t) {

    	if (!terminals.contains(t.val)) {
    		
    		if (t.val.equals("expr")) {
    			getType(t);
    		}
            checkTypes(t);
    		
    		for (int i = 0; i < t.children.size(); i++) {
    			Tree child = t.children.get(i);
    			if (child.val.equals("ID")) {
    				Tree temp = child;
    				while (!temp.val.equals("dcl") && !temp.val.equals("factor") && !temp.val.equals("lvalue") && temp.parent != null) {
    					temp = temp.parent;
    				}

					String varName = child.rule.get(1);
    				if (temp.val.equals("dcl")) {
    					String type = temp.children.get(0).rule.get(1);
    		    		if (temp.children.get(0).rule.size() == 3) {
    						type += " "+temp.children.get(0).rule.get(2);
    					}
    		    		
    					if (type.equals("INT")) {
        					if (symbolTable.contains(varName)) {
        						bail("variable \""+varName+"\" redeclared");
        					}
    						symbolTable.add(varName);
    						symbolTable.add("int");
    					}
    					else if (type.equals("INT STAR")) {
        					if (symbolTable.contains(varName)) {
        						bail("variable \""+varName+"\" redeclared");
        					}
    						symbolTable.add(varName);
    						symbolTable.add("int*");
    					}
    					else {
    						bail("ID's type is neither int nor int*");
    					}
    				}
    				else if (temp.val.equals("factor") || temp.val.equals("lvalue")) {
    					if (!symbolTable.contains(varName)) {
        					bail("variable \""+varName+"\" used without declaration");
    					}
    				}
    				else {
    					//out("type is:", temp.val+" child is: "+child.val+" "+child.parent.val+" "+child.parent.parent.val);
    					bail("ID mentioned without dcl, factor, or lvalue as parent");
    				}
    			}
    			else {
    				genSymbols (child);
    			}
    		}
    	}
    }
    
    boolean hasChild(Tree t, String name) {
    	boolean has = false;
    	for (int i = 0; i < t.children.size(); i++) {
    		Tree child = t.children.get(i);
    		if (child.val.equals(name)) {
    			has = true;
    			break;
    		}
    		else {
    			has = hasChild(child, name);
    			if (has) break;
    		}
    	}
    	return has;
    }


    // Print an error message and exit the program.
    void bail(String msg) {
        System.err.println("ERROR: " + msg);
        System.exit(0);
    }

    // Generate the code for the parse tree t.
    String genCode(Tree t) {
    	return procedureCode(t.children.get(1));
    }
    
    String procedureCode(Tree t) {
    	assert(t.ruleString().equals("INT WAIN LPAREN dcl COMMA dcl RPAREN LBRACE dcls statements RETURN expr SEMI RBRACE"));
    	
    	String ret = push(31);

		if (getType(t.children.get(3)).equals("int")) {
			ret += "lis $2\n" + ".word 0\n";
		}

		ret += "lis $29\n" + ".word init\n" + "jalr $29\n";
    	
//    	ret += "\n; procedure -> dcl code \n";
    	ret += loadAddr(t.children.get(3).children.get(1), 3) + sw(1, 3);
    	
//    	ret += "\n; procedure -> dcl code \n";
    	ret += loadAddr(t.children.get(5).children.get(1), 3) + sw(2, 3);
    	
//    	ret += "\n; procedure -> dcls code \n";
    	ret += dclsCode(t.children.get(8));
    	
//    	ret += "\n; procedure -> statements code \n";
    	ret += statementsCode(t.children.get(9));
    	
//    	ret += "\n; procedure -> expr code \n";
    	ret += exprCode(t.children.get(11));
    	
    	ret += pop(31);
    	ret += "jr $31\n";
    	
//    	ret += "\n; symbol table addresses \n";
    	for (int i = 0; i < symbolTable.size(); i+= 2) {
    		ret += "V"+symbolTable.get(i)+": .word 0\n";
    	}

//    	ret += "\n; print code \n";
    	ret += printCode;

//    	ret += "\n; alloc code \n";
    	ret += alloc;
    	
    	return ret;
    }
    
    String dclsCode (Tree t) {
    	String ret = "";
    	if (t.ruleString().equals("dcls dcl BECOMES NUM SEMI")) {
//        	ret += "\n; dcls -> dcls dcl BECOMES NUM SEMI code \n";
        	
        	String varVal = t.children.get(3).children.get(0).val;
        	String varName = t.children.get(1).children.get(1).children.get(0).val;
        	
    		ret += dclsCode(t.children.get(0)) + "lis $1\n" + ".word "+varVal+"\n" + "lis $3\n" + ".word "+"V"+varName+"\n" + sw(1, 3);
    	}
    	else if (t.ruleString().equals("dcls dcl BECOMES NULL SEMI")) {
//        	ret += "\n; dcls -> dcls dcl BECOMES NULL SEMI code \n";
        	
        	String varName = t.children.get(1).children.get(1).children.get(0).val;
        	
    		ret += dclsCode(t.children.get(0)) + "lis $1\n" + ".word 1\n" + "lis $3\n" + ".word "+"V"+varName+"\n" + sw(1, 3);
    	}
    	else if (t.ruleString().equals("")) {
//        	ret += "\n; dcls ->  code \n";
    	}
    	else {
    		bail("no match for dcls");
    	}
    	return ret;
    }
    
    String addressCode (Tree t) {
    	String ret = "";
    	if (t.ruleString().equals("ID")) {
//        	ret += "\n; address code for ID \n";
        	ret += loadAddr(t.children.get(0), 3);
    	}
    	else if (t.ruleString().equals("LPAREN lvalue RPAREN")) {
//        	ret += "\n; address code for LPAREN lvalue RPAREN \n";
        	ret += addressCode(t.children.get(1));
    	}
		else if (t.ruleString().equals("STAR factor")) {
//        	ret += "\n; address code for STAR factor \n";
			ret += factorCode(t.children.get(1));
		}
    	else {
    		bail("no match for addressCode");
    	}
    	return ret;
    }
    
    String statementsCode (Tree t) {
    	String ret = "";
    	if (t.ruleString().equals("statements statement")) {
//        	ret += "\n; statements -> statements statement code \n";
    		ret += statementsCode(t.children.get(0)) + statementCode(t.children.get(1));
    	}
    	else if (t.ruleString().equals("")) {
//        	ret += "\n; statements ->  code \n";
    	}
    	else {
    		bail("no match for statements: "+t.ruleString());
    	}
    	return ret;
    }
    
    String testCode (Tree t, String label) {
    	String ret = "";
    	if (t.ruleString().equals("expr LT expr")) {
//        	ret += "\n; test -> expr LT expr code \n";
    		ret += exprCode(t.children.get(0)) + push(3) + exprCode(t.children.get(2)) + pop(1) + "slt $1, $1, $3\n" + "beq $1, $0, L"+label+"\n";
    	}
    	else if (t.ruleString().equals("expr EQ expr")) {
//        	ret += "\n; test -> expr EQ expr code \n";
    		ret += exprCode(t.children.get(0)) + push(3) + exprCode(t.children.get(2)) + pop(1) + "bne $1, $3, L"+label+"\n";
    	}
    	else if (t.ruleString().equals("expr NE expr")) {
//        	ret += "\n; test -> expr NE expr code \n";
    		ret += exprCode(t.children.get(0)) + push(3) + exprCode(t.children.get(2)) + pop(1) + "beq $1, $3, L"+label+"\n";
    	}
    	else if (t.ruleString().equals("expr LE expr")) {
//        	ret += "\n; test -> expr LE expr code \n";
    		ret += exprCode(t.children.get(0)) + push(3) + exprCode(t.children.get(2)) + pop(1) + "slt $1, $3, $1\n" + "bne $1, $0, L"+label+"\n";
    	}
    	else if (t.ruleString().equals("expr GE expr")) {
//        	ret += "\n; test -> expr GE expr code \n";
    		ret += exprCode(t.children.get(0)) + push(3) + exprCode(t.children.get(2)) + pop(1) + "slt $1, $1, $3\n" + "bne $1, $0, L"+label+"\n";
    	}
    	else if (t.ruleString().equals("expr GT expr")) {
//        	ret += "\n; test -> expr GT expr code \n";
    		ret += exprCode(t.children.get(0)) + push(3) + exprCode(t.children.get(2)) + pop(1) + "beq $1, $3, L"+label+"\n" + "slt $1, $1, $3\n" + "bne $1, $0, L"+label+"\n";
    	}
    	else {
    		bail("no match for test: "+t.ruleString());
    	}
    	return ret;
    }

    String statementCode (Tree t) {
    	String ret = "";
    	if (t.ruleString().equals("PRINTLN LPAREN expr RPAREN SEMI")) {
//        	ret += "\n; statement -> PRINTLN statement code \n";
    		ret += exprCode(t.children.get(2)) + "\nadd $1, $3, $0\n" + "lis $29\n" + ".word print\n" + "jalr $29\n";
    	}
    	else if (t.ruleString().equals("lvalue BECOMES expr SEMI")) {
//        	ret += "\n; statement -> lvalue BECOMES expr SEMI code \n";
        	ret += exprCode(t.children.get(2)) + push(3) + addressCode(t.children.get(0)) + pop(1) + sw(1,3);
    	} 
    	else if (t.ruleString().equals("WHILE LPAREN test RPAREN LBRACE statements RBRACE")) {
//        	ret += "\n; statement -> WHILE statement code \n";
        	String begin = newLabel();
        	String end = newLabel();
        	ret += "L"+begin+":\n" + testCode(t.children.get(2), end) + statementsCode(t.children.get(5)) + "beq $0, $0, L"+begin+"\n" + "L"+end+":\n";
    	} 
    	else if (t.ruleString().equals("IF LPAREN test RPAREN LBRACE statements RBRACE ELSE LBRACE statements RBRACE")) {
//        	ret += "\n; statement -> IF statement code \n";
        	String end = newLabel();
        	String elseLabel = newLabel();
        	ret += testCode(t.children.get(2), elseLabel) + statementsCode(t.children.get(5)) + "beq $0, $0, L"+end+"\n" + "L"+elseLabel+":\n" + statementsCode(t.children.get(9)) + "L"+end+":\n";
    	}
		else if (t.ruleString().equals("DELETE LBRACK RBRACK expr SEMI")) {
//        	ret += "\n; statement -> DELETE LBRACK RBRACK expr SEMI code \n";
        	ret += exprCode(t.children.get(3)) + "add $1, $3, $0\n" + "lis $29\n" + ".word delete\n" + "jalr $29\n";
		}
    	else {
    		bail("no match for statement: "+t.ruleString());
    	}
    	return ret;
    }
    
    String exprCode (Tree t) {
    	String ret = "";
    	if (t.ruleString().equals("term")) {
//        	ret += "\n; expr -> term code \n";
    		ret += termCode(t.children.get(0));
    	}
    	else if (t.ruleString().equals("expr PLUS term")) {
//        	ret += "\n; expr -> expr PLUS term code \n";
			if (getType(t.children.get(0)).equals("int") && getType(t.children.get(2)).equals("int")) {
    			ret += exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2)) + pop(1) + "\nadd $3, $1, $3\n";
			}
			else if (getType(t.children.get(0)).equals("int*") && getType(t.children.get(2)).equals("int")) {
    			ret += exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2)) + pop(1) + "lis $4\n" + ".word 4\n" + "mult $3, $4\n" + "mflo $3\n" + "add $3, $1, $3\n";
			}
			else if (getType(t.children.get(0)).equals("int") && getType(t.children.get(2)).equals("int*")) {
    			ret += exprCode(t.children.get(0)) + "lis $4\n" + ".word 4\n" + "mult $3, $4\n" + "mflo $3\n" + push(3) + termCode(t.children.get(2)) + pop(1) + "add $3, $1, $3\n";
			}
			else {
				assert(false);
				bail("addition of two pointers");
			}
    	}
    	else if (t.ruleString().equals("expr MINUS term")) {
//        	ret += "\n; expr -> expr MINUS term code \n";
			if (getType(t.children.get(0)).equals("int") && getType(t.children.get(2)).equals("int")) {
    			ret += exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2)) + pop(1) + "\nsub $3, $1, $3\n";
			}
			else if (getType(t.children.get(0)).equals("int*") && getType(t.children.get(2)).equals("int")) {
    			ret += exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2)) + pop(1) + "lis $4\n" + ".word 4\n" + "mult $3, $4\n" + "mflo $3\n" + "sub $3, $1, $3\n";
			}
			else if (getType(t.children.get(0)).equals("int") && getType(t.children.get(2)).equals("int*")) {
				assert(false);
				bail("subtraction of int and pointer");
			}
			else {
    			ret += exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2)) + pop(1) + "sub $3, $1, $3\n" + "lis $4\n" + ".word 4\n" + "div $3, $4\n" + "mflo $3\n";
			}

    	}
    	else {
    		bail("no match for expr");
    	}
    	return ret;
    }
    
    long labelCounter = 0;
    String newLabel() {
    	labelCounter++;
    	return "n"+labelCounter;
    }
    
    String termCode (Tree t) {
    	String ret = "";
    	if (t.ruleString().equals("factor")) {
//    		ret += "\n; term -> factor code \n";
    		ret += factorCode(t.children.get(0));
    	}
    	else if (t.ruleString().equals("term STAR factor")) {
//    		ret += "\n; term -> term STAR factor code \n";
    		ret += termCode(t.children.get(0)) + push(3) + factorCode(t.children.get(2)) + pop(1) + "\nmult $1, $3\n" + "mflo $3\n";
    	}
    	else if (t.ruleString().equals("term SLASH factor")) {
//    		ret += "\n; term -> term SLASH factor code \n";
    		ret += termCode(t.children.get(0)) + push(3) + factorCode(t.children.get(2)) + pop(1) + "\ndiv $1, $3\n" + "mflo $3\n";
    	}
    	else if (t.ruleString().equals("term PCT factor")) {
//    		ret += "\n; term -> term PCT factor code \n";
    		ret += termCode(t.children.get(0)) + push(3) + factorCode(t.children.get(2)) + pop(1) + "\ndiv $1, $3\n" + "mfhi $3\n";
    	}
    	else {
    		bail("no match for term");
    	}
    	return ret;
    }
    
    String factorCode (Tree t) {
    	String ret = "";
    	if (t.ruleString().equals("ID")) {
//        	ret += "\n; factor -> ID code \n";
    		ret += loadAddr (t.children.get(0), 1) + lw(3, 1);
    	}
    	else if (t.ruleString().equals("LPAREN expr RPAREN")) {
//        	ret += "\n; factor -> LPAREN expr RPAREN code \n";
    		ret += exprCode(t.children.get(1));
    	}
    	else if (t.ruleString().equals("NUM")) {
//        	ret += "\n; factor -> NUM code \n";
    		ret += "lis $3\n";
    		ret += ".word "+t.children.get(0).children.get(0).val+"\n";
    	}
		else if (t.ruleString().equals("NULL")) {
//        	ret += "\n; factor -> NULL code \n";
			ret += "lis $3\n" + ".word 1\n";
		}
		else if (t.ruleString().equals("AMP lvalue")) {
//        	ret += "\n; factor -> AMP lvalue code \n";
			ret += addressCode(t.children.get(1));
		}
		else if (t.ruleString().equals("STAR factor")) {
//        	ret += "\n; factor -> STAR factor code \n";
			ret += factorCode(t.children.get(1)) + lw(3, 3);
		}
		else if (t.ruleString().equals("NEW INT LBRACK expr RBRACK")) {
//        	ret += "\n; factor -> NEW INT LBRACK expr RBRACK code \n";
			ret += exprCode(t.children.get(3)) + "add $1, $3, $0\n" + "lis $29\n" + ".word new\n" + "jalr $29\n";
		}
    	else {
    		bail("no match for factor");
    	}
    	return ret;
    }
    
    String push(int a) {
//    	String ret = "\n; push ("+a+") code \n";
    	String ret = "sw $"+a+", -4($30)\n";
    	ret += "lis $"+a+"\n";
    	ret += ".word 4\n";
    	ret += "sub $30, $30, $"+a+"\n";
    	ret += "lw $"+a+", 0($30)\n";
    	return ret;
    }

    String pop(int a) {
//    	String ret = "\n; pop ("+a+") code \n";
    	String ret = "lis $"+a+"\n";
    	ret += ".word 4\n";
    	ret += "add $30, $30, $"+a+"\n";
    	ret += "lw $"+a+", -4($30)\n";
    	return ret;
    }
    
    String loadAddr(Tree t, int a) {
    	if (t.val.equals("ID")) {
    		return "lis $"+a+"\n.word V" + t.children.get(0).val + "\n";
    	}
    	else {
    		bail("not ID, but instead: "+t.val);
    		return "";
    	}
    }
    
    String sw (int a, int b) {
    	return "sw $" + a + ", 0($" + b + ")\n";
    }

    String lw (int a, int b) {
    	return "lw $" + a + ", 0($" + b + ")\n";
    }
    
    
    
    // Main program
    public static final void main(String args[]) {
        new WLPPGen().go();
    }

    public void go() {
        Tree parseTree = readParse("S", null);

        genSymbols(parseTree);

    	if (getType(parseTree.children.get(1).children.get(11)).equals("int*")) {
			bail("return type must be int");
    	}
    	
    	if (symbolTable.get(3).equals("int*")) {
			bail("second dcl must be an int");
    	}
    	
    	System.out.print(genCode(parseTree));
    	
//        for (int i = 0; i < symbolTable.size(); i+=2)
//    		System.err.println(""+symbolTable.get(i)+" "+symbolTable.get(i+1));
        //printList();
    	
//        out("Tree:", printTree(parseTree));
    	
    	
    	
    	
    }
    
    
    public String printTree (Tree t) {
    	String ret = "";
    	ret = t.val+"\n";
    	for (int i = 0; i < t.children.size(); i++) {
    		Tree child = t.children.get(i);
//    		ret = ret + getType(child) + " " + printTree (child);
    		ret = ret + printTree (child);
    	}
    	return ret;
    }

    public void out(String info, String output) {
    	System.out.println(info+" "+output);
    }
    
    public void printList (List<String>list) {
    	out("List size:", ""+list.size());
    	for (int i = 0; i < list.size(); i++)
    		out (""+i+":", list.get(i));
    }
    
    String printCode = "print: sw $1, -4($30)\n"+
"sw $2, -8($30)\n"+
"sw $3, -12($30)\n"+
"sw $4, -16($30)\n"+
"sw $5, -20($30)\n"+
"sw $6, -24($30)\n"+
"sw $7, -28($30)\n"+
"sw $8, -32($30)\n"+
"sw $9, -36($30)\n"+
"sw $10, -40($30)\n"+
"lis $3\n"+
".word -40\n"+
"add $30, $30, $3\n"+

"lis $3\n"+
".word 0xffff000c\n"+
"lis $4\n"+
".word 10\n"+
"lis $5\n"+
".word 4\n"+
"add $6, $1, $0\n"+
"slt $7, $1, $0\n"+
"beq $7, $0, IfDone\n"+
"lis $8\n"+
".word 0x0000002d\n"+
"sw $8, 0($3)\n"+
"sub $6, $0, $6\n"+

"IfDone: add $9, $30, $0\n"+

"Loop: divu $6, $4\n"+
"mfhi $10\n"+
"sw $10, -4($9)\n"+
"mflo $6\n"+
"sub $9, $9, $5\n"+
"slt $10, $0, $6\n"+
"bne $10, $0, Loop\n"+

"lis $7\n"+
".word 48\n"+
"Loop2: lw $8, 0($9)\n"+
"add $8, $8, $7\n"+
"sw $8, 0($3)\n"+
"add $9, $9, $5\n"+
"bne $9, $30, Loop2\n"+
"sw $4, 0($3)\n"+

"lis $3\n"+
".word 40\n"+
"add $30, $30, $3\n"+
"lw $1, -4($30)\n"+
"lw $2, -8($30)\n"+
"lw $3, -12($30)\n"+
"lw $4, -16($30)\n"+
"lw $5, -20($30)\n"+
"lw $6, -24($30)\n"+
"lw $7, -28($30)\n"+
"lw $8, -32($30)\n"+
"lw $9, -36($30)\n"+
"lw $10, -40($30)\n"+

"jr $31\n";

String alloc = "init:\nsw $1, -4($30)\nsw $2, -8($30)\nsw $3, -12($30)\nsw $4, -16($30)\nsw $5, -20($30)\nsw $6, -24($30)\nsw $7, -28($30)\nsw $8, -32($30)\nlis $4\n.word 32\nsub $30, $30, $4\nlis $1\n.word end\nlis $3\n.word 1024\nlis $6\n.word 16\nlis $7\n.word 4096\nlis $8\n.word 1\nadd $2, $2, $2\nadd $2, $2, $2\nadd $2, $2, $6\nadd $5, $1, $6\nadd $5, $5, $2\nadd $5, $5, $3\nsw $5, 0($1)\nadd $5, $5, $7\nsw $5, 4($1)\nsw $8, 8($1)\nadd $5, $1, $6\nadd $5, $5, $2\nsw $5, 12($1)\nsw $8, 0($5)\nsw $0, 4($5)\nadd $30, $30, $4\nlw $1, -4($30)\nlw $2, -8($30)\nlw $3, -12($30)\nlw $4, -16($30)\nlw $5, -20($30)\nlw $6, -24($30)\nlw $7, -28($30)\nlw $8, -32($30)\njr $31\nnew:\nsw $1, -4($30)\nsw $2, -8($30)\nsw $4, -12($30)\nsw $5, -16($30)\nsw $6, -20($30)\nsw $7, -24($30)\nsw $8, -28($30)\nsw $9, -32($30)\nsw $10, -36($30)\nsw $11, -40($30)\nsw $12, -44($30)\nlis $10\n.word 44\nsub $30, $30, $10\nslt $3, $0, $1\nbeq $3, $0, cleanupN\nlis $11\n.word 1\nadd $1, $1, $11\nadd $1, $1, $1\nadd $1, $1, $1\nadd $2, $11, $11\nadd $4, $0, $0\nsub $1, $1, $11\ntopN:\nbeq $1, $0, endloopN\ndiv $1, $2\nmflo $1\nadd $4, $4, $11\nbeq $0, $0, topN\nendloopN:\nadd $1, $1, $11\nadd $4, $4, $11\nlis $5\n.word 14\nsub $4, $5, $4\nlis $5\n.word 9\nslt $6, $5, $4\nbeq $6, $0, doNotFixN\nadd $4, $5, $0\ndoNotFixN:\nslt $3, $0, $4\nbeq $3, $0, cleanupN\nadd $6, $4, $0\nadd $7, $11, $0\ntop2N:\nadd $7, $7, $7\nsub $6, $6, $11\nbne $6, $0, top2N\nsub $7, $7, $11\nlis $8\n.word findWord\nsw $31, -4($30)\nlis $31\n.word 4\nsub $30, $30, $31\njalr $8\nlis $31\n.word 4\nadd $30, $30, $31\nlw $31, -4($30)\nbeq $3, $0, cleanupN\nadd $7, $7, $11\ndiv $7, $2\nmflo $7\nexactN:\nslt $6, $3, $7\nbne $6, $0, largerN\nbeq $0, $0, convertN\nlargerN:\nadd $3, $3, $3\nlis $6\n.word free\nlw $8, -4($6)\nlw $6, 0($6)\nadd $8, $8, $8\nadd $8, $8, $8\nadd $6, $6, $8\nadd $8, $3, $11\nsw $8, 0($6)\nsw $0, 4($6)\nlis $6\n.word free\nlw $8, -4($6)\nadd $8, $8, $11\nsw $8, -4($6)\nbeq $0, $0, exactN\nconvertN:\nadd $12, $3, $0\nadd $7, $0, $0\nlis $8\n.word end\nlw $9, 4($8)\nlw $8, 0($8)\nsub $9, $9, $8\ntop5N:\nbeq $3, $11, doneconvertN\ndiv $3, $2\nmflo $3\nmfhi $10\nbeq $10, $0, evenN\nadd $7, $7, $9\nevenN:\ndiv $7, $2\nmflo $7\nbeq $0, $0, top5N\ndoneconvertN:\nadd $3, $8, $7\nlis $4\n.word 4\nadd $3, $3, $4\nsw $12, -4($3)\ncleanupN:\nlis $10\n.word 44\nadd $30, $30, $10\nlw $1, -4($30)\nlw $2, -8($30)\nlw $4, -12($30)\nlw $5, -16($30)\nlw $6, -20($30)\nlw $7, -24($30)\nlw $8, -28($30)\nlw $9, -32($30)\nlw $10, -36($30)\nlw $11, -40($30)\nlw $12, -44($30)\njr $31\ndelete:\nsw $1, -4($30)\nsw $2, -8($30)\nsw $3, -12($30)\nsw $4, -16($30)\nsw $5, -20($30)\nsw $6, -24($30)\nsw $11, -28($30)\nsw $12, -32($30)\nsw $14, -36($30)\nlis $6\n.word 36\nsub $30, $30, $6\nlis $11\n.word 1\nlis $12\n.word 2\nlis $14\n.word 4\nlw $2, -4($1)\nnextBuddyD:\nbeq $2, $11, notFoundD\nadd $3, $2, $0\ndiv $3, $12\nmfhi $4\nbeq $4, $0, evenD\nsub $3, $3, $11\nbeq $0, $0, doneParityD\nevenD:\nadd $3, $3, $11\ndoneParityD:\nlis $5\n.word findAndRemove\nsw $31, -4($30)\nsub $30, $30, $14\nadd $1, $3, $0\njalr $5\nadd $30, $30, $14\nlw $31, -4($30)\nbeq $3, $0, notFoundD\ndiv $2, $12\nmflo $2\nbeq $0, $0, nextBuddyD\nnotFoundD:\nlis $4\n.word free\nlw $5, -4($4)\nlw $4, 0($4)\nadd $5, $5, $5\nadd $5, $5, $5\nadd $5, $4, $5\nsw $2, 0($5)\nsw $0, 4($5)\nlis $4\n.word free\nlw $5, -4($4)\nadd $5, $5, $11\nsw $5, -4($4)\nlis $6\n.word 36\nadd $30, $30, $6\nlw $1, -4($30)\nlw $2, -8($30)\nlw $3, -12($30)\nlw $4, -16($30)\nlw $5, -20($30)\nlw $6, -24($30)\nlw $11, -28($30)\nlw $12, -32($30)\nlw $14, -36($30)\njr $31\nfindWord:\nsw $1, -4($30)\nsw $2, -8($30)\nsw $4, -12($30)\nsw $5, -16($30)\nsw $6, -20($30)\nsw $7, -24($30)\nsw $8, -28($30)\nsw $9, -32($30)\nsw $10, -36($30)\nlis $1\n.word 36\nsub $30, $30, $1\nlis $1\n.word free\nlw $2, -4($1)\nlw $1, 0($1)\nlis $4\n.word 4\nlis $9\n.word 1\nadd $3, $0, $0\nadd $10, $0, $0\nbeq $2, $0, cleanupFW\nadd $5, $2, $0\ntopFW:\nlw $6, 0($1)\nslt $8, $7, $6\nbne $8, $0, ineligibleFW\nslt $8, $3, $6\nbeq $8, $0, ineligibleFW\nadd $3, $6, $0\nadd $10, $1, $0\nineligibleFW:\nadd $1, $1, $4\nsub $5, $5, $9\nbne $5, $0, topFW\nbeq $3, $0, cleanupFW\ntop2FW:\nlw $6, 4($10)\nsw $6, 0($10)\nadd $10, $10, $4\nbne $6, $0, top2FW\nlis $2\n.word end\nlw $4, 8($2)\nsub $4, $4, $9\nsw $4, 8($2)\ncleanupFW:\nlis $1\n.word 36\nadd $30, $30, $1\nlw $1, -4($30)\nlw $2, -8($30)\nlw $4, -12($30)\nlw $5, -16($30)\nlw $6, -20($30)\nlw $7, -24($30)\nlw $8, -28($30)\nlw $9, -32($30)\nlw $10, -36($30)\njr $31\nfindAndRemove:\nsw $1, -4($30)\nsw $2, -8($30)\nsw $4, -12($30)\nsw $5, -16($30)\nsw $6, -20($30)\nsw $7, -24($30)\nsw $8, -28($30)\nsw $9, -32($30)\nsw $11, -36($30)\nsw $14, -40($30)\nlis $9\n.word 40\nsub $30, $30, $9\nlis $11\n.word 1\nlis $14\n.word 4\nlis $2\n.word free\nlw $4, -4($2)\nlw $2, 0($2)\nadd $3, $0, $0\nadd $6, $0, $0\nadd $7, $0, $0\ntopFaR:\nbeq $4, $0, cleanupFaR\nlw $5, 0($2)\nbne $5, $1, notEqualFaR\nadd $6, $6, $2\nbeq $0, $0, removeFaR\nnotEqualFaR:\nadd $2, $2, $14\nadd $7, $7, $11\nbne $7, $4, topFaR\nremoveFaR:\nbeq $6, $0, cleanupFaR\ntop2FaR:\nlw $8, 4($2)\nsw $8, 0($2)\nadd $2, $2, $14\nadd $7, $7, $11\nbne $7, $4, top2FaR\nadd $3, $11, $0\nlis $2\n.word free\nlw $5, -4($2)\nsub $5, $5, $11\nsw $5, -4($2)\ncleanupFaR:\nlis $9\n.word 40\nadd $30, $30, $9\nlw $1, -4($30)\nlw $2, -8($30)\nlw $4, -12($30)\nlw $5, -16($30)\nlw $6, -20($30)\nlw $7, -24($30)\nlw $8, -28($30)\nlw $9, -32($30)\nlw $11, -36($30)\nlw $14, -40($30)\njr $31\nprintFreeList:\nsw $1, -4($30)\nsw $2, -8($30)\nsw $3, -12($30)\nsw $4, -16($30)\nsw $5, -20($30)\nsw $6, -24($30)\nsw $7, -28($30)\nsw $8, -32($30)\nlis $6\n.word 32\nsub $30, $30, $6\nlis $3\n.word free\nlis $4\n.word 4\nlis $5\n.word print\nlis $6\n.word 1\nlw $2, -4($3)\nlw $3, 0($3)\ntopPFL:\nbeq $2, $0, endPFL\nlw $1, 0($3)\nsw $31, -4($30)\nsub $30, $30, $4\njalr $5\nadd $30, $30, $4\nlw $31, -4($30)\nadd $3, $3, $4\nsub $2, $2, $6\nbne $2, $0, topPFL\nendPFL:\nlis $6\n.word 0xffff000c\nlis $5\n.word 10\nsw $5, 0($6)\nlis $6\n.word 32\nadd $30, $30, $6\nlw $1, -4($30)\nlw $2, -8($30)\nlw $3, -12($30)\nlw $4, -16($30)\nlw $5, -20($30)\nlw $6, -24($30)\nlw $7, -28($30)\nlw $8, -32($30)\njr $31\nend:\n.word 0\n.word 0\n.word 0\nfree: .word 0\n";


}

