import java.io.IOException;
import java.lang.Character;

/**
 * Code: Syntax Analyser   SyntaxAnalyser.java
 * Date: 25/02/19
 *
 * Syntax analyser class which extends the AbstractSyntaxAnalyser superclass.
 * Used for analysing the terminals and non-terminals contained within a given file.
 *
 * @author Harry Baines
 */

public class SyntaxAnalyser extends AbstractSyntaxAnalyser {

  private int numTabs = 0;  /** Indendation state **/

  /**
   * Initialises a new syntax analyser object for a given file.
   * @param filename the name of the file to read tokens from.
   * @throws IOException if an IOException occured such as the provided file doesn't exist.
   */
  public SyntaxAnalyser(String filename) throws IOException {
    try {
      lex = new LexicalAnalyser(filename);
    } catch (IOException e) {
      System.out.println("Couldn't create a lexical analyser for file: " + filename);
    }
  }

  /**
   * Accept a token based on context and indent the output for the new terminal.
   * @param symbol the next terminal symbol to accept at this point.
   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  @Override
  public void acceptTerminal(int symbol) throws IOException, CompilationException {
    indent();
    if (nextToken.symbol == symbol) {
      myGenerate.insertTerminal(nextToken);
      nextToken = lex.getNextToken();
    } else {
      reportError("expected " + Token.getName(symbol));
    }
  }

  /** ================================================================================
          Non-terminal methods to parse non-terminals from the provided grammar
      ================================================================================ **/

