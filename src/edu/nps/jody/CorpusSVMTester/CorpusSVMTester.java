/**
 * 
 */
package edu.nps.jody.CorpusSVMTester;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.nps.jody.GroupAndSlice.GroupAndSlice;
import edu.nps.jody.MergeAndAnalyze.MergeAndAnalyze;

/**
 * @author Jody Grady, Masters student, Naval Postgraduate School
 *
 */
public class CorpusSVMTester 
{
	//Data Members
	
	
	//Constructors
	
	
	//Methods
	public void prepareTextForSVM(File textDirectory, int maxGap, int groupType, int[] groupSizes, int titleDigits, int nCrossValidation) throws FileNotFoundException, IOException
	{
		File parentDirectory = textDirectory.getParentFile();
		
		TextToSVM textToSVM = new TextToSVM();
		
		textToSVM.processFiles(textDirectory, maxGap, TextToSVM.ORTHOGONAL_SPARSE_BIGRAM);
		
		File largeSVMDirectory = new File(parentDirectory, TextToSVM.SVM_NAME);
		
		SVMToSmallSVM svmToSmallSVM = new SVMToSmallSVM();
		
		svmToSmallSVM.processLargeSVMDirectory(largeSVMDirectory);
		
		File smallSVMDirectory = new File(parentDirectory, SVMToSmallSVM.SMALL_SVM_DIR_NAME);
		
		for (int i = 0; i < groupSizes.length; i++)
		{
			GroupAndSlice.groupAndSlicePrep(smallSVMDirectory, groupType, groupSizes[i], titleDigits, nCrossValidation);
			//Put magical SVM management tool here...
			MergeAndAnalyze.makeMergeAndAnalysisFiles(parentDirectory);
		}
		
		//Put magical SVM management tool here...
		
		
		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
