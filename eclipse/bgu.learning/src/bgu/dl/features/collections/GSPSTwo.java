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

/**
 * @author Shashank Shekhar
 * BGU of the Negev
 * @throws FileNotFoundException 
 */
public class GSPSTwo {

	ArrayList init,goal,actions,constants;
	ArrayList<PossibleGroundedActions> gActions; 	

	PDDLObject root;

	//Constructor with root obj
	public GSPSTwo(PDDLObject obj) { 

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
	public PlanActionPairs genPlan(ArrayList<AtomicFormula> cur, ArrayList<AtomicFormula> goal, ArrayList<ActionPair> tmpPlan, int level) {

		//DS to store plan 
		PlanActionPairs plan = new PlanActionPairs();

		while(true) {				

			//Check if cur state is cur goal state
			if(isSubsetOf(goal,cur)) {						
				plan.setFailure(false);
				return plan;
			}

			//Generate all relevant actions for the goal
			ArrayList<ActionPair> relActn = getRelevantActionPair(goal);
			System.out.print("Goal:");
			printList(goal);
			boolean foundRelAct = false;

			//If no relevant action available, return failure
			if(relActn==null) {						
				return plan;
			}				

			else { //Non deterministically choose any action

				int numRelActn = relActn.size();

				for(int i=0;i<numRelActn;i++) {

					ActionPair actns = (ActionPair) relActn.get(i);					
					String action = actns.toString();
					System.out.println(action);

					if(isPairPresent(plan,actns)) {						
						continue;
					}

					if(isPairPresent(tmpPlan,actns)) {						
						continue;						
					}

					foundRelAct = true;
					tmpPlan.add(actns);

					ArrayList<AtomicFormula> newGoal;					
					newGoal = actns.getA2().getPreCond();

					level++;


					PlanActionPairs piD = genPlan(cur, newGoal, tmpPlan, level);
					ArrayList <ActionPair> partPlan = piD.getAction();				

					level--;

					if(piD.isFailure()) {
						//return plan;
						continue;
					}

					if(level==0)
						tmpPlan.clear();

					ArrayList<AtomicFormula> newState = new ArrayList<AtomicFormula>();
					ArrayList<ActionPair> newPlan = new ArrayList<ActionPair>();

					newState = cur;

					//Apply all actions in partPlan to cur state
					Iterator itrPlan = partPlan.iterator();
					while(itrPlan.hasNext()) {

						ActionPair act = (ActionPair) itrPlan.next();
						newState = applyAction(act,newState);
					}

					//Apply the action to current state to generate a new state
					newState = applyAction(actns,newState);				

					//Add actions in partPlan to plan
					itrPlan = partPlan.iterator();
					while(itrPlan.hasNext()) {

						ActionPair act = (ActionPair) itrPlan.next();
						newPlan.add(act);
					}

					//Add action to plan
					newPlan.add(actns);						

					//Assign cur state n plan with modified ones
					cur = newState;
					plan.setAction(newPlan);

					break;
				}
			}			
			if(foundRelAct==false)
				return plan;
		}
	}

	private boolean isPairPresent(ArrayList<ActionPair> tmpPlan,ActionPair actns) {

		boolean flag=false;
		Iterator itr = tmpPlan.iterator();
		while(itr.hasNext()) {

			ActionPair pair = (ActionPair) itr.next();
			if((pair.getA1()==actns.getA1()) && (pair.getA2() == actns.getA2())) 
				flag = true;
		}
		return false;
	}

	private boolean isPairPresent(PlanActionPairs plan, ActionPair actns) {

		boolean flag=false;
		Iterator itr = plan.getAction().iterator();
		while(itr.hasNext()) {

			ActionPair pair = (ActionPair) itr.next();
			if((pair.getA1()==actns.getA1()) && (pair.getA2() == actns.getA2())) 
				flag = true;
		}
		return flag;		
	}

	private boolean isPresent(Plan plan, String action) {

		boolean flag=false;
		Iterator itr = plan.getAction().iterator();
		while(itr.hasNext()) {

			PossibleGroundedActions ga = (PossibleGroundedActions) itr.next();
			if(ga.getGA().toString().equals(action)) 
				flag = true;
		}
		return flag;
	}

	private boolean goalVisited(ArrayList<ArrayList<AtomicFormula>> visGoal,ArrayList<AtomicFormula> newGoal) {

		Iterator itr = visGoal.iterator();

		boolean flag = false;
		while(itr.hasNext()) {

			ArrayList<AtomicFormula> visitedGoal = (ArrayList<AtomicFormula>) itr.next();
			if (visitedGoal.containsAll(newGoal) && newGoal.containsAll(visitedGoal))
				flag=true;
			//			for(int i=0;i<visitedGoal.size();i++)
			//				for(int j=0;j<newGoal.size();j++)
			//					if(visitedGoal.get(i).equals(newGoal.get(j)))
			//						flag = true;
		}
		return flag;
	}

	//Applies a grounded action on the current state
	private ArrayList<AtomicFormula> applyAction(ActionPair act,ArrayList<AtomicFormula> cur) {

		ArrayList<AtomicFormula> newState = new ArrayList<AtomicFormula>();

		//gA.printGroundedAction();

		newState.addAll(cur);

		for(int i=0;i<2;i++) {

			ArrayList<Exp> removeNeg = new ArrayList<Exp>();

			//Performing set minus 
			Iterator<AtomicFormula> itrNew = newState.iterator();
			while(itrNew.hasNext()) {								

				Exp exp = itrNew.next();
				//if(act.getNegEff().contains(exp))
				removeNeg.add(exp);
			}

			//Cur State - Neg Effects
			newState.removeAll(removeNeg);

			//Union all Pos effects
			//newState.addAll(act.getPosEff());
		}


		return newState;
	}

	//Generate all relevant actions
	private ArrayList<PossibleGroundedActions> getRelevantActions(ArrayList<AtomicFormula> goal) {

		ArrayList<PossibleGroundedActions> relActn = new ArrayList<PossibleGroundedActions>();
		Iterator itr = gActions.iterator();
		while(itr.hasNext()) {

			PossibleGroundedActions ga = (PossibleGroundedActions) itr.next();
			//System.out.println(ga.getGA().toString());
			ArrayList<AtomicFormula> posEff = ga.getPosEff();
			ArrayList<AtomicFormula> negEff = ga.getNegEff();

			//If pos effects of action is a subset of current goal and 
			//neg effects of action does not invalidate the current goal

			boolean flag = false;

			for(int i=0;i<goal.size();i++)
				for(int j=0;j<posEff.size();j++)
					if(goal.get(i).equals(posEff.get(j)))
						flag = true;

			for(int i=0;i<goal.size();i++)
				for(int j=0;j<negEff.size();j++)
					if(goal.get(i).equals(negEff.get(j)))
						flag = false;


			if( flag ) {				

				//Mark action as relavant
				relActn.add(ga);
			}			
		}

		return relActn;
	}

	private ArrayList<ActionPair> getRelevantActionPair(ArrayList<AtomicFormula> goal) {

		ArrayList<ActionPair> relPairs = new ArrayList<ActionPair>();

		ArrayList<PossibleGroundedActions> relActn = getRelevantActions(goal);

		Iterator itr1 = relActn.iterator();		
		while(itr1.hasNext()) {

			PossibleGroundedActions a1 = (PossibleGroundedActions) itr1.next();
			ArrayList<PossibleGroundedActions> nextRelActn = getRelevantActions(a1.getPreCond()); 

			Iterator itr2 = nextRelActn.iterator(); 

			while(itr2.hasNext()) {

				PossibleGroundedActions a2 = (PossibleGroundedActions) itr2.next();				
				ActionPair actns = new ActionPair(a1,a2);	
				relPairs.add(actns);
			}			
		}		
		return relPairs;		
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
			GSPSTwo str = new GSPSTwo(obj);

			ArrayList start = str.getInit();
			ArrayList goal = str.getGoal();

			//			//Mark start state as visited
			//			str.visitedStates.add(start);

			//Start recursive algo with start state, goal state and empty plan
			PlanActionPairs pl = new PlanActionPairs();
			pl = str.genPlan(start, goal, new ArrayList<ActionPair>(),0);

			ArrayList<ActionPair> plan = pl.getAction();

			//if(plan.isEmpty())
			if(pl.isFailure())
				System.out.println("Plan not found!!");

			else {

				ArrayList<AtomicFormula> newState = null;
				for(int i=0;i<plan.size();i++) {

					newState = str.applyAction(plan.get(i), start);
				}

				if(newState.containsAll(goal)) {

					System.out.println("Plan found!!");
					//str.printPlan(plan);
				}
				else
					System.out.println("Plan not found!!");
			}	

		}
	}

