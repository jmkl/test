
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


import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;

import org.hermit.android.notice.ErrorDialog;
import org.hermit.formula.parser.FormulaParserConstants;
import org.hermit.formula.parser.InputNode;
import org.hermit.formula.parser.OutputNode;
import org.hermit.formula.parser.ParseException;
import org.hermit.formula.parser.ProgramNode;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


/**
 * A view which displays and runs a formula.
 */
public class FormulaView
	extends TableLayout
	implements FormulaParserConstants
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a formula view.
	 * 
	 * @param	context			Context we're running in.
	 */
	public FormulaView(Context context) {
		super(context);
        init(context);
	}


    /**
     * Construct a formula view from a given attribute set.  This is
     * required to allow this widget to be used from XML layouts.
     * 
     * @param	context			Context we're running in.
     * @param	attrs			Attributes for this widget.
     */
    public FormulaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    
    /**
     * Set up this view.
     * 
     * @param	context			Context we're running in.
     */
    private void init(Context context) {
		appContext = context;
		
		// Allow the values column to resize.
		setColumnShrinkable(1, true);
		setColumnStretchable(1, true);
		
		// Create a dialog for errors.
        errorDialog = new ErrorDialog(context, R.string.button_close);
    }
    

	// ******************************************************************** //
	// View Control.
	// ******************************************************************** //

	/**
	 * Set the formula to be displayed and run in this view.
	 * 
	 * @param	formula			The formula to display.
	 */
    public void setFormula(String formula)  {
    	// Make sure we leave it at null in case of an exception.
    	currentFormula = null;

    	try {
    		// Now try to parse the formula.
    		if (formulaInterp == null)
    			formulaInterp = new Interpreter(formula);
    		else
    			formulaInterp.reInit(formula);
    		currentFormula = formulaInterp.getTree();
    		
    		removeAllViews();
    		setGui(currentFormula);
    	} catch (ParseException e) {
    		errorDialog.show("Parse error in formula: " + e.getMessage());
    	}
    }


	/**
	 * Clear this view.
	 */
	public void clearFormula() {
		formulaInterp = null;
		removeAllViews();
	}
	
	
	// ******************************************************************** //
	// Gui Building.
	// ******************************************************************** //

	private void setGui(ProgramNode formula) {
		widgetTable = new HashMap<String, View>();
		createInputs(formula.getInputs());
		createControls();
		createOutputs(formula.getOutputs());
	}
	

	private void createInputs(Iterator<InputNode> inputs) {
		while (inputs.hasNext()) {
			InputNode n = inputs.next();
			EditText input = new EditText(appContext);
			input.setInputType(InputType.TYPE_CLASS_NUMBER |
							   InputType.TYPE_NUMBER_FLAG_DECIMAL |
							   InputType.TYPE_NUMBER_FLAG_SIGNED);
			input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			input.setPadding(8, 0, 8, 0);
			input.setTypeface(Typeface.MONOSPACE);
			addRow(n.getPrompt(), input);
			widgetTable.put(n.getVariable(), input);
		}
	}
	

	private void createControls() {
		Button run = new Button(appContext);
		run.setText(R.string.button_run);
		run.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					runFormula();
				} catch (SemanticException e) {
					errorDialog.show(e.getMessage());
				}
			}
		});
		addRow("", run);
	}
	

	private void createOutputs(Iterator<OutputNode> outputs) {
		while (outputs.hasNext()) {
			OutputNode n = outputs.next();
			EditText output = new EditText(appContext);
			output.setEnabled(false);
			output.setFocusable(false);
			output.setInputType(InputType.TYPE_NULL);
			output.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			output.setPadding(8, 0, 8, 0);
			output.setTypeface(Typeface.MONOSPACE);
			addRow(n.getLabel(), output);
			widgetTable.put(n.getVariable(), output);
		}
	}


	/**
	 * Add a row to the GUI, containing a label and a widget.
	 * 
	 * @param	lab				The label for this row.
	 * @param	widget			The widget.
	 */
	private void addRow(String lab, View widget) {
		// Create the output fields.
		TableRow row = new TableRow(appContext);
		addView(row);

		// Add a label field to display the output name.
		TextView label = new TextView(appContext);
		label.setPadding(0, 0, 20, 0);
		label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		label.setText(lab);
		row.addView(label);

		// Add the widget.
		row.addView(widget);
	}


	// ******************************************************************** //
	// Execution.
	// ******************************************************************** //

	private void runFormula() throws SemanticException {
		if (formulaInterp == null)
			return;
		
		// Load the input field values into the interpreter's variable table.
		Iterator<InputNode> inputs = currentFormula.getInputs();
		while (inputs.hasNext()) {
			InputNode n = inputs.next();
			String varName = n.getVariable();
			EditText field = (EditText) widgetTable.get(varName);
			String sval = field.getText().toString();
			try {
				if (sval.length() > 0) {
					Double dval = Double.valueOf(sval);
					formulaInterp.setVariable(varName, dval);
				} else
					formulaInterp.deleteVariable(varName);
			} catch (NumberFormatException e) {
				throw new SemanticException("Invalid value \"" + sval +
							     			"\" in field \"" + n.getPrompt() +
							     			"\": " + e.getMessage());
			}
		}
		
		// Create variables for all of the outputs.  This means that
		// when the formula sets an output, it sets this one rather than
		// creating a local which then gets discarded.  (Unless of course
		// it has declared a local with that name.)
		Iterator<OutputNode> outputs = currentFormula.getOutputs();
		while (outputs.hasNext()) {
			OutputNode n = outputs.next();
			String varName = n.getVariable();
			formulaInterp.declareVariable(varName);
		}

		// Now execute the formula.
		formulaInterp.runProgram();

		// Finally, set up the outputs.
		outputs = currentFormula.getOutputs();
		while (outputs.hasNext()) {
			OutputNode n = outputs.next();
			String varName = n.getVariable();
			String format = n.getFormat();
			TextView field = (TextView) widgetTable.get(varName);
			Double vval = formulaInterp.getVariable(varName);
			if (vval != null) {
				try {
					field.setText(String.format(format, vval));
				} catch (IllegalFormatException e) {
					throw new SemanticException("Invalid format \"" + format +
							"\" on field \"" + varName + "\".");
				}
			} else {
				field.setText("");
			}
		}
	}
	
	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "formula";


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Our application context.
	private Context appContext = null;

	// Dialog used to display errors.
	private ErrorDialog errorDialog;

	// The interpreter we use for parsing and running formulae.
	// null if not created yet.
	private Interpreter formulaInterp = null;

	// The current formula.  null if there isn't one.
	private ProgramNode currentFormula = null;

	// The table of UI fields corresponding to inputs and outputs in the
	// current formula.  null if there isn't one.
	private HashMap<String, View> widgetTable = null;
	
}

