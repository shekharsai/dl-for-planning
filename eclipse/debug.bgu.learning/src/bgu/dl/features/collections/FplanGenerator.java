package bgu.dl.features.collections;

import java.awt.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import javax.sql.rowset.Predicate;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import pddl4j.Domain;
import pddl4j.ErrorManager;
import pddl4j.PDDLObject;
import pddl4j.Parser;
import pddl4j.Problem;
import pddl4j.RequireKey;
import pddl4j.ErrorManager.Message;
import pddl4j.exp.AbstractExp;
import pddl4j.exp.AndExp;
import pddl4j.exp.AtomicFormula;
import pddl4j.exp.DerivedPredicate;
import pddl4j.exp.ExistsExp;
import pddl4j.exp.Exp;
import pddl4j.exp.InitEl;
import pddl4j.exp.NotAtomicFormula;
import pddl4j.exp.NotExp;
import pddl4j.exp.OrExp;
import pddl4j.exp.action.Action;
import pddl4j.exp.term.Constant;
import pddl4j.exp.term.Substitution;
import pddl4j.exp.term.Substitution.Binding;
import pddl4j.exp.term.Term;
import pddl4j.exp.term.Variable;


public class FplanGenerator {

	ArrayList init,goal,actions,constants;
	ArrayList<PossibleGroundedActions> gActions; 	
	ArrayList<ArrayList<AtomicFormula>> visitedStates;
	PDDLObject root;

	//Constructor with root obj
	public FplanGenerator(PDDLObject obj) {  

		//Initialize root
		root = obj;
		root.predicatesIterator();


		//Generate set of initial predicates
		init = new ArrayList();
		Iterator initItr = root.getInit().iterator();
		while (initItr.hasNext()) {
			InitEl i = (InitEl) initItr.next();
			init.add(i);
			//System.out.println(exp);
		}	

		//Generate goal 
		AndExp g = (AndExp) root.getGoal();
		goal = new ArrayList<Exp>();
		Iterator eItr = g.iterator();

		while(eItr.hasNext()){
			goal.add((Exp) eItr.next());						
		}

		//Generate set of actions
		actions = new ArrayList();
		Action a;
		Iterator actItr = root.actionsIterator();
		while(actItr.hasNext()) {
			a = (Action) actItr.next();
			actions.add(a);
			//System.out.println(a.getEffect());
		}

		//List all constants
		constants = new ArrayList();
		Iterator constItr = root.constantsIterator();
		while(constItr.hasNext()) {
			Constant c = (Constant) constItr.next();
			constants.add(c);
			//System.out.println(c.toString());
		}

		gActions = new ArrayList<PossibleGroundedActions>();
		visitedStates = new ArrayList<ArrayList<AtomicFormula>>();

		//Generate all the grounded actions
		generateGroundedActions();

	}

	/**
	 * Generates all possible combinations of actions and constants by grounding the actions
	 */
	public void generateGroundedActions() {


		Action actn;
		Iterator actItr = root.actionsIterator();

		//Iterate over all possible actions
		while(actItr.hasNext()) {

			actn = (Action) actItr.next();	

			ArrayList<Exp> precond = getPreConditions(actn);		

			Set<Variable> freeVar = actn.getPrecondition().getFreeVariables();
			int noFreeVar = freeVar.size();		

			//For operators with one free variable
			if(noFreeVar==1){

				Iterator itrFV = freeVar.iterator();
				while(itrFV.hasNext()) {

					Variable var = (Variable) itrFV.next();

					Iterator<Constant> itrConst = getConstants().iterator();

					//Iterate over all constants
					while(itrConst.hasNext()) {

						//Generate a GroundedActions object for this combination of action and constant
						PossibleGroundedActions GA = new PossibleGroundedActions();

						//Set the action
						GA.setA(actn);

						Constant cons = itrConst.next();
						Substitution theta = new Substitution();
						theta.bind(var, cons);

						//Set the substitution
						GA.setTheta(theta);

						//Ground preconditions
						Iterator<Exp> itrPC = precond.iterator();
						while(itrPC.hasNext()) {
							Exp pc = itrPC.next();		
							pc = pc.apply(theta);
							AtomicFormula af = (AtomicFormula) pc;
							GA.getPreCond().add(af);												
						}

						//Ground Effects into positive and negative effects
						AndExp eff = (AndExp) actn.getEffect();
						eff = eff.apply(theta);

						Iterator<Exp> itrEff = eff.iterator();
						while(itrEff.hasNext()) {

							Exp exp = itrEff.next();

							//Set positive effects
							if(exp.getClass().equals(AtomicFormula.class)) {

								AtomicFormula af = (AtomicFormula) exp;
								GA.getPosEff().add(af);
							}

							//Set negative effects
							if(exp.getClass().equals(NotAtomicFormula.class)) {

								NotAtomicFormula notEff = (NotAtomicFormula) exp;	
								Exp pos = notEff.getExp();
								AtomicFormula af = (AtomicFormula) pos;
								GA.getNegEff().add(af);
							}							
						}						

						//Add the grounded action to the set of all grounded actions in the domain
						gActions.add(GA);
					}
				}				
			}
			//If multiple free variables are present, generate combinations of them
			else {

				//Generate all combinations of constants
				ArrayList<ArrayList<Constant>> constCombi = generateCombinations(getConstants(),noFreeVar);	
				Iterator<ArrayList<Constant>> itrConst = constCombi.iterator();

				//Iterate over all combinations of constants
				while(itrConst.hasNext()) {

					//Generate a GroundedActions object for this combination of action and constant
					PossibleGroundedActions GA = new PossibleGroundedActions();

					//Set the action
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

					//Add the grounded action to the set of all grounded actions in the domain
					gActions.add(GA);
				}				
			}
		}		
	}

