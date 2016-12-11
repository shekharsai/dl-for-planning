package bgu.dl.features.learning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import pddl4j.PDDLObject;
import pddl4j.exp.AtomicFormula;

import bgu.dl.features.collections.PlanDetails;

/**
 * @author Shashank Shekhar
 * BGU of the Negev, Israel.
 * */
public class MainCallRandomProblemDatasetGeneration 
{
	public static void main(String[] args) throws IOException {
		MainCallRandomProblemDatasetGeneration problem = new MainCallRandomProblemDatasetGeneration();
		problem.generateProblemFile();
	}

	@SuppressWarnings("resource")
	public void generateProblemFile() throws IOException
	{		
		// Currently hardcoded
		int nGram = 2; // not generic
		System.out.println("Will you able to fulfil your dreams?");
		ArrayList<String> blocksList = new ArrayList<String>();
		blocksList.add("ontable");
		for (char letter = 'A'; letter <= 'Z'; letter++) {			
			blocksList.add(""+letter);
		}

		BufferedReader br = 
				new BufferedReader(new 
						FileReader("/home/shashank/Documents/Copy-IITM/Planning-Domain-IPC2/2000-Tests/Blocks/Generator/blocksworld-generator/bwstates/shashank-rand-probs"));

		String line = new String();
		ArrayList<ArrayList<Integer>> listOfStates = new ArrayList<ArrayList<Integer>>();
		// 'listOfStates' will contain all states in integer form, 0 represents on-table
		ArrayList<Integer> list;
		while (null != (line = br.readLine())) {
			String[] line_parts = line.split(" ");
			list = new ArrayList<Integer>();
			for (int i = 0; i < line_parts.length; i++) {
				String str = (String)line_parts[i];
				int x = Integer.parseInt(str);
				list.add(x);
			} 
			listOfStates.add(list);
		}

		File file, file_unigram_words, file_unigram_context, file_ngram_words, file_ngram_context; 		
		Writer writer, writer_unigram_words, writer_unigram_context, writer_ngram_words, writer_ngram_context;

		// training samples
		file = new File("/home/shashank/Documents/Experiments-DL-NGrams/TrainingSet.txt");
		writer = new BufferedWriter(new FileWriter(file));

		// words (for word-2-vec unigram representation)
		file_unigram_words = new File("/home/shashank/Documents/Experiments-DL-NGrams/UnigramDictionary.txt");
		writer_unigram_words = new BufferedWriter(new FileWriter(file_unigram_words));

		// words (for word-2-vec unigram representation)
		file_unigram_context = new File("/home/shashank/Documents/Experiments-DL-NGrams/ContextOfEachUnigramWordInTheDict.txt");
		writer_unigram_context = new BufferedWriter(new FileWriter(file_unigram_context));

		// words (for word-2-vec ngram representation)
		file_ngram_words = new File("/home/shashank/Documents/Experiments-DL-NGrams/ngramDictionary.txt");
		writer_ngram_words = new BufferedWriter(new FileWriter(file_ngram_words));

		// words (for word-2-vec ngram representation)
		file_ngram_context = new File("/home/shashank/Documents/Experiments-DL-NGrams/ContextOfEachNgramWordInTheDict.txt");
		writer_ngram_context = new BufferedWriter(new FileWriter(file_ngram_context));

		// Now play with these numbers.
		for (int i = 0, j=listOfStates.size()-1; i < listOfStates.size(); i++, j--) 
		{
			ArrayList<Integer> initNumber = listOfStates.get(i);
			ArrayList<Integer> goalNumber = listOfStates.get(j);

			ArrayList<String> objectList = new ArrayList<String>();
			ArrayList<String> initialState = new ArrayList<String>();
			ArrayList<String> goalState = new ArrayList<String>();

			// Capturing ALL relations for the initial state
			initialState.add("(HANDEMPTY)");
			for (int k = 0; k < initNumber.size(); k++) {
				int index = initNumber.get(k);
				int index_onto = k+1;
				objectList.add(blocksList.get(index_onto));

				if (index != 0) {
					String block_under = blocksList.get(index);
					String block_above = blocksList.get(index_onto);				
					String prop = "(ON "+block_above+" "+block_under+")";
					initialState.add(prop);
				}
				else {
					String block_above = blocksList.get(index_onto);				
					String prop = "(ONTABLE "+block_above+")";
					initialState.add(prop);
				}
				if(!initNumber.contains(index_onto))
				{
					String block_above = blocksList.get(index_onto);
					String prop = "(CLEAR "+block_above+")";
					initialState.add(prop);
				}
			}

			// Capturing ALL relations for the goal state
			goalState.add("(HANDEMPTY)");
			for (int k = 0; k < goalNumber.size(); k++) {
				int index = goalNumber.get(k);
				int index_onto = k+1;
				if (index != 0) {
					String block_under = blocksList.get(index);
					String block_above = blocksList.get(index_onto);				
					String prop = "(ON "+block_above+" "+block_under+")";
					goalState.add(prop);
				}
				else {
					String block_above = blocksList.get(index_onto);				
					String prop = "(ONTABLE "+block_above+")";
					goalState.add(prop);
				}
				if(!goalNumber.contains(index_onto))
				{
					String block_above = blocksList.get(index_onto);
					String prop = "(CLEAR "+block_above+")";
					goalState.add(prop);
				}
			}

			// Instantiate for the bootstrapping purpose
			String pathToTheRandProblem = "/home/shashank/Dropbox/Bgu-Files/bgu.dl.heuristic/eclipse/bgu.learning/src/bgu/dl/features/learning/Rand-Problem/problem.pddl";
			String pathToTheBWDomain = "/home/shashank/Dropbox/Bgu-Files/bgu.dl.heuristic/eclipse/bgu.learning/src/bgu/dl/features/learning/Rand-Problem/domain.pddl";

			// Generates a complete problem file	
			generateRandomProblemFile(initialState, goalState, objectList, pathToTheRandProblem);

			InstantiatedByRandomProblem instantiatedByRandomProblem = new InstantiatedByRandomProblem();
			PDDLObject pddlObject = instantiatedByRandomProblem.instantiationUsingARandomProblem(pathToTheBWDomain, pathToTheRandProblem);

			// Solve the same problem using FD.
			PlanDetails planDetails = targetByFastDownward(pathToTheBWDomain, pathToTheRandProblem);

			PossibleGroundedLiterals possibleGroundedLiterals = new PossibleGroundedLiterals(pddlObject);		
			ArrayList<AtomicFormula> listOfPossiblePropositions = new ArrayList<>();
			listOfPossiblePropositions = possibleGroundedLiterals.allPossibleLiteralsMayOccur();

			if(i==0)
			{
				ArrayList<String> header_init = new ArrayList<String>();
				ArrayList<String> header_goal = new ArrayList<>();
				for (int k = 0; k < listOfPossiblePropositions.size(); k++) {
					String string = listOfPossiblePropositions.get(k).toString();
					header_init.add(string +"-I");
					header_init.add("(not"+ string + ")" +"-I");
					header_goal.add(string +"-G");
					header_goal.add("(not"+ string + ")" +"-G");
				}			

				// For the purpose of unigram
				String header_str_init = "";
				String header_str_goal = "";				
				for (int k = 0; k < header_init.size(); k++) {
					header_str_init = header_str_init + header_init.get(k) + "\t";
					header_str_goal = header_str_goal + header_goal.get(k) + "\t";
				}				
				String header_unigram_vocab = header_str_init + header_str_goal;				
				writer_unigram_words.append(header_unigram_vocab); writer_unigram_words.close(); // Will write the whole vocabulary

				// For the purpose for bi-gram
				ArrayList<String> listOfLiteralsForNGrams = new ArrayList<String>();
				listOfLiteralsForNGrams.addAll(header_init); listOfLiteralsForNGrams.addAll(header_goal);
				String bi_gram_header = "";
				int c=0;
				/*for (int k = 0; k < listOfLiteralsForNGrams.size(); k++) {
					for (int l = k+1; l < listOfLiteralsForNGrams.size(); l++) {						
						c++; bi_gram_header = bi_gram_header + listOfLiteralsForNGrams.get(k) + ":" + listOfLiteralsForNGrams.get(l) + "\t";
					}
				}*/
				// System.out.println(c);
				//writer_ngram_words.append(bi_gram_header); writer_ngram_words.close(); // Will write the whole bi-gram vocabulary
				
				// Below will be the training set so it will be similar for all N-Grams
				header_unigram_vocab = header_unigram_vocab + "target\n"; writer.append(header_unigram_vocab); // including (on a b) and (not(on a b)) 
			}
			instantiatedByRandomProblem.callForDatasetGeneration(planDetails, pddlObject, writer, writer_unigram_context, writer_ngram_context);
			if(i%5000 == 0)
			System.out.println(i);
		}
		writer_unigram_context.close();
		writer_ngram_context.close();
		writer.close();
	}

