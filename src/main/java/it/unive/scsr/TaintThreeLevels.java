package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class TaintThreeLevels extends BaseTaint<TaintThreeLevels>  {

	/*
	 * This is a simple taint lattice with three main levels:
	 * 
	 * 	  TOP
	 * 	/   \
	 * TAINT  CLEAN
	 *  \     /
	 *   BOTTOM
	 *   
	 * Basically:
	 * - TOP means it might be tainted or clean (we are unsure)
	 * - TAINT means definitely tainted
	 * - CLEAN means definitely clean
	 * - BOTTOM represents an error or undefined state
	 */

	// defining constants for the different lattice elements
	public static final TaintThreeLevels TOP = new TaintThreeLevels(TaintValue.TOP);
	public static final TaintThreeLevels TAINT = new TaintThreeLevels(TaintValue.TAINT);
	public static final TaintThreeLevels CLEAN = new TaintThreeLevels(TaintValue.CLEAN);
	public static final TaintThreeLevels BOTTOM = new TaintThreeLevels(TaintValue.BOTTOM);
	
	// the actual value stored in this element
	private enum TaintValue {
		TOP, TAINT, CLEAN, BOTTOM
	}
	
	private final TaintValue value;
	
	// private constructor used internally to create lattice elements
	private TaintThreeLevels(TaintValue value) {
		this.value = value;
	}
	
	// default constructor defaults to TOP because we are unsure
	public TaintThreeLevels() {
		this.value = TaintValue.TOP;
	}
	
	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
		// join operation: combines two elements
		if (this == BOTTOM) return other; // BOTTOM doesn't affect join
		if (other == BOTTOM) return this;
		if (this == other) return this; // same element, just return it
		// if one is TAINT and the other is CLEAN, result is TOP
		return TOP;
	}
	
	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		// define the partial order of the lattice
		// BOTTOM ≤ TAINT ≤ TOP and BOTTOM ≤ CLEAN ≤ TOP
		if (this == BOTTOM) return true;
		if (other == TOP) return true;
		if (this == other) return true;
		return false;
	}
	
	@Override
	public TaintThreeLevels top() {
		// return the TOP element
		return TOP;
	}
	
	@Override
	public TaintThreeLevels bottom() {
		// return the BOTTOM element
		return BOTTOM;
	}
	
	@Override
	protected TaintThreeLevels tainted() {
		// return the TAINT element
		return TAINT;
	}
	
	@Override
	protected TaintThreeLevels clean() {
		// return the CLEAN element
		return CLEAN;
	}
	
	@Override
	public boolean isAlwaysTainted() {
		// only TAINT is definitely tainted
		return this == TAINT;
	}
	
	@Override
	public boolean isPossiblyTainted() {
		// TOP and TAINT can both be tainted
		return this == TOP || this == TAINT;
	}
	
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// if either operand is BOTTOM, result is BOTTOM (error)
		if (left == BOTTOM || right == BOTTOM) return BOTTOM;
		// if either operand is TAINT, result is definitely tainted
		if (left == TAINT || right == TAINT) return TAINT;
		// if either operand is TOP, result is TOP
		if (left == TOP || right == TOP) return TOP;
		// otherwise, both are CLEAN, so result is CLEAN
		return CLEAN;
	}
	
	@Override
	public TaintThreeLevels wideningAux(TaintThreeLevels other) throws SemanticException {
		// widening is same as lub for this lattice because it's simple
		return lubAux(other);
	}
	
	// REPRESENTATION OF THE ELEMENT
	// this is just for printing or visualizing the element
	@Override
	public StructuredRepresentation representation() {
		// BOTTOM and TOP get special representations, CLEAN is "_" and TAINT is "#"
		return this == BOTTOM ? Lattice.bottomRepresentation() : 
			   this == TOP ? Lattice.topRepresentation() : 
			   this == CLEAN ? new StringRepresentation("_") : 
			   new StringRepresentation("#");
	}
}
