/**
 * Code: Syntax Analyser   SyntaxAnalyser.java
 * Date: 25/02/19
 *
 * Syntax analyser class which extends the AbstractSyntaxAnalyser superclass.
 *
 * @author Harry Baines
 */

import java.io.IOException;

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

  /** Accept a token based on context. */
  @Override
  public void acceptTerminal(int symbol) throws IOException, CompilationException {
    if (nextToken.symbol == symbol) {
      myGenerate.insertTerminal(nextToken);
      nextToken = lex.getNextToken();
    } else {
      myGenerate.reportError(nextToken, "found '" + nextToken.text + "', expected " + Token.getName(symbol));
    }
  }

  /*
    Begin processing the first (top level) token.
    <statement part> ::= begin <statement list> end
  */
  @Override
  public void _statementPart_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("StatementPart");
    increaseTabIndent();

    acceptTerminal(Token.beginSymbol);
    indent();
    _statementList_();

    indent();
    acceptTerminal(Token.endSymbol);

    decreaseTabIndent();
    myGenerate.finishNonterminal("StatementPart");
  }

  /*
    <statement list> ::= <statement> | <statement list> ; <statement>
  */
  public void _statementList_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("StatementList");
    increaseTabIndent();

    _statement_();

    while (nextToken.symbol == Token.semicolonSymbol) {
      indent();
      acceptTerminal(Token.semicolonSymbol);
      indent();
      _statementList_();
    }

    decreaseTabIndent();
    myGenerate.finishNonterminal("StatementList");
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
    myGenerate.commenceNonterminal("Statement");
    increaseTabIndent();

    switch (nextToken.symbol) {
      case (Token.identifier):
        _assignmentStatement_();
        break;
      case (Token.ifSymbol):
        _ifStatement_();
        break;
      case (Token.whileSymbol):
        _whileStatement_();
        break;
      case (Token.callSymbol):
        _procedureStatement_();
        break;
      case (Token.untilSymbol):
        _untilStatement_();
        break;
      case (Token.forSymbol):
        _forStatement_();
        break;
      default:
        break;
    }

    decreaseTabIndent();
    myGenerate.finishNonterminal("Statement");
  }

  /*
    <assignment statement> ::= identifier := <expression> | identifier := stringConstant
  */
  public void _assignmentStatement_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("AssignmentStatement");
    increaseTabIndent();

    String variableIdentifier = nextToken.text;

    acceptTerminal(Token.identifier);
    indent();
    acceptTerminal(Token.becomesSymbol);

    if (nextToken.symbol == Token.stringConstant) {
      indent();
      acceptTerminal(Token.stringConstant);
      indent();
      myGenerate.addVariable(new Variable(variableIdentifier, Variable.Type.STRING));
    } else {
      _expression_();
    }

    decreaseTabIndent();
    myGenerate.finishNonterminal("AssignmentStatement");
  }

  /*
    <if statement> ::= if <condition> then <statement list> end if |
                       if <condition> then <statement list> else <statement list> end if
  */
  public void _ifStatement_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("IfStatement");
    increaseTabIndent();

    // Start if statement
    acceptTerminal(Token.ifSymbol);
    _condition_();

    indent();
    acceptTerminal(Token.thenSymbol);

    indent();
    _statementList_();

    // Optional else statement list
    if (nextToken.symbol == Token.elseSymbol) {
      acceptTerminal(Token.elseSymbol);
      _statementList_();
    }

    // Close if statement
    acceptTerminal(Token.endSymbol);
    acceptTerminal(Token.ifSymbol);

    decreaseTabIndent();
    myGenerate.finishNonterminal("IfStatement");
  }

  /*
    <while statement> ::= while <condition> loop <statement list> end loop
  */
  public void _whileStatement_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("WhileStatement");
    increaseTabIndent();

    acceptTerminal(Token.whileSymbol);
    _condition_();
    
    indent();
    acceptTerminal(Token.loopSymbol);

    indent();
    _statementList_();

    indent();
    acceptTerminal(Token.endSymbol);

    indent();
    acceptTerminal(Token.loopSymbol);

    decreaseTabIndent();
    myGenerate.finishNonterminal("WhileStatement");
  }

  /*
    <procedure statement> ::= call identifier ( <argument list> )
  */
  public void _procedureStatement_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("ProcedureStatement");
    increaseTabIndent();

    acceptTerminal(Token.callSymbol);
    indent();

    acceptTerminal(Token.identifier);
    indent();

    acceptTerminal(Token.leftParenthesis);
    indent();

    _argumentList_();
    indent();

    acceptTerminal(Token.rightParenthesis);
    
    decreaseTabIndent();
    myGenerate.finishNonterminal("ProcedureStatement");
  }

  /*
    <until statement> ::= do <statement list> until <condition>
  */
  public void _untilStatement_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("UntilStatement");
    increaseTabIndent();

    acceptTerminal(Token.doSymbol);
    _statementList_();
    acceptTerminal(Token.untilSymbol);
    _condition_();

    decreaseTabIndent();
    myGenerate.finishNonterminal("UntilStatement");
  }

  /*
    <for statement> ::= for ( <assignment statement> ; <condition> ; <assignment statement> ) do <statement list> end loop
  */
  public void _forStatement_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("ForStatement");
    increaseTabIndent();

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

    decreaseTabIndent();
    myGenerate.finishNonterminal("ForStatement");
  }

  /*
    <argument list> ::= identifier | <argument list> , identifier
  */
  public void _argumentList_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("ArgumentList");
    increaseTabIndent();
    
    acceptTerminal(Token.identifier);

    while (nextToken.symbol == Token.commaSymbol) {
      acceptTerminal(Token.commaSymbol);
      _argumentList_();
    }

    decreaseTabIndent();
    myGenerate.finishNonterminal("ArgumentList");
  }

  /* 
    <condition> ::= identifier <conditional operator> identifier | 
                    identifier <conditional operator> numberConstant |
                    identifier <conditional operator> stringConstant
  */
  public void _condition_() throws IOException, CompilationException {
    indent();
    myGenerate.commenceNonterminal("Condition");
    increaseTabIndent();

    acceptTerminal(Token.identifier);
    _conditionalOperator_();

    switch (nextToken.symbol) {
      case Token.identifier:
        indent();
        acceptTerminal(Token.identifier);
        break;
      case Token.numberConstant:
        indent();
        acceptTerminal(Token.numberConstant);
        break;
      case Token.stringConstant:
        indent();
        acceptTerminal(Token.stringConstant);
        break;
      default:
        break;
    }

    decreaseTabIndent();
    myGenerate.finishNonterminal("Condition");
  }

  /*
    <conditional operator> ::= > | >= | = | /= | < | <=
  */
  public void _conditionalOperator_() throws IOException, CompilationException {
    indent();
    myGenerate.commenceNonterminal("ConditionalOperator");
    increaseTabIndent();

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
        break;
    }
    
    decreaseTabIndent();
    myGenerate.finishNonterminal("ConditionalOperator");
  }

  /*
    <expression> ::= <term> |
                     <expression> + <term> |
                     <expression> - <term>
  */
  public void _expression_() throws IOException, CompilationException {
    indent();
    myGenerate.commenceNonterminal("Expression");
    increaseTabIndent();

    _term_();

    switch (nextToken.symbol) {
      case Token.plusSymbol:
        indent();
        acceptTerminal(Token.plusSymbol);
        _expression_();
        break;
      case Token.minusSymbol:
        indent();
        acceptTerminal(Token.minusSymbol);
        _expression_();
        break;
      default:
        break;
    }

    decreaseTabIndent();
    myGenerate.finishNonterminal("Expression");
  }

  /*
    <term> ::= <factor> | <term> * <factor> | <term> / <factor>
  */
  public void _term_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("Term");
    increaseTabIndent();

    _factor_();

    switch (nextToken.symbol) {
      case Token.timesSymbol:
        indent();
        acceptTerminal(Token.timesSymbol);
        indent();
        _term_();
        break;
      case Token.divideSymbol:
        indent();
        acceptTerminal(Token.divideSymbol);
        indent();
        _term_();
        break;
      default:
        break;
    }

    decreaseTabIndent();
    myGenerate.finishNonterminal("Term");
  }

  /*
    <factor> ::= identifier | numberConstant | ( <expression> )
  */
  public void _factor_() throws IOException, CompilationException {
    myGenerate.commenceNonterminal("Factor");
    increaseTabIndent();
    
    switch (nextToken.symbol) {
      case Token.identifier:
        acceptTerminal(Token.identifier);
        break;
      case Token.numberConstant:
        acceptTerminal(Token.numberConstant);
        break;
      case Token.leftParenthesis:
        acceptTerminal(Token.leftParenthesis);
        _expression_();
        acceptTerminal(Token.rightParenthesis);
        break;
      default:
        myGenerate.reportError(nextToken, "Expected");
        break;
    }

    decreaseTabIndent();
    myGenerate.finishNonterminal("Factor");
  }

  /**
   * Indents a line in the output.txt file showing the parse tree.
   */
  public void indent() {
    System.out.print("\r" + new String(new char[numTabs*2]).replace('\0', ' '));
  }

  /**
   * Increases the indentation by 1 tab and indents that new amount.
   */
  public void increaseTabIndent() {
    numTabs += 1;
    indent();
  }

  /**
   * Decreases the indentation by 1 tab and indents that new amount.
   */
  public void decreaseTabIndent() {
    numTabs -= 1;
    indent();
  }
}