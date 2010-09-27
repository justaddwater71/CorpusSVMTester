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

import java.util.Iterator;
import java.util.Vector;

/**
 * Contains an entry point to several feature making methods such
 * as making N-Grams 1 <= N <= 5, Gappy Bigrams, Tagged Gappy Bigrams,
 * and Orthogonal Sparse Bigrams (OSB).  All these major methods
 * take a Vector of Strings (tokens) as an input and return a Vector of
 * Strings (features).
 * @author Jody Grady, CS Masters Student, Naval Postgraduate School
 *
 */
//FIXME need a maxGap safety check.  Cannot have a gap of less than 2
//FIXME not sure my gap calculation matches Dylan's gap calculation for his OSB makers VERIFY!!
public class FeatureMaker 
{
	//Data Members
/*	public static final int ONE_GRAM										= 1;
	public static final int TWO_GRAM										= 2;
	public static final int THREE_GRAM									= 3;
	public static final int FOUR_GRAM									= 4;
	public static final int FIVE_GRAM										= 5;
	public static final int GAPPY_BIGRAM								= 6;
	public static final int GAPPY_BIGRAM_TAGGED 				= 7;
	public static final int ORTHOGONAL_SPARSE_BIGRAM 	= 8;
	public static final int ONE_GRAM_DISTANCE					= 1;
	public static final int TWO_GRAM_DISTANCE					= 2;
	public static final int THREE_GRAM_DISTANCE				= 3;
	public static final int FOUR_GRAM_DISTANCE					= 4;
	public static final int FIVE_GRAM_DISTANCE					= 5;*/
	
	//Constructors
	
	
	//Methods
	/**
	 * Entry point method for parsing a tokenized text Vector into a feature
	 * such as a unigram (ONE_GRAM), bigram (TWO_GRAM), orthogonal
	 * sparse bigram (OSB)(ORTHOGONAL_SPARSE_BIGRAM), etc.  This method
	 * is simply a switch statement that invokes the appropriate feature
	 * method as indicated in the parameter, featureType.
	 * 
	 * @param tokenizedText a Vector of individual text tokens (words)
	 * @param maxGap the maximum "distance" in words that this feature should manipulate (ie unigram, bigram, OSB-3, etc)
	 * @param featureType type of feature to create (ie unigram, bigram, gappy bigram, orthogonal sparse bigram, etc)
	 * @return Vector of feature instances made up of unigrams, bigrams, gappy bigrams, etc.)
	 */
	public static Vector<String> parse(Vector<String> tokenizedText, int maxGap, FeatureTypes featureType)
	{
		Vector<String> featureVector = null;
		switch (featureType)
		{
		case ONE_GRAM:
		{
			featureVector = parseNGram(tokenizedText, featureType.distance());
			break;
		}
		case TWO_GRAM:
		{
			featureVector = parseNGram(tokenizedText, featureType.distance());
			break;
		}
		case THREE_GRAM:
		{
			featureVector = parseNGram(tokenizedText, featureType.distance());
			break;
		}
		case FOUR_GRAM:
		{
			featureVector = parseNGram(tokenizedText, featureType.distance());
			break;
		}
		case FIVE_GRAM:
		{
			featureVector = parseNGram(tokenizedText, featureType.distance());
			break;
		}
		case GAPPY_BIGRAM:
		{
			featureVector = parseGB(tokenizedText, maxGap);
			break;
		}
		case GAPPY_BIGRAM_TAGGED:
		{
			featureVector = parseGBwithTag(tokenizedText, maxGap);
			break;
		}
		case ORTHOGONAL_SPARSE_BIGRAM:
		{
			featureVector = parseOSB(tokenizedText, maxGap);
			break;
		}
		default:
		{
			System.out.print("NOT A VALID OPTION");
		}
		}
		
		return featureVector;
	}
	
