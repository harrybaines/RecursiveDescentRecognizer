import java.util.Set;
import java.util.HashSet;

/**
 * Code: Generate Class   Generate.java
 * Date: 25/02/19
 *
 * Generate class which extends the AbstractSyntaxAnalyser superclass.
 * Used for reporting errors and managing the creation and destruction of variables.
 *
 * @author Harry Baines
 */
public class Generate extends AbstractGenerate {

  private Set<Variable> variables;  /** Stores variable references **/

  /**
   * Initialises the variable references set.
   */
  public Generate() {
    variables = new HashSet<Variable>(); 
  }

  /**
   * Reports an error to the user through the use of a CompilationException.
   * This takes the token found along with an explanatory message and returns a CompilationException.
   *
   * @param token the token found in the input stream where the error occured.
   * @param explanatoryMessage the useful error message to display.
   * @throws CompilationException reporting this information.
   */
  @Override
  public void reportError( Token token, String explanatoryMessage ) throws CompilationException {
    System.out.println("rggERROR " + explanatoryMessage);
    throw new CompilationException(explanatoryMessage, token.lineNumber);
  }

  /**
   * Add a variable to the current symbol list.
   * The variable is only added to the set if it doesn't already exist.
   * If the same variable is being declared again, it is simply re-initialised with a new value.
   * @param v The variable to add.
   */
  @Override
  public void addVariable(Variable v) {
    if (getVariable(v.identifier) == null) {
      variables.add(v);
      System.out.println("rggDECL " + v);
    }
  }

  /**
   * Removes a variable from the current symbol list.
   * The variable is only removed from the set if it exists.
   * If the same variable is being declared again, it is simply re-initialised with a new value.
   * @param v The variable to add
   */
  @Override
  public void removeVariable(Variable v) {
    if (getVariable(v.identifier) != null) {
      variables.remove(v);
      System.out.println("rggDROP " + v);
    }
  }

  /**
   * Should return a single variable object, if the variable is known to the compiler, otherwise null.
   * @param identifier The identifier to match
   * @return A variable object matching the supplied identifier, or null if non exists.
   */
  @Override
  public Variable getVariable(String identifier) {
    Variable returnVar = null;
    for (Variable v : variables) {
      if (v.identifier.equals(identifier)) {
        returnVar = v;
      }
    }
    return returnVar;
  }
}