	private void printPlan(ArrayList<PossibleGroundedActions> plan) {

		Iterator itr = plan.iterator();
		//System.out.print("");
		while(itr.hasNext()) {
			PossibleGroundedActions g = (PossibleGroundedActions) itr.next();
			g.printGroundedAction();
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

	private ArrayList<Exp> getPreConditions(Action actn) {

		AndExp aExp = (AndExp) actn.getPrecondition();
		ArrayList<Exp> precond = new ArrayList<Exp>();
		Iterator eItr = aExp.iterator();

		while(eItr.hasNext()){
			precond.add((Exp) eItr.next());						
		}

		//System.out.println(precond.toString());
		return precond;
	}

	//Check if goal is subset of new state
	private boolean goalTest(ArrayList<AtomicFormula> cur) {

		ArrayList<AtomicFormula> g = getGoal();
		return isSubsetOf(g,cur);
	}

	private boolean isSubsetOf(ArrayList<AtomicFormula> g, ArrayList<AtomicFormula> s) {

		if(g.isEmpty())
			return false;

		else {

			boolean flag = s.containsAll(g);
			return(flag);
		}	
	}

	private void printList(ArrayList<AtomicFormula> list) {

		Iterator itr = list.iterator();
		//System.out.print("");
		String lst="";
		while(itr.hasNext()) {
			//System.out.print(itr.next());
			lst = lst + itr.next();			
		}	
		System.out.println(lst);
		//System.out.println("");
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

class PlanActionPairs {

	boolean failure;
	ArrayList<ActionPair> action;

	public PlanActionPairs() {

		action = new ArrayList<ActionPair>();
		failure = true;		
	} 

	public boolean isFailure() {
		return failure;
	}

	public void setFailure(boolean failure) {
		this.failure = failure;
	}

	public ArrayList<ActionPair> getAction() {
		return action;
	}

	public void setAction(ArrayList<ActionPair> action) {
		this.action = action;
	}


}

class ActionPair {

	PossibleGroundedActions a1,a2;

	public ActionPair(PossibleGroundedActions a1, PossibleGroundedActions a2) {

		this.a1 = a1;
		this.a2 = a2;
	}

	public PossibleGroundedActions getA1() {
		return a1;
	}

	public void setA1(PossibleGroundedActions a1) {
		this.a1 = a1;
	}

	public PossibleGroundedActions getA2() {
		return a2;
	}

	public void setA2(PossibleGroundedActions a2) {
		this.a2 = a2;
	}

	public String toString() {

		String pair = "<"+a1.getGA().toString()+","+a2.getGA().toString();
		return pair;	
	}
}