  /**
    Begin processing the first (top level) token.

    <statement part> ::= begin <statement list> end

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  @Override
  public void _statementPart_() throws IOException, CompilationException {
    try {
      commenceNonterminal("StatementPart");

      acceptTerminal(Token.beginSymbol);
      _statementList_();
      acceptTerminal(Token.endSymbol);

      finishNonterminal("StatementPart");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _statementPart_", nextToken.lineNumber, e);
    }
    
  }

  /**
    Begin processing a list of statements.

    <statement list> ::= <statement> | <statement list> ; <statement>

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _statementList_() throws IOException, CompilationException {
    try {
      commenceNonterminal("StatementList");

      _statement_();

      if (nextToken.symbol == Token.semicolonSymbol) {
        acceptTerminal(Token.semicolonSymbol);
        _statementList_();
      }

      finishNonterminal("StatementList");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _statementList_", nextToken.lineNumber, e);
    }
  }

  /**
    Begin processing a statement.

    <statement> ::= <assignment statement> |
                    <if statement> |
                    <while statement> |
                    <procedure statement> |
                    <until statement> |
                    <for statement>

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _statement_() throws IOException, CompilationException {
    try {
      switch (nextToken.symbol) {
        case (Token.identifier):
          commenceNonterminal("Statement");
          _assignmentStatement_();
          break;
        case (Token.ifSymbol):
          commenceNonterminal("Statement");
          _ifStatement_();
          break;
        case (Token.whileSymbol):
          commenceNonterminal("Statement");
          _whileStatement_();
          break;
        case (Token.callSymbol):
          commenceNonterminal("Statement");
          _procedureStatement_();
          break;
        case (Token.doSymbol):
          commenceNonterminal("Statement");
          _untilStatement_();
          break;
        case (Token.forSymbol):
          commenceNonterminal("Statement");
          _forStatement_();
          break;
        default:
          reportError("expected a statement (if/while/procedure/until/for)");
          break;
      }
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _statement_", nextToken.lineNumber, e);
    }

    finishNonterminal("Statement");
  }

  /**
    Begin processing an assignment statement.

    <assignment statement> ::= identifier := <expression> | identifier := stringConstant

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public Variable _assignmentStatement_() throws IOException, CompilationException {
    Variable v = null;
    try {
      commenceNonterminal("AssignmentStatement");
      Variable.Type curType = Variable.Type.UNKNOWN;
      
      // Keep reference to variable identifier
      String variableIdentifier = nextToken.text;
      acceptTerminal(Token.identifier);
      acceptTerminal(Token.becomesSymbol);

      if (nextToken.symbol == Token.stringConstant) {
        acceptTerminal(Token.stringConstant);
        curType = Variable.Type.STRING;
      } else {
        curType = _expression_();
      }

      // Attempt to add the variable (if it already exists, don't declare again just re-use)
      v = addVariable(variableIdentifier, curType);
      finishNonterminal("AssignmentStatement");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _assignmentStatement_", nextToken.lineNumber, e);
    }
    return v;
  }

  /**
    Begin processing an if statement.

    <if statement> ::= if <condition> then <statement list> end if |
                       if <condition> then <statement list> else <statement list> end if
   
   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _ifStatement_() throws IOException, CompilationException {
    try {
      commenceNonterminal("IfStatement");

      // Start if statement
      acceptTerminal(Token.ifSymbol);
      _condition_();
      acceptTerminal(Token.thenSymbol);
      _statementList_();

      // Optional else statement list
      if (nextToken.symbol == Token.elseSymbol) {
        acceptTerminal(Token.elseSymbol);
        _statementList_();
      }

      // Close if statement
      acceptTerminal(Token.endSymbol);
      acceptTerminal(Token.ifSymbol);

      finishNonterminal("IfStatement");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _ifStatement_", nextToken.lineNumber, e);
    }
  }

  /**
    Begin processing a while statement.

    <while statement> ::= while <condition> loop <statement list> end loop

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _whileStatement_() throws IOException, CompilationException {
    try {
      commenceNonterminal("WhileStatement");

      acceptTerminal(Token.whileSymbol);
      _condition_();
      acceptTerminal(Token.loopSymbol);
      _statementList_();

      acceptTerminal(Token.endSymbol);
      acceptTerminal(Token.loopSymbol);

      finishNonterminal("WhileStatement");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _whileStatement_", nextToken.lineNumber, e);
    }
  }

  /**
    Begin processing a procedure statement.

    <procedure statement> ::= call identifier ( <argument list> )

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _procedureStatement_() throws IOException, CompilationException {
    try {
      commenceNonterminal("ProcedureStatement");

      acceptTerminal(Token.callSymbol);
      acceptTerminal(Token.identifier);

      acceptTerminal(Token.leftParenthesis);
      _argumentList_();
      acceptTerminal(Token.rightParenthesis);
      
      finishNonterminal("ProcedureStatement");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _procedureStatement_", nextToken.lineNumber, e);
    }
  }

  /**
    Begin processing an until statement.

    <until statement> ::= do <statement list> until <condition>

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _untilStatement_() throws IOException, CompilationException {
    try {
      commenceNonterminal("UntilStatement");

      acceptTerminal(Token.doSymbol);
      _statementList_();
      acceptTerminal(Token.untilSymbol);
      _condition_();

      finishNonterminal("UntilStatement");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _untilStatement_", nextToken.lineNumber, e);
    }
  }

  /**
    Begin processing a for statement.

    <for statement> ::= for ( <assignment statement> ; <condition> ; <assignment statement> ) do <statement list> end loop

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _forStatement_() throws IOException, CompilationException {
    try {
      commenceNonterminal("ForStatement");

      acceptTerminal(Token.forSymbol);
      acceptTerminal(Token.leftParenthesis);

      // Store first assigned variable
      Variable firstTempVariable = _assignmentStatement_();
      acceptTerminal(Token.semicolonSymbol);
      _condition_();
      acceptTerminal(Token.semicolonSymbol);

      // Store second assigned variable
      Variable secondTempVariable =_assignmentStatement_();
      acceptTerminal(Token.rightParenthesis);

      acceptTerminal(Token.doSymbol);
      _statementList_();

      acceptTerminal(Token.endSymbol);
      acceptTerminal(Token.loopSymbol);

      finishNonterminal("ForStatement");

      // Remove any temporary variables (if only existing in this scope - otherwise keep)
      removeVariable(firstTempVariable);
      removeVariable(secondTempVariable);
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _forStatement_", nextToken.lineNumber, e);
    }
  }

  /**
    Begin processing an argument list.

    <argument list> ::= identifier | <argument list> , identifier
  
   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _argumentList_() throws IOException, CompilationException {
    try {
      commenceNonterminal("ArgumentList");
      
      // Check to see if a variable with this identifier actually exists
      checkIfDeclared(nextToken.text);
      acceptTerminal(Token.identifier);

      if (nextToken.symbol == Token.commaSymbol) {
        acceptTerminal(Token.commaSymbol);
        _argumentList_();
      }

      finishNonterminal("ArgumentList");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _argumentList_", nextToken.lineNumber, e);
    }
  }

  /**
    Begin processing a condition.

    <condition> ::= identifier <conditional operator> identifier | 
                    identifier <conditional operator> numberConstant |
                    identifier <conditional operator> stringConstant

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _condition_() throws IOException, CompilationException {
    try {
      commenceNonterminal("Condition");

      // Check to see if a variable with this identifier actually exists
      checkIfDeclared(nextToken.text);
      acceptTerminal(Token.identifier);
      _conditionalOperator_();

      switch (nextToken.symbol) {
        case Token.identifier:
          checkIfDeclared(nextToken.text);
          acceptTerminal(Token.identifier);
          break;
        case Token.numberConstant:
          acceptTerminal(Token.numberConstant);
          break;
        case Token.stringConstant:
          acceptTerminal(Token.stringConstant);
          break;
        default:
          reportError("expected identifier/number/string");
          break;
      }

      finishNonterminal("Condition");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _condition_", nextToken.lineNumber, e);
    }
  }

  /**
    Begin processing a conditional operator.

    <conditional operator> ::= > | >= | = | /= | < | <=

   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void _conditionalOperator_() throws IOException, CompilationException {
    try {
      commenceNonterminal("ConditionalOperator");

      switch (nextToken.symbol) {
        case Token.greaterThanSymbol:
          acceptTerminal(Token.greaterThanSymbol);
          break;
        case Token.greaterEqualSymbol:
          acceptTerminal(Token.greaterEqualSymbol);
          break;
        case Token.equalSymbol:
          acceptTerminal(Token.equalSymbol);
          break;
        case Token.notEqualSymbol:
          acceptTerminal(Token.notEqualSymbol);
          break;
        case Token.lessThanSymbol:
          acceptTerminal(Token.lessThanSymbol);
          break;
        case Token.lessEqualSymbol:
          acceptTerminal(Token.lessEqualSymbol);
          break;
        default:
          reportError("expected a conditional operator (>, >=, =, /=, <, <=)");
          break;
      }
      
      finishNonterminal("ConditionalOperator");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _conditionalOperator_", nextToken.lineNumber, e);
    }
  }

  /**
    Begin processing an expression.

    <expression> ::= <term> |
                     <expression> + <term> |
                     <expression> - <term>
  
   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public Variable.Type _expression_() throws IOException, CompilationException {
    Variable.Type curType = null;
    try {
      commenceNonterminal("Expression");

      String curTokRef = nextToken.text;
      curType = _term_();
      int nextSymbol = nextToken.symbol;

      if (nextSymbol == Token.plusSymbol || nextSymbol == Token.minusSymbol) {
        acceptTerminal(nextSymbol);

        // String operation checks (cannot  subtract strings or add strings to any other types)
        // If the next token is an identifier, check if it is a declared variable - if so, get the type and use that
        // Otherwise check if the nextToken is just a plain string or just a number
        Variable nextVar = null;

        if (nextToken.symbol == Token.identifier) {
          nextVar = checkIfDeclared(nextToken.text);

          // Don't allow subtraction of strings (but skip + because that is allowed)
          if (nextVar != null && (nextVar.type == Variable.Type.STRING && curType == Variable.Type.STRING)) {
            if (nextSymbol == Token.minusSymbol) {
              reportError("cannot subtract variable of type " + nextVar.type + " from " + curTokRef + " (" + curType.name + ")");
            }
          }

          // Cannot add or subtract strings with any other type
          if (nextVar != null && (nextVar.type == Variable.Type.STRING && curType != Variable.Type.STRING) || (nextVar.type != Variable.Type.STRING && curType == Variable.Type.STRING)) {
            if (nextSymbol == Token.plusSymbol) {
              reportError("cannot add variable of type " + nextVar.type + " to " + curTokRef + " (" + curType.name + ")");
            } else if (nextSymbol == Token.minusSymbol) {
              reportError("cannot subtract variable of type " + nextVar.type + " from " + curTokRef + " (" + curType.name + ")");
            }
          }
        } else {
          // Cannot allow adding plain strings to anything other than strings
          if (curType == Variable.Type.STRING && nextToken.symbol != Token.stringConstant) {
            if (nextSymbol == Token.plusSymbol) {
              reportError("cannot be added to " + curTokRef + " (" + curType.name + ")");
            } else if (nextSymbol == Token.minusSymbol) {
              reportError("cannot be subtracted from " + curTokRef + " (" + curType.name + ")");
            }
          } else if (curType != Variable.Type.STRING && nextToken.symbol == Token.stringConstant) {
            if (nextSymbol == Token.plusSymbol) {
              reportError("cannot be added to " + curTokRef + " (" + curType.name + ")");
            } else if (nextSymbol == Token.minusSymbol) {
              reportError("cannot be subtracted from " + curTokRef + " (" + curType.name + ")");
            }
          }
        }

        // Check if the resulting types aren't equal and if the whole expression is a string or the next term is a string
        Variable.Type expressionType = _expression_();
        if (curType != expressionType && (curType == Variable.Type.STRING || expressionType == Variable.Type.STRING)) {
          if (nextSymbol == Token.plusSymbol) {
            reportError("cannot add expressions of type Number to type String");
          } else if (nextSymbol == Token.minusSymbol) {
            reportError("cannot subtract expressions of type Number to type String");
          } 
        }
      }

      finishNonterminal("Expression");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _expression_", nextToken.lineNumber, e);
    }
    return curType;
  }

  /**
    Begin processing a term.

    <term> ::= <factor> | <term> * <factor> | <term> / <factor>
  
   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public Variable.Type _term_() throws IOException, CompilationException {
    Variable.Type curType = null;
    try {
      commenceNonterminal("Term");

      String curTokRef = nextToken.text;
      curType = _factor_();
      int nextSymbol = nextToken.symbol;

      if (nextSymbol == Token.timesSymbol || nextSymbol == Token.divideSymbol) {
        acceptTerminal(nextSymbol);

        // String operation checks (cannot multiply or divide strings with any other type)
        // If the next token is an identifier, check if it is a declared variable - if so, get the type and use that
        // Otherwise check if the nextToken is just a plain string or just a number
        Variable nextVar = null;

        if (nextToken.symbol == Token.identifier) {
          nextVar = checkIfDeclared(nextToken.text);

          // Don't allow multiplication or division of strings
          if (nextVar != null && (nextVar.type == Variable.Type.STRING && curType == Variable.Type.STRING)) {
            if (nextSymbol == Token.timesSymbol) {
              reportError("cannot multiply variable of type " + nextVar.type + " to " + curTokRef + " (" + curType.name + ")");
            } else if (nextSymbol == Token.divideSymbol) {
              reportError("cannot divide variable of type " + nextVar.type + " into " + curTokRef + " (" + curType.name + ")");
            }
          }

          // Cannot mutliply or divide strings with strings or any other types
          if (nextVar != null && (nextVar.type == Variable.Type.STRING && curType != Variable.Type.STRING) || (nextVar.type != Variable.Type.STRING && curType == Variable.Type.STRING)) {
            if (nextSymbol == Token.timesSymbol) {
              reportError("cannot multiply variable of type " + nextVar.type + " to " + curTokRef + " (" + curType.name + ")");
            } else if (nextSymbol == Token.divideSymbol) {
              reportError("cannot divide variable of type " + nextVar.type + " into " + curTokRef + " (" + curType.name + ")");
            }
          }
        } else {
          // Cannot allow multiplying or dividing plain strings with anything other than strings
          if (curType == Variable.Type.STRING && nextToken.symbol != Token.stringConstant) {
            if (nextSymbol == Token.timesSymbol) {
              reportError("cannot multiply " + nextToken.text + " with a String");
            } else if (nextSymbol == Token.divideSymbol) {
              reportError("cannot divide " + nextToken.text + " into a String");
            }
          } else if (curType != Variable.Type.STRING && nextToken.symbol == Token.stringConstant) {
            if (nextSymbol == Token.timesSymbol) {
              reportError("cannot multiply " + curTokRef + " with a String");
            } else if (nextSymbol == Token.divideSymbol) {
              reportError("cannot divide " + curTokRef + " by a String");
            }
          }
        }

        // Check if the resulting types aren't equal and if the whole term is a string or the next term is a string
        Variable.Type expressionType = _term_();
        if (curType != expressionType && (curType == Variable.Type.STRING || expressionType == Variable.Type.STRING)) {
          if (nextSymbol == Token.timesSymbol) {
            reportError("cannot multiply terms of type Number with type String");
          } else if (nextSymbol == Token.divideSymbol) {
            reportError("cannot divide expressions of type Number with type String");
          } 
        }
      }

      finishNonterminal("Term");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _term_", nextToken.lineNumber, e);
    }
    return curType;
  }

  /**
    Begin processing a factor.

    <factor> ::= identifier | numberConstant | ( <expression> )
  
   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public Variable.Type _factor_() throws IOException, CompilationException {
    Variable.Type curType = null;
    try {
      commenceNonterminal("Factor");
      curType = Variable.Type.UNKNOWN;
      
      switch (nextToken.symbol) {
        case Token.identifier:
          // Get the type of this variable identifier (if exists)
          Variable v = checkIfDeclared(nextToken.text);
          curType = v.type;

          acceptTerminal(Token.identifier);
          break;
        case Token.numberConstant:
          acceptTerminal(Token.numberConstant);
          curType = Variable.Type.NUMBER;
          break;
        case Token.leftParenthesis:
          acceptTerminal(Token.leftParenthesis);
          curType = _expression_();
          acceptTerminal(Token.rightParenthesis);
          break;
        default:
          reportError("expected an identifier, number constant or ( expression )");
          break;
      }

      finishNonterminal("Factor");
    } catch (CompilationException e) {
      throw new CompilationException("compilation error parsing _factor_", nextToken.lineNumber, e);
    }
    return curType;
  }

  /** ==========================================================================================
          Error methods, variable declaration/destruction methods and output methods
      ========================================================================================== **/