	/**
	 * Function call to the fast-downward (FD) planner | keep in mind that the call gets killed after a certain time (say after 30 minutes). 
	 * @param initialState
	 * @param goalState
	 * @return returns the target value, basically, the plan length found by the FD planner.
	 * */
	private PlanDetails targetByFastDownward(String pathToTheBWDomain, String pathToTheRandProblem) 
	{
		int target = 1000000;
		ArrayList<String> plan = new ArrayList<String>();
		PlanDetails details =  new PlanDetails();
		try 
		{
			String[] command = {
					"/home/shashank/Documents/Copy-IITM/Research-Edited/Fast-Downward/fast-downward.py",
					pathToTheBWDomain,
					pathToTheRandProblem,
					"--heuristic",
					"h=ff()",
					"--search",
					"lazy_greedy(h, preferred=h)"
				};

			Process pro = Runtime.getRuntime().exec(command);
			BufferedReader in = new BufferedReader(new InputStreamReader(pro.getInputStream()));
			String line = null;
			String[] planDetails = null;
			boolean firstLine = false;
			boolean secondLine = false;
			int count = 0;
			while ((line = in.readLine()) != null) 
			{
				// Conditions for getting the real plans
				if(firstLine) 
					count++;
				if (line.contains("Actual search")) 
					firstLine = true;
				if (line.contains("Plan length"))  
					secondLine = true;

				if(firstLine && count >= 1 && !secondLine) {					
					String[] currAction = line.split(" ");
					String str = "(";
					for (int i = 0; i < currAction.length; i++) {
						if (i < currAction.length-1) { 
							if(i < currAction.length-2) 
								str = str + currAction[i] + " ";
							else
								str = str + currAction[i];
						}
					}
					str = str + ")";		
					plan.add(str.toString());
				}
				// Conditions for getting the real targets predicted by FD.
				if (line.contains("Plan length")) {
					planDetails = line.split(" ");
				}
			}	
			target = Integer.parseInt(planDetails[2]);
			details.setPlanLength(target);
			details.setGeneratedRealPlan(plan);
			// The real plan extraction is ridiculously done, really need an update on that!
		} catch (Exception e) {
			System.err.println("Error in writing the planner output in file !!");
		}
		return details; 
	}

