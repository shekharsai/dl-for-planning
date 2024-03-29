package bgu.dl.features.collections;

import java.util.ArrayList;
import java.util.Iterator;

import pddl4j.exp.AtomicFormula;
import pddl4j.exp.action.Action;
import pddl4j.exp.term.Constant;
import pddl4j.exp.term.Substitution;
import pddl4j.exp.term.Term;
import pddl4j.exp.term.Variable;

/**
 * @author Shashank Shekhar
 * BGU of the Negev
 */
public class PossibleGroundedActions {

	Action a; //Actions
	Substitution theta; //Its corresponding substitution	

	ArrayList<AtomicFormula> preCond; //Grounded Precondition of action
	ArrayList<AtomicFormula> posEff; //Grounded positive effect of action
	ArrayList<AtomicFormula> negEff; //Grounded negative effect of action

	public PossibleGroundedActions() {
		preCond = new ArrayList<AtomicFormula>();
		posEff = new ArrayList<AtomicFormula>();
		negEff = new ArrayList<AtomicFormula>();
	}

	public void printGroundedAction() {
		AtomicFormula af = new AtomicFormula(a.getName());
		for (Term p : a.getParameters()) {
			af.add((Constant)getTheta().getBinding((Variable) p));
		}	
		// System.out.println(af.toString());
	}

	public boolean isThatGroundedAction(String actString) {
		AtomicFormula af = new AtomicFormula(a.getName());
		for (Term p : a.getParameters()) {
			af.add((Constant)getTheta().getBinding((Variable) p));
		}	
		if(af.toString().equalsIgnoreCase(actString))
			return true;
		else
			return false;
	}

	public AtomicFormula getGA() {
		AtomicFormula af = new AtomicFormula(a.getName());
		for (Term p : a.getParameters()) {
			af.add((Constant)getTheta().getBinding((Variable) p));
		}
		return af;
	}

	@SuppressWarnings("unused")
	private void printList(ArrayList<AtomicFormula> list) {
		@SuppressWarnings("rawtypes")
		Iterator itr = list.iterator();
		while(itr.hasNext()) {
			System.out.print(itr.next()+"\t");
		}		
	}

	public Substitution getTheta() {
		return theta;
	}

	public void setTheta(Substitution theta) {
		this.theta = theta;
	}	

	public Action getA() {
		return a;
	}

	public void setA(Action a) {
		this.a = a;
	}

	public ArrayList<AtomicFormula> getPreCond() {
		return preCond;
	}

	public void setPreCond(ArrayList<AtomicFormula> preCond) {
		this.preCond = preCond;
	}

	public ArrayList<AtomicFormula> getPosEff() {
		return posEff;
	}

	public void setPosEff(ArrayList<AtomicFormula> posEff) {
		this.posEff = posEff;
	}

	public ArrayList<AtomicFormula> getNegEff() {
		return negEff;
	}

	public void setNegEff(ArrayList<AtomicFormula> negEff) {
		this.negEff = negEff;
	}	
}