  /**
   * Reports an error to the output.txt file and throws a CompilationException at the end.
   * @param errorStr the human-readable error to output.
   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   */
  public void reportError(String errorStr) throws IOException, CompilationException {
    String formattedErrStr = formatErrString(errorStr);
    indent();
    myGenerate.reportError(nextToken, formattedErrStr);
  }

  /**
   * Formats an error string to be output to the output.txt file.
   * @param expected the string containing details of what is expected next.
   * @return the formatted error string.
   */
  public String formatErrString(String expected) {
    return "(" + lex.getFilename() + ":" + nextToken.lineNumber + ") found '" + nextToken.text + "' (" + Token.getName(nextToken.symbol) + "), " + expected;
  }

  /**
   * Checks if a provided token has been declared yet based on its text (i.e. identifier).
   * @param identifier the identifier to check.
   * @throws IOException if an IOException occured.
   * @throws CompilationException if a compilation error occured.
   * @return the declared variable.
   */
  public Variable checkIfDeclared(String identifier) throws IOException, CompilationException {
    Variable v = myGenerate.getVariable(identifier);
    if (v == null) {
      reportError("variable with identifier '" + identifier + "' has not been declared");
    }
    return v;
  }

  /**
   * Commences a non terminal symbol by calling the relevant method in the Generate class.
   * This method also provides output indentation for easier reading (+ less code repetition).
   * @param nonTerminal the non terminal string to commence.
   */
  public void commenceNonterminal(String nonTerminal) {
    indent();
    myGenerate.commenceNonterminal(nonTerminal);
    increaseTabIndent();
  }