	// Generate a random problem file "problem.pddl" 
	private void generateRandomProblemFile(ArrayList<String> initialState, ArrayList<String> goalState, ArrayList<String> objectlist, String problempath) 
	{ 
		List<String> lines = new ArrayList<String>();
		String line = null;
		try {
			File f1 = new File(problempath);
			FileReader fr = new FileReader(f1);
			BufferedReader br = new BufferedReader(fr);

			String str ="(:init ";
			for (int i = 0; i < initialState.size(); i++) {
				str = str + initialState.get(i).toString();
			}
			str = str + ")";

			while ((line = br.readLine()) != null) {
				if (line.contains(":init") || line.contains(":INIT") )
				{
					line = line.replace(line, str);
				}
				lines.add(line);
			}			
			fr.close();
			br.close();

			FileWriter fw = new FileWriter(f1);
			BufferedWriter out = new BufferedWriter(fw);
			for(String s : lines)
			{
				out.write(s);
				out.write("\n");
			}
			out.flush();
			out.close();

			lines = new ArrayList<String>();
			File f2 = new File(problempath);
			fr = new FileReader(f2);
			br = new BufferedReader(fr);
			str = new String();
			str = "(:goal (AND ";
			for (int i = 0; i < goalState.size(); i++) {
				str = str + goalState.get(i).toString();
			}
			str = str + "))";

			while ((line = br.readLine()) != null) {
				if (line.contains(":goal") || line.contains(":GOAL") )
				{
					line = line.replace(line, str);
				}
				lines.add(line);
			}

			fr.close();
			br.close();

			fw = new FileWriter(f2);
			out = new BufferedWriter(fw);
			for(String s : lines)
			{
				out.write(s);
				out.write("\n");
			}
			out.flush();
			out.close();

			lines = new ArrayList<String>();
			f2 = new File(problempath);
			fr = new FileReader(f2);
			br = new BufferedReader(fr);
			str = new String();
			str = "(:objects ";
			for (int i = 0; i < objectlist.size(); i++) {
				str = str +" "+ objectlist.get(i).toString();
			}
			str = str + ")";

			while ((line = br.readLine()) != null) {
				if (line.contains(":objects") || line.contains(":OBJECTS") )
				{
					line = line.replace(line, str);
				}
				lines.add(line);
			}

			fr.close();
			br.close();

			fw = new FileWriter(f2);
			out = new BufferedWriter(fw);
			for(String s : lines)
			{
				out.write(s);
				out.write("\n");
			}
			out.flush();
			out.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}