	/**
	 * Concatenate a Vector of Strings into a single String with each String from
	 * the Vector separated by a space with no leading or trailing spaces
	 * 
	 * @param sourceVector Vector of Strings to be concatenated
	 * @return concated String constructed from all Strings in sourceVector 
	 *     separated by a space with no leading or trailing spaces
	 */
	private static String catVector(Vector<String>sourceVector)
	{
		Iterator<String> iterator = sourceVector.iterator();
		
		String string = null;
		
		while (iterator.hasNext())
		{
			string = iterator.next() + " ";
		}
		
		return string.trim();
	}
	
	//FIXME test that a 1 gets a unigram and a 2 gets a bigram, etc

	/**
	 * Uses String tokens in tokenizedText (words) to create N-Grams and
	 * then provides all those N-Grams back to the calling method.  A N-Gram
	 * consists of a "sliding window" of N tokens.  For instance the 2-Grams 
	 * (bigrams) of "the quick brown fox" are "the quick", "quick brown",
	 * "brown fox" and the 3-Grams are "the quick brown", "quick brown fox".
	 * 
 	 * @param tokenizedText a Vector of individual text tokens (words)
	 * @param maxGap the maximum "distance" in words that this feature should manipulate (ie unigram, bigram, trigram)
	 * @return Vector of NGrams in the form of Strings
	 */
	private static Vector<String> parseNGram(Vector<String> tokenizedText, int maxGap)
	{
		//Create Vector of NGrams to be returned
		Vector<String> NGramsVector = new Vector<String>();
		Iterator<String> iterator = NGramsVector.iterator();
		
		//Create a "fixed size" sliding window to move over the tokenizedText Vector
		Vector<String> slidingWindow = new Vector<String>();
		
		//Preload the textVector "sliding window"
		for (int i = 0; i < maxGap; i++)
		{
			if (iterator.hasNext())
			{
				slidingWindow.add(iterator.next());
			}
			else
			{
				//Not enough tokens in tokenizedText to make the required size of NGram, return empty vector
				return NGramsVector;
			}
		}
		
		//Initial "sliding window" filled, commit to NGramsVector and then move into processing the rest of tokenizedText
		NGramsVector.add(catVector(slidingWindow));
		
		//Continue moving strings from tokenizedText through the fixed textVector buffer
		while(iterator.hasNext())
		{
			//Pull first word out of "sliding window"
			slidingWindow.remove(0);
			//Put next word in tokenizedText into "sliding window"
			slidingWindow.add(iterator.next());
			//Add current "sliding window"
			NGramsVector.add(catVector(slidingWindow));
		}

		//No more words in tokenizedText, return the NGrams
		return NGramsVector;
	}
	
	//FIXME add reference to GB paper
	/**
	 * Uses String tokens in tokenizedText (words) to create Gappy Bigrams and
	 * then provides all those Gappy Bigrams back to the calling method.  A Gappy Bigram
	 * is a two token combination where the two tokens occur within a given distance. 
	 * For example: "the quick brown fox jumps over the lazy dog" results in Gappy
	 * Bigrams (maxGap 3) of "the quick", "the brown", "the fox", "the jumps", "quick brown",
	 * "quick fox", etc.  There is no numerical value assigned to each Gappy Bigram to track
	 * the actual distance the tokens were apart.
	 * 
 	 * @param tokenizedText a Vector of individual text tokens (words)
	 * @param maxGap the maximum "distance" in words that this feature should manipulate
	 * @return Vector of Gappy Bigrams (distance = maxGap) in the form of Strings
	 */
	private static Vector<String> parseGB(Vector<String> tokenizedText, int maxGap)
	{
		Vector<String> GBVector = new Vector<String>();
		int totalTokens = tokenizedText.size();
		
		//Create loop for word1. Use totalTokens vice words.length to save cycles.
		for (int i=0; i < totalTokens; i++)
		{
			//Create loop for word2.  Going from 1 to (< maxGap + 1) vice traditional 0 to (< maxGap).
			for (int j=1; j < maxGap + 1; j++)
			{
				//Don't run off the end of the words array
				if ((i + j) > totalTokens - 1) 
				{
					break; //Get out of current word2 loop, but continue word1 loop.
				}			
					GBVector.add(tokenizedText.get(i) + " " + tokenizedText.get(i + j));
			}
		}
		
		return GBVector;
	}
	