  /**
   * Finishes a non terminal symbol by calling the relevant method in the Generate class.
   * This method also provides output indentation for easier reading (+ less code repetition).
   * @param nonTerminal the non terminal string to finish.
   */
  public void finishNonterminal(String nonTerminal) {
    decreaseTabIndent();
    indent();
    myGenerate.finishNonterminal(nonTerminal);
  }

  /**
   * Attempt to add the variable to the list.
   * If declaring, indent output ready for printing, otherwise ignore.
   * @param variableIdentifier the identifier of the variable to add.
   * @param type the type of the variable.
   * @return the added variable.
   */
  public Variable addVariable(String variableIdentifier, Variable.Type type) {
    Variable v = null;
    if (myGenerate.getVariable(variableIdentifier) == null) {
      indent();
      v = new Variable(variableIdentifier, type);
      myGenerate.addVariable(v);
    }
    return v;
  }

  /**
   * Removes a variable from the list. 
   * If the variable hasn't been removed yet, indent the output ready for printing and remove it.
   * @param v the variable to remove.
   */
  public void removeVariable(Variable v) {
    if (v != null && myGenerate.getVariable(v.identifier) != null) {
      indent();
      myGenerate.removeVariable(v);
    }
  }

  /**
   * Indents a line in the output.txt file showing the parse tree.
   */
  public void indent() {
    System.out.print(new String(new char[numTabs*2]).replace('\0', ' '));
  }

  /**
   * Increases the indentation by 1 tab and indents that new amount.
   */
  public void increaseTabIndent() {
    numTabs += 1;
  }

  /**
   * Decreases the indentation by 1 tab and indents that new amount.
   */
  public void decreaseTabIndent() {
    if (--numTabs < 0) {
      numTabs = 0;
    }
  }
}
