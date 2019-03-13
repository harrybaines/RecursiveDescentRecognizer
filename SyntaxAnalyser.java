/**
 * Code: Syntax Analyser   SyntaxAnalyser.java
 * Date: 25/02/19
 *
 * Syntax analyser class which extends the AbstractSyntaxAnalyser superclass.
 *
 * @author Harry Baines
 */

import java.io.IOException;
import java.lang.Character;

public class SyntaxAnalyser extends AbstractSyntaxAnalyser {

  private int numTabs = 0;  /** Indendation state **/

  /**
   * Initialises a new syntax analyser object for a given file.
   * @param filename the name of the file to read tokens from.
   * @throws IOException if an IOException occured.
   */
  public SyntaxAnalyser(String filename) throws IOException {
    lex = new LexicalAnalyser(filename);
  }

  /** Accept a token based on context and indent the output for the new terminal. */
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

  public String formatErrString(Token token, String expected) {
    return "(" + lex.getFilename() + ":" + token.lineNumber + ") found '" + token.text + "' (" + Token.getName(token.symbol) + "), " + expected;
  }

  public void reportError(String errorStr) throws IOException, CompilationException {
    String formattedErrStr = formatErrString(nextToken, errorStr);
    indent();
    myGenerate.reportError(nextToken, formattedErrStr);
    throw new CompilationException(formattedErrStr, nextToken.symbol);
  }
  
  // Deal with variables - if declaring, indent output, otherwise ignore
  public void addVariable(String variableIdentifier, Variable.Type type) {
    if (myGenerate.getVariable(variableIdentifier) == null) {
      indent();
    }
    myGenerate.addVariable(new Variable(variableIdentifier, type));
  }

  public Variable checkIfDeclared(Token nextToken) throws IOException, CompilationException {
    Variable v = myGenerate.getVariable(nextToken.text);
    if (v == null) {
      reportError("'" + nextToken.text + "' has not been declared");
    }
    return v;
  }

  /**
   * Commences a non terminal symbol by calling the relevant method in the Generate class.
   * This method also provides output indentation for easier reading (+ less repetition).
   *
   * @param nonTerminal the non terminal string to commence.
   */
  public void commenceNonterminal(String nonTerminal) {
    indent();
    myGenerate.commenceNonterminal(nonTerminal);
    increaseTabIndent();
  }

  /**
   * Finishes a non terminal symbol by calling the relevant method in the Generate class.
   * This method also provides output indentation for easier reading (+ less repetition).
   *
   * @param nonTerminal the non terminal string to finish.
   */
  public void finishNonterminal(String nonTerminal) {
    decreaseTabIndent();
    indent();
    myGenerate.finishNonterminal(nonTerminal);
  }

  /*
    Begin processing the first (top level) token.
    <statement part> ::= begin <statement list> end
  */
  @Override
  public void _statementPart_() throws IOException, CompilationException {
    commenceNonterminal("StatementPart");

    acceptTerminal(Token.beginSymbol);
    _statementList_();
    acceptTerminal(Token.endSymbol);

    finishNonterminal("StatementPart");
  }

  /*
    <statement list> ::= <statement> | <statement list> ; <statement>
  */
  public void _statementList_() throws IOException, CompilationException {
    commenceNonterminal("StatementList");

    _statement_();

    if (nextToken.symbol == Token.semicolonSymbol) {
      acceptTerminal(Token.semicolonSymbol);
      _statementList_();
    }

    finishNonterminal("StatementList");
  }

  /*
    <statement> ::= <assignment statement> |
                    <if statement> |
                    <while statement> |
                    <procedure statement> |
                    <until statement> |
                    <for statement>
  */
  public void _statement_() throws IOException, CompilationException {
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
      case (Token.untilSymbol):
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

    finishNonterminal("Statement");
  }

  /*
    <assignment statement> ::= identifier := <expression> | identifier := stringConstant
  */
  public void _assignmentStatement_() throws IOException, CompilationException {
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

    // Attempt to add the variable
    addVariable(variableIdentifier, curType);
    finishNonterminal("AssignmentStatement");
  }

  /*
    <if statement> ::= if <condition> then <statement list> end if |
                       if <condition> then <statement list> else <statement list> end if
  */
  public void _ifStatement_() throws IOException, CompilationException {
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
  }

  /*
    <while statement> ::= while <condition> loop <statement list> end loop
  */
  public void _whileStatement_() throws IOException, CompilationException {
    commenceNonterminal("WhileStatement");

    acceptTerminal(Token.whileSymbol);
    _condition_();
    acceptTerminal(Token.loopSymbol);
    _statementList_();

    acceptTerminal(Token.endSymbol);
    acceptTerminal(Token.loopSymbol);

    finishNonterminal("WhileStatement");
  }

  /*
    <procedure statement> ::= call identifier ( <argument list> )
  */
  public void _procedureStatement_() throws IOException, CompilationException {
    commenceNonterminal("ProcedureStatement");

    acceptTerminal(Token.callSymbol);
    acceptTerminal(Token.identifier);

    acceptTerminal(Token.leftParenthesis);
    _argumentList_();
    acceptTerminal(Token.rightParenthesis);
    
    finishNonterminal("ProcedureStatement");
  }

