package bgu.dl.features.learning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import bgu.dl.features.collections.PossibleGroundedActions;

import pddl4j.PDDLObject;
import pddl4j.exp.AndExp;
import pddl4j.exp.AtomicFormula;
import pddl4j.exp.Exp;
import pddl4j.exp.InitEl;
import pddl4j.exp.NotAtomicFormula;
import pddl4j.exp.action.Action;
import pddl4j.exp.term.Constant;
import pddl4j.exp.term.Substitution;
import pddl4j.exp.term.Variable;

/**
 * @author Shashank Shekhar
 * BGU of the Negev
 */
public class ProblemDetails 
{	
	@SuppressWarnings("rawtypes")
	private ArrayList initialState, goalState, actions, constants;
	private PDDLObject pddlObject;	
	ArrayList<PossibleGroundedActions> gActions = null;

	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
	public ProblemDetails(PDDLObject object) 
	{
		/** Initializing the root. */
		pddlObject = object; 
		pddlObject.predicatesIterator();		

		/** Generate set of initial predicates */
		initialState = new ArrayList();
		Iterator initItrator = pddlObject.getInit().iterator();
		System.out.print("The initial state is : ");
		while (initItrator.hasNext()) {
			InitEl item = (InitEl) initItrator.next();
			initialState.add(item);			
			System.out.print(item.toString());
		}	
		System.out.println();

		/** Generates the goal predicates. */
		AndExp goal = (AndExp) pddlObject.getGoal();
		goalState = new ArrayList<Exp>();
		System.out.print("The goal state is : ");
		Iterator goalIterator = goal.iterator();
		while(goalIterator.hasNext()){
			goalState.add((Exp) goalIterator.next());
			System.out.print(goalState.get(goalState.size()-1));
		}
		System.out.println("\n");

		/** Generates a set of the actions. */
		actions = new ArrayList();
		Action act = null;
		Iterator actItr = pddlObject.actionsIterator();
		while(actItr.hasNext()) {
			act = (Action) actItr.next();			
			actions.add(act);
			Set<Variable> freeVar = act.getPrecondition().getFreeVariables();
		}

		/** Generates a list of all the constants. */
		constants = new ArrayList();		
		Iterator constItr = pddlObject.constantsIterator();		
		while(constItr.hasNext()) {
			Constant c = (Constant) constItr.next();
			constants.add(c);
		}	
	}

	/** A method to return all the preconditions of an action. */
	@SuppressWarnings("incomplete-switch")
	public ArrayList<AtomicFormula> getPreConditions(Action actn) 
	{
		Stack<Exp> stack = new Stack<Exp>();
		stack.add(actn.getPrecondition());
		ArrayList<AtomicFormula> precond = new ArrayList<AtomicFormula>();
		while (!stack.isEmpty()) 
		{
			Exp e = stack.pop();
			switch (e.getExpID()) 
			{
			case AND:
				AndExp andExp = (AndExp) e;
				for (Exp sexp : andExp) 
				{
					stack.push(sexp);
				}
				break;
			case ATOMIC_FORMULA:
				AtomicFormula p = (AtomicFormula) e;
				precond.add(p.clone());
				break;
			}			
		}
		return precond;	
	}

	public String[] constantList()
	{		
		Iterator<Constant> constantsIterator = pddlObject.constantsIterator();
		String s1 = new String();
		while(constantsIterator.hasNext()) {
			s1 = s1.concat(constantsIterator.next().toString() + " ");			
		}
		String[] str =  s1.split(" ");	
		return str;
	}

	/** Method to return effects of an action. */
	@SuppressWarnings("rawtypes")
	public ArrayList<Exp> geteffects(Action actn) 
	{
		int i=0;
		AndExp aExp = (AndExp) actn.getEffect();
		ArrayList<Exp> effect = new ArrayList<Exp>();
		Iterator effectItr = aExp.iterator();
		while(effectItr.hasNext()){
			effect.add((Exp) effectItr.next());
			//System.out.println("eff "+effect.get(i++));
		}
		return effect;
	}

