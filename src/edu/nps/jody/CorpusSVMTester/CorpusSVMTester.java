/**
 * 
 */
package edu.nps.jody.CorpusSVMTester;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.nps.LibLinearManager.LibLinearManager;
import edu.nps.jody.GroupAndSlice.GroupAndSlice;
import edu.nps.jody.GroupAndSlice.GroupTypes;
import edu.nps.jody.MergeAndAnalyze.MergeAndAnalyze;

/**
 * @author Jody Grady, Masters student, Naval Postgraduate School
 *
 */
public class CorpusSVMTester 
{
	//Data Members
	public static final String PATH_DELIM = System.getProperty("path.separator");
	
	//Constructors
	
	
	//Methods
	public static void prepareTextForSVM(File parentDirectory, int maxGap,  FeatureTypes featureType, int[] groupSizes, int titleDigits, int nCrossValidation) throws FileNotFoundException, IOException
	{
		TextToSVM textToSVM = new TextToSVM();
		
		textToSVM.processFiles(parentDirectory, maxGap, featureType);
		
		File largeSVMDirectory = new File(parentDirectory, TextToSVM.SVM_DIR_NAME);
		
		SVMToSmallSVM svmToSmallSVM = new SVMToSmallSVM();
		
		svmToSmallSVM.processLargeSVMDirectory(largeSVMDirectory);
		
		File smallSVMDirectory = new File(parentDirectory, SVMToSmallSVM.SMALL_SVM_DIR_NAME);
		
		File sliceDirectory;
		
		for (GroupTypes groupType:GroupTypes.values())
			for (int i = 0; i < groupSizes.length; i++)
			{
				//REMINDER !!!!!!This is NOT the way we'd chain this for a qsub job!!!!!
				GroupAndSlice.groupAndSlicePrep(smallSVMDirectory, groupType, groupSizes[i], titleDigits, nCrossValidation);
				sliceDirectory = new File(smallSVMDirectory, groupType + PATH_DELIM + nCrossValidation);
				LibLinearManager.dummyPredictDirectory(sliceDirectory);//This directory is not correct
				MergeAndAnalyze.makeMergeAndAnalysisFiles(sliceDirectory);//This directory is not correct
			}
	}
	

	
	/**
	 * --parent
	 * --gap
	 *  --featuretype
	 * --groupsize
	 * --titledigits
	 * --slices number of slices in cross validation
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException 
	{
		//If the program is given no parameters or incomplete parameters, these parameters are used
		File 						parentDirectory	= new File(System.getProperty("user.home"));
		int 						maxGap					= 3;
		FeatureTypes 	featureType			= FeatureTypes.ORTHOGONAL_SPARSE_BIGRAM;
		int[] 					groupSizes			= {5, 10, 20};
		int 						titleDigits				= 3;
		int 						nCrossValidation = 5;
		int						groupSizesCount	= 0;
		
		for (int i = 0; i < args.length; i++)
		{
			if(args[i].equalsIgnoreCase("--parent"))
			{
				parentDirectory = new File(args[i + 1]);
				i++;
			}
			else if(args[i].equalsIgnoreCase("--gap"))
			{
				maxGap = Integer.parseInt(args[i + 1]);
				i++;
			}
			else if(args[i].equalsIgnoreCase("--featuretype"))
			{
				featureType = FeatureTypes.valueOf(args[i + 1]);
				i++;
			}
			else if(args[i].equalsIgnoreCase("--groupsizes"))
			{
				i++;
				int j = i;
				while ((j < args.length)&& ( !args[j].contains("--")))
				{
					groupSizesCount++;
					j++;
				}
				
				groupSizes = new int[groupSizesCount];
				
				for (int k = 0; k < groupSizesCount; k++)
				{
					groupSizes[k] = Integer.parseInt(args[i]);
					i++;
				}
			}
			else if(args[i].equalsIgnoreCase("--titledigits"))
			{
				titleDigits = Integer.parseInt(args[i+1]);
				i++;
			}
			else if(args[i].equalsIgnoreCase("--slices"))
			{
				nCrossValidation = Integer.parseInt(args[i+1]);
				i++;
			}
			
			prepareTextForSVM(parentDirectory, maxGap, featureType, groupSizes, titleDigits, nCrossValidation);
		}

	}

}
