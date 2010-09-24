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
	public static final String 						PATH_DELIM 						= System.getProperty("path.separator");
	public static final String						PAIRS_DELIM						=" ";//TODO make this selectable in the constructur, but still final
	public static final char						FEATURE_COUNT_DELIM = ':';//TODO make this selectable in the constructor, but still final
	private static final String 					SMALL_SVM_DIR_NAME 	= "smallSVMFiles";
	private HashMap<Integer, Integer> largeToSmallHashMap;
	private int 												mapMax;
	
	//FIXME maybe making a blank hashmap when a file is expected is not a good idea as a fallback plan
	//Constructors
	/**
	 * Constructor used when a hashMap is already saved to an object file
	 * to be reloaded. If this object load fails, then an empty hashmap is made
	 * in its place
	 * 
	 * @param hashMapFileName
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	SVMToSmallSVM(String hashMapFileName) throws IOException, ClassNotFoundException
	{
		initlializeLargeToSmallHashMap(hashMapFileName);
	}
	
	/**
	 * Constructor used when no save object file containing a HashMap is expected.
	 */
	SVMToSmallSVM()
	{
		largeToSmallHashMap = new HashMap<Integer, Integer>();
	}
	
	//Methods
	//FIXME figure out why we can write, but not read object file of hashMap
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
	
	/**
	 * This is thetop level entry point to transform a libSVM sparse format file comprised of minimum perfect hash values
	 * that range too large for libSVM or libLinear to handle into a libSVM sparse format file comprised of sequentially 
	 * encountered values that libSVM and libLinear can handle.  This version IS recursive.  
	 * 
	 * @param directoryNamedirecotry containing  libSVM formatted file containing minimum perfect hash values too large for libLinear to handle
	 * @throws FileNotFoundException If the largeSVMFile is not found, then throw exception
	 * @throws IOException If an IO error other than FileNotFound is encountered, throw exception.  This is most likely a permissions or directory issue.
	 */
	public void processLargeSVMDirectoryRecursive(String directoryName) throws FileNotFoundException, IOException
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
			processLargeSVMFileRecursive(fileArray[i]);
		}
	}
	
	/**
	 * This is thetop level entry point to transform a libSVM sparse format file comprised of minimum perfect hash values
	 * that range too large for libSVM or libLinear to handle into a libSVM sparse format file comprised of sequentially 
	 * encountered values that libSVM and libLinear can handle.  This version IS NOT recursive.  
	 * 
	 * @param directoryNamedirecotry containing  libSVM formatted file containing minimum perfect hash values too large for libLinear to handle
	 * @throws FileNotFoundException If the largeSVMFile is not found, then throw exception
	 * @throws IOException If an IO error other than FileNotFound is encountered, throw exception.  This is most likely a permissions or directory issue.
	 */
	public void processLargeSVMDirectory(String directoryName) throws FileNotFoundException, IOException
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
			processLargeSVMFile(fileArray[i]);
		}
	}
	
	/**
	 * This is thetop level entry point to transform a libSVM sparse format file comprised of minimum perfect hash values
	 * that range too large for libSVM or libLinear to handle into a libSVM sparse format file comprised of sequentially 
	 * encountered values that libSVM and libLinear can handle.  This version IS NOT recursive.  This version allows
	 * the destination directory of the small SVM files to be designated.  The destination directory is created if it does
	 * not already exist.
	 * 
	 * @param directoryNamedirecotry containing  libSVM formatted file containing minimum perfect hash values too large for libLinear to handle
	 * @throws FileNotFoundException If the largeSVMFile is not found, then throw exception
	 * @throws IOException If an IO error other than FileNotFound is encountered, throw exception.  This is most likely a permissions or directory issue.
	 */
	public void processLargeSVMDirectory(String directoryName, String smallDirName) throws FileNotFoundException, IOException
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
			processLargeSVMFile(fileArray[i], smallDirName);
		}
	}
	
	/**
	 * Determines if the minimum perfect hash value entered has already been seen by the converter.  If it has,
	 * then the previously assigned small feature label is returned.  If not, a new value is assigned and then 
	 * returned.  The new value is updated to the mapMax value of this object.
	 * 
	 * @param feature String representation of the Integer value from the minimum perfect hash membership checks.
	 * @return a String representation of the new Integer value assigned to replace the minimum perfect hash Integer value previously used.
	 */
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
	
	/**
	 * Converts a line of libSVM formatted feature:count pairs made up of minimum perfect hash values that
	 * are too large to be handled by libSVM and libLinear arrays and converts them into sequentially assigned
	 * values that are small enough for libSVM and libLinear to handle.
	 * 
	 * @param oldLine single line String libSVM formatted feature:count pairs made up of minimum perfect 
	 * hash values that are too large to be handled by libSVM
	 * @return single line String of sequentially assigned values that are small enough for libSVM and libLinear to handle.
	 */
	public String convert(String oldLine)
	{
		String newLine;
		String pair;
		String feature;
		String count;
		String smallFeature;
		Integer key;
		int delimIndex;
		SortedMap<Integer, String> sortedMap = new TreeMap<Integer, String>();
		
		StringTokenizer tokenizer = new StringTokenizer(oldLine);
		
		//Get the id of this line -- no safety check here, yet
		newLine = tokenizer.nextToken();
		
		while (tokenizer.hasMoreElements())
		{
			pair = tokenizer.nextToken();
			
			delimIndex = pair.indexOf(FEATURE_COUNT_DELIM);
			
			if (delimIndex < 0)
			{
				//TODO turn this into a custom exception that provides pair data, and allows methods above to output errant file and line number as well and continue with job
				System.out.println(pair + " is a malformed pair");
				return null;
			}
			
			feature = pair.substring(0, delimIndex);
			count = pair.substring(delimIndex + 1);
			
			smallFeature = checkFeature(feature);
			
			sortedMap.put(Integer.valueOf(smallFeature), count);
		}
		
		Iterator<Integer> iterator = sortedMap.keySet().iterator();
		
		while (iterator.hasNext())
		{
			key = iterator.next();
			newLine = newLine + PAIRS_DELIM + key + FEATURE_COUNT_DELIM + sortedMap.get(key);
		}
		
		return newLine;
	}
	
	/**
	 *  transform a libSVM sparse format file comprised of minimum perfect hash values
	 * that range too large for libSVM or libLinear to handle into a libSVM sparse format file 
	 * comprised of sequentially encountered values that libSVM and libLinear can handle.  
	 * 
	 * @param largeFile libSVM formatted file containing minimum perfect hash values too large for libLinear to handle
	 * @param smallDirName path and filename to directory where small SVM files will be written
	 * @throws IOException if directory existence is not the issue with writing or reading a file, then IOException is thrown
	 */
	public void writeSmallSVMFile(File largeFile,  String smallDirName) throws IOException
	{
		File smallFile = new File(smallDirName + PATH_DELIM + largeFile.getName());
		BufferedReader largeBufferedReader = null;
		
		String oldLine;
		String newLine = null;
		
		try 
		{
			largeBufferedReader = new BufferedReader( new FileReader(largeFile));
			smallFile.createNewFile();
			PrintWriter smallPrintWriter = new PrintWriter(smallFile);
			
			while ((oldLine = largeBufferedReader.readLine()) != null)
			{
				newLine = convert(oldLine);
				smallPrintWriter.println(newLine);
			}
			
			smallPrintWriter.flush();
		}
		catch (IOException i)
		{
			//Most likely IOException is the director does not exists, this avoids a wasted grunch of IF statements
			//File mkDirFile = new File(parentDirectory + PATH_DELIM + SMALL_SVM_DIR_NAME + PATH_DELIM);
			File mkDirFile = new File(smallDirName);
			mkDirFile.mkdirs();
						
			if (mkDirFile.isDirectory())
			{
				//Close bufferedreader to avoid conflicts on the file (unlikely, but....)
				largeBufferedReader.close();
				writeSmallSVMFile(largeFile, smallFile.getName());
			}
			else
			{
				//Okay, directory was not the issue, throw the IOException now.
				throw new IOException();
			}
		}
	}
	
	/**
	 *  transform a libSVM sparse format file comprised of minimum perfect hash values
	 * that range too large for libSVM or libLinear to handle into a libSVM sparse format file 
	 * comprised of sequentially encountered values that libSVM and libLinear can handle.  
	 * This variant of writeSmallSVMFile uses a preset directory for the small SVM files, namely,
	 * a sibling of the largeSVMFile directory named "smallSVMFiles".  If this directory does not
	 * exist, then it is created by this method.
	 * 
	 * @param largeFile libSVM formatted file containing minimum perfect hash values too large for libLinear to handle
	 * @throws IOException if directory existence is not the issue with writing or reading a file, then IOException is thrown
	 */
	public void writeSmallSVMFile(File largeFile) throws IOException
	{
		String parentDirectory = largeFile.getParentFile().getParent();
		
		String smallDirName = parentDirectory + PATH_DELIM + SMALL_SVM_DIR_NAME + PATH_DELIM  + largeFile.getName();
		
		File smallFile = new File(smallDirName + PATH_DELIM  + largeFile.getName());
		
		BufferedReader largeBufferedReader = null;
		
		String oldLine;
		String newLine = null;
		
		try 
		{
			largeBufferedReader = new BufferedReader( new FileReader(largeFile));
			smallFile.createNewFile();
			PrintWriter smallPrintWriter = new PrintWriter(smallFile);
			
			while ((oldLine = largeBufferedReader.readLine()) != null)
			{
				newLine = convert(oldLine);
				smallPrintWriter.println(newLine);
			}
			
			smallPrintWriter.flush();
		}
		catch (IOException i)
		{
			//Most likely IOException is the director does not exists, this avoids a wasted grunch of IF statements
			File mkDirFile = new File(smallDirName);
			mkDirFile.mkdirs();
			
			if (mkDirFile.isDirectory())
			{
				//Close bufferedreader to avoid conflicts on the file (unlikely, but....)
				largeBufferedReader.close();
				writeSmallSVMFile(largeFile);
			}
			else
			{
				//Okay, directory was not the issue, throw the IOException now.
				throw new IOException();
			}
		}

	}
	/**
	 * This is the 2nd level entry point to transform a libSVM sparse format file comprised of minimum perfect hash values
	 * that range too large for libSVM or libLinear to handle into a libSVM sparse format file comprised of sequentially 
	 * encountered values that libSVM and libLinear can handle.  This version IS recursive.
	 * 
	 * @param largeSVMFile libSVM formatted file containing minimum perfect hash values too large for libLinear to handle
	 * @throws FileNotFoundException If the largeSVMFile is not found, then throw exception
	 * @throws IOException If an IO error other than FileNotFound is encountered, throw exception.  This is most likely a permissions or directory issue.
	 */
	public void processLargeSVMFileRecursive(File largeSVMFile) throws FileNotFoundException, IOException
	{
		if (largeSVMFile.isDirectory())
		{
			processLargeSVMDirectory(largeSVMFile.getAbsolutePath());
		}
		
		writeSmallSVMFile(largeSVMFile);
	}
	
	/**
	 * This is the 2nd level entry point to transform a libSVM sparse format file comprised of minimum perfect hash values
	 * that range too large for libSVM or libLinear to handle into a libSVM sparse format file comprised of sequentially 
	 * encountered values that libSVM and libLinear can handle.  This version IS NOT recursive.
	 * 
	 * @param largeSVMFile libSVM formatted file containing minimum perfect hash values too large for libLinear to handle
	 * @throws FileNotFoundException If the largeSVMFile is not found, then throw exception
	 * @throws IOException If an IO error other than FileNotFound is encountered, throw exception.  This is most likely a permissions or directory issue.
	 */
	public void processLargeSVMFile(File largeSVMFile) throws FileNotFoundException, IOException
	{
		if (largeSVMFile.isFile())
		{
			writeSmallSVMFile(largeSVMFile);
		}
		//else ignore the directory just found and return...

	}
	
	/**
	 * This is the2nd level entry point to transform a libSVM sparse format file comprised of minimum perfect hash values
	 * that range too large for libSVM or libLinear to handle into a libSVM sparse format file comprised of sequentially 
	 * encountered values that libSVM and libLinear can handle.  This version IS NOT recursive.  This version allows
	 * the destination directory for the small SVM files to be designated.
	 * 
	 * @param largeSVMFile libSVM formatted file containing minimum perfect hash values too large for libLinear to handle
	 * @throws FileNotFoundException If the largeSVMFile is not found, then throw exception
	 * @throws IOException If an IO error other than FileNotFound is encountered, throw exception.  This is most likely a permissions or directory issue.
	 */
	public void processLargeSVMFile(File largeSVMFile, String smallFileName) throws FileNotFoundException, IOException
	{
		if (largeSVMFile.isFile())
		{
			writeSmallSVMFile(largeSVMFile, smallFileName);
		}
		//else ignore the directory just found and return...

	}
	
}
