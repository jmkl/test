
/**
 * Formula: programmable custom computations.
 * <br>Copyright 2009 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */


package org.hermit.formula;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import org.hermit.formula.parser.FormulaNode;
import org.hermit.formula.parser.FormulaParser;
import org.hermit.formula.parser.FormulaParserConstants;
import org.hermit.formula.parser.FuncNode;
import org.hermit.formula.parser.NameList;
import org.hermit.formula.parser.ParseException;
import org.hermit.formula.parser.ProgramNode;
import org.hermit.formula.parser.TokenMgrError;

import android.util.Log;


/**
 * A view which displays and runs a formula.
 */
public class Interpreter
	implements FormulaParserConstants
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct an interpreter to run a given program.
	 * 
	 * @param	formula			The program to execute.
	 * @throws	ParseException 	Parse error in the given formula.
	 */
	public Interpreter(String formula) throws ParseException {
		currentFormula = parseFormula(formula);

		// Create the table of variables.
		varTable = new VariableTable();
	}


	/**
	 * Re-initialize this interpreter to run the given program.
	 * 
	 * @param	formula			The program to execute.
	 * @throws	ParseException 	Parse error in the given formula.
	 */
	public void reInit(String formula) throws ParseException {
		currentFormula = parseFormula(formula);

		// Create the table of variables.
		varTable.reInit();
	}


	// ******************************************************************** //
	// Formula Parsing.
	// ******************************************************************** //

	/**
	 * Check whether the given formula is valid.
	 * 
	 * @param	formula			The formula to parse.
	 * @return					If the formula is not syntactically correct,
	 * 							an error message; else null.
	 */
	public static String verifyFormula(String formula) {
		try {
			parseFormula(formula);
			return null;
		} catch (ParseException e) {
			return e.getMessage();
		}
	}


	/**
	 * Parse the given formula.
	 * 
	 * @param	formula			The formula to parse.
	 * @return					The parse tree.
	 * @throws	ParseException 	Parse error in the given formula.
	 */
	public static ProgramNode parseFormula(String formula)
		throws ParseException
	{
		// Create a reader which reads from the given String; then
		// create or re-initialize the parser.
		Reader reader = new StringReader(formula);
		try {
			// Either create or re-initialize the parser, as needed.
			// Since the parser is static (see option STATIC in the .jj
			// file), instantiating it is just a trick to give it
			// an input reader.
			if (formulaParser == null)
				formulaParser = new FormulaParser(reader);
			else
				FormulaParser.ReInit(reader);
			
			// Start parsing from the nonterminal "Program".
			try {
				return FormulaParser.Program();
			} catch (TokenMgrError e) {
				throw new ParseException(e.getMessage());
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) { }
		}
	}


	// ******************************************************************** //
	// ParseTree.
	// ******************************************************************** //

	/**
	 * Get the parse tree for the program.
	 * 
	 * @return					The parse tree.
	 */
	public ProgramNode getTree() {
		return currentFormula;
	}
	
	
	// ******************************************************************** //
	// Variables.
	// ******************************************************************** //

	/**
	 * Declare a variable.  This causes the variable to exist, but not
	 * to have a value.  The effect is that if the formula sets a variable
	 * of this name, it sets this one rather than creating a local copy
	 * (unless of course it declares a local with that name); but if the
	 * formula doesn't set it, it still won't have a value.
	 * 
	 * @param	name			Name of the variable to declare.
	 */
	void declareVariable(String name) {
		varTable.declare(name);
	}
	

	/**
	 * Get the state of a named variable.
	 * 
	 * @param	name			Name of the variable to get.
	 * @return					The value, null if it is not set.
	 */
	Double getVariable(String name) {
		return varTable.get(name);
	}
	

	/**
	 * Set the state of a named variable.
	 * 
	 * @param	name			Name of the variable to set.
	 * @param	value			The value to set.
	 */
	void setVariable(String name, double value) {
		varTable.set(name, value);
	}
	

	/**
	 * Delete a named variable from the current context.  This causes
	 * the variable not to exist in this context; it may reveal a variable
	 * with the same name in a lower context.
	 * 
	 * <p>If the variable doesn't exist in the current context, nothing
	 * happens.
	 * 
	 * @param	name			Name of the variable to delete.
	 */
	void deleteVariable(String name) {
		varTable.delete(name);
	}
	

	// ******************************************************************** //
	// Execution.
	// ******************************************************************** //

	/**
	 * Run the program.
	 * 
	 * @throws	SemanticException	A semantic error occurred.
	 */
	public void runProgram() throws SemanticException {
		// Execute the main() function in the formula.
		executeFunction(MAIN_FUNC, new FormulaNode(COMMA));
	}
	
	
	/**
	 * Execute the named function.
	 * 
	 * @param	name		The name of the function.
	 * @param	actuals		A node whose children are the expressions
	 * 						providing the actual parameters to the function.
	 * @return				The return value of the function; 0 if none
	 * 						specified.
	 * @throws	SemanticException	A semantic error occurred.
	 */
	private double executeFunction(String name, FormulaNode actuals) throws SemanticException {
		// Find the function node.
		FuncNode func = currentFormula.getFunc(name);
		if (func == null)
			throw new SemanticException("Formula does not have a \"" + name +
			"\" function.");

		// Check that the parameters match.
		NameList formals = func.getParams();
		if (actuals.getKeyword() != COMMA)
			throw new RuntimeException("Invalid actual parameter node type " +
					actuals.getKeyword());
		if (formals.size() != actuals.getNumChildren())
			throw new SemanticException("Wrong number of parameters to " + name +
					": expected " + formals.size() +
					", got " + actuals.getNumChildren());

		try {
			// Push a level of context on the variable table.
			varTable.push();

			// Make all the parameters into local variables, set to the
			// values of the actual parameters.
			Iterator<String> fnames = formals.iterator();
			Iterator<FormulaNode> avals = actuals.getChildren();
			while (fnames.hasNext() && avals.hasNext()) {
				String formal = fnames.next();
				FormulaNode expr = avals.next();
				varTable.local(formal, evaluateExpr(expr));
			}

			// Execute the function.
			int broke = executeStatement(func.getBody());

			// Check the reason it came back.
			if (broke != 0 && broke != RETURN) {
				if (broke == BREAK)
					throw new SemanticException("\"break\" used outside of a loop.");
				else
					throw new RuntimeException("Unknown break code.");
			}

			// Get the return value, if any.
			Double ret = varTable.getReturn();

			return ret != null ? ret : 0.0;
		} finally {
			// Pop the function's context off the variable table.
			varTable.pop();
		}
	}

	
	/**
	 * Execute the statement represented by a given parse tree.
	 * 
	 * @param	stmt		The parse tree representing the statement.
	 * @return				A token indicating if the control flow was
	 * 						broken.  Zero indicates no break; BREAK indicates
	 * 						that a "break" statement was found; RETURN
	 * 						indicates that a "return" statement was found.
	 * @throws	SemanticException	A semantic error occurred.
	 */
	private int executeStatement(FormulaNode stmt) throws SemanticException {
		FormulaNode expr, s1, s2;
		Iterator<FormulaNode> iter;
		
		int type = stmt.getKeyword();
		int broken = 0;
		switch (type) {
		case LOCAL:
			iter = stmt.getChildren();
			while (iter.hasNext()) {
				s1 = iter.next();
				String lname = s1.getStringValue();
				expr = s1.getChild(0);
				if (expr != null)
					varTable.local(lname, evaluateExpr(expr));
				else
					varTable.declare(lname);
			}
			break;
		case SEMICOL:
			s1 = stmt.getChild(0);
			evaluateExpr(s1);
			break;
		case ASSIGN:
			s1 = stmt.getChild(0);
			s2 = stmt.getChild(1);
			if (s1.getKeyword() != IDENTIFIER)
				throw new RuntimeException("Invalid lvalue in assignment: " +
										   s1.getKeyword());
			String lname = s1.getStringValue();
			double value = evaluateExpr(s2);
			varTable.set(lname, value);
			break;
		case IF:
			expr = stmt.getChild(0);
			s1 = stmt.getChild(1);
			s2 = stmt.getChild(2);
			if (evaluateExpr(expr) != 0)
				broken = executeStatement(s1);
			else if (s2 != null)
				broken = executeStatement(s2);
			break;
		case WHILE:
			expr = stmt.getChild(0);
			s1 = stmt.getChild(1);
			while (evaluateExpr(expr) != 0 && broken == 0)
				broken = executeStatement(s1);
			if (broken == BREAK)
				broken = 0;
			break;
		case BREAK:
			broken = BREAK;
			break;
		case RETURN:
			// Set up the return value, if any.
			expr = stmt.getChild(0);
			if (expr != null)
				varTable.setReturn(evaluateExpr(expr));
			broken = RETURN;
			break;
		case OBRACE:
			iter = stmt.getChildren();
			while (iter.hasNext() && broken == 0)
				broken = executeStatement(iter.next());
			break;
		default:
			throw new RuntimeException("Unknown statement type " + type);
		}
		
		return broken;
	}


	/**
	 * Evaluate the expression represented by a given parse tree.
	 * 
	 * @param	expr		The parse tree representing the expression.
	 * @return				The resulting expression value.
	 * @throws	SemanticException	A semantic error occurred.
	 */
	private double evaluateExpr(FormulaNode expr) throws SemanticException {
		int type = expr.getKeyword();
		FormulaNode left = expr.getChild(0);
		FormulaNode right = expr.getChild(1);
		FormulaNode third = expr.getChild(2);
		double res = 0.0;
		
		switch (type) {
		case QUERY:
			if (evaluateExpr(left) != 0)
				res = evaluateExpr(right);
			else
				res = evaluateExpr(third);
			break;
		case EQ:
			res = evaluateExpr(left) == evaluateExpr(right) ? 1 : 0;
			break;
		case NE:
			res = evaluateExpr(left) != evaluateExpr(right) ? 1 : 0;
			break;
		case LT:
			res = evaluateExpr(left) < evaluateExpr(right) ? 1 : 0;
			break;
		case LE:
			res = evaluateExpr(left) <= evaluateExpr(right) ? 1 : 0;
			break;
		case GT:
			res = evaluateExpr(left) > evaluateExpr(right) ? 1 : 0;
			break;
		case GE:
			res = evaluateExpr(left) >= evaluateExpr(right) ? 1 : 0;
			break;
		case ADD:
			res = evaluateExpr(left);
			if (right != null)
				res += evaluateExpr(right);
			break;
		case SUB:
			res = evaluateExpr(left);
			if (right != null)
				res -= evaluateExpr(right);
			else
				res = -res;
			break;
		case LSHIFT:
			res = (int) evaluateExpr(left) << (int) evaluateExpr(right);
			break;
		case RSHIFT:
			res = (int) evaluateExpr(left) >> (int) evaluateExpr(right);
			break;
		case MUL:
			res = evaluateExpr(left) * evaluateExpr(right);
			break;
		case DIV:
			res = evaluateExpr(left) / evaluateExpr(right);
			break;
		case MOD:
			res = evaluateExpr(left) % evaluateExpr(right);
			break;
		case NOT:
			res = evaluateExpr(left) == 0 ? 1 : 0;
			break;
		case PREINCR:
		case PREDECR:
		case POSTINCR:
		case POSTDECR:
			res = evaluateIncDec(expr);
			break;
		case OPAREN:
			String funcName = expr.getStringValue();
			res = executeFunction(funcName, left);
			break;
		case IDENTIFIER:
			String varName = expr.getStringValue();
			if (!varTable.exists(varName))
				throw new SemanticException("Variable \"" + varName + "\" is not set.");
			res = varTable.get(varName);
			break;
		case FLOAT_LITERAL:
			res = expr.getDoubleValue();
			break;
		default:
			throw new RuntimeException("Unknown expression node type " + type);
		}
		
		Log.i(TAG, expr.toString() + " = " + res);
		return res;
	}


	/**
	 * Evaluate the increment or decrement expression represented
	 * by a given parse tree.  This consists of the ++ or -- operator
	 * applied to an lvalue.
	 * 
	 * @param	expr		The parse tree representing the expression.
	 * @return				The resulting expression value.
	 * @throws	SemanticException	A semantic error occurred.
	 */
	private double evaluateIncDec(FormulaNode expr) throws SemanticException {
		int type = expr.getKeyword();

		// Get the operand, which must be a variable.
		FormulaNode left = expr.getChild(0);
		if (left.getKeyword() != IDENTIFIER)
			throw new RuntimeException("Invalid lvalue in " +
									   tokenImage[type] + ": " +
									   left.getKeyword());
	
		// Get the variable.
		String varName = left.getStringValue();
		if (!varTable.exists(varName))
			throw new SemanticException("Variable \"" + varName +
										"\" is not set in " +
									    tokenImage[type] + ".");
		double value = varTable.get(varName);
		
		// Apply the operator.
		double res;
		switch (type) {
		case PREINCR:
			res = ++value;
			break;
		case PREDECR:
			res = --value;
			break;
		case POSTINCR:
			res = value++;
			break;
		case POSTDECR:
			res = value--;
			break;
		default:
			throw new RuntimeException("Invalid inc/dec type.");
		}
		varTable.set(varName, value);
		
		return res;
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	private static final String TAG = "formula";

	// The name of the "main" function.
	private static final String MAIN_FUNC = "main";

	// Our formula parser.  This is constructed once and used repeatedly.
	// This will be null until we initialize it.
	private static FormulaParser formulaParser = null;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// The current formula.
	private ProgramNode currentFormula;

	// The table of variables in the current formula.
	private VariableTable varTable;
	
}