  /*
    <until statement> ::= do <statement list> until <condition>
  */
  public void _untilStatement_() throws IOException, CompilationException {
    commenceNonterminal("UntilStatement");

    acceptTerminal(Token.doSymbol);
    _statementList_();
    acceptTerminal(Token.untilSymbol);
    _condition_();

    finishNonterminal("UntilStatement");
  }

  /*
    <for statement> ::= for ( <assignment statement> ; <condition> ; <assignment statement> ) do <statement list> end loop
  */
  public void _forStatement_() throws IOException, CompilationException {
    commenceNonterminal("ForStatement");

    acceptTerminal(Token.forSymbol);
    acceptTerminal(Token.leftParenthesis);
    _assignmentStatement_();
    acceptTerminal(Token.semicolonSymbol);
    _condition_();
    acceptTerminal(Token.semicolonSymbol);
    _assignmentStatement_();
    acceptTerminal(Token.rightParenthesis);

    acceptTerminal(Token.doSymbol);
    _statementList_();

    acceptTerminal(Token.endSymbol);
    acceptTerminal(Token.loopSymbol);

    finishNonterminal("ForStatement");
  }

  /*
    <argument list> ::= identifier | <argument list> , identifier
  */
  public void _argumentList_() throws IOException, CompilationException {
    commenceNonterminal("ArgumentList");
    
    acceptTerminal(Token.identifier);

    if (nextToken.symbol == Token.commaSymbol) {
      acceptTerminal(Token.commaSymbol);
      _argumentList_();
    }

    finishNonterminal("ArgumentList");
  }

  /* 
    <condition> ::= identifier <conditional operator> identifier | 
                    identifier <conditional operator> numberConstant |
                    identifier <conditional operator> stringConstant
  */
  public void _condition_() throws IOException, CompilationException {
    commenceNonterminal("Condition");

    acceptTerminal(Token.identifier);
    _conditionalOperator_();

    switch (nextToken.symbol) {
      case Token.identifier:
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
  }

  /*
    <conditional operator> ::= > | >= | = | /= | < | <=
  */
  public void _conditionalOperator_() throws IOException, CompilationException {
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
  }

  /*
    <expression> ::= <term> |
                     <expression> + <term> |
                     <expression> - <term>
  */
  public Variable.Type _expression_() throws IOException, CompilationException {
    commenceNonterminal("Expression");

    String curTokRef = nextToken.text;
    Variable.Type curType = _term_();
    int nextSymbol = nextToken.symbol;

    if (nextSymbol == Token.plusSymbol || nextSymbol == Token.minusSymbol) {
      acceptTerminal(nextSymbol);

      // Can only permit string concatenation (not -,*,/)
      // If the next token is an identifier, check if it is a declared variable - if so, get the type and use that
      // Otherwise check if the nextToken is just a plain string or just a number
      Variable nextVar = null;

      if (nextToken.symbol == Token.identifier) {
        nextVar = checkIfDeclared(nextToken);
        // Cannot add or subtract strings
        if (nextVar != null && (nextVar.type == Variable.Type.STRING && curType != Variable.Type.STRING) || (nextVar.type != Variable.Type.STRING && curType == Variable.Type.STRING)) {
          if (nextSymbol == Token.plusSymbol) {
            reportError("cannot add variable of type " + nextVar.type + " to " + curTokRef + " (" + curType.name + ")");
          } else if (nextSymbol == Token.minusSymbol) {
            reportError("cannot subtract variable of type " + nextVar.type + " from " + curTokRef + " (" + curType.name + ")");
          }
        }
      } else {
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

      _expression_();
    }

    finishNonterminal("Expression");
    return curType;
  }

  /*
    <term> ::= <factor> | <term> * <factor> | <term> / <factor>
  */
  public Variable.Type _term_() throws IOException, CompilationException {
    commenceNonterminal("Term");
    Variable.Type curType = _factor_();

    int nextSymbol = nextToken.symbol;

    if (nextSymbol == Token.timesSymbol || nextSymbol == Token.divideSymbol) {
      acceptTerminal(nextSymbol);

      if (nextSymbol == Token.timesSymbol && curType == Variable.Type.STRING) {
        reportError("cannot multiply " + nextToken.text + " with a String");
      } else if (nextSymbol == Token.divideSymbol && curType == Variable.Type.STRING) {
        reportError("cannot divide " + nextToken.text + " with a String");
      }
      _term_();
    }

    finishNonterminal("Term");
    return curType;
  }

  /*
    <factor> ::= identifier | numberConstant | ( <expression> )
  */
  public Variable.Type _factor_() throws IOException, CompilationException {
    commenceNonterminal("Factor");
    Variable.Type curType = Variable.Type.UNKNOWN;
    
    switch (nextToken.symbol) {
      case Token.identifier:
        // Get the type of this variable identifier (if exists)
        Variable v = checkIfDeclared(nextToken);
        curType = v.type;

        acceptTerminal(Token.identifier);
        break;
      case Token.numberConstant:
        acceptTerminal(Token.numberConstant);
        curType = Variable.Type.NUMBER;
        break;
      case Token.leftParenthesis:
        acceptTerminal(Token.leftParenthesis);
        Variable.Type intermediateType = _expression_();
        acceptTerminal(Token.rightParenthesis);
        break;
      default:
        reportError("expected an identifier, number constant or ( expression )");
        break;
    }

    finishNonterminal("Factor");
    return curType;
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
    if (--numTabs < 0)
      numTabs = 0;
  }
}
