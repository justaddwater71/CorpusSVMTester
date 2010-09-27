/*
 * This package is a collection of classes used to process a directory of text files,
 * where each file represents an author, topic, or some other classification,
 * and creates a minimum perfect hash function value based on a pre-computed
 * set of accepts words/ngrams/instances, creates SVM files, created n cross
 * validation sets for each grouping (selected by user), trains liblinear against
 * the training sets, uses liblinear to run predictions, compares predictions to 
 * "truth" in the original data, and compiles that data into a confusions matrix
 * plus other key statistics.
 */
package edu.nps.jody.CorpusSVMTester;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.io.FileNotFoundException;

import edu.nps.jody.HashFinder.MembershipChecker;

/**
 * This class begins the entire analysis process.  This class finds the minimum
 * perfect has keys, the standard hash signatures, the original text files, the
 * type of features desired and created the initial set of SVM files.  Once those
 * processes are complete, this class passes control to another class that 
 * rehashes all the minimum perfect hash values down to smaller numbers
 * so as to not overflow liblinear's arrays
 * 
 * @author Jody Grady, CS Masters Student, Naval Postgraduate School
 *
 */
public class TextToSVM 
{
	//Data Members
/*	public static final int ONE_GRAM										= FeatureMaker.ONE_GRAM;
	public static final int TWO_GRAM										= FeatureMaker.TWO_GRAM;
	public static final int THREE_GRAM									= FeatureMaker.THREE_GRAM;
	public static final int FOUR_GRAM									= FeatureMaker.FOUR_GRAM;
	public static final int FIVE_GRAM										= FeatureMaker.FIVE_GRAM;
	public static final int GAPPY_BIGRAM								= FeatureMaker.GAPPY_BIGRAM;
	public static final int GAPPY_BIGRAM_TAGGED 				= FeatureMaker.GAPPY_BIGRAM_TAGGED;
	public static final int ORTHOGONAL_SPARSE_BIGRAM 	= FeatureMaker.ORTHOGONAL_SPARSE_BIGRAM;*/
	
	public static final String 	PATH_DELIM 						= System.getProperty("path.separator");
	public static final String PAIR_DELIM							= ":";
	public static final String CMPH_DIR_NAME				= "cmph";
	public static final String KEY_FILE_NAME					= "keys.mph";
	public static final String SIGNATURE_FILE_NAME	= "signature";
	public static final String SVM_DIR_NAME					= "svmFiles";
	private static final String TEXT_DIR_NAME 				= "text";
	private						int		maxMapValue;
	
	//Constructors
	/**
	 * Sole constructor for TextToSVM.  Sets maxMapValue = 0. If this class goes back to 
	 * being all static methods, then this constructor will be empty. 
	 */
	TextToSVM()
	{
		maxMapValue = 0;
	}
	
	//Methods
	/**
	 * Find the maximum Integer value within nameToIntegerMap
	 * 
	 * @param nameToIntegerMap - the set of strings and their counts/ids
	 * @return the highest value Integer in the value portion of nameToIntegerMap
	 */
	public static  Integer getMaxMapValue(HashMap<String, Integer> nameToIntegerMap)
	{
		if (nameToIntegerMap.isEmpty())
		{
			return 0;
		}
		else
		{
			return Collections.max(nameToIntegerMap.values());
		}
	}
	
	/**
	 * Finds the id number (value) for a given filename (key) within the provided hashmap. 
	 * If the filename (key) is not found, then that filename is entered into the map along 
	 * with the next highest value within the map as the new filename's id
	 * 
	 * @param filename - names of the file being checked for an id
	 * @param nameToIntegerMap - hashMap of filename to id values
	 * @return an int value of the id for the given filename
	 */
	public int getIntegerIdOfFilename(String filename, HashMap<String, Integer> nameToIntegerMap)
	{
		if (nameToIntegerMap.containsKey(filename))
		{
			return nameToIntegerMap.get(filename);
		}
		else
		{
			maxMapValue++;
			nameToIntegerMap.put(filename, maxMapValue);
			return maxMapValue;
		}
	}
	
	/**
	 * Finds the id number (value) for a given filename (key) within the provided hashmap. 
	 * If the filename (key) is not found, then that filename is entered into the map along 
	 * with the next highest value within the map as the new filename's id.  This version
	 * of getIntegerIdOfFilename is NOT as efficient as using a counter within an object.
	 * That more efficient method IS included within this class, but is commented out
	 * to attempt to implement this tester statically vice as an object.
	 * 
	 * @param filename names of the file being checked for an id
	 * @param nameToIntegerMap hashMap of filename to id values
	 * @return an int value of the id for the given filename
	 */
	public static  int getIntegerIdOfFilenameStatic(String filename, HashMap<String, Integer> nameToIntegerMap)
	{
		if (nameToIntegerMap.containsKey(filename))
		{
			return nameToIntegerMap.get(filename);
		}
		else
		{
			int maxMapValue = getMaxMapValue(nameToIntegerMap);
			nameToIntegerMap.put(filename, maxMapValue + 1);
			return (maxMapValue + 1);
		}
	}
	
