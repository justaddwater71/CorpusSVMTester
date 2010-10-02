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
	public static final String FILE_DELIM = System.getProperty("file.separator");
	
	//Constructors
	
	
	//Methods
	public static void prepareTextForSVM(File corpusDirectory, int maxGap, FeatureTypes featureType, int modelNumber,  int[]groupSizes, int titleDigits, int nCrossValidation) throws FileNotFoundException, IOException
	{
		//FIXME get all the dir_name constants coming out of one file to eliminate matching issues after the basic process is proven out
		//Feature type is not its own directory because the model must match the feature, so this is a human being task to match model to feature creation
		TextToSVM textToSVM = new TextToSVM();
		
		File textDirectory 				= new File(corpusDirectory, TextToSVM.TEXT_DIR_NAME);
		
		File featureDirectory		= new File(corpusDirectory, featureType.toString());
		
		File modelDirectory 			= new File(featureDirectory, Integer.toString(modelNumber));
		
		File largeSVMDirectory 	= new File(modelDirectory, TextToSVM.SVM_DIR_NAME);
		
		File cmphDirectory 			= new File(modelDirectory, TextToSVM.CMPH_DIR_NAME);
		
		File keyFile							= new File(cmphDirectory, TextToSVM.KEY_FILE_NAME);
		
		File signatureFile				= new File(cmphDirectory, TextToSVM.SIGNATURE_FILE_NAME);
		
		textToSVM.processFiles(textDirectory, nCrossValidation, featureType, keyFile.getAbsolutePath(), signatureFile.getAbsolutePath(), largeSVMDirectory);
		
		SVMToSmallSVM svmToSmallSVM = new SVMToSmallSVM();
		
		svmToSmallSVM.processLargeSVMDirectory(largeSVMDirectory);
		
		File smallSVMDirectory = new File(modelDirectory, SVMToSmallSVM.SMALL_SVM_DIR_NAME);
		
		File sliceDirectory;
		
		for (GroupTypes groupType:GroupTypes.values())
			for (int i = 0; i < groupSizes.length; i++)
			{
				//REMINDER !!!!!!This is NOT the way we'd chain this for a qsub job!!!!!
				GroupAndSlice.groupAndSlicePrep(smallSVMDirectory, groupType, groupSizes[i], titleDigits, nCrossValidation);
				//sliceDirectory = new File(smallSVMDirectory, groupType.dirName() + FILE_DELIM + groupSizes[i] + FILE_DELIM + nCrossValidation);
				//LibLinearManager.dummyPredictDirectory(sliceDirectory);
				//MergeAndAnalyze.makeMergeAndAnalysisFiles(sliceDirectory);
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
		//user.dir is current working direct -- where the command "java CorpusSVMTester.java" was issued
		File 						corpusDirectory	= new File(System.getProperty("user.dir"));
		int 						maxGap					= 3;
		FeatureTypes 	featureType			= FeatureTypes.ORTHOGONAL_SPARSE_BIGRAM;
		int[] 					groupSizes			= {5, 10, 20, 40, 75, 150};
		int 						titleDigits				= 3;
		int 						nCrossValidation = 5;
		int						modelNumber		= 0;
		
		//Initialize the count for groupSizes
		int						groupSizesCount	= 0;
		
		for (int i = 0; i < args.length; i++)
		{
			if(args[i].equalsIgnoreCase("--parent"))
			{
				corpusDirectory = new File(args[i + 1]);
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
		}
		//prepareTextForSVM(parentDirectory, maxGap, featureType, groupSizes, titleDigits, nCrossValidation);
		prepareTextForSVM(corpusDirectory, maxGap, featureType, modelNumber, groupSizes, titleDigits, nCrossValidation);
	}

}
