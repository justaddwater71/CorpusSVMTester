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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class takes already created libSVM sparse formatted file containing
 * minimum perfect hash values for feature instances and then remaps
 * those minimum perfect hash values into a smaller hash space in order
 * to not overflow libLinear's arrays.  This is accomplished by taking each
 * minimum perfect hash value encountered in all the file within a directory,
 * assigning each value found a new sequential value, and placing that
 * old value/new value into a hashmap as key/pair.  
 * 
 * @author Jody Grady, CS Masters Student, Naval Postgraduate School
 *
 */
public class SVMToSmallSVM 
{
	//Data Members
	HashMap<Integer, Integer> largeToSmallHashMap;
	int 												mapMax;
	
	//Constructors
	
	
	//Methods
	/**
	 * Creates a hash map of Integer keys made up of minimum perfect hash values
	 * and Integer values made up of sequential numbers that attempt to shrink down
	 * the search space to a size that libLinear can handle.
	 * 
	 * @param filename file containing serialized hash map
	 * @throws FileNotFoundException if there is no hash map file, make a new hash map
	 * @throws IOException if permissions or path issues interfere with getting file, then 
	 * stop the process
	 * @throws ClassNotFoundException if the file does not contain a valid 
	 * HashMap<Integer, Integer> class, then stop the process
	 */
	@SuppressWarnings("unchecked")
	public void initlializeLargeToSmallHashMap(String filename) throws IOException, ClassNotFoundException
	{
		File largeToSmallHashMapFile = new File(filename);
		
		try
		{
			//Load hash map from a file
			InputStream inputStream = new FileInputStream(largeToSmallHashMapFile);
			ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
			largeToSmallHashMap = (HashMap<Integer, Integer>)objectInputStream.readObject();
			objectInputStream.close();
			
			//Find maximum value in the values set of the hash map
			Set<Integer> mapSet = new TreeSet<Integer>(largeToSmallHashMap.keySet());
			mapMax = Collections.max(mapSet);
		}
		catch (FileNotFoundException f)
		{
			//If there is no file, then create a new hashmap
			largeToSmallHashMap = new HashMap<Integer, Integer>();
			mapMax = 1;
		}
	}
	
	public void processLargeSVMDirectory(String directoryName) throws FileNotFoundException
	{
		File[] fileArray;
		
		File directory = new File(directoryName);
		
		if (directory.isDirectory())
		{
			fileArray = directory.listFiles();
		}
		else
		{
			fileArray = new File[1];
			fileArray[0] = directory;
		}
		
		for (int i=0; i < fileArray.length; i++)
		{
			System.out.println("Processing " + fileArray[i].getAbsolutePath());
			processLargeSVMFile(fileArray[i]);
		}
	}
	
	public String checkFeature(String feature)
	{
		Integer intFeature = Integer.valueOf(feature);
		Integer intSmallFeature;
		
		if (largeToSmallHashMap.containsKey(intFeature))
		{
			intSmallFeature = largeToSmallHashMap.get(intFeature);
		}
		else
		{
			intSmallFeature = mapMax;
			mapMax = mapMax + 1;
			largeToSmallHashMap.put(intFeature, intSmallFeature);
		}
		
		return intSmallFeature.toString();
	}
	
	public String convert(String oldLine)
	{
		String newLine;
		String pair;
		String feature;
		String count;
		String smallFeature;
		Integer key;
		int colonIndex;
		SortedMap<Integer, String> sortedMap = new TreeMap<Integer, String>();
		
		StringTokenizer tokenizer = new StringTokenizer(oldLine);
		
		//Get the id of this line -- no safety check here, yet
		newLine = tokenizer.nextToken();
		
		while (tokenizer.hasMoreElements())
		{
			pair = tokenizer.nextToken();
			
			colonIndex = pair.indexOf(':');
			
			if (colonIndex < 0)
			{
				System.out.println("Malformed pair");
				return null;
			}
			
			feature = pair.substring(0, colonIndex);
			count = pair.substring(colonIndex + 1);
			
			smallFeature = checkFeature(feature);
			
			sortedMap.put(Integer.valueOf(smallFeature), count);
		}
		
		Iterator<Integer> iterator = sortedMap.keySet().iterator();
		
		while (iterator.hasNext())
		{
			key = iterator.next();
			newLine = newLine + " " + key + ":" + sortedMap.get(key);
		}
		
		return newLine;
	}
	
	//FIXME make this directory dependent vice extension dependent
	public void writeSmallSVMFile(File largeFile, BufferedReader largeBufferedReader) throws IOException
	{
		String smallFilename = largeFile.getAbsolutePath();
		
		File smallFile = new File(smallFilename + ".small");
		
		String oldLine;
		String newLine = null;
		
		try 
		{
			smallFile.createNewFile();
			PrintWriter smallPrintWriter = new PrintWriter(smallFile);
			
			while ((oldLine = largeBufferedReader.readLine()) != null)
			{
				newLine = convert(oldLine);
				smallPrintWriter.println(newLine);
			}
			
			smallPrintWriter.flush();
		}
		catch (FileNotFoundException f)
		{
			smallFile.createNewFile();
		}

	}
	
	public void processLargeSVMFile(File largeSVMFile) throws FileNotFoundException, IOException
	{
		if (largeSVMFile.isDirectory())
		{
			processLargeSVMDirectory(largeSVMFile.getAbsolutePath());
		}
		BufferedReader largeBufferedReader =new BufferedReader(new FileReader(largeSVMFile));
		
		writeSmallSVMFile(largeSVMFile, largeBufferedReader);
	}
	
}