	/**
	 * Creates a Vector of String phrases using a supplied JFlex parser object.
	 * This allows customized parsing of strings of text for arbirary situations 
	 * such as reverse engineering the parsing rules used for the Google Web1T
	 * corpus
	 * 
	 * @param scanner a JFlex generated object that parses a string of text
	 * @return a Vector of Strings containing the parsed tokens (words) from scanner
	 * @throws IOException the JFlex object, scanner, throws an IOException and
	 *  a FileNotFoundException if there are issues reading the intended text file. 
	 *  Those expections are thrown up to this method.
	 */
	public Vector<String> getPhraseFromLex(Yylex scanner) throws IOException
	{
		Vector<String> stringVector = new Vector<String>();
		
		while (!scanner.zzAtEOF)
		{
			if (scanner.yylex() == Yylex.NEXTWORD)
			{
				stringVector.add(scanner.yytext());
			}
			else
			{
				 return stringVector;
			}
		}
		
		return null;
	}
	
	/**
	 * Converts a String vector of text instances (unigrams, bigram, Gappy Bigrams, etc)  into a minimum perfect hash value
	 * using the CHD method of minimum perfect hash.  These CHD minimum perfect has values are then stored in
	 * a hash map that maps CHD minimum perfect hash values to counts of the number of times those values
	 * have been encountered
	 * 
	 * @param instanceVector		Vector of text instances built from lines of text and feature selection (unigrams, bigrams, etc)
	 * @param membershipChecker an object that checks a given instance against a precomputed minimum perfect hash function
	 * for membership and then against a traditional hash function "signature" for collision detection.  Returns the minimum perfect
	 * hash value if the instance is a member.  Returns a -1 if the instance is not a member
	 * @return HashMap of minimum perfect hash values along with a count of how many times those values appeared
	 */
	public HashMap<Integer, Integer> turnInstanceIntoCHDMap(Vector<String>instanceVector, MembershipChecker membershipChecker)
	{
		HashMap<Integer, Integer> chdMap = new HashMap<Integer, Integer>();
		Integer key;
		Integer value;
		Iterator<String> iterator = instanceVector.iterator();
		String bigram;
		String[] bigramTokens;
		
		
		while (iterator.hasNext())
		{
			bigram = iterator.next();
			
			key = membershipChecker.getIndex(bigram);
			
			//Deal with unknown words when <UNK> tag is allowed
			if (key == -1)
			{
				bigramTokens = bigram.split(" ");
				bigram = "<UNK> " + bigramTokens[1] + " " + bigramTokens[2];
				key = membershipChecker.getIndex(bigram);
				
				bigram = bigramTokens[1] + " <UNK> " + bigramTokens[2];
				key = key * membershipChecker.getIndex(bigram);
				
				/* 
				 * If both bigrams with <UNK> subbed are multiplied together and 
				 * the result is negative, then ONE of the <UNK> subs was valid.
				 * If the result is positive, then BOTH of the subs was valid, meaning
				 * that the pair we originally test is simply no good.  By multiplying
				 * by -1 below, then we get the actual bigram cmph we need.
				 */
				if (key < 0)
				{
					key = key * -1;
				}
				else
				{
					key = -1;
				}
			}
			
			if (key >= 0)
			{
				if (chdMap.containsKey(key))
				{
					//Increment value of this key already in the HashMap
					value = chdMap.get(key);
					chdMap.put(key, value + 1);
				}
				else
				{
					//Initialize the new key
					chdMap.put(key, 1);
				}
			}
		}
		
		return chdMap;
	}
	
	/**
	 * Writes a HashMap (of CHD generated miminum perfect hash values mapped to words to the numbers
	 * of times that words has appeared) to a SVM formatted file.  This transforms the chdMap of hashes and counts
	 * into a formatted filed suitable for processing by libSVM or libLinear (or any program that uses libSVM sparse format.)
	 * 
	 * @param chdMap			the Hash Map of CHD miminum perfect hash values mapped to number of occurrences
	 * @param printWriter	the PrintWriter object that will be used to write out the chdMap into a libSVM formatted file
	 * @param id					integer id representing the name of the original  file that this map was drawn from
	 */
	public void writeCHDMapToSVMFile(HashMap<Integer, Integer> chdMap, PrintWriter printWriter, int id)
	{
		SortedSet<Integer> sortedSet = new TreeSet<Integer>(chdMap.keySet());
		
		Iterator<Integer> iterator = sortedSet.iterator();
		
		Integer key;
		Integer value;
		
		printWriter.print(id + " ");
		
		while (iterator.hasNext())
		{
			key = iterator.next();
			
			value = chdMap.get(key);
			
			if (value == null)
			{
				System.out.println("For key " + key + " in file " + id);
				System.out.println("NULL in VALUE!!! HALT!!!");
				return;
			}
			
			printWriter.print(key.toString() + ":" + value.toString() + " ");
		}
		
		printWriter.print("\n");
	}
	
