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

public class WLPPAnalyzer {
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
        return null;
    }
    
    // Main program
    public static final void main(String args[]) {
        new WLPPAnalyzer().go();
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
        //for (int i = 0; i < symbolTable.size(); i+=2)
    	//	System.err.println(""+symbolTable.get(i)+" "+symbolTable.get(i+1));
        //printList();
    	
        //out("Tree:", printTree(parseTree));
    }
    
    public String printTree (Tree t) {
    	String ret = "";
    	ret = t.val+"\n";
    	//if (!terminals.contains(t.val)) {
    	for (int i = 0; i < t.children.size(); i++) {
    		Tree child = t.children.get(i);
    		ret = ret + getType(child) + " " + printTree (child);
    		//ret += child.rule.get(child.rule.size() > 1 ? 1 : 0)+"\n";
    	}
    	//}
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
}