	//Recursive method that generated the plan
	public void genPlan(ArrayList<AtomicFormula> cur, ArrayList<AtomicFormula> plan) {

		//Check if cur state is goal state
		if(goalTest(cur)) {
			//goalFound  = true;
			System.out.println("Goal state reached!!\nPlan found:");
			printList(plan);			
			System.out.println(plan.size()+" actions in plan!");
			return;
		}

		//Generate all applicable actions
		ArrayList<PossibleGroundedActions> appActn = getApplicableActions(cur);

		//If none available, return
		if(appActn==null)
			return;

		else {

			ArrayList<AtomicFormula> newPlan = new ArrayList<AtomicFormula>();
			newPlan.addAll(plan);
			int noAppActns = appActn.size();

			//Iterate over all applicable actions
			Iterator itrAppActn = appActn.iterator();
			while(itrAppActn.hasNext()) {

				if(!newPlan.equals(plan))
					newPlan=plan;

				PossibleGroundedActions GA = (PossibleGroundedActions) itrAppActn.next();
				ArrayList<AtomicFormula> newState = new ArrayList<AtomicFormula>();

				//Apply the action to current state to generate a new state
				newState = applyAction(GA,cur);

				//If newstate is not already visited
				if(!alreadyVisited(newState)) {	

					AtomicFormula af = new AtomicFormula(GA.getA().getName());
					for (Term p : GA.getA().getParameters()) {
						af.add((Constant)GA.getTheta().getBinding((Variable) p));
					}
					//System.out.println(af);

					//Add action to plan
					newPlan.add(af);	

					//Mark state as visited
					visitedStates.add(newState);

					//Call recursively with new state
					genPlan(newState,newPlan);
				}
				else
					noAppActns--;

				//If all applicable actions are already visited, return
				if(noAppActns==0) {
					System.out.println("Plan not found!!");
					return;					
				}	
			}
		}
	}

	//Applies a grounded action on the current state
	private ArrayList<AtomicFormula> applyAction(PossibleGroundedActions gA,	ArrayList<AtomicFormula> cur) {

		ArrayList<AtomicFormula> newState = new ArrayList<AtomicFormula>();

		gA.printGroundedAction();

		newState.addAll(cur);

		ArrayList<Exp> removeNeg = new ArrayList<Exp>();

		//Performing set minus 
		Iterator<AtomicFormula> itrNew = newState.iterator();
		while(itrNew.hasNext()) {								

			Exp exp = itrNew.next();
			if(gA.getNegEff().contains(exp))
				removeNeg.add(exp);
		}

		//Cur State - Neg Effects
		newState.removeAll(removeNeg);

		//Union all Pos effects
		newState.addAll(gA.getPosEff());

		return newState;
	}

	//Generate all applicable actions
	private ArrayList<PossibleGroundedActions> getApplicableActions(ArrayList<AtomicFormula> state) {

		ArrayList<PossibleGroundedActions> appActn = new ArrayList<PossibleGroundedActions>();
		Iterator itr = gActions.iterator();
		while(itr.hasNext()) {

			PossibleGroundedActions ga = (PossibleGroundedActions) itr.next();

			ArrayList<AtomicFormula> preCond = ga.getPreCond();

			//If preconditions of action is a subset of current state
			if(isSubsetOf(preCond,state)) {				

				//Mark action as applicable
				appActn.add(ga);
			}
		}

		return appActn;
	}

	//Method to print all the grounded actions data structure
	public void printGActions() {

		Iterator itr = gActions.iterator();
		while(itr.hasNext()) {

			PossibleGroundedActions ga = (PossibleGroundedActions) itr.next();
			ga.printGroundedAction();
		}
	}