	/**
	 * Turns all files in a given directory or a single file into a corresponding libSVM SPARSE formatted file madu up of hash value for tokens
	 * and counts of the occurrences of each token within a string.  This method does NOT recurse the text directory.  Any encountered
	 * subdirectories will be ignored.  This function requires that if a directory is given, then a sibling directory named "cmph" exists or
	 * if a file is given, then a sibling to that files parent directory exists name "cmph".  The "cmph" directory contains a file named "keys.mph"
	 * and a file named "signature" that are used to build a MembershipChecker for this processing.
	 */
	public void processFiles(File parentDirectory, int maxGap,  FeatureTypes featureType) throws FileNotFoundException, IOException
	{
		File textDir				= new File(parentDirectory.getParentFile(), TEXT_DIR_NAME);
		File cmphDir			= new File(parentDirectory, 	CMPH_DIR_NAME);
		File keyFile				= new File(cmphDir, 				KEY_FILE_NAME);
		File signatureFile	= new File(cmphDir, 				SIGNATURE_FILE_NAME);
		File svmDir				= new File(parentDirectory,	SVM_DIR_NAME);
		
		processFiles(textDir, maxGap, featureType, keyFile.getAbsolutePath(), signatureFile.getAbsolutePath(), svmDir);
	}
		
	
	/**
	 * Turns all files in a given directory or a single file into a corresponding libSVM SPARSE formatted file madu up of hash value for tokens
	 * and counts of the occurrences of each token within a string.  This method does NOT recurse the text directory.  Any encountered
	 * subdirectories will be ignored.  This function requires that if a directory is given, then a sibling directory named "cmph" exists or
	 * if a file is given, then a sibling to that files parent directory exists name "cmph".  The "cmph" directory contains a file named "keys.mph"
	 * and a file named "signature" that are used to build a MembershipChecker for this processing.
	 * 
	 * @param fileArray
	 * @param maxGap
	 * @param nameToIntegerMap
	 * @param featureType
	 * @param membershipChecker
	 * @throws IOException
	 */
	public void processFiles(String textDirName, int maxGap,  FeatureTypes featureType, String keyFileName, String signatureFileName, String svmDirName) throws FileNotFoundException, IOException
	{
		File textDir = new File(textDirName);
		File svmDir = new File(svmDirName);
		
		processFiles(textDir, maxGap, featureType, keyFileName, signatureFileName, svmDir);
	}
	
	public void processFiles(File textDirectory, int maxGap, FeatureTypes featureType, String keyFileName, String signatureFileName, File svmDir) throws FileNotFoundException, IOException
	{
		Yylex scanner;
		Vector<String> vectorString;
		Vector<String> instanceVector;
		HashMap<String, Integer>nameToIntegerMap = new HashMap<String, Integer>();
		MembershipChecker membershipChecker;
		HashMap<Integer, Integer> chdMap;
		PrintWriter printWriter;
		File[] fileArray;
		File sourceFile;
		File writeFile;

		int id;
		
		if (textDirectory.isDirectory())
		{
			fileArray = textDirectory.listFiles();
			//parentFileName = file.getParent();
		}
		else
		{
			fileArray = new File[1];
			fileArray[0] = textDirectory;
			//parentFileName = file.getParentFile().getParent();
		}
		
		membershipChecker = new MembershipChecker(keyFileName, signatureFileName);
		
		for (int i=0;i < fileArray.length;i++)
		{
			if (fileArray[i].isFile())
			{
				//Get the source file
				sourceFile = fileArray[i];
				scanner = new Yylex(new FileReader(sourceFile));
				
				//Set up svm file to be written to, if path to writeFile does not exist, create it, but only once
				try
				{
					writeFile = new File(svmDir, sourceFile.getName());
					writeFile.createNewFile();
				}
				catch(IOException e)
				{
					svmDir.mkdirs();
					writeFile = new File(svmDir, sourceFile.getName());
					writeFile.createNewFile();
				}
				printWriter = new PrintWriter(writeFile);
				
				//Get Integer ID of file
				id = getIntegerIdOfFilename(sourceFile.getName(), nameToIntegerMap);
				
				while((vectorString = getPhraseFromLex(scanner)) != null)
				{
					if (!vectorString.isEmpty())
					{
						instanceVector =FeatureMaker.parse(vectorString, maxGap, featureType);
						chdMap = turnInstanceIntoCHDMap(instanceVector, membershipChecker);
						writeCHDMapToSVMFile(chdMap, printWriter, id);
						printWriter.flush();
					}
				}
				
				printWriter.flush();
				printWriter.close();
			}
		}
	}
}
