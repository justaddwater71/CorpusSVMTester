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
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