	public static void main(String[] args) throws FileNotFoundException {

		Properties options = new Properties();
		options.put(RequireKey.STRIPS, true);
		options.put(RequireKey.EQUALITY, true);
		options.put(RequireKey.DERIVED_PREDICATES, true);
		options.put(RequireKey.TYPING, true);

		//Parser.setOptions(options);
		// Checks the command line
		if (args.length != 2) {
			System.err.println("Invalid command line");
		} 
		else {
			// Creates an instance of the java pddl parser
			Parser parser = new Parser(options);
			Domain domain = parser.parse(new File(args[0]));
			Problem problem = parser.parse(new File(args[1]));
			PDDLObject obj = parser.link(domain, problem);

			//Create object of Forward Plan algorithm and pass root of parse tree
			FplanGenerator fwdObj = new FplanGenerator(obj);

			ArrayList start = fwdObj.getInit();

			//Mark start state as visited
			fwdObj.visitedStates.add(start);

			//Start recursive algo with start state
			fwdObj.genPlan(start, new ArrayList<AtomicFormula>());

		}
	}

	private ArrayList<ArrayList<Constant>> generateCombinations(ArrayList constants, int noFreeVar) {

		ArrayList<ArrayList<Constant>> combi = new ArrayList<ArrayList<Constant>>();

		// Create the initial vector
		String freeVar[] = new String[constants.size()];
		for(int i=0,j=0;j<constants.size();i++,j++) {
			freeVar[i]=constants.get(j).toString();
		}

		ICombinatoricsVector<String> initialVector = Factory.createVector(freeVar);

		// Create a simple combination generator to generate noFreeVar-combinations of the initial vector
		Generator<String> gen = Factory.createSimpleCombinationGenerator(initialVector, noFreeVar);


		// Read all possible combinations
		for (ICombinatoricsVector<String> combination : gen) {

			java.util.List<String> l = combination.getVector();

			ICombinatoricsVector<String> temp = Factory.createVector(l);
			Generator<String> genPerm = Factory.createPermutationGenerator(temp);

			for (ICombinatoricsVector<String> perm : genPerm) {

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

	public ArrayList<Exp> getPreConditions(Action actn) {

		AndExp aExp = (AndExp) actn.getPrecondition();
		ArrayList<Exp> precond = new ArrayList<Exp>();
		Iterator eItr = aExp.iterator();

		while(eItr.hasNext()){
			precond.add((Exp) eItr.next());						
		}

		//System.out.println(precond.toString());
		return precond;
	}

	//Check if a newly generated state is already visited.
	private boolean alreadyVisited(ArrayList<AtomicFormula> newState) {

		Iterator itr = visitedStates.iterator();

		while(itr.hasNext()) {

			ArrayList<AtomicFormula> visited = (ArrayList<AtomicFormula>) itr.next();
			if(visited.containsAll(newState) && newState.containsAll(visited))
				return true;
		}
		return false;
	}

	//Check if goal is subset of new state
	private boolean goalTest(ArrayList<AtomicFormula> cur) {

		ArrayList<AtomicFormula> g = getGoal();
		return isSubsetOf(g,cur);
	}

	private boolean isSubsetOf(ArrayList<AtomicFormula> g, ArrayList<AtomicFormula> cur) {

		return(cur.containsAll(g));		
	}

	private void printList(ArrayList<AtomicFormula> list) {

		Iterator itr = list.iterator();
		//System.out.print("");
		while(itr.hasNext()) {
			System.out.println(itr.next());
		}		
	}

	private void print(ArrayList<AtomicFormula> plan) {

		System.out.print("");
		for(int i=0;i<plan.size()-1;i++) {
			System.out.print(plan.get(i));
		}
	}

	public ArrayList getConstants() {
		return constants;
	}

	public ArrayList getGoal() {
		return goal;
	}

	public ArrayList getInit() {
		return init;
	}
}	

//Data structure to store the grounded actions
class GroundedAction {

	Action a; //Actions
	Substitution theta; //Its corresponding substitution	

	ArrayList<AtomicFormula> preCond; //Grounded Precondition of action
	ArrayList<AtomicFormula> posEff; //Grounded positive effect of action
	ArrayList<AtomicFormula> negEff; //Grounded negative effect of action

	public GroundedAction() {

		preCond = new ArrayList<AtomicFormula>();
		posEff = new ArrayList<AtomicFormula>();
		negEff = new ArrayList<AtomicFormula>();
	}

	public void printGroundedAction() {

		AtomicFormula af = new AtomicFormula(a.getName());
		for (Term p : a.getParameters()) {
			af.add((Constant)getTheta().getBinding((Variable) p));
		}
		/*System.out.println("Action: "+af);
		System.out.print("Preconditions: ");
		printList(preCond);
		System.out.print("Positive Effects: ");
		printList(posEff);
		System.out.print("Negative Effects: ");
		printList(negEff);
		System.out.println("");*/
	}

	public AtomicFormula getGA() {

		AtomicFormula af = new AtomicFormula(a.getName());
		for (Term p : a.getParameters()) {
			af.add((Constant)getTheta().getBinding((Variable) p));
		}
		return af;
	}

	private void printList(ArrayList<AtomicFormula> list) {

		Iterator itr = list.iterator();
		//System.out.print("");
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

///home/skanda/expt/PDDL4J-v1.0/pddl/blockworld/blocksworld.pddl /home/skanda/expt/PDDL4J-v1.0/pddl/blockworld/pb5.pddl