	//FIXME add reference to GB paper
	/**
	 * Uses String tokens in tokenizedText (words) to create Gappy Bigrams and
	 * then provides all those Gappy Bigrams back to the calling method.  A Gappy Bigram
	 * is a two token combination where the two tokens occur within a given distance. 
	 * For example: "the quick brown fox jumps over the lazy dog" results in Gappy
	 * Bigrams (maxGap 3) of "the quick", "the brown", "the fox", "the jumps", "quick brown",
	 * "quick fox", etc.  The Gappy Bigram with tag includes a number for the max distance 
	 * used to get the Gappy Bigram.  This is done to allow Gappy Bigrams to be run
	 * against models built with Orthogonal Sparse Bigrams, so that additional Gappy
	 * Bigram models do not have to be built (since OSBs by nature contain Gappy
	 * Bigrams at their maximum distance).
	 * 
 	 * @param tokenizedText a Vector of individual text tokens (words)
	 * @param maxGap the maximum "distance" in words that this feature should manipulate
	 * @return Vector of Gappy Bigrams (distance = maxGap) in the form of Strings
	 */
	private static Vector<String> parseGBwithTag(Vector<String> tokenizedText, int maxGap)
	{
		Vector<String> GBVector = new Vector<String>();
		int totalTokens = tokenizedText.size();
		
		//Create loop for word1. Use totalTokens vice words.length to save cycles.
		for (int i=0; i < totalTokens; i++)
		{
			//Create loop for word2.  Going from 1 to (< maxGap + 1) vice traditional 0 to (< maxGap).
			for (int j=1; j < maxGap + 1; j++)
			{
				//Don't run off the end of the words array
				if ((i + j) > totalTokens - 1) 
				{
					break; //Get out of current word2 loop, but continue word1 loop.
				}			
					GBVector.add(tokenizedText.get(i) + " " + tokenizedText.get(i + j) + " " + (maxGap));
			}
		}
		
		return GBVector;
	}
	
	//FIXME add reference to OSB paper
	/**
	* Uses String tokens in tokenizedText (words) to create Orthogonal Sparse Bigrams 
	* (OSB) and then provides all those OSBs back to the calling method.  An OSB
	 * is a two token combination where the two tokens occur within a given distance. 
	 * For example: "the quick brown fox jumps over the lazy dog" results in Gappy
	 * Bigrams (maxGap 3) of "the quick 0", "the quick 1", "the quick 2", "the quick 3",
	 *  "the brown 0", etc.  The OSB includes a number for the greater included distance 
	 * used to get the OSB.
	 * 
	 * @param tokenizedText a Vector of individual text tokens (words)
	 * @param maxGap the maximum "distance" in words that this feature should manipulate
	 * @return Vector of Gappy Bigrams (distance = maxGap) in the form of Strings
	 */
	private static Vector<String> parseOSB(Vector<String> tokenizedText, int maxGap)
	{
		Vector<String> OSBVector = new Vector<String>();
		int totalTokens = tokenizedText.size();
		
		//Create loop for word1. Use totalTokens vice words.length to save cycles.
		for (int i=0; i < totalTokens; i++)
		{
			//Create loop for word2.  Going from 1 to (< maxGap + 1) vice traditional 0 to (< maxGap).
			for (int j=1; j < maxGap + 1; j++)
			{
				//Don't run off the end of the words array
				if ((i + j) > totalTokens - 1) 
				{
					break; //Get out of current word2 loop, but continue word1 loop.
				}
				//k goes from the current gap out to the maxGap
				for (int k=(j - 1); k < maxGap; k++)
				{
					OSBVector.add(tokenizedText.get(i) + " " + tokenizedText.get(i + j)+ " " + k);
				}
			}
		}
		
		return OSBVector;
	}
}