	/** Method to check if a list is subset of another list. */
	@SuppressWarnings("unused")
	private boolean isSubsetOf(ArrayList<AtomicFormula> goal, ArrayList<AtomicFormula> start) 
	{
		if(goal.isEmpty()) {
			return false;
		} else {
			boolean flag = start.containsAll(goal);
			return(flag);
		}	
	}

	/** Method to print a list. */
	@SuppressWarnings({ "unused", "rawtypes" })
	private void printList(ArrayList<AtomicFormula> list) 
	{		
		Iterator itr = list.iterator();
		String lst="";
		while(itr.hasNext()) {
			lst = lst + itr.next();			
		}	
	}

	// Getting action preconditions Exp types
	private ArrayList<Exp> getExpPreConditions(Action actn) {
		AndExp aExp = (AndExp) actn.getPrecondition();
		ArrayList<Exp> precond = new ArrayList<Exp>();
		Iterator eItr = aExp.iterator();
		while(eItr.hasNext()){
			precond.add((Exp) eItr.next());						
		}

		//System.out.println(precond.toString());
		return precond;
	}

	/**
	 * Generates all possible combinations of actions and constants by grounding the actions
	 */
	public void generateGroundedActions() 
	{
		Action actn;
		Iterator actItr = this.pddlObject.actionsIterator();
		//Iterate over all possible actions
		while(actItr.hasNext()) 
		{
			actn = (Action) actItr.next();
			ArrayList<Exp> precond = getExpPreConditions(actn);

			Set<Variable> freeVar = actn.getPrecondition().getFreeVariables();
			int noFreeVar = freeVar.size();		
			/**
			 * For operators with one free variable
			 */
			if(noFreeVar==1) {
				Iterator itrFV = freeVar.iterator();
				while(itrFV.hasNext()) 
				{
					Variable var = (Variable) itrFV.next();
					Iterator<Constant> itrConst = getConstants().iterator();
					//Iterate over all constants
					while(itrConst.hasNext()) 
					{
						/**
						 * Generate a GroundedActions object for this combination of action and constant */
						PossibleGroundedActions groundedActions = new PossibleGroundedActions();
						groundedActions.setA(actn);

						Constant cons = itrConst.next();
						Substitution theta = new Substitution();
						theta.bind(var, cons);
						groundedActions.setTheta(theta);

						Iterator<Exp> itrPC = precond.iterator();
						while(itrPC.hasNext()) 
						{
							Exp pc = itrPC.next();		
							pc = pc.apply(theta);
							AtomicFormula af = (AtomicFormula) pc;
							groundedActions.getPreCond().add(af);												
						}
						/**
						 * Ground Effects into positive and negative effects
						 */
						AndExp eff = (AndExp) actn.getEffect();
						eff = eff.apply(theta);

						Iterator<Exp> itrEff = eff.iterator();
						while(itrEff.hasNext()) 
						{
							Exp exp = itrEff.next();
							if(exp.getClass().equals(AtomicFormula.class)) 
							{
								AtomicFormula af = (AtomicFormula) exp;
								groundedActions.getPosEff().add(af);
							}

							//Set negative effects
							if(exp.getClass().equals(NotAtomicFormula.class)) {

								NotAtomicFormula notEff = (NotAtomicFormula) exp;	
								Exp pos = notEff.getExp();
								AtomicFormula af = (AtomicFormula) pos;
								groundedActions.getNegEff().add(af);
							}							
						}						
						/** Add the grounded action to the set of all grounded actions in the domain. */
						gActions.add(groundedActions);
					}
				}				
			}			
			else 
			{
				ArrayList<ArrayList<Constant>> constCombi = generateCombinations(getConstants(), noFreeVar);	
				Iterator<ArrayList<Constant>> itrConst = constCombi.iterator();
				// Iterate over all combinations of constants
				while(itrConst.hasNext()) 
				{
					PossibleGroundedActions GA = new PossibleGroundedActions();
					GA.setA(actn);
					ArrayList<Constant> cons = itrConst.next();					
					Substitution theta = new Substitution();					

					Iterator itrFV = freeVar.iterator();
					Iterator itrC = cons.iterator();
					while(itrFV.hasNext()&&itrC.hasNext()) {				

						Variable var = (Variable) itrFV.next();
						Constant constant = (Constant) itrC.next();
						theta.bind(var, constant);
					}
					//Set the substitution
					GA.setTheta(theta);
					//Ground preconditions
					Iterator<Exp> itrPC = precond.iterator();
					while(itrPC.hasNext()) {

						Exp pc = itrPC.next();	
						pc = pc.apply(theta);
						GA.getPreCond().add((AtomicFormula) pc);
					}

					//Ground Effects into positive and negative effects
					AndExp eff = (AndExp) actn.getEffect();
					eff = eff.apply(theta);
					Iterator<Exp> itrEff = eff.iterator();
					while(itrEff.hasNext()) {
						Exp exp = itrEff.next();
						//Set positive effects
						if(exp.getClass().equals(AtomicFormula.class))
							GA.getPosEff().add((AtomicFormula) exp);

						//Set negative effects
						if(exp.getClass().equals(NotAtomicFormula.class)) {
							NotAtomicFormula notEff = (NotAtomicFormula) exp;	
							Exp pos = notEff.getExp();
							GA.getNegEff().add((AtomicFormula) pos);
						}							
					}
					gActions.add(GA);
				}				
			}
		}		
	}

	private ArrayList<ArrayList<Constant>> generateCombinations(ArrayList constants, int noFreeVar) 
	{
		ArrayList<ArrayList<Constant>> combi = new ArrayList<ArrayList<Constant>>();
		/**
		 * Create the initial vector */
		String freeVar[] = new String[constants.size()];
		for(int i=0,j=0;j<constants.size();i++,j++) {
			freeVar[i]=constants.get(j).toString();
		}

		ICombinatoricsVector<String> initialVector = Factory.createVector(freeVar);
		// Create a simple combination generator to generate noFreeVar-combinations of the initial vector
		Generator<String> gen = Factory.createSimpleCombinationGenerator(initialVector, noFreeVar);
		// Read all possible combinations
		for (ICombinatoricsVector<String> combination : gen) 
		{
			java.util.List<String> l = combination.getVector();
			ICombinatoricsVector<String> temp = Factory.createVector(l);
			Generator<String> genPerm = Factory.createPermutationGenerator(temp);
			for (ICombinatoricsVector<String> perm : genPerm) 
			{
				java.util.List<String> p = perm.getVector();
				ArrayList<Constant> c = new ArrayList<Constant>();
				Iterator<String> itr = p.iterator();
				while(itr.hasNext()) {
					c.add(new Constant(itr.next()));				
				}
				combi.add(c);
			}
		}	
		return combi;
	}

	@SuppressWarnings("rawtypes")
	public ArrayList getInitialState() {
		return initialState;
	}

	@SuppressWarnings("rawtypes")
	public void setInitialState(ArrayList initialState) {
		this.initialState = initialState;
	}

	@SuppressWarnings("rawtypes")
	public ArrayList getGoalState() {
		return goalState;
	}

	@SuppressWarnings("rawtypes") 
	public void setGoalState(ArrayList goalState) {
		this.goalState = goalState;
	}

	@SuppressWarnings("rawtypes")
	public ArrayList getActions() {
		return actions;
	}

	@SuppressWarnings("rawtypes") 
	public void setActions(ArrayList actions) {
		this.actions = actions;
	}

	@SuppressWarnings("rawtypes")
	public ArrayList getConstants() {
		return constants;
	}

	@SuppressWarnings("rawtypes")
	public void setConstants(ArrayList constants) {
		this.constants = constants;
	}

	public PDDLObject getPddlObject() {
		return pddlObject;
	}

	public void setPddlObject(PDDLObject pddlObject) { 
		this.pddlObject = pddlObject;
	}
}
